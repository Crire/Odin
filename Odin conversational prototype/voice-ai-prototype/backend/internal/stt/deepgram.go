package stt

import (
	"context"
	"errors"
	"fmt"
	"strings"
	"sync"

	dgapi "github.com/deepgram/deepgram-go-sdk/pkg/api/listen/v1/websocket/interfaces"
	dginterfaces "github.com/deepgram/deepgram-go-sdk/pkg/client/interfaces"
	dglisten "github.com/deepgram/deepgram-go-sdk/pkg/client/listen"

	"voice-ai-prototype-backend/internal/config"
	"voice-ai-prototype-backend/internal/logging"
)

var initDeepgramOnce sync.Once

const finalizeSilenceDurationMs = 700

type DeepgramStreamingProvider struct {
	config config.DeepgramConfig
	logger *logging.Logger
	events Events

	mu       sync.Mutex
	client   *dglisten.WSCallback
	cancel   context.CancelFunc
	isClosed bool
}

type deepgramCallback struct {
	provider *DeepgramStreamingProvider
}

func NewDeepgramStreamingProvider(cfg config.DeepgramConfig, logger *logging.Logger, events Events) *DeepgramStreamingProvider {
	initDeepgramOnce.Do(dglisten.InitWithDefault)
	return &DeepgramStreamingProvider{
		config: cfg,
		logger: logger,
		events: events,
	}
}

func (p *DeepgramStreamingProvider) Start(format AudioFormat) error {
	if p.config.APIKey == "" {
		return errors.New("DEEPGRAM_API_KEY is required in backend/.env.example")
	}

	ctx, cancel := context.WithCancel(context.Background())
	options := &dginterfaces.LiveTranscriptionOptions{
		Model:          p.config.Model,
		Language:       firstNonEmpty(format.Language, p.config.Language),
		Encoding:       firstNonEmpty(format.Encoding, p.config.Encoding),
		SampleRate:     firstNonZero(format.SampleRate, p.config.SampleRate),
		Channels:       1,
		InterimResults: true,
		Punctuate:      true,
		SmartFormat:    true,
		Endpointing:    "300",
		UtteranceEndMs: "1000",
		VadEvents:      true,
	}

	clientOptions := &dginterfaces.ClientOptions{
		EnableKeepAlive: true,
	}

	client, err := dglisten.NewWSUsingCallback(
		ctx,
		p.config.APIKey,
		clientOptions,
		options,
		&deepgramCallback{provider: p},
	)
	if err != nil {
		cancel()
		return err
	}

	if connected := client.Connect(); !connected {
		cancel()
		return errors.New("Deepgram client connect failed")
	}

	p.mu.Lock()
	p.client = client
	p.cancel = cancel
	p.isClosed = false
	p.mu.Unlock()

	p.logger.Info(
		"Deepgram streaming connected model=%s language=%s encoding=%s sampleRate=%d",
		options.Model,
		options.Language,
		options.Encoding,
		options.SampleRate,
	)
	return nil
}

func (p *DeepgramStreamingProvider) SendAudio(chunk []byte) error {
	if len(chunk) == 0 {
		return nil
	}

	p.mu.Lock()
	client := p.client
	p.mu.Unlock()

	if client == nil {
		return errors.New("Deepgram client is not connected")
	}

	_, err := client.Write(chunk)
	return err
}

func (p *DeepgramStreamingProvider) FinalizeUtterance() error {
	p.mu.Lock()
	client := p.client
	p.mu.Unlock()

	if client == nil {
		return errors.New("Deepgram client is not connected")
	}

	if err := p.sendTrailingSilence(client); err != nil {
		return err
	}
	return client.Finalize()
}

func (p *DeepgramStreamingProvider) sendTrailingSilence(client *dglisten.WSCallback) error {
	sampleRate := firstNonZero(p.config.SampleRate, 16000)
	sampleCount := sampleRate * finalizeSilenceDurationMs / 1000
	silence := make([]byte, sampleCount*2)

	_, err := client.Write(silence)
	if err != nil {
		return err
	}

	p.logger.Debug("sent %dms trailing silence to Deepgram before finalize", finalizeSilenceDurationMs)
	return nil
}

