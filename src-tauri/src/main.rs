// Prevents additional console window on Windows in release, DO NOT REMOVE!!
#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

use futures_util::StreamExt;
use std::cmp::min;
use std::env;
use std::fs::File;
use std::io::Write;
use tokio;
use tokio::runtime::Runtime;

static mut DOWNLOAD_PROGRESS: u64 = 0;

pub async fn svr_download(url_in: String, path_in: String) -> Result<(), String> {
    let url: &str = url_in.as_str();
    let path: &str = path_in.as_str();

    println!("{:?}", std::env::current_exe());
    println!("{}", url);
    println!("{}", path);

    // Reqwest setup
    let res = reqwest::Client::new()
        .get(url)
        .send()
        .await
        .or(Err(format!("Failed to GET from '{}'", &url)))?;
    let total_size = res
        .content_length()
        .ok_or(format!("Failed to get content length from '{}'", &url))?;

    // download chunks
    let mut file = File::create(path).or(Err(format!("Failed to create file '{}'", path)))?;
    let mut downloaded: u64 = 0;
    let mut stream = res.bytes_stream();

    while let Some(item) = stream.next().await {
        let chunk = item.or(Err(format!("Error while downloading file")))?;
        file.write_all(&chunk)
            .or(Err(format!("Error while writing to file")))?;
        let new = min(downloaded + (chunk.len() as u64), total_size);
        downloaded = new;
    }

    println!("Got to the end!");
    return Ok(());
}

#[tauri::command]
fn get_download_progress() -> u64 {
    unsafe { DOWNLOAD_PROGRESS }
}

#[tauri::command]
async fn download_app(url: String, path: String) {
    let rt = Runtime::new().unwrap();
    let _handle = rt.spawn(svr_download(url, path));
    println!("{}", _handle.await.unwrap_err().to_string());
}

#[tokio::main]
async fn main() {
    tauri::Builder::default()
        .invoke_handler(tauri::generate_handler![
            download_app,
            get_download_progress
        ])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
