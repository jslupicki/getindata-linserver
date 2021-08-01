package main

import (
	"github.com/gin-gonic/gin"
	log "github.com/sirupsen/logrus"
	easy "github.com/t-tomalak/logrus-easy-formatter"
	"os"
	"runtime"
)

func main() {
	log.SetOutput(os.Stdout)
	log.SetFormatter(&easy.Formatter{
		TimestampFormat: "2006-01-02 15:04:05",
		LogFormat:       "[%lvl%]: %time% - %msg%\n",
	})
	log.Infof("GOMAXPROCS is %d", runtime.GOMAXPROCS(0))
	fileName := "20_000_mil_podmorskiej_zeglugi.txt"
	TEXT = readFile(fileName)
	log.Infof("Read %d lines from '%s'", len(TEXT), fileName)
	indexText(&ROOT, &TEXT)
	router := gin.Default()
	router.GET("/", helloEndpoint)
	router.GET("/get/:line", getLineEndpoint)
	router.GET("/search", searchEndpoint)
	router.GET("/perf", performanceTestEndpoint)
	err := router.Run("0.0.0.0:8080")
	if err != nil {
		log.Fatal("Unable to start router")
	}
}
