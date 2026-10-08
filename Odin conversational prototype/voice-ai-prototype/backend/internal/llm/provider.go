package llm

import "context"

type ConversationRole string

const (
	RoleUser      ConversationRole = "user"
	RoleAssistant ConversationRole = "assistant"
)

type ConversationMessage struct {
	Role    ConversationRole
	Content string
}

type GenerateResponseInput struct {
	SystemPrompt string
	UserText     string
	History      []ConversationMessage
}

type TokenUsage struct {
	InputTokens  *int
	OutputTokens *int
	TotalTokens  *int
}

type GenerateResponseResult struct {
	Text       string
	TokenUsage TokenUsage
}

type Provider interface {
	CountUserInputTokens(ctx context.Context, userText string) (int, error)
	GenerateResponse(ctx context.Context, input GenerateResponseInput) (GenerateResponseResult, error)
}
