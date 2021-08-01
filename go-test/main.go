package main

import (
	"fmt"
	"github.com/gin-gonic/gin"
	log "github.com/sirupsen/logrus"
	easy "github.com/t-tomalak/logrus-easy-formatter"
	"net/http"
	"os"
	"runtime"
	"strconv"
)

func hello(c *gin.Context) {
	c.String(http.StatusOK, "Hello %s", "name")
}

func getLine(c *gin.Context) {
	lineParam := c.Param("line")
	line, err := strconv.ParseUint(lineParam, 10, 32)
	if err == nil {
		if int(line) < len(TEXT) {
			c.String(http.StatusOK, TEXT[line])
		} else {
			c.String(http.StatusNotFound, "Can't find line %d. Read only %d lines.", line, len(TEXT))
		}
	} else {
		msg := fmt.Sprintf("Can't parse parameter '%s'", lineParam)
		log.Error(msg)
		c.String(http.StatusInternalServerError, msg)
	}
}

func search(c *gin.Context) {
	phrase := c.Query("phrase")
	result, err := searchPhrase(phrase)
	if err != nil {
		c.String(http.StatusNotFound, "Not found phrase '%s'", phrase)
	} else {
		c.String(http.StatusOK, result)
	}
}

func performanceTestEndpoint(c *gin.Context) {
	result := performanceTest("Ned Land", 24, 10_000)
	c.String(http.StatusOK, result)
}

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
	router.GET("/", hello)
	router.GET("/get/:line", getLine)
	router.GET("/search", search)
	router.GET("/perf", performanceTestEndpoint)
	err := router.Run("0.0.0.0:8080")
	if err != nil {
		log.Fatal("Unable to start router")
	}
}
