package main

import (
	"fmt"
	"github.com/gin-gonic/gin"
	log "github.com/sirupsen/logrus"
	"github.com/stretchr/testify/assert"
	"github.com/t-tomalak/logrus-easy-formatter"
	"os"
	"testing"
)

func Test_search(t *testing.T) {
	type args struct {
		c *gin.Context
	}
	tests := []struct {
		name string
		args args
	}{
		// TODO: Add test cases.
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
		})
	}
}

func Test_main(t *testing.T) {
	fmt.Printf("Hello %s\n", "abba")
	log.SetOutput(os.Stdout)
	log.SetFormatter(&easy.Formatter{
		TimestampFormat: "2006-01-02 15:04:05",
		LogFormat:       "[%lvl%]: %time% - %msg%\n",
	})
	log.Infof("Hello to log: %s", "abba")
}

func Test_tokenizer(t *testing.T) {
	text := "abba ab12 AAACCC   ,.,. ŁucjaBrzęczyszczykiewicz"
	expectedResult := []string{
		"abba", " ", "ab12", " ", "aaaccc", "   ,.,. ", "łucjabrzęczyszczykiewicz",
	}
	tokenized := tokenizer(&text)

	assert.ElementsMatch(t, tokenized, expectedResult)
}

func Test_indexLine(t *testing.T) {
	line := "a B c"
	root := Node{
		"",
		map[int]bool{},
		map[string]*Node{},
	}

	indexLine(&root, &line, 1)

	keys := getKeys(root.children)
	expectedKeys := []string{"a", " ", "b", "c"}
	assert.ElementsMatch(t, keys, expectedKeys)

	key := "b"
	node := root.children[key]
	assert.True(t, node.lines[1])
	assert.True(t, len(node.lines) == 1)
	keys = getKeys(node.children)
	expectedKeys = []string{" "}
	assert.ElementsMatch(t, keys, expectedKeys)

	key = "c"
	node = root.children[key]
	assert.True(t, node.lines[1])
	assert.True(t, len(node.lines) == 1)
	keys = getKeys(node.children)
	expectedKeys = []string{}
	assert.ElementsMatch(t, keys, expectedKeys)

	key = " "
	node = root.children[key]
	assert.True(t, node.lines[1])
	assert.True(t, len(node.lines) == 1)
	keys = getKeys(node.children)
	expectedKeys = []string{"b", "c"}
	assert.ElementsMatch(t, keys, expectedKeys)
}

func getKeys(nodeMap map[string]*Node) []string {
	result := []string{}
	for k := range nodeMap {
		result = append(result, k)
	}
	return result
}
