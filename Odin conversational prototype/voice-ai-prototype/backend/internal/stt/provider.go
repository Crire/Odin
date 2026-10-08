package stt

type AudioFormat struct {
	SampleRate int
	Encoding   string
	Language   string
}

type TranscriptResult struct {
	Text    string
	IsFinal bool
}

type Events struct {
	OnOpen       func()
	OnTranscript func(TranscriptResult)
	OnError      func(error)
	OnClose      func(reason string)
}

type Provider interface {
	Start(format AudioFormat) error
	SendAudio(chunk []byte) error
	FinalizeUtterance() error
	Close()
}
