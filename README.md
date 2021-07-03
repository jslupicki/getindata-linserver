# getindata-linserver
Test program for Getindata recruitment process

# Zadanie

Opis zadania znajduje się tu [link](documentation/Linserver%20v2%20.pdf)
Dodatkowo zostało ustalone, że:
1. Wyszukiwana fraza ma się składać z pełnych wyrazów
2. Wyszukiwanie ma być case insensitive
3. Odstępy między wyrazami są istotne. Czyli fraza "quick brown" jest inna niż "quick  brown" (2 spacje).

# Rozwiązanie

W opisie zadania były wymienione testy performance więc założyłem, że to jest najistotniejsze
i optymalizowałem pod kątem wydajności kosztem zajętości pamięci.

Zastosowałem asynchroniczny kontroler jako dobry kompromis między szybkościa odpowiedzi,
przepustowością i użyciem wątków. Rozważałem jako alternatywę Spring Webflux ale nie
mam z nim doświadczenia a czas naglił :-)

## Pobranie n-tej linii

Endpoint:
`/get/{id}`

Zostało zrealizowanane bardzo prosto - 'id' jest użyte jako indeks w tablicy wszystkich wierszy tekstu.
Uwaga: indeks jest liczony od '1'. Czyli pierwszy wiersz tekstu to:
`/get/1`

## Wyszukanie frazy:

Endpoint:
`/search?phrase=search+phrase`

Tu rozwiązanie polega na indeksie wszystkich możliwych fraz w tekście. Tworzona jest mapa
phrase -> set of lines (numery wierszy gdzie dana fraza występuje)
W efekcie wyszukianie polega na użyciu parametru 'phrase' jako klucza do indeksu i ze
zbioru wierszy skomponowaniu wyniku.

Ponieważ wymaganie, że odstępy między wyrazami we frazie są istotne dotarło do mnie dopiero
gdy miałem ten mechanizm gotowy to jest on zaprojektowany na ignorowanie tych odstępów.
Nie miałem zabardzo czasu przepisać go od nowa to dodałem po prostu dodatkowy krok w algorytmie,
że wynikowe wiersze są filtrowane na koniec względem wyszukiwanej frazy.

Ponieważ indeks zajmuje bardzo dużo pamięci a jego twożenie jest kosztowne dodałem ograniczenie
na wielkość indeksowanej frazy (parametr '--limit=n' przy uruchamianiu programu). W takim
przypadku indeks zawiera tylko frazy o maksimum 'n' wyrazach. Przy wyszukiwaniu wyszukiwana
fraza jest dzielona na fragmenty o długości maksimum 'n' wyrazów. Z indeksu są pobierane zbiory
wierszy w który występuje każdy z fragmentów i jest wyliczana część wspólna zbioru a potem
algorytm działa już jak normalnie.

### Alternatywny algorytm

Alternatywny indeks w postaci drzewa gdzie każdy liść to token (fragment frazy - słowo, odstęp a może nawet litera) plus zbiór wszystkich wierszy w jakich występuje
fraza złożona ze wszystkich tokenów powyżej aż do korzenia. Dodatkowo w liściu jest mapa 'token' -> 'child' wszystkich potomków danego liścia.

Przykład dla tekstu:
1. a
2. a b
3. a b c


Mielibyśmy drzewo (format liścia 'token' (wiersze w których występuje)):
```
root
 ^- 'a' (1,2,3)
     ^- ' ' (2,3)
         ^- 'b' (2,3)
             ^- ' ' (3)
                 ^- 'c' (3)
 ^- ' ' (2,3)
     ^- 'b' (2,3)
         ^- ' ' (3)
             ^- 'c' (3)
     ^- 'c' (3)
 ^- 'b' (2,3)
     ^- ' ' (3)
         ^- 'c' (3)
 ^- 'c' (3)
```

# Wymagania

1. Java 15
2. Docker
3. 8GB RAM - indeks bez ograniczeń dla "20000 mil podmorskiej żeglugi" zajmuje około 3GB

# Budowanie

