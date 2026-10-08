package protocol

import (
	"encoding/json"
	"errors"
	"fmt"
)

type ServerStatus string

const (
	StatusListening     ServerStatus = "listening"
	StatusTranscribing  ServerStatus = "transcribing"
	StatusThinking      ServerStatus = "thinking"
	StatusResponding    ServerStatus = "responding"
	StatusError         ServerStatus = "error"
	AudioEncodingLinear              = "linear16"
)

type ClientMessage struct {
	Type       string `json:"type"`
	SampleRate int    `json:"sampleRate,omitempty"`
	Encoding   string `json:"encoding,omitempty"`
	Language   string `json:"language,omitempty"`
}

type StartSessionMessage struct {
	SampleRate int
	Encoding   string
	Language   string
}

type ServerMessage struct {
	Type    string       `json:"type"`
	Value   ServerStatus `json:"value,omitempty"`
	Text    string       `json:"text,omitempty"`
	Message string       `json:"message,omitempty"`
}

func ParseClientMessage(raw []byte) (ClientMessage, error) {
	var message ClientMessage
	if err := json.Unmarshal(raw, &message); err != nil {
		return ClientMessage{}, errors.New("Invalid JSON message")
	}

	if message.Type == "" {
		return ClientMessage{}, errors.New("Message must contain a type")
	}

	switch message.Type {
	case "start_session":
		if message.SampleRate <= 0 {
			return ClientMessage{}, errors.New("start_session.sampleRate must be a positive integer")
		}
		if message.Encoding != AudioEncodingLinear {
			return ClientMessage{}, errors.New("Only linear16 audio encoding is supported in this prototype")
		}
		if message.Language == "" {
			return ClientMessage{}, errors.New("start_session.language is required")
		}
	case "end_utterance", "close_session":
	default:
		return ClientMessage{}, fmt.Errorf("Unsupported message type: %s", message.Type)
	}

	return message, nil
}

func StatusMessage(value ServerStatus) ServerMessage {
	return ServerMessage{Type: "status", Value: value}
}

func TranscriptMessage(text string) ServerMessage {
	return ServerMessage{Type: "transcript", Text: text}
}

func AssistantResponseMessage(text string) ServerMessage {
	return ServerMessage{Type: "assistant_response", Text: text}
}

func ErrorMessage(message string) ServerMessage {
	return ServerMessage{Type: "error", Message: message}
}
