package llm

const DefaultSystemPrompt = "Você é uma IA conversacional por voz. Responda em português brasileiro. Seja natural, direto e útil. Use respostas curtas, com no máximo 3 frases. Não explique demais. Se não entender a fala do usuário, peça para ele repetir."

func BuildVoiceAssistantPrompt(input GenerateResponseInput) string {
	history := input.History
	if len(history) > 8 {
		history = history[len(history)-8:]
	}

	lines := []string{
		"Instruções do assistente:",
		input.SystemPrompt,
		"",
		"Histórico recente:",
	}

	if len(history) == 0 {
		lines = append(lines, "Sem histórico anterior.")
	} else {
		for _, item := range history {
			role := "Usuário"
			if item.Role == RoleAssistant {
				role = "Assistente"
			}
			lines = append(lines, role+": "+item.Content)
		}
	}

	lines = append(lines, "", "Fala atual do usuário:", input.UserText, "", "Resposta curta:")

	result := ""
	for i, line := range lines {
		if i > 0 {
			result += "\n"
		}
		result += line
	}
	return result
}
