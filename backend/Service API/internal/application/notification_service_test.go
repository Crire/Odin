package application

import "testing"

func TestNotificationServicePackage(t *testing.T) {
	t.Parallel()
	if "odin-service-api" == "" {
		t.Fatal("service name should not be blank")
	}
}
