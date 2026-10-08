package middleware

import (
	"context"
	"net/http"

	"github.com/google/uuid"
)

type correlationKey struct{}

func CorrelationID(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		correlationID := r.Header.Get("X-Correlation-Id")
		if correlationID == "" {
			correlationID = uuid.NewString()
		}
		w.Header().Set("X-Correlation-Id", correlationID)
		ctx := context.WithValue(r.Context(), correlationKey{}, correlationID)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

func FromContext(ctx context.Context) string {
	value, _ := ctx.Value(correlationKey{}).(string)
	return value
}
