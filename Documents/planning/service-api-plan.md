# Plano da Odin Service API / Notification Worker

## Papel

A Odin Service API e o servico Go interno responsavel por processamento confiavel de notificacoes e tarefas assincronas. No contexto do produto, ela funciona como `notification-worker`.

## Responsabilidades

- Ler eventos de notificacao por RabbitMQ quando configurado.
- Fazer polling controlado de `notification_jobs` pendentes no PostgreSQL.
- Usar locks e idempotencia no Redis para evitar envio duplicado.
- Aplicar throttle por usuario antes do envio.
- Buscar dispositivos ativos do usuario.
- Enviar notificacoes por provedor configuravel, com base inicial FCM/noop.
- Registrar entregas em `notification_deliveries`.
- Marcar jobs como `sent`, `failed`, `skipped` ou reenfileirar por retry.
- Expor endpoints internos de health, readiness e execucao manual.

## Tecnologias

- Go 1.26
- `net/http` + `chi`
- PostgreSQL via `pgx`
- Redis via `go-redis`
- RabbitMQ via `amqp091-go`
- Logs estruturados com `slog`
- Docker

## Camadas

- `cmd/service-api`: bootstrap do processo.
- `config`: leitura de variaveis de ambiente.
- `http`: rotas internas, middleware e handlers.
- `domain`: modelos de notification job, device e delivery.
- `postgres`: acesso ao banco compartilhado de notificacoes.
- `redislock`: locks, idempotencia e throttle.
- `queue`: consumidor RabbitMQ opcional.
- `notification`: orquestracao de envio.
- `provider`: provedores de push, inicialmente noop e FCM HTTP.
- `worker`: scheduler/polling e retries.
- `observability`: logger e health.

## Arquitetura de pastas

```text
backend/Service API/
  go.mod
  Dockerfile
  cmd/service-api/main.go
  internal/
    application/
    config/
    domain/
    http/
      handlers/
      middleware/
    observability/
    provider/
    queue/
    redislock/
    storage/postgres/
    worker/
  migrations/
```

## Arquivos principais

- `cmd/service-api/main.go`
- `internal/config/config.go`
- `internal/http/router.go`
- `internal/http/server.go`
- `internal/http/handlers/health_handler.go`
- `internal/http/handlers/notification_handler.go`
- `internal/domain/notification.go`
- `internal/application/notification_service.go`
- `internal/storage/postgres/notification_repository.go`
- `internal/redislock/client.go`
- `internal/provider/noop.go`
- `internal/provider/fcm.go`
- `internal/queue/rabbitmq.go`
- `internal/worker/scheduler.go`

## Requisitos de aceite

- Expor `GET /internal/v1/health` e `GET /internal/v1/ready`.
- Proteger endpoints internos com token de servico.
- Processar jobs pendentes vencidos.
- Respeitar idempotency_key.
- Aplicar throttle por usuario.
- Registrar entregas por dispositivo.
- Reenfileirar ou falhar jobs conforme `attempts` e `max_attempts`.
- Funcionar em modo `noop` sem credenciais FCM.
- Nao conter regras complexas de permissao de kanban; isso pertence a General API.
