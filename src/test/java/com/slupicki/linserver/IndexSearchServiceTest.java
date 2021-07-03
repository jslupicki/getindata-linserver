package com.slupicki.linserver;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class IndexSearchServiceTest extends AbstractSearchServiceTest {

    @Override
    SearchService getServiceUnderTest() {
        return new IndexSearchServiceImpl();
    }

    @Test
    void shouldFindPhraseContainingMultipleWordsWhenIndexHaveOnlyShorterPhrases() {
        IndexSearchServiceImpl searchService = (IndexSearchServiceImpl) getServiceUnderTest();
        searchService.setMaxPhraseLengthToIndex(2);
        searchService.index();
        assertThat(searchService.search("fox jumps over")).isEqualTo("fox jumps over the");
    }
}
