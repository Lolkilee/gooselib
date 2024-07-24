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
    let mut e_path = path;
    #[cfg(target_os = "windows")]
    {
        e_path = str::replace(e_path.as_str(), "/", "\\");
    }
    println!("{}", e_path);
    let _ = Command::new(e_path).spawn();
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
            start_exec,
            open_path
        ])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
