package config

import (
	"os"
	"strconv"
	"time"
)

type Config struct {
	HTTPAddr             string
	ServiceToken         string
	DatabaseURL          string
	RedisAddr            string
	RedisPassword        string
	RedisDB              int
	RabbitMQURL          string
	RabbitMQQueue        string
	WorkerEnabled        bool
	WorkerInterval       time.Duration
	WorkerBatchSize      int
	NotificationProvider string
	FCMServerKey         string
	ThrottleWindow       time.Duration
	IdempotencyTTL       time.Duration
}

func Load() Config {
	return Config{
		HTTPAddr:             get("HTTP_ADDR", ":8081"),
		ServiceToken:         get("SERVICE_TOKEN", "local-service-token"),
		DatabaseURL:          get("DATABASE_URL", "postgres://odin:odin@localhost:5432/odin_general?sslmode=disable"),
		RedisAddr:            get("REDIS_ADDR", "localhost:6379"),
		RedisPassword:        get("REDIS_PASSWORD", ""),
		RedisDB:              getInt("REDIS_DB", 0),
		RabbitMQURL:          get("RABBITMQ_URL", ""),
		RabbitMQQueue:        get("RABBITMQ_QUEUE", "notification.jobs"),
		WorkerEnabled:        getBool("WORKER_ENABLED", true),
		WorkerInterval:       getDuration("WORKER_INTERVAL", 10*time.Second),
		WorkerBatchSize:      getInt("WORKER_BATCH_SIZE", 25),
		NotificationProvider: get("NOTIFICATION_PROVIDER", "noop"),
		FCMServerKey:         get("FCM_SERVER_KEY", ""),
		ThrottleWindow:       getDuration("THROTTLE_WINDOW", 15*time.Minute),
		IdempotencyTTL:       getDuration("IDEMPOTENCY_TTL", 24*time.Hour),
	}
}

func get(key, fallback string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return fallback
}

func getInt(key string, fallback int) int {
	value, err := strconv.Atoi(get(key, ""))
	if err != nil {
		return fallback
	}
	return value
}

func getBool(key string, fallback bool) bool {
	value := get(key, "")
	if value == "" {
		return fallback
	}
	parsed, err := strconv.ParseBool(value)
	if err != nil {
		return fallback
	}
	return parsed
}

func getDuration(key string, fallback time.Duration) time.Duration {
	value := get(key, "")
	if value == "" {
		return fallback
	}
	parsed, err := time.ParseDuration(value)
	if err != nil {
		return fallback
	}
	return parsed
}
