use std::{collections::{HashMap, HashSet}, env::{current_dir}, fs, str};
use stopwatch::Stopwatch;

#[derive(Debug)]
struct Node<'a> {
    token: &'a str,
    lines: HashSet<&'a str>,
    children: HashMap<&'a str, Node<'a>>,
}

fn tokenizer(t: &str) -> Vec<&str> {
    let mut r = Vec::with_capacity(512);
    let mut li = 0;
    let mut i = 0;
    let mut last_was_whitespace = false;
    for c in t.chars() {
        if c.is_whitespace() != last_was_whitespace && li < i {
            r.push(&t[li..i]);
            li = i;
        }
        last_was_whitespace = c.is_whitespace();
        i += c.len_utf8();
    }
    r.push(&t[li..]);
    r
}

#[allow(dead_code)]
fn f_tokenizer(t: &str) -> Vec<&str> {
    let mut r = Vec::with_capacity(512);
    let (li, _) = t.chars().enumerate().fold(
        (0, false),
        |(li, last_was_whitespace), (i, c)|
            if c.is_whitespace() != last_was_whitespace && li < i {
                r.push(&t[li..i]);
                (i, c.is_whitespace())
            } else {
                (li, c.is_whitespace())
            },
    );
    r.push(&t[li..]);
    r
}

fn index(txt: &str) -> Node {
    let mut root = Node {
        token: "root",
        lines: Default::default(),
        children: Default::default(),
    };
    let mut l = 0;
    let line_count = txt.lines().count();
    for line in txt.lines() {
        root = index_line(line, root);
        println!("Indexed line {} from {}", l, line_count);
        l += 1;
    };
    root
}

fn index_line<'a>(line: &'a str, mut root: Node<'a>) -> Node<'a> {
    let tokenized_line = tokenizer(line);
    for i in 0..tokenized_line.len() {
        let mut node = &mut root;
        for token in &tokenized_line[i..] {
            node = node.children.entry(token).or_insert_with(|| Node {
                token,
                lines: Default::default(),
                children: Default::default(),
            });
            node.lines.insert(line);
        }
    }
    root
}

fn search<'a>(phrase: &'a str, root: &'a Node) -> Option<&'a HashSet<&'a str>> {
    let tokenized_phrase = tokenizer(phrase);
    let mut node = root;
    for token in tokenized_phrase {
        if !node.children.contains_key(token) {
            return None;
        }
        node = node.children.get(token).unwrap();
    }
    Some(&node.lines)
}

fn main() {
    let mut stopwatch = Stopwatch::new();
    let txt = "a
a b
a b c";
    let phrases = vec!["a", "b", "c", "a b", "b c", "a b c", " ", "non existent"];
    println!("-------------");
    println!("'{}'", txt);
    println!("-------------");
    let root = index(txt);
    println!("{:#?}", root);
    for phrase in phrases {
        let lines = search(phrase, &root);
        println!("'{}' -> {:?}", phrase, lines.unwrap_or(&Default::default()));
    }
    println!("Current dir: {:?}", current_dir().unwrap());
    let source_txt = fs::read_to_string("../20_000_mil_podmorskiej_zeglugi.txt").unwrap();
    println!("Read source text - {} lines", source_txt.lines().count());
    stopwatch.start();
    let indexed_source = index(source_txt.as_str());
    stopwatch.stop();
    let indexing_took = stopwatch.elapsed();
    let phrase = "Ned Land";
    stopwatch.restart();
    let how_many_search = 1_000_000;
    for _ in 0..how_many_search {
        search(phrase, &indexed_source);
    }
    let lines = search(phrase, &indexed_source);
    stopwatch.stop();
    let search_took = stopwatch.elapsed();
    println!("Found '{}' in {} lines", phrase, lines.unwrap().len());
    println!("Indexing took {:?}", indexing_took);
    println!("Searching took {:?}", search_took);
    println!("Perform {}/s searches", how_many_search as f64 / search_took.as_micros() as f64 * 1_000_000f64);
}
