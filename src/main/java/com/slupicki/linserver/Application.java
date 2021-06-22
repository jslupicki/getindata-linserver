package com.slupicki.linserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

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
        String fileName = "test.txt";
        if (args.getNonOptionArgs().isEmpty()) {
            log.info("No args - use default file '{}'", fileName);
        } else {
            fileName = args.getNonOptionArgs().get(0);
        }
        Path path = Paths.get(fileName);
        String[] lines = Files.lines(path).toArray(String[]::new);
        searchService.setLines(lines);
        log.info("Readed {} lines from '{}'", lines.length, fileName);
        searchService.index();
        log.info("Indexed {} phrases", searchService.getIndex().keySet().size());
    }

    @Bean()
    public TaskExecutor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(100);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setThreadNamePrefix("Async-");
        return executor;
    }
}
