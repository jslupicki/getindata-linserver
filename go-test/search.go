package main

import (
	"bufio"
	"errors"
	"fmt"
	"github.com/bradhe/stopwatch"
	"github.com/sirupsen/logrus"
	"os"
	"strings"
	"sync"
	"unicode"
)

var TEXT []string
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

func readFile(fileName string) []string {
	file, err := os.Open(fileName)
	if err != nil {
		logrus.Fatalf("Can't open file '%s'", fileName)
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
			logrus.Infof("Indexed line %d from %d", i+1, howManyLines)
		}
	}
	logrus.Infof("Indexed line %d from %d", howManyLines, howManyLines)
}

func searchPhrase(phrase string) (string, error) {
	return searchPhraseInTree(&ROOT, phrase, &TEXT)
}

func searchPhraseInTree(root *Node, phrase string, text *[]string) (string, error) {
	var result []string
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

func performanceTest(phrase string, howManyThreads int, howManySearches int) string {
	searchResult, err := searchPhrase(phrase)
	if err != nil {
		return fmt.Sprintf("Error: %s", err.Error())
	}
	watch := stopwatch.Start()
	var wg sync.WaitGroup
	for i := 0; i < howManyThreads; i++ {
		wg.Add(1)
		go func(wg *sync.WaitGroup) {
			defer wg.Done()
			for ii := 0; ii < howManySearches; ii++ {
				_, _ = searchPhrase(phrase)
			}
		}(&wg)
	}
	wg.Wait()
	watch.Stop()
	foundInLines := len(splitByNewline(searchResult))
	result := fmt.Sprintf("Performance test took %dms\n", watch.Milliseconds())
	result += fmt.Sprintf("Phrase '%s' found in %d lines\n", phrase, foundInLines)
	result += fmt.Sprintf("Performed %d searches\n", howManySearches*howManyThreads)
	result += fmt.Sprintf("It give %d/s\n", howManySearches*howManyThreads*1000/int(watch.Milliseconds()))
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
