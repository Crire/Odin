package voice

import (
	"context"
	"encoding/binary"
	"errors"
	"math"
	"os"
	"path/filepath"
	"strings"
	"sync"
	"time"

	"github.com/gorilla/websocket"

	"voice-ai-prototype-backend/internal/llm"
	"voice-ai-prototype-backend/internal/logging"
	"voice-ai-prototype-backend/internal/protocol"
	"voice-ai-prototype-backend/internal/stt"
)

const transcriptionTimeout = 20 * time.Second
const defaultDebugAudioDir = "/home/crire/Projects/Odin/Odin/audio-files-for-testing"

type STTFactory func(events stt.Events) stt.Provider

type Session struct {
	socket     *websocket.Conn
	llm        llm.Provider
	sttFactory STTFactory
	logger     *logging.Logger

	mu               sync.Mutex
	writeMu          sync.Mutex
	responseMu       sync.Mutex
	stt              stt.Provider
	history          []llm.ConversationMessage
	isClosed         bool
	awaitingText     bool
	audioByteCount   int
	audioSampleCount int
	audioSquareSum   uint64
	audioPeak        int
	audioSampleRate  int
	latestTranscript string
	debugAudioDir    string
	debugAudio       []byte
	timer            *time.Timer
}

func NewSession(socket *websocket.Conn, llmProvider llm.Provider, sttFactory STTFactory, logger *logging.Logger) *Session {
	debugAudioDir := strings.TrimSpace(os.Getenv("ODIN_AUDIO_DEBUG_DIR"))
	if debugAudioDir == "" {
		debugAudioDir = defaultDebugAudioDir
	}

	return &Session{
		socket:        socket,
		llm:           llmProvider,
		sttFactory:    sttFactory,
		logger:        logger,
		debugAudioDir: debugAudioDir,
	}
}

func (s *Session) Start(message protocol.ClientMessage) error {
	s.mu.Lock()
	if s.isClosed {
		s.mu.Unlock()
		return nil
	}
	oldProvider := s.stt
	s.stt = nil
	s.audioByteCount = 0
	s.audioSampleCount = 0
	s.audioSquareSum = 0
	s.audioPeak = 0
	s.audioSampleRate = message.SampleRate
	s.awaitingText = false
	s.latestTranscript = ""
	s.debugAudio = nil
	s.clearTranscriptionTimerLocked()
	s.mu.Unlock()

	if oldProvider != nil {
		oldProvider.Close()
	}

	provider := s.sttFactory(stt.Events{
		OnTranscript: s.HandleTranscript,
		OnError:      s.HandleError,
		OnClose:      s.HandleSTTClose,
	})

	format := stt.AudioFormat{
		SampleRate: message.SampleRate,
		Encoding:   message.Encoding,
		Language:   message.Language,
	}
	if err := provider.Start(format); err != nil {
		return err
	}

	s.mu.Lock()
	if s.isClosed {
		s.mu.Unlock()
		provider.Close()
		return nil
	}
	s.stt = provider
	s.mu.Unlock()

	s.sendStatus(protocol.StatusListening)
	s.logger.Info("Voice session started")
	return nil
}

func (s *Session) HandleAudio(chunk []byte) {
	s.mu.Lock()
	if s.isClosed || s.stt == nil {
		s.mu.Unlock()
		return
	}
	s.audioByteCount += len(chunk)
	s.updateAudioStatsLocked(chunk)
	if s.debugAudioDir != "" {
		s.debugAudio = append(s.debugAudio, chunk...)
	}
	provider := s.stt
	s.mu.Unlock()

	if err := provider.SendAudio(chunk); err != nil {
		s.HandleError(err)
	}
}

func (s *Session) EndUtterance() {
	s.mu.Lock()
	if s.isClosed || s.stt == nil {
		s.mu.Unlock()
		return
	}

	if s.audioByteCount == 0 {
		s.mu.Unlock()
		s.HandleError(errors.New("Nenhum áudio foi recebido para transcrever"))
		return
	}

	s.awaitingText = true
	audioBytes := s.audioByteCount
	sampleCount := s.audioSampleCount
	peak := s.audioPeak
	rms := s.audioRMSLocked()
	debugAudio := append([]byte(nil), s.debugAudio...)
	s.startTranscriptionTimerLocked()
	provider := s.stt
	s.mu.Unlock()

	s.logger.Info(
		"utterance ended audioBytes=%d samples=%d peak=%d rms=%.1f",
		audioBytes,
		sampleCount,
		peak,
		rms,
	)
	if sampleCount > 0 && (peak < 500 || rms < 80) {
		s.logger.Warn(
			"audio level is very low peak=%d rms=%.1f; microphone input may be silent or unavailable",
			peak,
			rms,
		)
	}
	s.saveDebugAudio(debugAudio)
	s.sendStatus(protocol.StatusTranscribing)
	if err := provider.FinalizeUtterance(); err != nil {
		s.HandleError(err)
	}
}

