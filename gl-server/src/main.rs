use actix_web::{get, web, App, HttpResponse, HttpServer, Responder};
use chrono::prelude::*;
use openssl::ssl::{SslAcceptor, SslFiletype, SslMethod};

mod database;

// File path constants
const KEY_PATH: &str = "./creds/key.pem";
const CERT_PATH: &str = "./creds/cert.pem";
const DB_PATH: &str = "database";

// Basic status route
#[get("/")]
async fn status() -> impl Responder {
    HttpResponse::Ok().body("gooselib service is running")
}

#[actix_web::main]
async fn main() -> std::io::Result<()> {
    // Load TLS certificates
    let mut builder = SslAcceptor::mozilla_intermediate(SslMethod::tls()).unwrap();
    builder
        .set_private_key_file(KEY_PATH, SslFiletype::PEM)
        .unwrap();
    builder.set_certificate_chain_file(CERT_PATH).unwrap();

    // Initialize database
    let db = sled::open(DB_PATH).unwrap();
    let start_time: String = Utc::now().to_string();
    let _ = db.insert(b"last-start", start_time.as_bytes());

    // Start the HTTP server
    HttpServer::new(move || {
        App::new()
            .app_data(web::Data::new(db.clone()))
            .service(database::last_start)
            .service(status)
    })
    .bind_openssl("0.0.0.0:8765", builder)?
    .run()
    .await
}
