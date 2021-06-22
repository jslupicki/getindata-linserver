package com.slupicki.linserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
@EnableAsync
public class Application implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    private final SearchService searchService;

    public Application(SearchService searchService) {
        this.searchService = searchService;
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Current directory: {}", System.getProperty("user.dir"));
        if (args.getNonOptionArgs().isEmpty()) {
            log.info("No args");
        }
        for (String arg : args.getNonOptionArgs()) {
            log.info("arg: {}", arg);
        }
        //Path path = Paths.get("20_000_mil_podmorskiej_zeglugi.txt");
        Path path = Paths.get("test.txt");
        //Path path = Paths.get("small.txt");
        String[] lines = Files.lines(path).toArray(String[]::new);
        searchService.setLines(lines);
        log.info("Readed {} lines", lines.length);
        searchService.index();
        log.info("Indexed {} phrases", searchService.getIndex().keySet().size());
    }
}
