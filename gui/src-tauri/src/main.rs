// Prevents additional console window on Windows in release, DO NOT REMOVE!!
#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

use std::{fs, path::Path, process::Command};

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
fn start_exec(path: String) {
    let e_path = path;
    #[cfg(target_os = "windows")]
    {
        e_path = str::replace(e_path.as_str(), "/", "\\");
    }
    let _ = Command::new(e_path).spawn();
}

fn main() {
    #[cfg(any(
        target_os = "linux",
        target_os = "freebsd",
        target_os = "dragonfly",
        target_os = "openbsd",
        target_os = "netbsd"
    ))]
    std::env::set_var("WEBKIT_DISABLE_COMPOSITING_MODE", "1");

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
            remove_dir,
            check_dir_exists,
            start_exec
        ])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
