# Plano da Odin General API

## Papel

A Odin General API e a API principal da aplicacao de produtividade, mensageria, kanban colaborativo e notificacoes. Ela deve ser consumida pelo mobile e pelo dashboard web.

## Responsabilidades

- Autenticacao e autorizacao com JWT.
- Gerenciamento de usuarios e dispositivos de push.
- CRUD de grupos, membros, quadros, colunas e tasks/cards.
- Validacao de RBAC por grupo: ADMIN, EDITOR, MEMBER e VIEWER.
- Validacao de movimentacao de cards no backend.
- Criacao de tasks pessoais e cards de grupo.
- Agendamento de notification_jobs, usando idempotency_key.
- Persistencia canonica em PostgreSQL.
- Cache/locks/throttle auxiliares em Redis.
- Publicacao de eventos para RabbitMQ e registro em outbox_events.
- Atualizacao em tempo real via WebSocket/STOMP.
- Auditoria basica de movimentacoes e eventos de dominio.

## Tecnologias

- Kotlin 2.4
- Spring Boot 4.1
- Java 21
- PostgreSQL como banco principal
- Redis para cache, locks, throttle e idempotencia temporaria
- RabbitMQ para fila de eventos de notificacao
- Flyway para migrations
- Spring Security com JWT proprio
- Spring Data JPA
- Spring WebSocket/STOMP
- Spring AMQP
- Spring Actuator
- Springdoc OpenAPI

## Camadas

- `auth/identity`: autenticacao, JWT e usuario atual.
- `device`: cadastro e remocao de tokens de dispositivo.
- `group`: grupos, membros e papeis.
- `board`: quadros e colunas kanban.
- `task`: tasks pessoais, cards, responsaveis, movimentos e validadores.
- `notification`: regras, jobs, entregas e agendamento.
- `outbox`: persistencia e publicacao de eventos.
- `websocket`: eventos em tempo real para dashboard e app.
- `common`: filtros, excecoes, respostas e utilitarios web.
- `config`: seguranca, OpenAPI, RabbitMQ, Redis e WebSocket.

## Arquitetura de pastas

```text
backend/General API/
  build.gradle.kts
  settings.gradle.kts
  Dockerfile
  docker-compose.yml
  src/main/kotlin/br/com/odin/general/
    config/
    common/
    identity/
    device/
    group/
    board/
    task/
    notification/
    outbox/
    websocket/
  src/main/resources/
    application.yml
    db/migration/
```

## Classes principais

- `OdinGeneralApiApplication`
- `SecurityConfig`, `RabbitMqConfig`, `WebSocketConfig`, `OpenApiConfig`
- `CorrelationIdFilter`, `GlobalExceptionHandler`
- `AppUser`, `AuthService`, `JwtTokenService`, `JwtAuthenticationFilter`, `AuthController`
- `Device`, `DeviceService`, `DeviceController`
- `OdinGroup`, `GroupMember`, `GroupPermissionService`, `GroupService`, `GroupController`, `GroupMemberController`
- `Board`, `BoardColumn`, `BoardService`, `BoardController`, `BoardColumnController`
- `TaskItem`, `TaskAssignee`, `TaskMovement`, `TaskPermissionValidator`, `TaskService`, `TaskController`
- `NotificationRule`, `NotificationJob`, `NotificationDelivery`, `NotificationSchedulerService`, `NotificationController`
- `OutboxEvent`, `OutboxEventPublisher`
- `RealtimeEventPublisher`

## Requisitos de aceite

- Expor `GET /api/v1/health`.
- Permitir cadastro, login e consulta do usuario autenticado.
- Proteger endpoints de dominio com JWT.
- Registrar dispositivos com push token e provedor.
- Criar grupos com membro ADMIN e board/colunas padrao.
- Validar papel do membro para criar boards, colunas, cards, convites e movimentacoes.
- Permitir task pessoal sem grupo.
- Permitir card de grupo com responsaveis.
- Permitir membro comum mover apenas card atribuido a ele.
- Criar notification_jobs para atribuicoes, prazos e lembretes.
- Registrar eventos em outbox_events e publicar em RabbitMQ quando disponivel.
- Emitir eventos WebSocket para grupos/boards.
- Nao enviar push notification diretamente no ciclo HTTP.
