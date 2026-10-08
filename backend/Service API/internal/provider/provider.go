package provider

import (
	"context"

	"odin/service-api/internal/domain"
)

type Sender interface {
	Send(ctx context.Context, device domain.Device, job domain.NotificationJob) (string, error)
}
