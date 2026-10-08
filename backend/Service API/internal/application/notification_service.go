package application

import (
	"context"
	"log/slog"
	"time"

	"odin/service-api/internal/domain"
	"odin/service-api/internal/provider"
	"odin/service-api/internal/redislock"
	"odin/service-api/internal/storage/postgres"

	"github.com/google/uuid"
)

type NotificationService struct {
	repository     *postgres.NotificationRepository
	redis          *redislock.Client
	sender         provider.Sender
	logger         *slog.Logger
	throttleWindow time.Duration
	idempotencyTTL time.Duration
}

func NewNotificationService(
	repository *postgres.NotificationRepository,
	redis *redislock.Client,
	sender provider.Sender,
	logger *slog.Logger,
	throttleWindow time.Duration,
	idempotencyTTL time.Duration,
) *NotificationService {
	return &NotificationService{
		repository:     repository,
		redis:          redis,
		sender:         sender,
		logger:         logger,
		throttleWindow: throttleWindow,
		idempotencyTTL: idempotencyTTL,
	}
}

func (s *NotificationService) ProcessDue(ctx context.Context, limit int) (int, error) {
	jobs, err := s.repository.ClaimDueJobs(ctx, limit)
	if err != nil {
		return 0, err
	}
	for _, job := range jobs {
		if err := s.ProcessJob(ctx, job); err != nil {
			s.logger.ErrorContext(ctx, "failed to process notification job", "job_id", job.ID.String(), "error", err)
		}
	}
	return len(jobs), nil
}

func (s *NotificationService) ProcessJob(ctx context.Context, job domain.NotificationJob) error {
	locked, err := s.redis.AcquireJobLock(ctx, job.ID, 2*time.Minute)
	if err != nil {
		return err
	}
	if !locked {
		return nil
	}

	firstTime, err := s.redis.MarkIdempotent(ctx, job.IdempotencyKey, s.idempotencyTTL)
	if err != nil {
		return err
	}
	if !firstTime {
		s.logger.InfoContext(ctx, "notification skipped by idempotency", "job_id", job.ID.String())
		return s.repository.MarkSkipped(ctx, job.ID)
	}

	actionable, err := s.repository.TaskStillActionable(ctx, job)
	if err != nil {
		return err
	}
	if !actionable {
		s.logger.InfoContext(ctx, "notification skipped because task is not actionable", "job_id", job.ID.String())
		return s.repository.MarkSkipped(ctx, job.ID)
	}

	allowed, err := s.repository.NotificationsAllowedNow(ctx, job, time.Now().UTC())
	if err != nil {
		return err
	}
	if !allowed {
		s.logger.InfoContext(ctx, "notification rescheduled outside user window", "job_id", job.ID.String())
		return s.repository.Reschedule(ctx, job.ID, time.Now().UTC().Add(15*time.Minute))
	}

	throttleAllowed, err := s.redis.AllowUserThrottle(ctx, job.UserID, s.throttleWindow)
	if err != nil {
		return err
	}
	if !throttleAllowed {
		s.logger.InfoContext(ctx, "notification rescheduled by throttle", "job_id", job.ID.String(), "user_id", job.UserID.String())
		return s.repository.Reschedule(ctx, job.ID, time.Now().UTC().Add(s.throttleWindow))
	}

	devices, err := s.repository.ActiveDevices(ctx, job.UserID)
	if err != nil {
		return err
	}
	if len(devices) == 0 {
		s.logger.InfoContext(ctx, "notification skipped without active devices", "job_id", job.ID.String())
		return s.repository.MarkSkipped(ctx, job.ID)
	}

	successes := 0
	for _, device := range devices {
		sentAt := time.Now().UTC()
		response, sendErr := s.sender.Send(ctx, device, job)
		delivery := domain.Delivery{
			JobID:            job.ID,
			DeviceID:         device.ID,
			Provider:         device.Provider,
			ProviderResponse: response,
			SentAt:           &sentAt,
		}
		if sendErr != nil {
			delivery.Status = "FAILED"
			delivery.ErrorMessage = sendErr.Error()
			s.logger.WarnContext(ctx, "notification delivery failed", "job_id", job.ID.String(), "device_id", device.ID.String(), "error", sendErr)
		} else {
			delivery.Status = "SENT"
			successes++
		}
		if err := s.repository.InsertDelivery(ctx, delivery); err != nil {
			return err
		}
	}

	if successes > 0 {
		return s.repository.MarkSent(ctx, job.ID)
	}
	return s.repository.MarkFailedOrRetry(ctx, job, "all deliveries failed")
}

func (s *NotificationService) RetryJob(ctx context.Context, jobID uuid.UUID) error {
	return s.repository.RetryJob(ctx, jobID)
}
