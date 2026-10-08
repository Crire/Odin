package provider

import (
	"context"
	"log/slog"

	"odin/service-api/internal/domain"
)

type NoopSender struct {
	logger *slog.Logger
}

func NewNoopSender(logger *slog.Logger) *NoopSender {
	return &NoopSender{logger: logger}
}

func (s *NoopSender) Send(ctx context.Context, device domain.Device, job domain.NotificationJob) (string, error) {
	s.logger.InfoContext(ctx, "noop notification sent",
		"job_id", job.ID.String(),
		"device_id", device.ID.String(),
		"user_id", job.UserID.String(),
	)
	return "noop", nil
}
