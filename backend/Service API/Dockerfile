FROM golang:1.26-alpine AS build
WORKDIR /workspace
COPY go.mod ./
RUN go mod download
COPY . .
RUN go build -o /out/odin-service-api ./cmd/service-api

FROM alpine:3.22
WORKDIR /app
COPY --from=build /out/odin-service-api /app/odin-service-api
EXPOSE 8081
ENTRYPOINT ["/app/odin-service-api"]
