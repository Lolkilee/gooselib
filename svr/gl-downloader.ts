// Downloader script that is embedded in the tauri client
// Handles downloading, encryption and unpacking
// Args: [url] [path (folder)] [password]
// Progress is sent through stdout json

import { tar } from "https://deno.land/x/compress@v0.4.4/mod.ts";
import { lock } from "https://cdn.jsdelivr.net/gh/hexagon/lock@0.9.9/mod.ts";

let status = "idle";
let progress = 0;

function logProgess() {
    console.log(JSON.stringify({ status: status, progress: progress }));
}

async function installApp(url: string, path: string, password: string) {
    const interval = setInterval(() => { logProgess(); }, 1);

    const tmpFile = "./tmp.tar";
    progress = 0;
    try {
        // Download file from server
        status = "downloading";
        const res = await fetch(url);
        const file = await Deno.open(tmpFile + ".lock", { create: true, write: true });

        let lHeader = res.headers.get("Content-Length");
        if (lHeader == null)
            lHeader = "0";
        
        const contentLength = parseInt(lHeader);
        let bytesWritten = 0;
        if (res.body != null) {
            for await (const chunk of res.body) {
                await file.write(chunk);
                bytesWritten += chunk.length;
                progress = (bytesWritten / contentLength) * 79;
            }
        }
        file.close();

        // Decrypt file
        status = "decrypting";
        progress = 85;
        await lock(tmpFile + ".lock", true, true, true, false, password);
        
        // Uncompress tar to folder
        status = "installing";
        progress = 95;
        await tar.uncompress(tmpFile, path);
        
        // Delete tmp file
        status = "done";
        progress = 100;
        await Deno.remove(tmpFile);
        logProgess();
    } catch (err) {
        console.log(err);
    }

    clearInterval(interval);
}

console.log(Deno.args);

if (Deno.args.length == 3) {
    const url = Deno.args[0];
    const path = Deno.args[1];
    const password = Deno.args[2];
   
    installApp(url, path, password);
} else {
    console.log("Invalid arguments!");
}