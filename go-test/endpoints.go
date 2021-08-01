package main

import (
	"fmt"
	"github.com/gin-gonic/gin"
	log "github.com/sirupsen/logrus"
	"net/http"
	"strconv"
)

func helloEndpoint(c *gin.Context) {
	c.String(http.StatusOK, "Hello %s", "name")
}

func getLineEndpoint(c *gin.Context) {
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

func searchEndpoint(c *gin.Context) {
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
