#[macro_use]
extern crate criterion;

use criterion::Criterion;
use std::fs::File;
use std::path::Path;

fn check_file_exists() {
    let path = Path::new("the-file-does-not-exist");
    let _ = path.exists();
}

fn open_missing_file() {
    let path = Path::new("the-file-does-not-exist");
    let _ = File::open(&path);
}


fn criterion_benchmark(c: &mut Criterion) {
    c.bench_function("check_file_exists", |b| b.iter(|| check_file_exists()));
    c.bench_function("open_missing_file", |b| b.iter(|| open_missing_file()));
}

criterion_group!(benches, criterion_benchmark);
criterion_main!(benches);
