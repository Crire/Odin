package voice

import (
	"encoding/json"
	"fmt"
	"net/http"

	"github.com/gorilla/websocket"

	"voice-ai-prototype-backend/internal/config"
	"voice-ai-prototype-backend/internal/llm"
	"voice-ai-prototype-backend/internal/logging"
	"voice-ai-prototype-backend/internal/protocol"
)

var upgrader = websocket.Upgrader{
	CheckOrigin: func(_ *http.Request) bool {
		return true
	},
}

func RegisterHandlers(
	mux *http.ServeMux,
	cfg config.Config,
	logger *logging.Logger,
	llmProvider llm.Provider,
	sttFactory STTFactory,
) {
	mux.HandleFunc("/", func(response http.ResponseWriter, request *http.Request) {
		response.Header().Set("Content-Type", "application/json")
		_ = json.NewEncoder(response).Encode(map[string]string{
			"name":      "voice-ai-prototype-backend",
			"websocket": fmt.Sprintf("ws://localhost:%d%s", cfg.Port, cfg.WSPath),
		})
	})

	mux.HandleFunc(cfg.WSPath, func(response http.ResponseWriter, request *http.Request) {
		socket, err := upgrader.Upgrade(response, request, nil)
		if err != nil {
			logger.Error("websocket upgrade failed: %v", err)
			return
		}
		defer socket.Close()

		logger.Info("Android client connected")
		var session *Session
		defer func() {
			if session != nil {
				session.Close()
			}
			logger.Info("Android client disconnected")
		}()

		for {
			messageType, data, err := socket.ReadMessage()
			if err != nil {
				return
			}

			if messageType == websocket.BinaryMessage {
				if session != nil {
					session.HandleAudio(data)
				}
				continue
			}

			if messageType != websocket.TextMessage {
				continue
			}

			message, err := protocol.ParseClientMessage(data)
			if err != nil {
				if session != nil {
					session.HandleError(err)
				} else {
					sendProtocolError(socket, logger, err)
				}
				continue
			}

			switch message.Type {
			case "start_session":
				if session != nil {
					session.Close()
				}
				session = NewSession(socket, llmProvider, sttFactory, logger)
				if err := session.Start(message); err != nil {
					session.HandleError(err)
				}
			case "end_utterance":
				if session != nil {
					session.EndUtterance()
				}
			case "close_session":
				if session != nil {
					session.Close()
					session = nil
				}
				_ = socket.WriteMessage(websocket.CloseMessage, websocket.FormatCloseMessage(websocket.CloseNormalClosure, "Session closed"))
				return
			}
		}
	})

	logger.Info("WebSocket server listening on %s", cfg.WSPath)
}

func sendProtocolError(socket *websocket.Conn, logger *logging.Logger, err error) {
	logger.Error("%v", err)
	_ = socket.WriteJSON(protocol.StatusMessage(protocol.StatusError))
	_ = socket.WriteJSON(protocol.ErrorMessage(err.Error()))
}
