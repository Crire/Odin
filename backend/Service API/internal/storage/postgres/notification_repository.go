package postgres

import (
	"context"
	"database/sql"
	"errors"
	"time"

	"odin/service-api/internal/domain"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
)

type NotificationRepository struct {
	pool *pgxpool.Pool
}

func NewNotificationRepository(ctx context.Context, databaseURL string) (*NotificationRepository, error) {
	pool, err := pgxpool.New(ctx, databaseURL)
	if err != nil {
		return nil, err
	}
	return &NotificationRepository{pool: pool}, nil
}

func (r *NotificationRepository) Close() {
	r.pool.Close()
}

func (r *NotificationRepository) Ping(ctx context.Context) error {
	return r.pool.Ping(ctx)
}

func (r *NotificationRepository) ClaimDueJobs(ctx context.Context, limit int) ([]domain.NotificationJob, error) {
	rows, err := r.pool.Query(ctx, `
		WITH due AS (
			SELECT id
			FROM notification_jobs
			WHERE status = 'PENDING'
			  AND scheduled_at <= NOW()
			ORDER BY scheduled_at ASC
			LIMIT $1
			FOR UPDATE SKIP LOCKED
		)
		UPDATE notification_jobs nj
		SET status = 'PROCESSING',
		    attempts = attempts + 1,
		    updated_at = NOW()
		FROM due
		WHERE nj.id = due.id
		RETURNING nj.id::text, nj.user_id::text, nj.task_id::text, nj.group_id::text,
		          nj.type, nj.title, nj.message, nj.scheduled_at, nj.status,
		          nj.attempts, nj.max_attempts, nj.idempotency_key
	`, limit)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var jobs []domain.NotificationJob
	for rows.Next() {
		job, err := scanJob(rows)
		if err != nil {
			return nil, err
		}
		jobs = append(jobs, job)
	}
	return jobs, rows.Err()
}

