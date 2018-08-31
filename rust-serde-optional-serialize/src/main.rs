#[macro_use]
extern crate serde_derive;

extern crate serde;
extern crate serde_json;

#[derive(Serialize)]
struct Serialize<'a> {
    id: &'a str,

    #[serde(skip_serializing_if = "Option::is_none")]
    secret_path: Option<&'a str>
}

fn main() {
    let some = Serialize { id: "some", secret_path: Some("secret.path") };
    let none = Serialize { id: "none", secret_path: None };

    println!("some: {}", serde_json::to_string(&some).unwrap());
    println!("none: {}", serde_json::to_string(&none).unwrap());
}