func (p *DeepgramStreamingProvider) Close() {
	p.mu.Lock()
	if p.isClosed {
		p.mu.Unlock()
		return
	}
	p.isClosed = true
	client := p.client
	cancel := p.cancel
	p.client = nil
	p.cancel = nil
	p.mu.Unlock()

	if client != nil {
		client.Stop()
	}
	if cancel != nil {
		cancel()
	}
}

func (c *deepgramCallback) Open(_ *dgapi.OpenResponse) error {
	if c.provider.events.OnOpen != nil {
		c.provider.events.OnOpen()
	}
	return nil
}

func (c *deepgramCallback) Message(message *dgapi.MessageResponse) error {
	if message == nil || len(message.Channel.Alternatives) == 0 {
		return nil
	}

	transcript := strings.TrimSpace(message.Channel.Alternatives[0].Transcript)
	isFinal := message.IsFinal || message.SpeechFinal
	if transcript == "" {
		if message.IsFinal || message.SpeechFinal || message.FromFinalize {
			c.provider.logger.Debug(
				"Deepgram empty result received isFinal=%t speechFinal=%t fromFinalize=%t duration=%.2f",
				message.IsFinal,
				message.SpeechFinal,
				message.FromFinalize,
				message.Duration,
			)
		}
		return nil
	}

	c.provider.logger.Debug(
		"Deepgram result received isFinal=%t speechFinal=%t fromFinalize=%t transcript=%q",
		message.IsFinal,
		message.SpeechFinal,
		message.FromFinalize,
		transcript,
	)

	if c.provider.events.OnTranscript != nil {
		c.provider.events.OnTranscript(TranscriptResult{
			Text:    transcript,
			IsFinal: isFinal,
		})
	}
	return nil
}

func (c *deepgramCallback) Metadata(response *dgapi.MetadataResponse) error {
	if response == nil {
		return nil
	}
	c.provider.logger.Debug(
		"Deepgram metadata requestId=%s channels=%d duration=%.2f",
		strings.TrimSpace(response.RequestID),
		response.Channels,
		response.Duration,
	)
	return nil
}

func (c *deepgramCallback) SpeechStarted(_ *dgapi.SpeechStartedResponse) error {
	c.provider.logger.Debug("Deepgram speech started")
	return nil
}

func (c *deepgramCallback) UtteranceEnd(response *dgapi.UtteranceEndResponse) error {
	if response == nil {
		c.provider.logger.Debug("Deepgram utterance ended")
		return nil
	}
	c.provider.logger.Debug("Deepgram utterance ended lastWordEnd=%.2f", response.LastWordEnd)
	return nil
}

func (c *deepgramCallback) Close(_ *dgapi.CloseResponse) error {
	c.provider.mu.Lock()
	isClosed := c.provider.isClosed
	c.provider.client = nil
	c.provider.cancel = nil
	c.provider.mu.Unlock()

	if !isClosed && c.provider.events.OnClose != nil {
		c.provider.events.OnClose("Deepgram connection closed")
	}
	return nil
}

func (c *deepgramCallback) Error(response *dgapi.ErrorResponse) error {
	if c.provider.events.OnError == nil {
		return nil
	}

	if response == nil {
		c.provider.events.OnError(errors.New("Deepgram returned an error"))
		return nil
	}

	parts := []string{}
	if response.ErrMsg != "" {
		parts = append(parts, response.ErrMsg)
	}
	if response.Description != "" {
		parts = append(parts, response.Description)
	}
	if response.ErrCode != "" {
		parts = append(parts, "code="+response.ErrCode)
	}
	if len(parts) == 0 {
		parts = append(parts, "Deepgram returned an error")
	}

	c.provider.events.OnError(errors.New(strings.Join(parts, " - ")))
	return nil
}

func (c *deepgramCallback) UnhandledEvent(data []byte) error {
	c.provider.logger.Debug("Deepgram unhandled event: %s", string(data))
	return nil
}

func firstNonEmpty(values ...string) string {
	for _, value := range values {
		if strings.TrimSpace(value) != "" {
			return value
		}
	}
	return ""
}

func firstNonZero(values ...int) int {
	for _, value := range values {
		if value != 0 {
			return value
		}
	}
	return 0
}

func (p *DeepgramStreamingProvider) String() string {
	return fmt.Sprintf("DeepgramStreamingProvider(model=%s)", p.config.Model)
}
