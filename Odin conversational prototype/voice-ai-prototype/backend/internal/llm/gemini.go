package llm

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"strings"
	"time"

	"voice-ai-prototype-backend/internal/config"
	"voice-ai-prototype-backend/internal/logging"
)

type GeminiProvider struct {
	config config.GeminiConfig
	logger *logging.Logger
	client *http.Client
}

type geminiContent struct {
	Role  string       `json:"role"`
	Parts []geminiPart `json:"parts"`
}

type geminiPart struct {
	Text string `json:"text"`
}

type geminiGenerateRequest struct {
	Contents         []geminiContent        `json:"contents"`
	GenerationConfig geminiGenerationConfig `json:"generationConfig"`
}

type geminiGenerationConfig struct {
	MaxOutputTokens int     `json:"maxOutputTokens"`
	Temperature     float64 `json:"temperature"`
}

type geminiGenerateResponse struct {
	Candidates []struct {
		Content struct {
			Parts []geminiPart `json:"parts"`
		} `json:"content"`
	} `json:"candidates"`
	Error         *geminiError `json:"error,omitempty"`
	UsageMetadata struct {
		PromptTokenCount     *int `json:"promptTokenCount,omitempty"`
		CandidatesTokenCount *int `json:"candidatesTokenCount,omitempty"`
		TotalTokenCount      *int `json:"totalTokenCount,omitempty"`
	} `json:"usageMetadata,omitempty"`
}

type geminiCountTokensRequest struct {
	Contents []geminiContent `json:"contents"`
}

type geminiCountTokensResponse struct {
	TotalTokens *int         `json:"totalTokens,omitempty"`
	Error       *geminiError `json:"error,omitempty"`
}

type geminiError struct {
	Message string `json:"message,omitempty"`
}

func NewGeminiProvider(cfg config.GeminiConfig, logger *logging.Logger) *GeminiProvider {
	return &GeminiProvider{
		config: cfg,
		logger: logger,
		client: &http.Client{Timeout: 30 * time.Second},
	}
}

func (p *GeminiProvider) CountUserInputTokens(ctx context.Context, userText string) (int, error) {
	if p.config.APIKey == "" {
		return 0, errors.New("GEMINI_API_KEY is required in backend/.env.example")
	}

	cleanText := strings.TrimSpace(userText)
	if cleanText == "" {
		p.logger.Info("Gemini countTokens user input model=%s inputTokens=0", p.config.Model)
		return 0, nil
	}

	reqBody := geminiCountTokensRequest{
		Contents: []geminiContent{{
			Role:  "user",
			Parts: []geminiPart{{Text: cleanText}},
		}},
	}

	var body geminiCountTokensResponse
	if err := p.post(ctx, "countTokens", reqBody, &body); err != nil {
		return 0, err
	}

	if body.TotalTokens == nil {
		return 0, errors.New("Gemini countTokens returned no totalTokens value")
	}

	p.logger.Info("Gemini countTokens user input model=%s inputTokens=%d", p.config.Model, *body.TotalTokens)
	return *body.TotalTokens, nil
}

func (p *GeminiProvider) GenerateResponse(ctx context.Context, input GenerateResponseInput) (GenerateResponseResult, error) {
	if p.config.APIKey == "" {
		return GenerateResponseResult{}, errors.New("GEMINI_API_KEY is required in backend/.env.example")
	}

	prompt := BuildVoiceAssistantPrompt(input)
	reqBody := geminiGenerateRequest{
		Contents: []geminiContent{{
			Role:  "user",
			Parts: []geminiPart{{Text: prompt}},
		}},
		GenerationConfig: geminiGenerationConfig{
			MaxOutputTokens: p.config.MaxOutputTokens,
			Temperature:     p.config.Temperature,
		},
	}

	var body geminiGenerateResponse
	if err := p.post(ctx, "generateContent", reqBody, &body); err != nil {
		return GenerateResponseResult{}, err
	}

	var parts []string
	if len(body.Candidates) > 0 {
		for _, part := range body.Candidates[0].Content.Parts {
			parts = append(parts, part.Text)
		}
	}

	text := strings.TrimSpace(strings.Join(parts, ""))
	if text == "" {
		return GenerateResponseResult{}, errors.New("Gemini returned an empty response")
	}

	usage := TokenUsage{
		InputTokens:  body.UsageMetadata.PromptTokenCount,
		OutputTokens: body.UsageMetadata.CandidatesTokenCount,
		TotalTokens:  body.UsageMetadata.TotalTokenCount,
	}
	p.logger.Info(
		"Gemini generateContent token usage model=%s inputTokens=%s outputTokens=%s totalTokens=%s",
		p.config.Model,
		formatOptionalInt(usage.InputTokens),
		formatOptionalInt(usage.OutputTokens),
		formatOptionalInt(usage.TotalTokens),
	)

	return GenerateResponseResult{Text: text, TokenUsage: usage}, nil
}

func (p *GeminiProvider) post(ctx context.Context, method string, payload any, target any) error {
	encoded, err := json.Marshal(payload)
	if err != nil {
		return err
	}

	req, err := http.NewRequestWithContext(ctx, http.MethodPost, p.endpoint(method), bytes.NewReader(encoded))
	if err != nil {
		return err
	}
	req.Header.Set("Content-Type", "application/json")

	response, err := p.client.Do(req)
	if err != nil {
		return err
	}
	defer response.Body.Close()

	raw, err := io.ReadAll(response.Body)
	if err != nil {
		return err
	}

	if err := json.Unmarshal(raw, target); err != nil {
		return fmt.Errorf("Gemini returned invalid JSON: %w", err)
	}

	if response.StatusCode < 200 || response.StatusCode >= 300 {
		if message := readGeminiError(target); message != "" {
			return errors.New(message)
		}
		return fmt.Errorf("Gemini request failed with %d", response.StatusCode)
	}

	return nil
}

func (p *GeminiProvider) endpoint(method string) string {
	query := url.Values{}
	query.Set("key", p.config.APIKey)
	return fmt.Sprintf(
		"https://generativelanguage.googleapis.com/v1beta/models/%s:%s?%s",
		url.PathEscape(p.config.Model),
		method,
		query.Encode(),
	)
}

func readGeminiError(value any) string {
	switch body := value.(type) {
	case *geminiGenerateResponse:
		if body.Error != nil {
			return body.Error.Message
		}
	case *geminiCountTokensResponse:
		if body.Error != nil {
			return body.Error.Message
		}
	}
	return ""
}

func formatOptionalInt(value *int) string {
	if value == nil {
		return "unknown"
	}
	return fmt.Sprintf("%d", *value)
}