func (s *Session) HandleTranscript(result stt.TranscriptResult) {
	cleanText := strings.TrimSpace(result.Text)
	if cleanText == "" {
		return
	}

	s.mu.Lock()
	s.latestTranscript = cleanText
	s.mu.Unlock()

	if !result.IsFinal {
		return
	}

	s.clearTranscriptionWait(false)
	go s.respondToTranscript(cleanText)
}

func (s *Session) HandleSTTClose(reason string) {
	s.mu.Lock()
	if s.isClosed {
		s.mu.Unlock()
		return
	}
	s.stt = nil
	s.mu.Unlock()

	if strings.TrimSpace(reason) == "" {
		reason = "Deepgram connection closed"
	}
	s.HandleError(errors.New(reason))
}

func (s *Session) Close() {
	s.mu.Lock()
	if s.isClosed {
		s.mu.Unlock()
		return
	}

	s.isClosed = true
	s.clearTranscriptionTimerLocked()
	provider := s.stt
	s.stt = nil
	s.mu.Unlock()

	if provider != nil {
		provider.Close()
	}
	s.logger.Info("Voice session closed")
}

func (s *Session) HandleError(err error) {
	if err == nil {
		err = errors.New("Unexpected server error")
	}

	s.clearTranscriptionWait(true)
	s.logger.Error("Voice session error: %v", err)
	s.sendStatus(protocol.StatusError)
	s.sendJSON(protocol.ErrorMessage(err.Error()))
}

func (s *Session) respondToTranscript(userText string) {
	s.responseMu.Lock()
	defer s.responseMu.Unlock()

	s.sendJSON(protocol.TranscriptMessage(userText))

	ctx, cancel := context.WithTimeout(context.Background(), 45*time.Second)
	defer cancel()

	if _, err := s.llm.CountUserInputTokens(ctx, userText); err != nil {
		s.logger.Warn("could not count user input tokens: %v", err)
	}

	s.sendStatus(protocol.StatusThinking)
	response, err := s.llm.GenerateResponse(ctx, llm.GenerateResponseInput{
		SystemPrompt: llm.DefaultSystemPrompt,
		UserText:     userText,
		History:      s.historySnapshot(),
	})
	if err != nil {
		s.HandleError(err)
		return
	}

	s.appendHistory(userText, response.Text)
	s.sendStatus(protocol.StatusResponding)
	s.sendJSON(protocol.AssistantResponseMessage(response.Text))
}

func (s *Session) historySnapshot() []llm.ConversationMessage {
	s.mu.Lock()
	defer s.mu.Unlock()

	history := append([]llm.ConversationMessage(nil), s.history...)
	if len(history) > 8 {
		history = history[len(history)-8:]
	}
	return history
}

func (s *Session) appendHistory(userText string, assistantText string) {
	s.mu.Lock()
	defer s.mu.Unlock()

	s.history = append(
		s.history,
		llm.ConversationMessage{Role: llm.RoleUser, Content: userText},
		llm.ConversationMessage{Role: llm.RoleAssistant, Content: assistantText},
	)

	if len(s.history) > 8 {
		s.history = s.history[len(s.history)-8:]
	}
}

func (s *Session) startTranscriptionTimerLocked() {
	s.clearTranscriptionTimerLocked()
	s.timer = time.AfterFunc(transcriptionTimeout, func() {
		s.mu.Lock()
		if s.isClosed || !s.awaitingText {
			s.mu.Unlock()
			return
		}
		audioBytes := s.audioByteCount
		sampleCount := s.audioSampleCount
		peak := s.audioPeak
		rms := s.audioRMSLocked()
		fallbackTranscript := strings.TrimSpace(s.latestTranscript)
		s.mu.Unlock()

		if fallbackTranscript != "" {
			s.logger.Warn(
				"transcription final timed out after %s with %d audio bytes; using latest interim transcript",
				transcriptionTimeout,
				audioBytes,
			)
			s.clearTranscriptionWait(false)
			go s.respondToTranscript(fallbackTranscript)
			return
		}

		s.logger.Warn(
			"transcription timed out after %s with %d audio bytes, %d samples, peak=%d, rms=%.1f and no transcript",
			transcriptionTimeout,
			audioBytes,
			sampleCount,
			peak,
			rms,
		)
		s.HandleError(errors.New("A transcrição demorou demais ou não retornou texto. Tente falar novamente."))
	})
}

