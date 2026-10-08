package handlers

import (
	"encoding/json"
	"net/http"
	"time"

	"odin/service-api/internal/redislock"
	"odin/service-api/internal/storage/postgres"
)

type HealthHandler struct {
	repository *postgres.NotificationRepository
	redis      *redislock.Client
}

func NewHealthHandler(repository *postgres.NotificationRepository, redis *redislock.Client) *HealthHandler {
	return &HealthHandler{repository: repository, redis: redis}
}

func (h *HealthHandler) Health(w http.ResponseWriter, r *http.Request) {
	writeJSON(w, http.StatusOK, map[string]any{
		"status":    "UP",
		"service":   "odin-service-api",
		"timestamp": time.Now().UTC(),
	})
}

func (h *HealthHandler) Ready(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()
	if err := h.repository.Ping(ctx); err != nil {
		writeJSON(w, http.StatusServiceUnavailable, map[string]string{"status": "DOWN", "dependency": "postgres", "error": err.Error()})
		return
	}
	if err := h.redis.Ping(ctx); err != nil {
		writeJSON(w, http.StatusServiceUnavailable, map[string]string{"status": "DOWN", "dependency": "redis", "error": err.Error()})
		return
	}
	writeJSON(w, http.StatusOK, map[string]string{"status": "READY"})
}

func writeJSON(w http.ResponseWriter, status int, payload any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(payload)
}
