package com.slupicki.linserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PerformanceTest {

    private static final Logger log = LoggerFactory.getLogger(PerformanceTest.class);

    private static final String TEST_FILE = "20_000_mil_podmorskiej_zeglugi.txt";
    private static final String SEARCH_PHRASE = "Ned Land";
    private static final int HOW_MANY_THREADS = 24;
    private static final int HOW_MANY_SEARCHES = 100_000;
    private static final int HOW_OFTEN_LOG_PROGRESS = 10_000;

    public static void main(String[] args) throws Exception {
        SearchService searchService = new SearchService();
        searchService.loadFile(TEST_FILE);
        log.info("Start test");
        Thread[] threads = new Thread[HOW_MANY_THREADS];
        long startTime = System.currentTimeMillis();
        for(int i = 0; i < HOW_MANY_THREADS; i++) {
            threads[i] = new Thread(() -> runTest(searchService), "test-thread-" + i);
            threads[i].start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        long endTime = System.currentTimeMillis();
        log.info("End test");
        long howManySearchesTotal = HOW_MANY_SEARCHES * HOW_MANY_THREADS;
        long howLongItTakeInSeconds = (endTime - startTime) / 1000;
        double searchesPerSecond = howManySearchesTotal * 1.0 / howLongItTakeInSeconds;
        log.info("Finished {} searches in {}s which is {}/s",
                howManySearchesTotal,
                howLongItTakeInSeconds,
                searchesPerSecond
        );
    }

    private static void runTest(SearchService searchService) {
        for(int i = 0; i < HOW_MANY_SEARCHES; i++) {
            searchService.search(SEARCH_PHRASE);
            if ((i+1) % HOW_OFTEN_LOG_PROGRESS == 0) {
                log.info("Performed {} searches of '{}' in text from '{}'", i + 1, SEARCH_PHRASE, TEST_FILE);
            }
        }
    }
}
