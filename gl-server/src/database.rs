// File containing all functions related to the database

use actix_web::{get, web, HttpResponse, Responder};

// Testing route to check database
#[get("/db")]
async fn last_start(db: web::Data<sled::Db>) -> impl Responder {
    let val = db
        .get(b"last-start")
        .unwrap_or_else(|_| Some(sled::IVec::from(b"undefined")));
    let val_vec: Vec<u8> = val.map_or_else(|| Vec::new(), |v| v.as_ref().to_vec());
    HttpResponse::Ok()
        .body("Last database start: ".to_string() + &String::from_utf8(val_vec).unwrap())
}
