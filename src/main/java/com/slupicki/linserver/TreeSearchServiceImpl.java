package com.slupicki.linserver;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TreeSearchServiceImpl implements SearchService {

    private static final Logger log = LoggerFactory.getLogger(TreeSearchServiceImpl.class);

    private final Node root = new Node();

    @Override
    public String search(String phrase) {
        List<String> tokenizedPhrase = tokenizer(phrase.toLowerCase(LOCALE_PL));
        if (tokenizedPhrase.isEmpty()) {
            throw new NotFoundException();
        }
        Set<Integer> linesWithPhrase = searchPhrase(tokenizedPhrase, root);
        return linesWithPhrase.stream()
                .map(SourceText::getLine)
                .collect(Collectors.joining("\n"));
    }

    private Set<Integer> searchPhrase(List<String> tokenizedPhrase, Node node) {
        if (tokenizedPhrase.isEmpty()) {
            return node.lines;
        }
        String token = tokenizedPhrase.get(0);
        tokenizedPhrase.remove(0);
        if (!node.children.containsKey(token)) {
            throw new NotFoundException();
        }
        return searchPhrase(tokenizedPhrase, node.children.get(token));
    }

    @Override
    public void index() {
        int lineIdx = 0;
        for (String line : SourceText.lines()) {
            List<String> tokenizedLine = tokenizer(line.toLowerCase(LOCALE_PL));
            indexLine(tokenizedLine, lineIdx++);
            if (lineIdx % 10 == 0) {
                log.info("Indexed line {} from {}", lineIdx, SourceText.size());
            }
        }
        log.info("Indexed line {} from {}", lineIdx, SourceText.size());
    }

    private void indexLine(List<String> tokenizedLine, int lineIdx) {
        for (int idx = 0; idx < tokenizedLine.size(); idx++) {
            indexPhrase(root, tokenizedLine, lineIdx, idx);
        }
    }

    private void indexPhrase(Node node, List<String> tokenizedLine, int lineIdx, int idx) {
        if (tokenizedLine.size() <= idx) {
            return;
        }
        String token = tokenizedLine.get(idx);
        Node tokenNode = node.children.get(token);
        if (tokenNode == null) {
            tokenNode = new Node(token);
            node.children.put(token, tokenNode);
        }
        tokenNode.lines.add(lineIdx);
        indexPhrase(tokenNode, tokenizedLine, lineIdx, idx + 1);
    }

    private List<String> tokenizer(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder token = new StringBuilder();
        boolean lastWasLetter = true;
        for (char c : line.toCharArray()) {
            if (Character.isLetter(c)) {
                if (!lastWasLetter) {
                    addTokenIfNotEmpty(token, result);
                    token = new StringBuilder();
                }
                token.append(c);
                lastWasLetter = true;
            } else {
                if (lastWasLetter) {
                    addTokenIfNotEmpty(token, result);
                    token = new StringBuilder();
                }
                token.append(c);
                lastWasLetter = false;
            }
        }
        addTokenIfNotEmpty(token, result);
        return result;
    }

    private void addTokenIfNotEmpty(StringBuilder token, List<String> result) {
        if (token.length() > 0) {
            result.add(token.toString());
        }
    }

    private static class Node {
        final String token;
        final Set<Integer> lines = Sets.newHashSet();
        final Map<String, Node> children = Maps.newHashMap();

        private Node() {
            this.token = null;
        }

        private Node(String token) {
            this.token = token;
        }
    }
}
