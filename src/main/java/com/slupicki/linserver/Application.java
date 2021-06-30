package com.slupicki.linserver;

import org.apache.commons.lang3.StringUtils;
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

import java.io.File;
import java.util.List;
import java.util.Set;

@SpringBootApplication
@EnableAsync
public class Application implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(Application.class);
    public static final String LIMIT_OPTION = "limit";
    public static final String DEFAULT_INPUT_FILE = "test.txt";

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
        String fileName = DEFAULT_INPUT_FILE;
        validateArgs(args);
        if (args.getNonOptionArgs().isEmpty()) {
            log.info("No args - use default file '{}'", fileName);
        } else {
            fileName = args.getNonOptionArgs().get(0);
        }
        List<String> limit = args.getOptionValues(LIMIT_OPTION);
        if (limit == null) {
            log.info("'-limit' option not set - will be no limit to length of indexed phrases");
        } else {
            int maxPhraseLength = Integer.parseInt(limit.get(0));
            searchService.setMaxPhraseLengthToIndex(maxPhraseLength);
            log.info("Maximum length indexed phrases is set to {}", maxPhraseLength);
        }
        SourceText.load(fileName);
        searchService.index();
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

    private void validateArgs(ApplicationArguments args) {
        List<String> nonOptionArgs = args.getNonOptionArgs();
        Set<String> optionNames = args.getOptionNames();
        List<String> limit = args.getOptionValues(LIMIT_OPTION);
        if (!nonOptionArgs.isEmpty()) {
            if (nonOptionArgs.size() > 1) {
                System.out.println("Only one source file allowed!");
                usage();
            }
            String fileName = nonOptionArgs.get(0);
            if (!new File(fileName).exists()) {
                System.out.printf("Source file '%s' don't exist!%n", fileName);
                usage();
            }
        }
        if (!optionNames.isEmpty()) {
            if (!(optionNames.size() == 1 && optionNames.contains(LIMIT_OPTION))) {
                System.out.println("Only '--limit=n' option is allowed!");
                usage();
            }
            if (limit.size() > 1) {
                System.out.println("Only one value of '--limit=n' option is allowed!");
                usage();
            }
            String limitValueString = limit.get(0);
            if (!StringUtils.isNumeric(limitValueString) || Integer.parseInt(limitValueString) <= 0) {
                System.out.println("In option '--limit=n' n have to be integer >= 1");
                usage();
            }
        }
    }

    private void usage() {
        System.out.printf("""
Usage:
    java -jar getindata-linserver-*.jar --limit=n fileName
Where:
    fileName - optional name of file with text (if missing default '%1$s' is used)
    --limit - optional limit to length of indexed phrases (if missing it means no limit)
Examples:
    java -jar getindata-linserver-*.jar                     ['%1$s' as input and no limit of phrase length] 
    java -jar getindata-linserver-*.jar  small.txt          ['small.txt' as input and no limit of phrase length] 
    java -jar getindata-linserver-*.jar --limit=2 small.txt ['small.txt' as input and limit of phrase length is set to 2] 
                """, DEFAULT_INPUT_FILE);
        System.exit(1);
    }
}
