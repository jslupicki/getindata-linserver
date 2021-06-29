package com.slupicki.linserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
public class AsyncController {

    private final Logger log = LoggerFactory.getLogger(AsyncController.class);

    private final SearchService searchService;

    public AsyncController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/get/{line}")
    @Async
    public CompletableFuture<String> getLine(@PathVariable("line") int line) {
        log.info("Request for line {}", line);
        return CompletableFuture
                .supplyAsync(() -> searchService.getLine(line));
    }

    @GetMapping("/search")
    @Async
    public CompletableFuture<String> searchPhrase(@RequestParam("phrase") String phrase) {
        log.info("Request to search phrase '{}'", phrase);
        return CompletableFuture.supplyAsync(() -> searchService.search(phrase));
    }

}
