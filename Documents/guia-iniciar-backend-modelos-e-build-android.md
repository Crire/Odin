# Guia Para Iniciar Backend, Definir Modelos E Instalar O App Android

Este guia explica como iniciar o prototipo de IA conversacional por voz, configurar modelos e chaves, e instalar o app Android em um dispositivo movel.

## 1. Iniciar O Backend

Entre na pasta do backend:

```bash
cd "/home/crire/Projects/Odin/Odin/Odin conversational prototype/voice-ai-prototype/backend"
npm install
cp .env.example .env
```

Abra o arquivo `backend/.env` e coloque suas chaves:

```env
GEMINI_API_KEY=sua_chave_gemini
DEEPGRAM_API_KEY=sua_chave_deepgram
```

Depois rode:

```bash
npm run dev
```

O backend sobe em:

```text
ws://localhost:8080/voice
```

Para celular fisico, o app deve acessar o backend pelo IP da sua maquina, por exemplo:

```text
ws://192.168.0.25:8080/voice
```

## 2. Definir Modelos

O modelo Gemini fica em `backend/.env`:

```env
GEMINI_MODEL=gemini-2.5-flash-lite
```

Esse e o padrao mais leve e economico para o prototipo.

Voce pode trocar para:

```env
GEMINI_MODEL=gemini-2.5-flash
```

Ou:

```env
GEMINI_MODEL=gemini-2.5-pro
```

O Deepgram tambem fica no `.env`:

```env
DEEPGRAM_MODEL=nova-3
DEEPGRAM_LANGUAGE=pt-BR
DEEPGRAM_SAMPLE_RATE=16000
```

Depois de mudar qualquer modelo, chave ou variavel de ambiente, reinicie o backend.

## 3. Autenticacao

Nesta fase, o app nao tem login.

A autenticacao acontece apenas entre o backend e as APIs externas:

```env
GEMINI_API_KEY=...
DEEPGRAM_API_KEY=...
```

O app Android nunca deve conter essas chaves. Ele conversa apenas com o backend local via WebSocket.

## 4. Configurar A URL Do App Android

A URL do WebSocket fica em:

```text
Odin conversational prototype/voice-ai-prototype/android-app/app/src/main/java/com/example/voiceai/AppConfig.kt
```

Para emulador Android, use:

```kotlin
const val DEFAULT_WS_URL = "ws://10.0.2.2:8080/voice"
```

Para celular fisico na mesma rede, troque `10.0.2.2` pelo IP local do computador:

```kotlin
const val DEFAULT_WS_URL = "ws://192.168.0.25:8080/voice"
```

Para descobrir seu IP no Linux:

```bash
ip addr
```

Procure o IP da rede Wi-Fi ou cabo, normalmente algo como `192.168.x.x`.

## 5. Maneira Mais Otimizada De Instalar No Celular

Para desenvolvimento, o caminho mais simples e eficiente e usar o Android Studio:

1. Abra a pasta `android-app` no Android Studio.
2. Conecte o celular via USB.
3. Ative `Opcoes do desenvolvedor` no Android.
4. Ative `Depuracao USB`.
5. Selecione seu dispositivo no Android Studio.
6. Clique em `Run`.

Esse fluxo compila e instala o app direto no celular, sem gerar APK manualmente.

## 6. Gerar APK Debug Manualmente

Se quiser gerar um APK debug pela linha de comando, use:

```bash
cd "/home/crire/Projects/Odin/Odin/Odin conversational prototype/voice-ai-prototype/android-app"
gradle :app:assembleDebug
```

O APK fica em:

```text
android-app/app/build/outputs/apk/debug/app-debug.apk
```

Para instalar com ADB:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Observacao: o projeto ainda nao inclui `gradlew`. Por isso, no estado atual, o Android Studio e o caminho mais simples. Para builds CLI mais reprodutiveis, o proximo ajuste recomendado e adicionar o Gradle Wrapper.

## 7. Ordem Recomendada De Uso

1. Configure `backend/.env`.
2. Rode `npm run dev` no backend.
3. Ajuste `AppConfig.DEFAULT_WS_URL` conforme emulador ou celular fisico.
4. Abra `android-app` no Android Studio.
5. Instale o app no dispositivo.
6. Permita o microfone.
7. Toque em `Conectar`.
8. Toque em `Falar`, fale uma frase e toque em `Parar`.
