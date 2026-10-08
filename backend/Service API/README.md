# Odin Service API

Servico interno em Go para processamento confiavel de notificacoes do Odin.

## Papel

Este servico funciona como `notification-worker`: ele busca `notification_jobs` vencidos no PostgreSQL, aplica locks/idempotencia/throttle no Redis, envia notificacoes pelo provedor configurado e registra o resultado em `notification_deliveries`.

## Stack

- Go 1.26
- net/http + chi
- PostgreSQL via pgx
- Redis via go-redis
- RabbitMQ opcional via amqp091-go
- slog para logs estruturados
- Provedor `noop` por padrao, FCM legado opcional

## Endpoints internos

- `GET /internal/v1/health`
- `GET /internal/v1/ready`
- `POST /internal/v1/notification-jobs/process`
- `POST /internal/v1/notification-jobs/{id}/retry`

Endpoints de acao exigem `X-Service-Token` ou `Authorization: Bearer`.
