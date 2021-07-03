package com.slupicki.linserver;

import java.util.Locale;

public interface SearchService {

    Locale LOCALE_PL = new Locale("pl", "Pl");

    default String getLine(int line) {
        return SourceText.getLine(line - 1);
    }

    String search(String phrase);

    void index();
}
