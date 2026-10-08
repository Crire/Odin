# Odin General API

API principal do Odin, construída em Kotlin com Spring Boot. Ela é o ponto de entrada para mobile/dashboard e centraliza autenticação, usuários, dispositivos, grupos, kanban, tarefas pessoais, regras de permissão, notificações agendadas, outbox e eventos em tempo real.

## Stack

- Kotlin 2.4
- Spring Boot 4.1
- Java 21
- PostgreSQL
- Redis
- Flyway
- Spring Security
- Spring Actuator
- Springdoc OpenAPI
- Spring WebSocket/STOMP
- Spring AMQP/RabbitMQ

## Responsabilidades

- Expor a API pública versionada em `/api/v1`.
- Autenticar usuários por JWT.
- Persistir usuários, dispositivos, grupos, membros, boards, colunas, tasks/cards e notificações.
- Validar RBAC por grupo no backend.
- Criar `notification_jobs` e `outbox_events`.
- Publicar eventos em RabbitMQ quando disponível.
- Emitir eventos WebSocket/STOMP para dashboard e mobile.
- Usar Redis como cache/lock/throttle/idempotência auxiliar.
- Não enviar push notification diretamente durante requisições HTTP comuns.

## Executando localmente

Suba as dependências:

```bash
docker compose up -d
```

Execute a aplicação:

```bash
gradle bootRun
```

Endpoints úteis:

- `GET /api/v1/health`
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `GET /api/v1/auth/me`
- `GET /api/v1/users/me`
- `POST /api/v1/devices`
- `POST /api/v1/groups`
- `POST /api/v1/groups/{groupId}/members/invite`
- `POST /api/v1/groups/{groupId}/boards`
- `POST /api/v1/boards/{boardId}/tasks`
- `PATCH /api/v1/tasks/{taskId}/move`
- `PATCH /api/v1/tasks/{taskId}/complete`
- `GET /api/v1/notifications`
- `GET /swagger-ui.html`
- `GET /ws`
