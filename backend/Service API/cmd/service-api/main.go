package main

import (
	"context"
	"errors"
	"log/slog"
	"net/http"
	"os"
	"os/signal"
	"strings"
	"syscall"
	"time"

	"odin/service-api/internal/application"
	"odin/service-api/internal/config"
	httpapi "odin/service-api/internal/http"
	"odin/service-api/internal/observability"
	"odin/service-api/internal/provider"
	"odin/service-api/internal/queue"
	"odin/service-api/internal/redislock"
	"odin/service-api/internal/storage/postgres"
	"odin/service-api/internal/worker"
)

func main() {
	logger := observability.NewLogger()
	cfg := config.Load()
	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()

	repository, err := postgres.NewNotificationRepository(ctx, cfg.DatabaseURL)
	if err != nil {
		logger.ErrorContext(ctx, "failed to connect postgres", "error", err)
		os.Exit(1)
	}
	defer repository.Close()

	redisClient := redislock.New(cfg.RedisAddr, cfg.RedisPassword, cfg.RedisDB)
	defer func() {
		if err := redisClient.Close(); err != nil {
			logger.WarnContext(ctx, "failed to close redis", "error", err)
		}
	}()

	sender := buildSender(cfg, logger)
	notificationService := application.NewNotificationService(
		repository,
		redisClient,
		sender,
		logger,
		cfg.ThrottleWindow,
		cfg.IdempotencyTTL,
	)

	if cfg.WorkerEnabled {
		scheduler := worker.NewScheduler(notificationService, logger, cfg.WorkerInterval, cfg.WorkerBatchSize)
		go scheduler.Start(ctx)
	}

	rabbitConsumer := queue.NewRabbitConsumer(cfg.RabbitMQURL, cfg.RabbitMQQueue, notificationService, logger, cfg.WorkerBatchSize)
	go rabbitConsumer.Start(ctx)

	router := httpapi.NewRouter(cfg, repository, redisClient, notificationService)
	server := httpapi.NewServer(cfg.HTTPAddr, router)

	go func() {
		logger.InfoContext(ctx, "odin service api listening", "addr", cfg.HTTPAddr)
		if err := server.ListenAndServe(); err != nil && !errors.Is(err, http.ErrServerClosed) {
			logger.ErrorContext(ctx, "http server failed", "error", err)
			stop()
		}
	}()

	<-ctx.Done()
	shutdownCtx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	if err := httpapi.Shutdown(shutdownCtx, server); err != nil {
		logger.Error("http shutdown failed", "error", err)
	}
}

func buildSender(cfg config.Config, logger *slog.Logger) provider.Sender {
	switch strings.ToLower(cfg.NotificationProvider) {
	case "fcm":
		return provider.NewFCMSender(cfg.FCMServerKey)
	default:
		return provider.NewNoopSender(logger)
	}
}
