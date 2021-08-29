#[macro_use]
extern crate lazy_static;
#[macro_use]
extern crate rocket;

use std::{
    collections::{HashMap, HashSet},
    fs, str,
};

use rocket::response::status::NotFound;

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
        l += 1;
        if l % 100 == 0 {
            println!("Indexed line {} from {}", l, line_count);
        }
    }
    println!("Indexed line {} from {}", l, line_count);
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

fn search_with_result<'a>(phrase: &'a str, root: &'a Node) -> Result<String, NotFound<String>> {
    let result = search(phrase, root);
    if let Some(result) = result {
        //result.iter().join("\n")
        Ok(join(result, "\n"))
    } else {
        Err(NotFound(format!("Can't find phrase '{}'", phrase)))
    }
}

fn join<'a, T>(str_collection: &'a T, sep: &str) -> String
    where
        T: 'a,
        &'a T: IntoIterator<Item=&'a &'a str>,
{
    let sep_count = str_collection.into_iter().count() - 1;
    let result_len = str_collection.into_iter().map(|s| s.len()).sum::<usize>() + sep_count * sep.len();
    let mut result = String::with_capacity(result_len);
    str_collection.into_iter().enumerate().for_each(|(i, s)| {
        result.push_str(s);
        if i < sep_count {
            result.push_str(sep);
        }
    });
    result
}

fn read_to_string(path: &str) -> &'static str {
    let source = fs::read_to_string(path).unwrap();
    Box::leak(Box::new(source))
}

fn to_lowercase(txt: &str) -> &'static str {
    let txt_lowercase = txt.to_lowercase();
    Box::leak(Box::new(txt_lowercase))
}

#[get("/")]
fn r_index() -> &'static str {
    "Hello, world!"
}

#[get("/get/<line>")]
fn get_line(line: usize) -> Result<&'static str, NotFound<String>> {
    if line < SOURCE_LINES.len() {
        println!("Get line {}", line);
        Ok(SOURCE_LINES[line])
    } else {
        let msg = format!("Can't find line {}", line);
        println!("{}", msg);
        Err(NotFound(msg))
    }
}

#[get("/search?<phrase>")]
fn search_phrase(phrase: &str) -> Result<String, NotFound<String>> {
    println!("Search for phrase '{}'", phrase);
    search_with_result(phrase, &ROOT)
}

#[launch]
fn rocket() -> _ {
    lazy_static::initialize(&ROOT);
    let figment = rocket::Config::figment()
        .merge(("port", 8080))
        .merge(("address", "0.0.0.0"));
    rocket::custom(figment).mount("/", routes![r_index, get_line, search_phrase])
}

#[cfg(test)]
mod tests {
    use std::{
        env::current_dir,
        sync::Arc,
        thread::{self, JoinHandle},
    };

    use num_format::{Locale, ToFormattedString};
    use stopwatch::Stopwatch;

    use super::*;

    #[test]
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
                    let _ = search_with_result(phrase, &local_index_source);
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

    #[test]
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
            println!("{}", result.unwrap_or("empty".to_string()));
            println!("^^^^^^^^^^");
        }
        println!("Current dir: {:?}", current_dir().unwrap());
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
        */
    }
}
