package provider

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"net/http"
	"time"

	"odin/service-api/internal/domain"
)

type FCMSender struct {
	serverKey string
	client    *http.Client
}

func NewFCMSender(serverKey string) *FCMSender {
	return &FCMSender{
		serverKey: serverKey,
		client: &http.Client{
			Timeout: 5 * time.Second,
		},
	}
}

func (s *FCMSender) Send(ctx context.Context, device domain.Device, job domain.NotificationJob) (string, error) {
	if s.serverKey == "" {
		return "", errors.New("missing FCM server key")
	}
	body, err := json.Marshal(map[string]any{
		"to": device.PushToken,
		"notification": map[string]string{
			"title": job.Title,
			"body":  job.Message,
		},
		"data": map[string]string{
			"notificationJobId": job.ID.String(),
			"type":              job.Type,
		},
	})
	if err != nil {
		return "", err
	}
	request, err := http.NewRequestWithContext(ctx, http.MethodPost, "https://fcm.googleapis.com/fcm/send", bytes.NewReader(body))
	if err != nil {
		return "", err
	}
	request.Header.Set("Authorization", "key="+s.serverKey)
	request.Header.Set("Content-Type", "application/json")

	response, err := s.client.Do(request)
	if err != nil {
		return "", err
	}
	defer response.Body.Close()
	if response.StatusCode >= 300 {
		return "", fmt.Errorf("fcm returned HTTP %d", response.StatusCode)
	}
	return response.Status, nil
}
