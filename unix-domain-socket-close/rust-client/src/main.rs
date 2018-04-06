use std::io::{Read, Write};
use std::os::unix::net::UnixStream;

fn main() {
    let mut stream = UnixStream::connect("/tmp/garbage-socket").unwrap();
    stream.write_all(b"hello world").unwrap();
    stream.shutdown(std::net::Shutdown::Write).unwrap();
    let mut response = String::new();
    stream.read_to_string(&mut response).unwrap();
    println!("{}", response);
}
