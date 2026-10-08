package queue

import (
	"context"
	"errors"
	"log/slog"
	"time"

	"odin/service-api/internal/application"

	amqp "github.com/rabbitmq/amqp091-go"
)

type RabbitConsumer struct {
	url       string
	queueName string
	service   *application.NotificationService
	logger    *slog.Logger
	batch     int
}

func NewRabbitConsumer(url, queueName string, service *application.NotificationService, logger *slog.Logger, batch int) *RabbitConsumer {
	return &RabbitConsumer{url: url, queueName: queueName, service: service, logger: logger, batch: batch}
}

func (c *RabbitConsumer) Start(ctx context.Context) {
	if c.url == "" {
		c.logger.InfoContext(ctx, "rabbitmq consumer disabled")
		return
	}
	for {
		if err := c.consume(ctx); err != nil {
			c.logger.WarnContext(ctx, "rabbitmq consumer disconnected", "error", err)
			select {
			case <-ctx.Done():
				return
			case <-time.After(5 * time.Second):
			}
		}
	}
}

func (c *RabbitConsumer) consume(ctx context.Context) error {
	connection, err := amqp.Dial(c.url)
	if err != nil {
		return err
	}
	defer connection.Close()

	channel, err := connection.Channel()
	if err != nil {
		return err
	}
	defer channel.Close()

	if _, err := channel.QueueDeclare(c.queueName, true, false, false, false, nil); err != nil {
		return err
	}
	deliveries, err := channel.Consume(c.queueName, "odin-service-api", false, false, false, false, nil)
	if err != nil {
		return err
	}
	c.logger.InfoContext(ctx, "rabbitmq consumer started", "queue", c.queueName)
	for {
		select {
		case <-ctx.Done():
			return ctx.Err()
		case delivery, ok := <-deliveries:
			if !ok {
				return errors.New("rabbitmq deliveries channel closed")
			}
			if _, err := c.service.ProcessDue(ctx, c.batch); err != nil {
				_ = delivery.Nack(false, true)
				c.logger.ErrorContext(ctx, "failed processing queue trigger", "error", err)
				continue
			}
			_ = delivery.Ack(false)
		}
	}
}
