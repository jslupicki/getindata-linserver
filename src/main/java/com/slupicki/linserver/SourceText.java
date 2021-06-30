package com.slupicki.linserver;

import com.google.common.collect.Iterators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class SourceText {

    private static final Logger log = LoggerFactory.getLogger(SourceText.class);

    private static String[] lines;

    private SourceText() {}

    public static String getLine(int n) {
        if (n < 0 || n >= lines.length) {
            throw new NotFoundException();
        }
        return lines[n];
    }

    public static void setLines(String[] sourceLines) {
        lines = sourceLines;
    }

    public static Iterable<String> lines() {
        return () -> Iterators.forArray(lines);
    }

    public static int size() {
        return lines.length;
    }

    public static void load(String fileName) throws IOException {
        Path path = Paths.get(fileName);
        lines = Files.lines(path).toArray(String[]::new);
        log.info("Readed {} lines from '{}'", lines.length, fileName);
    }
}
