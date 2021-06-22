package com.slupicki.linserver;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    public static final Locale LOCALE_PL = new Locale("pl", "Pl");

    private String[] lines;

    private Map<String, Set<Integer>> index = Maps.newHashMap();

    public String getLine(int line) {
        return "Here is line %d: %s".formatted(line, lines[line]);
    }

    public String search(String phrase) {
        String normalizedPhrase = String.join(" ", normalizeAndSplit(phrase));
        log.info("Search for '%s' -> '%s'".formatted(phrase, normalizedPhrase));
        if (!index.containsKey(normalizedPhrase)) {
            return null;
        }
        return index.get(normalizedPhrase).stream()
                .map(lineNumber -> lines[lineNumber])
                .collect(Collectors.joining("\n"));
    }

    public String[] getLines() {
        return lines;
    }

    public void setLines(String[] lines) {
        this.lines = lines;
    }

    public Map<String, Set<Integer>> getIndex() {
        return index;
    }

    public void index() {
        index.clear();
        ArrayList<String[]> normalizedLines = new ArrayList<>();
        int longestPhrase = 0;
        for (String line : lines) {
            String[] normalizedLine = normalizeAndSplit(line);
            normalizedLines.add(normalizedLine);
            longestPhrase = Math.max(longestPhrase, normalizedLine.length);
        }
        for (int phraseLength = 1; phraseLength <= longestPhrase; phraseLength++) {
            for (int lineNumber = 0; lineNumber < normalizedLines.size(); lineNumber++) {
                indexPhrase(normalizedLines.get(lineNumber), lineNumber, phraseLength);
            }
            log.info("Indexed phrases of length {}. The longest phrase is {}", phraseLength, longestPhrase);
        }
    }

    private void indexPhrase(String[] normalizedLine, int lineNumber, int phraseLength) {
        for(int wordIndex = 0; wordIndex < normalizedLine.length - phraseLength + 1; wordIndex++) {
            String[] phraseTable = new String[phraseLength];
            for(int i = 0; i < phraseLength; i++) {
                phraseTable[i] = normalizedLine[wordIndex + i];
            }
            String phrase = String.join(" ",phraseTable);
            if (!index.containsKey(phrase)) {
                index.put(phrase, Sets.newHashSet());
            }
            index.get(phrase).add(lineNumber);
        }
    }

    private String[] normalizeAndSplit(String line) {
        return line.toLowerCase(LOCALE_PL).split("\\P{L}+");
    }

}