Po prostu '`mvnw package`' - jest załączony Maven wrapper.

# Uruchamianie

Uwaga - najpierw trzeba zbudować 'uber jar-a' (punkt budowanie). W '`target/`' powinien się znajdować
`getindata-linserver-0.0.1-SNAPSHOT.jar`

## Uruchamianie programu

Skrypt '`run`'

Przyjmuje parametry (wszystkie opcjonalne):
* `nazwa-pliku` - plik do zaindeksowania. Jeśli pominięty to zostanie użyty 'test.txt'
* `limit=n` - maksymalna długość frazy do zaindeksowania. Musi być to liczba całkowita >= 1. Jeśli pominięty to nie ma ograniczenia.

Przykłady:
* '`run`' - plik wejściowy to 'test.txt' i bez ograniczeń na długość indeksowanej frazy
* '`run 20_000_mil_podmorskiej_zeglugi.txt`' - plik wejściowy to '20_000_mil_podmorskiej_zeglugi.txt' i bez ograniczeń na długość indeksowanej frazy.
* '`run 20_000_mil_podmorskiej_zeglugi.txt --limit=3`' - - plik wejściowy to '20_000_mil_podmorskiej_zeglugi.txt' i makszymalna długość indeksowanej frazy to 3 wyrazy.

## Uruchomienie programu w dockerze

Poprzez 'docker compose up <service>' gdzie 'service' to jedno z:
- '`search-service-test`' - plik źródłowy to '`test.txt`'
- '`search-service-small`' - plik źródłowy to '`small.txt`'
- '`search-service-20000`' - plik źródłowy to '`20_000_mil_podmorskiej_zeglugi.txt`'

Przykład:
`docker-compose up search-service-20000`

Każdy z serwisów ma ograniczenie '`cpus: 1`' - wyjaśnione dokładniej przy opisie testów JMeter

## Uruchomienie wewnętrznego testu performance

Skrypt '`run-performance-test`'

Ten test mierzy tylko wydajność samego wyszukiwania czyli metodę 'SearchService.search()'.
Indeksuje plik '20_000_mil_podmorskiej_zeglugi.txt' i w nim wyszukuje frazę 'Ned Land'.
Robi to w 24 wątkach 100 000 razy w każdym wątku co daje razem 2 400 000.

Skrypt przyjmuje jeden opcjonalny parameter '`--tree`'. Gdy jest on obecny to testuje
`TreeSearchServiceImpl` zamiast domyślnej `IndexSearchServiceImpl`.

Na moim komputerze wydajność była równa:
* `IndexSearchServiceImpl` -> 60 000 wyszukań na sekundę.
* `TreeSearchServiceImpl` -> 80 000 wyszukań na sekundę.

## Uruchomienie testów JMeter

**Uwaga** - najpierw trzeba uruchomić serwis 'search-service-20000' przez 'docker-compose up search-service-20000' i poczekać aż zaindeksuje cały tekstu
wejściowy.

Skrypt '`run-jmeter-test`'

Test JMeter (plik z definicją testów w `docker-jmeter/getindata-linserver.jmx` jest uruchamiany w kontenerze dockerowym.
Wyniki pojawią się jako pliki *.csv (`agregate-graph.csv, response-rime-graph.csv, summary-report.csv`) w katalogu głównym
a także jako dashboard w `docker-jmeter/report/index.html`

Test ten uruchamia 100 wątków (userów) z których każdy pobiera 1 linię tekstu ('`/get/123`') i wyszukuje fraze 'Ned Land'
('`/search?phrase=Ned+Land`') 10 000 razy.

Wyniki to średnio około 550 requestów na sekundę -  tu uwaga, że to przy ograniczeniu 'cpus: 1'.
Bez tego ograniczenia wynik nie chciał się ustabilizować i rósł do ponad 2000 requestów na sekundę.
Najwidoczniej JMeter i search-service-20000 walczyły o procesor.

# Co przydałoby się uzupełnić lub poprawić (a nie udało się z braku czasu).

1. Dodać testy samego kontrolera
2. Dodać komentarze JavaDoc
