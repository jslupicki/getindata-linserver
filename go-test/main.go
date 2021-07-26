package main

import (
	"bufio"
	"fmt"
	"net/http"
	"os"
	"strconv"

	"github.com/gin-gonic/gin"
	log "github.com/sirupsen/logrus"
	easy "github.com/t-tomalak/logrus-easy-formatter"
)

var text []string

func hello(c *gin.Context) {
	c.String(http.StatusOK, "Hello %s", "name")
}

func getLine(c *gin.Context) {
	lineParam := c.Param("line")
	line, err := strconv.ParseUint(lineParam, 10, 32)
	if err == nil {
		if int(line) < len(text) {
			c.String(http.StatusOK, text[line])
		} else {
			c.String(http.StatusNotFound, "Can't find line %d. Read only %d lines.", line, len(text))
		}
	} else {
		msg := fmt.Sprintf("Can't parse parameter '%s'", lineParam)
		log.Error(msg)
		c.String(http.StatusInternalServerError, msg)
	}
}

func search(c *gin.Context) {
	phrase := c.Query("phrase")
	c.String(http.StatusOK, "Search phrase '%s'", phrase)
}

func readFile(fileName string) []string {
	file, err := os.Open(fileName)
	if err != nil {
		log.Fatalf("Can't open file '%s'", fileName)
	}
	scanner := bufio.NewScanner(file)
	var text []string
	for scanner.Scan() {
		text = append(text, scanner.Text())
	}
	return text
}

func main() {
	log.SetOutput(os.Stdout)
	log.SetFormatter(&easy.Formatter{
		TimestampFormat: "2006-01-02 15:04:05",
		LogFormat:       "[%lvl%]: %time% - %msg%\n",
	})
	fileName := "20_000_mil_podmorskiej_zeglugi.txt"
	text = readFile(fileName)
	log.Infof("Read %d lines from '%s'", len(text), fileName)
	router := gin.Default()
	router.GET("/", hello)
	router.GET("/get/:line", getLine)
	router.GET("/search", search)
	err := router.Run("0.0.0.0:8080")
	if err != nil {
		log.Fatal("Unable to start router")
	}
}
