package main

import (
	"context"
	"fmt"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"voice-ai-prototype-backend/internal/config"
	"voice-ai-prototype-backend/internal/llm"
	"voice-ai-prototype-backend/internal/logging"
	"voice-ai-prototype-backend/internal/stt"
	"voice-ai-prototype-backend/internal/voice"
)

func main() {
	cfg, err := config.Load()
	if err != nil {
		fmt.Fprintf(os.Stderr, "failed to load config: %v\n", err)
		os.Exit(1)
	}

	logger := logging.New(cfg.LogLevel)
	gemini := llm.NewGeminiProvider(cfg.Gemini, logger)
	sttFactory := func(events stt.Events) stt.Provider {
		return stt.NewDeepgramStreamingProvider(cfg.Deepgram, logger, events)
	}

	mux := http.NewServeMux()
	voice.RegisterHandlers(mux, cfg, logger, gemini, sttFactory)

	server := &http.Server{
		Addr:              fmt.Sprintf(":%d", cfg.Port),
		Handler:           mux,
		ReadHeaderTimeout: 5 * time.Second,
	}

	go func() {
		logger.Info("HTTP server running on http://localhost:%d", cfg.Port)
		logger.Info("Voice WebSocket available at ws://localhost:%d%s", cfg.Port, cfg.WSPath)
		if err := server.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			logger.Error("server failed: %v", err)
			os.Exit(1)
		}
	}()

	stop := make(chan os.Signal, 1)
	signal.Notify(stop, syscall.SIGINT, syscall.SIGTERM)
	<-stop

	logger.Info("Shutting down backend")
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	if err := server.Shutdown(ctx); err != nil {
		logger.Error("server shutdown failed: %v", err)
	}
}
