use std::fs;
use std::io::{Read, Write};
use std::thread;
use std::os::unix::net::{UnixStream, UnixListener};

fn handle_client(mut stream: UnixStream) {
    let mut message = String::new();
    stream.read_to_string(&mut message).unwrap();
    println!("read message: {}", message);
    stream.write_all(&message.as_bytes()).unwrap();
    stream.shutdown(std::net::Shutdown::Both).unwrap();
}

fn main() {
    let _ = fs::remove_file("/tmp/garbage-socket");
    let listener = UnixListener::bind("/tmp/garbage-socket").unwrap();
    for stream in listener.incoming() {
        match stream {
            Ok(stream) => {
                thread::spawn(|| handle_client(stream));
            }
            Err(err) => {
                eprintln!("error: {}", err);
                break;
            }
        }
    }
}
