#[macro_use]
extern crate lazy_static;
#[macro_use]
extern crate rocket;

use std::{
    collections::{HashMap, HashSet},
    env::current_dir,
    fs, str,
    sync::Arc,
    thread::{self, JoinHandle},
};

use num_format::{Locale, ToFormattedString};
use stopwatch::Stopwatch;


//use itertools::Itertools;

const SOURCE_TXT_PATH: &str = "20_000_mil_podmorskiej_zeglugi.txt";

lazy_static! {
    static ref SOURCE_TXT: &'static str = read_to_string(SOURCE_TXT_PATH);
    static ref ROOT: Node<'static> = index(&SOURCE_TXT);
    static ref SOURCE_LINES: Vec<&'static str> = {
        let mut result: Vec<&'static str> = Vec::new();
        for l in SOURCE_TXT.lines() {
            result.push(l);
        }
        result
    };
}

#[derive(Debug)]
struct Node<'a> {
    token: &'a str,
    lines: HashSet<&'a str>,
    children: HashMap<&'a str, Node<'a>>,
}

fn tokenizer(t: &str) -> Vec<&str> {
    let t_lowercase = to_lowercase(t);
    let mut r = Vec::with_capacity(512);
    let mut li = 0;
    let mut i = 0;
    let mut last_was_alphanumeric = false;
    for c in t.chars() {
        if c.is_alphanumeric() != last_was_alphanumeric && li < i {
            r.push(&t_lowercase[li..i]);
            li = i;
        }
        last_was_alphanumeric = c.is_alphanumeric();
        i += c.len_utf8();
    }
    r.push(&t_lowercase[li..]);
    r
}

// Warning - this tokenizer can't handle UTF8
#[allow(dead_code)]
fn f_tokenizer(t: &str) -> Vec<&str> {
    let mut r = Vec::with_capacity(512);
    let (li, _) = t
        .chars()
        .enumerate()
        .fold((0, false), |(li, last_was_alphanumeric), (i, c)| {
            if c.is_alphanumeric() != last_was_alphanumeric && li < i {
                r.push(&t[li..i]);
                (i, c.is_alphanumeric())
            } else {
                (li, c.is_alphanumeric())
            }
        });
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
    }
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

fn search_with_result<'a>(phrase: &'a str, root: &'a Node) -> String {
    let empty: &HashSet<&str> = &HashSet::new();
    let result = search(phrase, root).unwrap_or(empty);
    //result.iter().join("\n")
    join(result, "\n")
}

fn read_to_string(path: &str) -> &'static str {
    let source = fs::read_to_string(path).unwrap();
    Box::leak(Box::new(source))
}

fn to_lowercase(txt: &str) -> &'static str {
    let txt_lowercase = txt.to_lowercase();
    Box::leak(Box::new(txt_lowercase))
}

#[allow(dead_code)]
fn performance_test() {
    let mut stopwatch = Stopwatch::new();
    let source_txt = read_to_string(SOURCE_TXT_PATH);
    println!("Read source text - {} lines", source_txt.lines().count());
    stopwatch.start();
    let indexed_source = index(source_txt);
    stopwatch.stop();
    let indexing_took = stopwatch.elapsed();
    let index_source = Arc::new(indexed_source);
    let mut handles: Vec<JoinHandle<()>> = Default::default();
    let how_many_search = 10_000;
    let how_many_threads = 24;
    stopwatch.restart();
    for _ in 0..how_many_threads {
        let local_index_source = index_source.clone();
        let handle = thread::spawn(move || {
            let phrase = "Ned Land";
            for _ in 0..how_many_search {
                //search(phrase, &local_index_source);
                search_with_result(phrase, &local_index_source);
            }
        });
        handles.push(handle);
    }
    for h in handles {
        h.join().unwrap();
    }
    stopwatch.stop();
    let phrase = "Ned Land";
    let lines = search(phrase, &index_source);
    let search_took = stopwatch.elapsed();
    println!("Found '{}' in {} lines", phrase, lines.unwrap().len());
    println!("Indexing took {:?}", indexing_took);
    println!("Searching took {:?}", search_took);
    println!(
        "Perform {}/s searches",
        ((how_many_search as f64 * how_many_threads as f64 / search_took.as_micros() as f64
            * 1_000_000f64) as i128)
            .to_formatted_string(&Locale::en)
    );
}

#[allow(dead_code)]
fn basic_test() {
    let txt = "a
a b
a b c
A
A b
A b c
A
A B
A b c
A
A B
A B c
A
A B
A B 
Jerzy Brzęczyszczykiewicz
";
    let phrases = vec!["a", "b", "c", "a b", "b c", "a b c", " ", "non existent"];
    let set_of_phrases: HashSet<&str> = phrases.into_iter().collect();
    let result = join(&set_of_phrases, ",");
    println!("set_of_phrases: {:#?}", set_of_phrases);
    println!("result: {}", result);

    println!("-------------");
    println!("'{}'", txt);
    println!("-------------");
    let root = index(txt);
    println!("{:#?}", root);
    for phrase in set_of_phrases {
        let empty: &HashSet<&str> = &HashSet::new();
        let lines = search(phrase, &root).unwrap_or(empty);
        let result = search_with_result(phrase, &root);
        println!("'{}' -> {:?}", phrase, lines);
        println!("vvvvvvvvvv");
        println!("{}", result);
        println!("^^^^^^^^^^");
    }
    println!("Current dir: {:?}", current_dir().unwrap());
}

fn join(set_of_str: &HashSet<&str>, sep: &str) -> String {
    let sep_count = set_of_str.len() - 1;
    let result_len: usize =
        set_of_str.iter().map(|s| s.len()).sum::<usize>() + sep_count * sep.len();
    let mut result = String::with_capacity(result_len);
    set_of_str.iter().enumerate().for_each(|(i, s)| {
        result.push_str(s);
        if i < sep_count {
            result.push_str(sep);
        }
    });
    result
}

#[get("/")]
fn r_index() -> &'static str {
    "Hello, world!"
}

#[get("/get/<line>")]
fn get_line(line: usize) -> String {
    let result = format!("Ask for line {}", line);
    println!("{}", result);
    result
}

#[get("/search?<phrase>")]
fn search_phrase(phrase: &str) -> String {
    let result = format!("Search for phrase '{}'", phrase);
    println!("{}", result);
    result
}

#[launch]
fn rocket() -> _ {
    let figment = rocket::Config::figment().merge(("port", 8080));

    rocket::custom(figment).mount("/", routes![r_index, get_line, search_phrase])
}

#[test]
fn main_test() {
     //performance_test();
     println!("Start");
     println!("Initializing...");
     lazy_static::initialize(&ROOT);
     println!("Line 10: {}", SOURCE_LINES[10]);
/* 
     let phrase = "Ned Land";
     let result = search_with_result(phrase, &ROOT);
     println!("Phrase '{}' found in lines:\n{}", phrase, result);
 */}