func (r *NotificationRepository) ActiveDevices(ctx context.Context, userID uuid.UUID) ([]domain.Device, error) {
	rows, err := r.pool.Query(ctx, `
		SELECT id::text, user_id::text, platform, push_token, provider
		FROM devices
		WHERE user_id = $1 AND active = TRUE
		ORDER BY last_seen_at DESC
	`, userID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var devices []domain.Device
	for rows.Next() {
		var id, uid string
		var device domain.Device
		if err := rows.Scan(&id, &uid, &device.Platform, &device.PushToken, &device.Provider); err != nil {
			return nil, err
		}
		parsedID, err := uuid.Parse(id)
		if err != nil {
			return nil, err
		}
		parsedUserID, err := uuid.Parse(uid)
		if err != nil {
			return nil, err
		}
		device.ID = parsedID
		device.UserID = parsedUserID
		devices = append(devices, device)
	}
	return devices, rows.Err()
}

func (r *NotificationRepository) TaskStillActionable(ctx context.Context, job domain.NotificationJob) (bool, error) {
	if job.TaskID == nil {
		return true, nil
	}
	var ok bool
	err := r.pool.QueryRow(ctx, `
		SELECT EXISTS (
			SELECT 1
			FROM tasks t
			WHERE t.id = $1
			  AND t.status NOT IN ('DONE', 'CANCELLED')
			  AND (
				t.created_by = $2
				OR EXISTS (
					SELECT 1 FROM task_assignees ta
					WHERE ta.task_id = t.id AND ta.user_id = $2
				)
			  )
		)
	`, *job.TaskID, job.UserID).Scan(&ok)
	return ok, err
}

func (r *NotificationRepository) NotificationsAllowedNow(ctx context.Context, job domain.NotificationJob, now time.Time) (bool, error) {
	var hasRules bool
	if err := r.pool.QueryRow(ctx, `SELECT EXISTS (SELECT 1 FROM notification_rules WHERE user_id = $1 AND enabled = TRUE)`, job.UserID).Scan(&hasRules); err != nil {
		return false, err
	}
	if !hasRules {
		return true, nil
	}
	day := dayKey(now)
	currentTime := now.Format("15:04:05")
	var allowed bool
	err := r.pool.QueryRow(ctx, `
		SELECT EXISTS (
			SELECT 1
			FROM notification_rules
			WHERE user_id = $1
			  AND enabled = TRUE
			  AND start_time <= $2::time
			  AND end_time >= $2::time
			  AND days_of_week ILIKE '%' || $3 || '%'
		)
	`, job.UserID, currentTime, day).Scan(&allowed)
	return allowed, err
}

func (r *NotificationRepository) InsertDelivery(ctx context.Context, delivery domain.Delivery) error {
	_, err := r.pool.Exec(ctx, `
		INSERT INTO notification_deliveries (
			id, notification_job_id, device_id, provider, status,
			provider_response, sent_at, error_message, created_at
		)
		VALUES (gen_random_uuid(), $1, $2, $3, $4, $5, $6, $7, NOW())
	`, delivery.JobID, delivery.DeviceID, delivery.Provider, delivery.Status, nullString(delivery.ProviderResponse), delivery.SentAt, nullString(delivery.ErrorMessage))
	return err
}

func (r *NotificationRepository) MarkSent(ctx context.Context, jobID uuid.UUID) error {
	_, err := r.pool.Exec(ctx, `UPDATE notification_jobs SET status = 'SENT', updated_at = NOW() WHERE id = $1`, jobID)
	return err
}

func (r *NotificationRepository) MarkSkipped(ctx context.Context, jobID uuid.UUID) error {
	_, err := r.pool.Exec(ctx, `UPDATE notification_jobs SET status = 'SKIPPED', updated_at = NOW() WHERE id = $1`, jobID)
	return err
}

func (r *NotificationRepository) MarkFailedOrRetry(ctx context.Context, job domain.NotificationJob, reason string) error {
	if job.Attempts >= job.MaxAttempts {
		_, err := r.pool.Exec(ctx, `UPDATE notification_jobs SET status = 'FAILED', updated_at = NOW() WHERE id = $1`, job.ID)
		return err
	}
	_, err := r.pool.Exec(ctx, `
		UPDATE notification_jobs
		SET status = 'PENDING',
		    scheduled_at = NOW() + ($2::int * INTERVAL '1 minute'),
		    updated_at = NOW()
		WHERE id = $1
	`, job.ID, retryDelayMinutes(job.Attempts))
	return err
}

func (r *NotificationRepository) Reschedule(ctx context.Context, jobID uuid.UUID, at time.Time) error {
	_, err := r.pool.Exec(ctx, `UPDATE notification_jobs SET status = 'PENDING', scheduled_at = $2, updated_at = NOW() WHERE id = $1`, jobID, at)
	return err
}

func (r *NotificationRepository) RetryJob(ctx context.Context, jobID uuid.UUID) error {
	tag, err := r.pool.Exec(ctx, `
		UPDATE notification_jobs
		SET status = 'PENDING', attempts = 0, scheduled_at = NOW(), updated_at = NOW()
		WHERE id = $1 AND status IN ('FAILED', 'SKIPPED', 'CANCELLED')
	`, jobID)
	if err != nil {
		return err
	}
	if tag.RowsAffected() == 0 {
		return errors.New("job not found or not retryable")
	}
	return nil
}

type rowScanner interface {
	Scan(dest ...any) error
}

func scanJob(row rowScanner) (domain.NotificationJob, error) {
	var job domain.NotificationJob
	var id, userID string
	var taskID, groupID sql.NullString
	var status string
	if err := row.Scan(
		&id,
		&userID,
		&taskID,
		&groupID,
		&job.Type,
		&job.Title,
		&job.Message,
		&job.ScheduledAt,
		&status,
		&job.Attempts,
		&job.MaxAttempts,
		&job.IdempotencyKey,
	); err != nil {
		return job, err
	}
	parsedID, err := uuid.Parse(id)
	if err != nil {
		return job, err
	}
	parsedUserID, err := uuid.Parse(userID)
	if err != nil {
		return job, err
	}
	job.ID = parsedID
	job.UserID = parsedUserID
	job.TaskID = parseNullableUUID(taskID)
	job.GroupID = parseNullableUUID(groupID)
	job.Status = domain.NotificationStatus(status)
	return job, nil
}

func parseNullableUUID(value sql.NullString) *uuid.UUID {
	if !value.Valid || value.String == "" {
		return nil
	}
	parsed, err := uuid.Parse(value.String)
	if err != nil {
		return nil
	}
	return &parsed
}

func nullString(value string) sql.NullString {
	if value == "" {
		return sql.NullString{}
	}
	return sql.NullString{String: value, Valid: true}
}

func retryDelayMinutes(attempts int) int {
	if attempts <= 1 {
		return 1
	}
	if attempts == 2 {
		return 5
	}
	return 15
}

func dayKey(t time.Time) string {
	switch t.Weekday() {
	case time.Monday:
		return "MON"
	case time.Tuesday:
		return "TUE"
	case time.Wednesday:
		return "WED"
	case time.Thursday:
		return "THU"
	case time.Friday:
		return "FRI"
	case time.Saturday:
		return "SAT"
	default:
		return "SUN"
	}
}
