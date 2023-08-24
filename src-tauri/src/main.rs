// Prevents additional console window on Windows in release, DO NOT REMOVE!!
#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

use std::fs;
use std::path::Path;

#[tauri::command]
fn check_dir_exists(dir: String) -> bool {
    Path::new(&dir).is_dir()
}

#[tauri::command]
fn remove_dir(dir: String) {
    let _ = fs::remove_dir_all(dir);
}

#[tokio::main]
async fn main() {
    tauri::Builder::default()
        .invoke_handler(tauri::generate_handler![check_dir_exists, remove_dir])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
