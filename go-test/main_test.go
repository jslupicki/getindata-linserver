package main

import (
	"bufio"
	"fmt"
	log "github.com/sirupsen/logrus"
	"github.com/stretchr/testify/assert"
	"github.com/t-tomalak/logrus-easy-formatter"
	"os"
	"strings"
	"testing"
)

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

func Test_search(t *testing.T) {
	text := `a
a b
a b c
A
A b
A b c
A
A B
A B c
A
A B
A B C
Jerzy Brzęczyszczykiewicz`
	phrasesAndExpectedResults := map[string][]string{
		"non existent": nil,
		"jerzy": {"Jerzy Brzęczyszczykiewicz"},
		"B c": {"a b c", "A b c", "A B c", "A B C"},
		" ": {"a b", "a b c", "A b", "A b c", "A B", "A B c", "A B", "A B C", "Jerzy Brzęczyszczykiewicz"},
	}
	root := Node{
		"",
		map[int]bool{},
		map[string]*Node{},
	}
	textTab := splitByNewline(text)
	indexText(&root, &textTab)

	for phrase, expectedResult := range phrasesAndExpectedResults {
		result, err := searchPhraseInTree(&root, phrase, &textTab)
		if err != nil {
			fmt.Printf("Phrase '%s' not found\n", phrase)
		} else {
			fmt.Printf("Phrase '%s':\n%s\n", phrase, result)
		}
		resultAsTab := splitByNewline(result)
		assert.ElementsMatch(t, resultAsTab, expectedResult)
	}
}

func getKeys(nodeMap map[string]*Node) []string {
	result := []string{}
	for k := range nodeMap {
		result = append(result, k)
	}
	return result
}

func splitByNewline(text string) []string {
	scanner := bufio.NewScanner(strings.NewReader(text))
	var textTab []string
	for scanner.Scan() {
		textTab = append(textTab, scanner.Text())
	}
	return textTab
}
