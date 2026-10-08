package redislock

import (
	"context"
	"time"

	"github.com/google/uuid"
	"github.com/redis/go-redis/v9"
)

type Client struct {
	redis *redis.Client
}

func New(addr, password string, db int) *Client {
	return &Client{
		redis: redis.NewClient(&redis.Options{
			Addr:     addr,
			Password: password,
			DB:       db,
		}),
	}
}

func (c *Client) Close() error {
	return c.redis.Close()
}

func (c *Client) Ping(ctx context.Context) error {
	return c.redis.Ping(ctx).Err()
}

func (c *Client) AcquireJobLock(ctx context.Context, jobID uuid.UUID, ttl time.Duration) (bool, error) {
	return c.redis.SetNX(ctx, "notification:lock:job:"+jobID.String(), "1", ttl).Result()
}

func (c *Client) MarkIdempotent(ctx context.Context, key string, ttl time.Duration) (bool, error) {
	return c.redis.SetNX(ctx, "notification:idempotency:"+key, "1", ttl).Result()
}

func (c *Client) AllowUserThrottle(ctx context.Context, userID uuid.UUID, window time.Duration) (bool, error) {
	return c.redis.SetNX(ctx, "notification:throttle:user:"+userID.String(), "1", window).Result()
}
