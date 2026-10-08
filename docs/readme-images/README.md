# Imagens do README

| Arquivo | Tipo | O que representa |
| --- | --- | --- |
| `arquitetura.svg` | Diagrama vetorial criado em SVG | General API, banco compartilhado, worker, Redis e gatilho RabbitMQ opcional |
| `fluxo-notificacoes.svg` | Diagrama vetorial criado em SVG | Agendamento, claim, controles, envio e registro do resultado |
| `fluxo-voz.svg` | Diagrama vetorial criado em SVG | Captura Android, backend Go, Deepgram, Gemini e TextToSpeech |
| `exemplos-de-uso.png` | Ideação raster gerada com a ferramenta integrada `image_gen` | Um possível cliente kanban e uma interface ilustrativa de conversa por voz |

Os diagramas foram elaborados a partir dos serviços, configurações e protocolos presentes no repositório. Os SVGs contêm títulos e descrições acessíveis e podem ser editados diretamente, sem ferramentas externas de geração de imagens.

A ilustração PNG não é uma captura real. Todos os dados são fictícios; os dois painéis representam usos separados. Ela não implica a existência de um frontend kanban nesta pasta nem integração do protótipo de voz com as APIs de tarefas.

## Referências do código

- [TaskService](../../backend/General%20API/src/main/kotlin/br/com/odin/general/task/service/TaskService.kt)
- [Agendamento de notificações](../../backend/General%20API/src/main/kotlin/br/com/odin/general/notification/service/NotificationSchedulingService.kt)
- [Publicação de outbox](../../backend/General%20API/src/main/kotlin/br/com/odin/general/outbox/service/OutboxEventPublisher.kt)
- [Processamento no worker](../../backend/Service%20API/internal/application/notification_service.go)
- [Gatilho RabbitMQ](../../backend/Service%20API/internal/queue/rabbitmq.go)
- [Sessão de voz](../../Odin%20conversational%20prototype/voice-ai-prototype/backend/internal/voice/session.go)
- [Tela Android](../../Odin%20conversational%20prototype/voice-ai-prototype/android-app/app/src/main/java/com/example/voiceai/ui/VoiceChatScreen.kt)

## Prompt da ideação

O prompt abaixo foi usado na ferramenta integrada, sem recorrer ao modo CLI/API:

```text
Use case: ui-mockup. Asset type: illustration for Portuguese Odin project README. Create a polished wide 3:2 editorial illustration with two clearly separate panels on a pale gray background, crisp blue and teal accents, legible Portuguese typography. Left panel: an imagined productivity client, NOT an existing app screenshot: a kanban board titled "Projeto Odin" with columns "A fazer", "Em andamento", "Concluído", sample cards "Planejar a semana", "Revisar proposta", "Preparar reunião", and a small notification card "Lembrete de tarefa". Right panel: Android voice conversation concept derived from a simple voice app, phone showing title "Voice AI Prototype", user bubble "Como organizar minha semana?", assistant bubble "Defina prioridades e reserve blocos de tempo.", status "Conectado", buttons "Falar" and "Encerrar sessão", subtle microphone and audio wave illustration. Clear title above panels "Odin — exemplos de uso" and footer verbatim "Ideação visual • dados fictícios • interfaces ilustrativas". Keep panels unrelated: no arrows connecting voice to kanban, no claim the voice assistant creates tasks or integrates with the APIs. Flat interface mockup with tasteful soft shadows, no photorealism, no credentials, no logos of external brands, no extra text. This is a concept illustration, not a screenshot.
```
