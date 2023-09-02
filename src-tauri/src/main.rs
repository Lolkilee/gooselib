// Prevents additional console window on Windows in release, DO NOT REMOVE!!
#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

extern crate fs_extra;

use fs_extra::dir::get_size;
use std::fs;
use std::path::Path;
use tauri::Manager;

#[tauri::command]
fn check_dir_exists(dir: String) -> bool {
    Path::new(&dir).is_dir()
}

#[tauri::command]
fn remove_dir(dir: String) {
    let _ = fs::remove_dir_all(dir);
}

#[tauri::command]
fn get_dir_size(dir: String) -> u64 {
    let folder_size = get_size(dir).unwrap();
    folder_size
}

fn main() {
    tauri::Builder::default()
        .setup(|app| {
            #[cfg(debug_assertions)] // only include this code on debug builds
            {
                let window = app.get_window("main").unwrap();
                window.open_devtools();
            }
            Ok(())
        })
        .invoke_handler(tauri::generate_handler![
            check_dir_exists,
            remove_dir,
            get_dir_size
        ])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
