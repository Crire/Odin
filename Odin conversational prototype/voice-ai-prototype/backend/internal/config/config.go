package config

import (
	"bufio"
	"errors"
	"fmt"
	"os"
	"path/filepath"
	"strconv"
	"strings"
)

type Config struct {
	Port     int
	WSPath   string
	LogLevel string
	Gemini   GeminiConfig
	Deepgram DeepgramConfig
}

type GeminiConfig struct {
	APIKey          string
	Model           string
	MaxOutputTokens int
	Temperature     float64
}

type DeepgramConfig struct {
	APIKey     string
	Model      string
	Language   string
	Encoding   string
	SampleRate int
}

func Load() (Config, error) {
	if err := loadDotenvExample(); err != nil {
		return Config{}, err
	}

	return Config{
		Port:     readInt("PORT", 8080),
		WSPath:   readString("WS_PATH", "/voice"),
		LogLevel: readString("LOG_LEVEL", "info"),
		Gemini: GeminiConfig{
			APIKey:          readOptionalSecret("GEMINI_API_KEY"),
			Model:           readString("GEMINI_MODEL", "gemini-3.1-flash-lite"),
			MaxOutputTokens: readInt("GEMINI_MAX_OUTPUT_TOKENS", 160),
			Temperature:     readFloat("GEMINI_TEMPERATURE", 0.6),
		},
		Deepgram: DeepgramConfig{
			APIKey:     readOptionalSecret("DEEPGRAM_API_KEY"),
			Model:      readString("DEEPGRAM_MODEL", "nova-3"),
			Language:   readString("DEEPGRAM_LANGUAGE", "pt-BR"),
			Encoding:   readString("DEEPGRAM_ENCODING", "linear16"),
			SampleRate: readInt("DEEPGRAM_SAMPLE_RATE", 16000),
		},
	}, nil
}

func loadDotenvExample() error {
	path, err := findDotenvExample()
	if err != nil {
		return err
	}

	file, err := os.Open(path)
	if err != nil {
		return fmt.Errorf("open %s: %w", path, err)
	}
	defer file.Close()

	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		line := strings.TrimSpace(scanner.Text())
		if line == "" || strings.HasPrefix(line, "#") {
			continue
		}

		key, value, ok := strings.Cut(line, "=")
		if !ok {
			continue
		}

		key = strings.TrimSpace(key)
		value = strings.Trim(strings.TrimSpace(value), `"'`)
		if key == "" {
			continue
		}

		if _, exists := os.LookupEnv(key); !exists {
			_ = os.Setenv(key, value)
		}
	}

	return scanner.Err()
}

func findDotenvExample() (string, error) {
	wd, err := os.Getwd()
	if err != nil {
		return "", err
	}

	for dir := wd; ; dir = filepath.Dir(dir) {
		candidate := filepath.Join(dir, ".env.example")
		if _, err := os.Stat(candidate); err == nil {
			return candidate, nil
		}

		parent := filepath.Dir(dir)
		if parent == dir {
			break
		}
	}

	return "", errors.New("backend/.env.example not found")
}

func readString(name string, fallback string) string {
	value := strings.TrimSpace(os.Getenv(name))
	if value == "" {
		return fallback
	}
	return value
}

func readInt(name string, fallback int) int {
	value := strings.TrimSpace(os.Getenv(name))
	if value == "" {
		return fallback
	}

	parsed, err := strconv.Atoi(value)
	if err != nil {
		return fallback
	}
	return parsed
}

func readFloat(name string, fallback float64) float64 {
	value := strings.TrimSpace(os.Getenv(name))
	if value == "" {
		return fallback
	}

	parsed, err := strconv.ParseFloat(value, 64)
	if err != nil {
		return fallback
	}
	return parsed
}

func readOptionalSecret(name string) string {
	value := strings.TrimSpace(os.Getenv(name))
	if value == "" || strings.HasPrefix(value, "replace_with_") {
		return ""
	}
	return value
}
