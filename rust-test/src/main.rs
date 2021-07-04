use std::{collections::{HashMap, HashSet}, str};

struct Node<'a> {
    token: &'a str,
    lines: HashSet<&'a str>,
    children: HashMap<&'a str, &'a Node<'a>>,
}

fn tokenizer(t: &str) -> Vec<&str> {
    let mut r = Vec::new();
    let mut li = 0;
    let mut last_was_whitespace = false;
    for (i, c) in t.chars().enumerate() {
        if c.is_whitespace() != last_was_whitespace {
                r.push(&t[li..i]);
                li = i;
        }
        last_was_whitespace = c.is_whitespace();
    }
    r.push(&t[li..]);
    r
}

fn main() {
    let txt = "the
quick brown
fox jumps   over the
lazy dog
Jerzy Brzęczyszczykiewicz";
    let lines: Vec<&str> = txt.lines().collect();
    println!("-------------");
    println!("'{}'", txt);
    println!("-------------");
    for (i, l) in lines.into_iter().enumerate() {
        println!("{}: '{}'", i, l);
        let from_tokenizer = tokenizer(l);
        for (ip, lp) in from_tokenizer.iter().enumerate() {
            println!("   {}: '{}'", ip, lp);
        }
    }
}
