use std::{collections::{HashMap, HashSet}, str};
use std::borrow::Borrow;

#[derive(Debug)]
struct Node<'a> {
    token: &'a str,
    lines: HashSet<&'a str>,
    children: HashMap<&'a str, Node<'a>>,
}

fn tokenizer(t: &str) -> Vec<&str> {
    let mut r = Vec::with_capacity(512);
    let mut li = 0;
    let mut last_was_whitespace = false;
    for (i, c) in t.chars().enumerate() {
        if c.is_whitespace() != last_was_whitespace && li < i {
            r.push(&t[li..i]);
            li = i;
        }
        last_was_whitespace = c.is_whitespace();
    }
    r.push(&t[li..]);
    r
}

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
    for line in txt.lines() {
        root = index_line(line, root);
    };
    root
}

fn index_line<'a>(line: &'a str, mut root: Node<'a>) -> Node<'a> {
    let mut tokenized_line = tokenizer(line);
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
    let txt = "a
a b
a b c";
    let lines: Vec<&str> = txt.lines().collect();
    println!("-------------");
    println!("'{}'", txt);
    println!("-------------");
    let root = index(txt);
    println!("{:#?}", root);
    let lines = search("b", &root);
    println!("{:#?}", lines);
}
