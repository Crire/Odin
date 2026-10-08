# Voice AI Prototype

Protótipo local de uma IA conversacional por voz para Android. O app captura áudio do microfone, envia PCM 16-bit por WebSocket para um backend Go, o backend encaminha o áudio para Deepgram Streaming STT usando o SDK oficial `deepgram-go-sdk`, envia a transcrição para Gemini Flash-Lite e devolve a resposta em texto para o app falar com TextToSpeech nativo.

Não há login, banco de dados, dashboard, persistência obrigatória ou modelo local pesado. A fase 1 foca apenas no fluxo funcional de conversa por voz com baixa latência e custo controlado.

## Arquitetura

```text
android-app/
  AudioRecord -> OkHttp WebSocket -> TextToSpeech

backend/
  Go HTTP/WebSocket server -> Deepgram Go SDK -> Gemini API -> ws response
```

O app Android nunca contém chaves de API. As chaves ficam somente no backend.

## Fluxo Da Conversa

1. O usuário toca em `Conectar`.
2. O app abre a URL configurada em `android-app/app/src/main/java/com/example/voiceai/AppConfig.kt`.
3. O app envia `start_session`.
4. O backend abre uma conexão live com a Deepgram.
5. O usuário toca em `Falar`.
6. O app captura áudio com `AudioRecord` em PCM 16-bit mono a 16000 Hz.
7. O app envia chunks binários para o backend.
8. O usuário toca em `Parar`.
9. O app envia `end_utterance`.
10. O backend chama `Finalize()` no cliente live da Deepgram.
11. A Deepgram retorna um evento `Results` com `transcript`, `is_final` e/ou `speech_final`.
12. O backend envia a transcrição ao app, chama Gemini e devolve `assistant_response`.
13. O app mostra a resposta e fala o texto com `TextToSpeech`.

## Tecnologias Usadas

- Android nativo
- Kotlin
- Jetpack Compose
- AudioRecord
- Android TextToSpeech
- OkHttp WebSocket
- Go
- Gorilla WebSocket
- Deepgram Go SDK
- Gemini API

## Como Configurar O Backend

O backend Go lê `backend/.env.example` ao iniciar. Variáveis de ambiente reais já exportadas no shell têm prioridade sobre o arquivo.

```bash
cd backend
go mod download
go run ./cmd/server
```

Se `go` não estiver ativo no PATH, mas `mise` estiver disponível:

```bash
cd backend
mise exec go@1.22 -- go mod download
mise exec go@1.22 -- go run ./cmd/server
```

O servidor sobe em:

```text
ws://localhost:8080/voice
```

Variáveis esperadas:

```env
PORT=8080
WS_PATH=/voice

GEMINI_API_KEY=sua_chave_gemini
GEMINI_MODEL=gemini-3.1-flash-lite
GEMINI_MAX_OUTPUT_TOKENS=160
GEMINI_TEMPERATURE=0.6

DEEPGRAM_API_KEY=sua_chave_deepgram
DEEPGRAM_MODEL=nova-3
DEEPGRAM_LANGUAGE=pt-BR
DEEPGRAM_ENCODING=linear16
DEEPGRAM_SAMPLE_RATE=16000

LOG_LEVEL=debug
```

## Uso Da Deepgram Em Go

O backend usa o SDK oficial:

```text
github.com/deepgram/deepgram-go-sdk
```

O exemplo `Transcribe Audio from a Remote Stream` usa a sequência:

1. `client.InitWithDefault()`
2. cria `context.Context`
3. define `interfaces.LiveTranscriptionOptions`
4. cria o cliente live
5. chama `Connect()`
6. alimenta a Deepgram com um stream via `Stream(reader)`
7. chama `Stop()` ao finalizar

Neste backend, a mesma lógica foi adaptada para áudio vindo do app Android:

- inicialização em `internal/stt/deepgram.go`
- opções `LiveTranscriptionOptions` com `Model`, `Language`, `Encoding`, `SampleRate`, `Channels`, `Punctuate`, `SmartFormat` e `Endpointing`
- conexão com `listen.NewWSUsingCallback(...)` e `Connect()`
- envio de chunks do Android com `client.Write(chunk)`
- finalização da fala atual com `client.Finalize()`
- encerramento com `client.Stop()`

O callback da Deepgram recebe respostas JSON do tipo `Results`. O backend usa o primeiro `channel.alternatives[0].transcript` quando `is_final` ou `speech_final` chegam como `true`. Esse é o mesmo ponto descrito no exemplo: `transcript` contém o texto reconhecido, `confidence` indica confiabilidade, `words` contém palavras com tempos, `speech_final` indica fim natural de fala, e `is_final` indica que a Deepgram considera aquele trecho definitivo.

## Como Configurar O Android

1. Abra a pasta `android-app` no Android Studio.
2. Sincronize o Gradle.
3. Rode em um emulador ou dispositivo físico.
4. Permita o acesso ao microfone.
5. Toque em `Conectar`.
6. Toque em `Falar`, fale uma frase e toque em `Parar`.

