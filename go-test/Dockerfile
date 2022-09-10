FROM golang:1.19.1-alpine

RUN mkdir /app
WORKDIR /app

COPY *.go ./
COPY go.mod ./
COPY go.sum ./
COPY 20_000_mil_podmorskiej_zeglugi.txt ./

RUN go mod download
RUN go build -o app

RUN ls -al

ENTRYPOINT ["/app/app"]



