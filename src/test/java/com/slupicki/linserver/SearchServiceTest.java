package com.slupicki.linserver;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SearchServiceTest {

    SearchService searchService;

    @BeforeEach
    void setUp() {
        searchService = new SearchService();
        String[] lines = {
                "the",
                "quick brown",
                "fox jumps over the",
                "lazy dog"
        };
        searchService.setLines(lines);
    }

    @Test
    void shouldGetExpectedLine() {
        assertThat(searchService.getLine(2)).isEqualTo("quick brown");
    }

    @Test
    void shouldThrowExceptionWhenLineNumberIsBiggerThanLastOne() {
        Assertions.assertThrows(NotFoundException.class, () -> searchService.getLine(searchService.getLines().length + 1));
    }

    @Test
    void shouldThrowExceptionWhenLineNumberIsLestThanOne() {
        Assertions.assertThrows(NotFoundException.class, () -> searchService.getLine(0));
    }

    @Test
    void shouldThrowExceptionWhenSearchDontFindPhrase() {
        searchService.index();
        Assertions.assertThrows(NotFoundException.class, () -> searchService.search("not existing phrase"));
    }

    @Test
    void shouldFindPhrase() {
        searchService.index();
        assertThat(searchService.search("quick brown")).isEqualTo("quick brown");
    }

    @Test
    void shouldNotFindPhraseWithDifferentSpcaces() {
        searchService.index();
        Assertions.assertThrows(NotFoundException.class, () -> searchService.search("quick   brown"));
    }

    @Test
    void shouldFindPhraseRegardlessOfCase() {
        searchService.index();
        assertThat(searchService.search("QuIcK BrOwN")).isEqualTo("quick brown");
    }

    @Test
    void shouldFindPhraseInMultipleLines() {
        searchService.index();
        assertThat(searchService.search("the").split("\n"))
                .containsExactlyInAnyOrder("the", "fox jumps over the");

    }
}