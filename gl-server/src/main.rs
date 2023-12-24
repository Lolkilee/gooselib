use actix_web::{get, App, HttpResponse, HttpServer, Responder};
use openssl::ssl::{SslAcceptor, SslFiletype, SslMethod};

// File path constants
const KEY_PATH: &str = "./creds/key.pem";
const CERT_PATH: &str = "./creds/cert.pem";

// Basic status route
#[get("/")]
async fn status() -> impl Responder {
    HttpResponse::Ok().body("gooselib service is running")
}

#[actix_web::main]
async fn main() -> std::io::Result<()> {
    let mut builder = SslAcceptor::mozilla_intermediate(SslMethod::tls()).unwrap();
    builder
        .set_private_key_file(KEY_PATH, SslFiletype::PEM)
        .unwrap();
    builder.set_certificate_chain_file(CERT_PATH).unwrap();

    HttpServer::new(|| App::new().service(status))
        .bind_openssl("0.0.0.0:8765", builder)?
        .run()
        .await
}
