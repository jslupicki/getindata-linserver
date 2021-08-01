package main

import (
	"bufio"
	"errors"
	"fmt"
	"net/http"
	"os"
	"runtime"
	"strconv"
	"strings"
	"unicode"

	"github.com/gin-gonic/gin"
	log "github.com/sirupsen/logrus"
	easy "github.com/t-tomalak/logrus-easy-formatter"
)

var text []string
var ROOT Node = Node{
	"",
	map[int]bool{},
	map[string]*Node{},
}

type Node struct {
	token    string
	lines    map[int]bool
	children map[string]*Node
}

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

func isAlphanumeric(c rune) bool {
	return unicode.IsLetter(c) || unicode.IsDigit(c)
}

func tokenizer(text *string) []string {
	var result []string
	lowercaseText := strings.ToLower(*text)
	lastWasAlphanumeric := false
	li := 0
	for i, c := range *text {
		isAlphanumeric := isAlphanumeric(c)
		if isAlphanumeric != lastWasAlphanumeric && li != i {
			result = append(result, lowercaseText[li:i])
			li = i
		}
		lastWasAlphanumeric = isAlphanumeric
	}
	return append(result, lowercaseText[li:])
}

func indexLine(root *Node, line *string, lineNumber int) {
	tokenizedLine := tokenizer(line)
	for i := 0; i < len(tokenizedLine); i++ {
		node := root
		phrase := tokenizedLine[i:]
		for _, t := range phrase {
			token := t
			child := node.children[token]
			if child == nil {
				child = &Node{
					token,
					map[int]bool{},
					map[string]*Node{},
				}
				node.children[token] = child
			}
			child.lines[lineNumber] = true
			node = child
		}
	}
}

func indexText(root *Node, text *[]string) {
	howManyLines := len(*text)
	for i, line := range *text {
		indexLine(root, &line, i)
		if i%100 == 0 {
			log.Infof("Indexed line %d from %d", i+1, howManyLines)
		}
	}
	log.Infof("Indexed line %d from %d", howManyLines, howManyLines)
}

func searchPhrase(phrase string) (string, error) {
	return searchPhraseInTree(&ROOT, phrase, &text)
}

func searchPhraseInTree(root *Node, phrase string, text *[]string) (string, error) {
	result := []string{}
	tokenizedPhrase := tokenizer(&phrase)
	node := root
	for _, token := range tokenizedPhrase {
		node = node.children[token]
		if node == nil {
			return "", errors.New("not found")
		}
	}
	for line := range node.lines {
		result = append(result, (*text)[line])
	}
	return strings.Join(result, "\n"), nil
}

func main() {
	log.SetOutput(os.Stdout)
	log.SetFormatter(&easy.Formatter{
		TimestampFormat: "2006-01-02 15:04:05",
		LogFormat:       "[%lvl%]: %time% - %msg%\n",
	})
	log.Infof("GOMAXPROCS is %d", runtime.GOMAXPROCS(0))
	fileName := "20_000_mil_podmorskiej_zeglugi.txt"
	text = readFile(fileName)
	log.Infof("Read %d lines from '%s'", len(text), fileName)
	indexText(&ROOT, &text)
	router := gin.Default()
	router.GET("/", hello)
	router.GET("/get/:line", getLine)
	router.GET("/search", search)
	err := router.Run("0.0.0.0:8080")
	if err != nil {
		log.Fatal("Unable to start router")
	}
}