func (s *Session) clearTranscriptionWait(clearTranscript bool) {
	s.mu.Lock()
	defer s.mu.Unlock()

	s.awaitingText = false
	s.audioByteCount = 0
	s.audioSampleCount = 0
	s.audioSquareSum = 0
	s.audioPeak = 0
	s.debugAudio = nil
	if clearTranscript {
		s.latestTranscript = ""
	}
	s.clearTranscriptionTimerLocked()
}

func (s *Session) clearTranscriptionTimerLocked() {
	if s.timer != nil {
		s.timer.Stop()
		s.timer = nil
	}
}

func (s *Session) sendStatus(status protocol.ServerStatus) {
	s.sendJSON(protocol.StatusMessage(status))
}

func (s *Session) updateAudioStatsLocked(chunk []byte) {
	for i := 0; i+1 < len(chunk); i += 2 {
		sample := int(binary.LittleEndian.Uint16(chunk[i : i+2]))
		if sample >= 32768 {
			sample -= 65536
		}

		absSample := sample
		if absSample < 0 {
			absSample = -absSample
		}
		if absSample > s.audioPeak {
			s.audioPeak = absSample
		}

		s.audioSampleCount++
		s.audioSquareSum += uint64(sample * sample)
	}
}

func (s *Session) audioRMSLocked() float64 {
	if s.audioSampleCount == 0 {
		return 0
	}
	return math.Sqrt(float64(s.audioSquareSum) / float64(s.audioSampleCount))
}

func (s *Session) saveDebugAudio(pcm []byte) {
	if s.debugAudioDir == "" || len(pcm) == 0 {
		return
	}

	if err := os.MkdirAll(s.debugAudioDir, 0o700); err != nil {
		s.logger.Warn("could not create audio debug dir: %v", err)
		return
	}

	name := "utterance-" + time.Now().Format("20060102-150405.000") + ".wav"
	path := filepath.Join(s.debugAudioDir, name)
	sampleRate := s.audioSampleRate
	if sampleRate == 0 {
		sampleRate = 16000
	}

	if err := writePCM16MonoWAV(path, pcm, sampleRate); err != nil {
		s.logger.Warn("could not write debug audio wav: %v", err)
		return
	}

	s.logger.Info("saved debug audio wav: %s", path)
}

func writePCM16MonoWAV(path string, pcm []byte, sampleRate int) error {
	const (
		channelCount  = 1
		bitsPerSample = 16
		headerSize    = 44
	)

	file, err := os.Create(path)
	if err != nil {
		return err
	}
	defer file.Close()

	byteRate := sampleRate * channelCount * bitsPerSample / 8
	blockAlign := channelCount * bitsPerSample / 8
	dataSize := uint32(len(pcm))

	header := make([]byte, headerSize)
	copy(header[0:4], "RIFF")
	binary.LittleEndian.PutUint32(header[4:8], 36+dataSize)
	copy(header[8:12], "WAVE")
	copy(header[12:16], "fmt ")
	binary.LittleEndian.PutUint32(header[16:20], 16)
	binary.LittleEndian.PutUint16(header[20:22], 1)
	binary.LittleEndian.PutUint16(header[22:24], channelCount)
	binary.LittleEndian.PutUint32(header[24:28], uint32(sampleRate))
	binary.LittleEndian.PutUint32(header[28:32], uint32(byteRate))
	binary.LittleEndian.PutUint16(header[32:34], uint16(blockAlign))
	binary.LittleEndian.PutUint16(header[34:36], bitsPerSample)
	copy(header[36:40], "data")
	binary.LittleEndian.PutUint32(header[40:44], dataSize)

	if _, err := file.Write(header); err != nil {
		return err
	}
	_, err = file.Write(pcm)
	return err
}

func (s *Session) sendJSON(message protocol.ServerMessage) {
	s.writeMu.Lock()
	defer s.writeMu.Unlock()

	if err := s.socket.WriteJSON(message); err != nil {
		s.logger.Warn("could not send websocket message: %v", err)
	}
}
