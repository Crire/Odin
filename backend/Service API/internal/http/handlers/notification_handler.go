package handlers

import (
	"encoding/json"
	"net/http"

	"odin/service-api/internal/application"

	"github.com/go-chi/chi/v5"
	"github.com/google/uuid"
)

type NotificationHandler struct {
	service   *application.NotificationService
	batchSize int
}

func NewNotificationHandler(service *application.NotificationService, batchSize int) *NotificationHandler {
	return &NotificationHandler{service: service, batchSize: batchSize}
}

func (h *NotificationHandler) ProcessDue(w http.ResponseWriter, r *http.Request) {
	processed, err := h.service.ProcessDue(r.Context(), h.batchSize)
	if err != nil {
		writeJSON(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}
	writeJSON(w, http.StatusAccepted, map[string]any{"processed": processed})
}

func (h *NotificationHandler) Retry(w http.ResponseWriter, r *http.Request) {
	id, err := uuid.Parse(chi.URLParam(r, "jobId"))
	if err != nil {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "invalid job id"})
		return
	}
	if err := h.service.RetryJob(r.Context(), id); err != nil {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": err.Error()})
		return
	}
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusAccepted)
	_ = json.NewEncoder(w).Encode(map[string]string{"status": "queued"})
}
