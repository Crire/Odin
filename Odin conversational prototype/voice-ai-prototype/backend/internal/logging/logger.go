package logging

import (
	"fmt"
	"log"
	"strings"
)

type Level int

const (
	LevelDebug Level = iota
	LevelInfo
	LevelWarn
	LevelError
)

type Logger struct {
	level Level
}

func New(level string) *Logger {
	return &Logger{level: parseLevel(level)}
}

func (l *Logger) Debug(format string, args ...any) {
	l.write(LevelDebug, "DEBUG", format, args...)
}

func (l *Logger) Info(format string, args ...any) {
	l.write(LevelInfo, "INFO", format, args...)
}

func (l *Logger) Warn(format string, args ...any) {
	l.write(LevelWarn, "WARN", format, args...)
}

func (l *Logger) Error(format string, args ...any) {
	l.write(LevelError, "ERROR", format, args...)
}

func (l *Logger) write(level Level, label string, format string, args ...any) {
	if level < l.level {
		return
	}

	log.Printf("%s %s", label, fmt.Sprintf(format, args...))
}

func parseLevel(value string) Level {
	switch strings.ToLower(strings.TrimSpace(value)) {
	case "debug":
		return LevelDebug
	case "warn":
		return LevelWarn
	case "error":
		return LevelError
	default:
		return LevelInfo
	}
}
