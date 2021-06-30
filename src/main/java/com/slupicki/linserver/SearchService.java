package com.slupicki.linserver;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    public static final Locale LOCALE_PL = new Locale("pl", "Pl");

    private int maxPhraseLengthToIndex = -1;
    private final Map<String, Set<Integer>> index = Maps.newHashMap();

    public String getLine(int line) {
        return SourceText.getLine(line - 1);
    }

    public String search(String phrase) {
        String lowerCasePhrase = phrase.toLowerCase(LOCALE_PL);
        String[] normalizedPhraseArray = normalizeAndSplit(phrase);
        List<List<String>> phrasesLists = Lists.partition(Arrays.asList(normalizedPhraseArray), maxPhraseLengthToIndex);
        Set<Integer> allMatchedLines = Sets.newHashSet();
        for (List<String> phraseList : phrasesLists) {
            String normalizedPhrase = String.join(" ", phraseList);
            Set<Integer> matchedLines = index.get(normalizedPhrase);
            if (matchedLines == null) {
                throw new NotFoundException();
            }
            if (allMatchedLines.isEmpty()) {
                allMatchedLines = matchedLines;
            } else {
                allMatchedLines = Sets.intersection(allMatchedLines, matchedLines);
            }
            if (allMatchedLines.isEmpty()) {
                throw new NotFoundException();
            }
        }
        List<String> result = Lists.newLinkedList();
        for (Integer matchedLine : allMatchedLines) {
            String line = SourceText.getLine(matchedLine);
            if (line.toLowerCase(LOCALE_PL).contains(lowerCasePhrase)) {
                result.add(line);
            }
        }
        if (result.isEmpty()) {
            throw new NotFoundException();
        }
        return String.join("\n", result);
    }

    public Map<String, Set<Integer>> getIndex() {
        return index;
    }

    public int getMaxPhraseLengthToIndex() {
        return maxPhraseLengthToIndex;
    }

    public void setMaxPhraseLengthToIndex(int maxPhraseLengthToIndex) {
        this.maxPhraseLengthToIndex = maxPhraseLengthToIndex;
    }

    public void index() {
        index.clear();
        ArrayList<String[]> normalizedLines = new ArrayList<>();
        int longestPhrase = 0;
        for (String line : SourceText.lines()) {
            String[] normalizedLine = normalizeAndSplit(line);
            normalizedLines.add(normalizedLine);
            longestPhrase = Math.max(longestPhrase, normalizedLine.length);
        }
        maxPhraseLengthToIndex = maxPhraseLengthToIndex < 0 ? longestPhrase : maxPhraseLengthToIndex;
        for (int phraseLength = 1; phraseLength <= maxPhraseLengthToIndex; phraseLength++) {
            for (int lineNumber = 0; lineNumber < normalizedLines.size(); lineNumber++) {
                indexPhrase(normalizedLines.get(lineNumber), lineNumber, phraseLength);
            }
            log.info("Indexed phrases of length {}. The longest phrase is {}. Max indexed phrase will be {}", phraseLength, longestPhrase, maxPhraseLengthToIndex);
        }
    }

    private void indexPhrase(String[] normalizedLine, int lineNumber, int phraseLength) {
        for(int wordIndex = 0; wordIndex < normalizedLine.length - phraseLength + 1; wordIndex++) {
            String[] phraseTable = new String[phraseLength];
            System.arraycopy(normalizedLine, wordIndex, phraseTable, 0, phraseLength);
            String phrase = String.join(" ",phraseTable);
            if (!index.containsKey(phrase)) {
                index.put(phrase, Sets.newHashSet());
            }
            index.get(phrase).add(lineNumber);
        }
    }

    private String[] normalizeAndSplit(String line) {
        return line.toLowerCase(LOCALE_PL).strip().split("\\P{L}+", -1);
    }

}
