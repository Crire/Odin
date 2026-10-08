package httpapi

import (
	"net/http"

	"odin/service-api/internal/application"
	"odin/service-api/internal/config"
	"odin/service-api/internal/http/handlers"
	"odin/service-api/internal/http/middleware"
	"odin/service-api/internal/redislock"
	"odin/service-api/internal/storage/postgres"

	"github.com/go-chi/chi/v5"
)

func NewRouter(
	cfg config.Config,
	repository *postgres.NotificationRepository,
	redis *redislock.Client,
	service *application.NotificationService,
) http.Handler {
	router := chi.NewRouter()
	router.Use(middleware.CorrelationID)

	healthHandler := handlers.NewHealthHandler(repository, redis)
	notificationHandler := handlers.NewNotificationHandler(service, cfg.WorkerBatchSize)

	router.Get("/internal/v1/health", healthHandler.Health)
	router.Get("/internal/v1/ready", healthHandler.Ready)

	router.Group(func(protected chi.Router) {
		protected.Use(middleware.ServiceToken(cfg.ServiceToken))
		protected.Post("/internal/v1/notification-jobs/process", notificationHandler.ProcessDue)
		protected.Post("/internal/v1/notification-jobs/{jobId}/retry", notificationHandler.Retry)
	})

	return router
}
