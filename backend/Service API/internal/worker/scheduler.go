package worker

import (
	"context"
	"log/slog"
	"time"

	"odin/service-api/internal/application"
)

type Scheduler struct {
	service  *application.NotificationService
	logger   *slog.Logger
	interval time.Duration
	batch    int
}

func NewScheduler(service *application.NotificationService, logger *slog.Logger, interval time.Duration, batch int) *Scheduler {
	return &Scheduler{service: service, logger: logger, interval: interval, batch: batch}
}

func (s *Scheduler) Start(ctx context.Context) {
	ticker := time.NewTicker(s.interval)
	defer ticker.Stop()

	s.runOnce(ctx)
	for {
		select {
		case <-ctx.Done():
			s.logger.InfoContext(ctx, "notification scheduler stopped")
			return
		case <-ticker.C:
			s.runOnce(ctx)
		}
	}
}

func (s *Scheduler) runOnce(ctx context.Context) {
	processed, err := s.service.ProcessDue(ctx, s.batch)
	if err != nil {
		s.logger.ErrorContext(ctx, "notification scheduler failed", "error", err)
		return
	}
	if processed > 0 {
		s.logger.InfoContext(ctx, "notification scheduler processed jobs", "count", processed)
	}
}
