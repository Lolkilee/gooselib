// Prevents additional console window on Windows in release, DO NOT REMOVE!!
#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

extern crate fs_extra;

use fs_extra::dir::get_size;
use std::fs;
use std::path::Path;
use std::process::Command;
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
    let mut size = 0;
    if check_dir_exists(dir.clone()) {
        let folder_size = get_size(dir).unwrap();
        size = folder_size;
    }
    size
}

#[tauri::command]
fn open_path(path: String) {
    // https://stackoverflow.com/questions/66485945/with-rust-open-explorer-on-a-file

    println!("{}", path);

    #[cfg(target_os = "macos")]
    {
        Command::new("open").arg(path).spawn().unwrap();
    }

    #[cfg(target_os = "windows")]
    {
        let win_path = str::replace(path.as_str(), "/", "\\");
        Command::new("explorer").arg(win_path).spawn().unwrap();
    }

    #[cfg(target_os = "linux")]
    {
        Command::new("xdg-open").arg(path).spawn().unwrap();
    }
}

#[tauri::command]
fn start_exec(path: String) {
    let mut e_path = path;
    #[cfg(target_os = "windows")]
    {
        e_path = str::replace(e_path.as_str(), "/", "\\");
    }
    let _ = Command::new(e_path).spawn();
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
            get_dir_size,
            open_path,
            start_exec
        ])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