## URL WebSocket No Android

A URL padrão fica em:

```text
android-app/app/src/main/java/com/example/voiceai/AppConfig.kt
```

Para emulador Android:

```kotlin
const val DEFAULT_WS_URL = "ws://10.0.2.2:8080/voice"
```

Para celular físico na mesma rede, troque `10.0.2.2` pelo IP local da máquina que está rodando o backend:

```kotlin
const val DEFAULT_WS_URL = "ws://IP_DA_MAQUINA:8080/voice"
```

## Links E Substituição De Modelos

O modelo Gemini é configurado por:

- `backend/.env.example`
- variável `GEMINI_MODEL`
- leitura centralizada em `backend/internal/config/config.go`

Modelo padrão:

```env
GEMINI_MODEL=gemini-3.1-flash-lite
```

Exemplos de substituição:

```env
GEMINI_MODEL=gemini-3.1-flash-lite
GEMINI_MODEL=gemini-3.5-flash
```

Links oficiais:

- Modelos disponíveis: https://ai.google.dev/gemini-api/docs/models
- Geração de conteúdo: https://ai.google.dev/api/generate-content
- Pricing Gemini: https://ai.google.dev/gemini-api/docs/pricing
- Deepgram STT streaming: https://developers.deepgram.com/reference/speech-to-text/listen-streaming
- Deepgram Go SDK: https://github.com/deepgram/deepgram-go-sdk
- Android TextToSpeech: https://developer.android.com/reference/kotlin/android/speech/tts/TextToSpeech

## Logs De Tokens Gemini

O backend registra tokens usando mecanismos oficiais do Gemini:

- `models.countTokens`: conta somente os tokens do texto transcrito do usuário, sem chamar `generateContent`.
- `usageMetadata`: retorna os tokens usados na chamada real de geração.

Os métodos ficam em:

```text
backend/internal/llm/gemini.go
```

Depois da resposta do Gemini, o backend grava:

```text
Gemini generateContent token usage model=... inputTokens=... outputTokens=... totalTokens=...
```

## Como Trocar Provedor De STT

O backend usa uma interface em:

```text
backend/internal/stt/provider.go
```

A implementação padrão fica em:

```text
backend/internal/stt/deepgram.go
```

Para trocar o provedor:

1. Crie outra implementação de `stt.Provider`.
2. Mantenha os métodos `Start`, `SendAudio`, `FinalizeUtterance` e `Close`.
3. Troque a factory em `cmd/server/main.go`.
4. Adicione novas variáveis em `backend/.env.example` se necessário.

## Protocolo WebSocket

Endpoint:

```text
ws://localhost:8080/voice
```

### App Para Backend

Iniciar sessão:

```json
{
  "type": "start_session",
  "sampleRate": 16000,
  "encoding": "linear16",
  "language": "pt-BR"
}
```

Áudio:

```text
Frames binários com PCM 16-bit mono em chunks pequenos.
```

Finalizar fala atual:

```json
{
  "type": "end_utterance"
}
```

Encerrar sessão:

```json
{
  "type": "close_session"
}
```

### Backend Para App

Status:

```json
{
  "type": "status",
  "value": "listening"
}
```

Valores possíveis:

```text
listening | transcribing | thinking | responding | error
```

Transcrição final:

```json
{
  "type": "transcript",
  "text": "texto reconhecido do usuário"
}
```

Resposta da IA:

```json
{
  "type": "assistant_response",
  "text": "resposta final da IA"
}
```

Erro:

```json
{
  "type": "error",
  "message": "descrição do erro"
}
```

## Como Rodar Localmente

Backend:

```bash
cd backend
go run ./cmd/server
```

Fallback com `mise`:

```bash
cd backend
mise exec go@1.22 -- go run ./cmd/server
```

Android:

1. Abra `android-app` no Android Studio.
2. Sincronize o Gradle.
3. Rode em emulador ou dispositivo físico.
4. Permita o microfone.
5. Clique em `Conectar`.
6. Fale usando o botão principal.

## Problemas Comuns

- `DEEPGRAM_API_KEY is required`: configure a chave em `backend/.env.example` ou exporte a variável no shell.
- `GEMINI_API_KEY is required`: configure a chave em `backend/.env.example` ou exporte a variável no shell.
- App não conecta no emulador: confirme se o backend está rodando e se `AppConfig.DEFAULT_WS_URL` usa `ws://10.0.2.2:8080/voice`.
- App não conecta em celular físico: use o IP local da máquina no lugar de `10.0.2.2` e mantenha ambos na mesma rede.
- Sem áudio: confirme a permissão `RECORD_AUDIO` no Android.
- Resposta demora: verifique latência de rede, cota das APIs e logs do backend.

## Próximos Passos

- Adicionar testes unitários para o protocolo, prompt builder e parser de ambiente.
- Mostrar indicador visual enquanto o TTS fala.
- Permitir edição da URL WebSocket em tela de configuração local.
- Trocar o STT por outro provedor implementando `stt.Provider`.
