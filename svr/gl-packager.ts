// Packager script for folder to .app conversion
// Input ./gl-packager [input folder] [upload password] [app name] [version] [url]

import { existsSync } from "https://deno.land/std@0.198.0/fs/exists.ts";
import { tar, GzipStream } from "https://deno.land/x/compress@v0.4.4/mod.ts";

const tmpFile = "./tmp";
const swapFile = "./swap";

export async function pack(input: string, passServer: string, appName: string, version: string, url: string) {
    try {
        // Compress to tar
        await tar.compress(input, tmpFile);

        // Compress to gzip
        const gzip = new GzipStream();
        gzip.on("progress", (progress: string) => {
            console.log("compression progress: " + progress); // 0.00% => 100.00%
        });
        await gzip.compress(tmpFile, swapFile);
        await Deno.rename(swapFile, tmpFile);
        
        // Send post request to server
        const file = await Deno.open(tmpFile, { read: true });
        const body = file.readable;
        const resp = await fetch(url + "/upload", {
            method: "POST",
            headers: {
                "app-name": appName,
                "app-version": version,
                "pw": passServer
            },
            body
        });
        console.log(await resp.text());

        // Cleanup
        if (existsSync(tmpFile))
            await Deno.remove(tmpFile);
        if (existsSync(swapFile))
            await Deno.remove(swapFile);
        
        // Server refresh
        await fetch(url + "/refresh", {
            headers: {
                "pw": passServer
            }
        });
    } catch (err) {
        console.log(err);
    }
}

if (Deno.args[0] == "help" || Deno.args.length != 5) {
    console.log("Command format: [input folder] [upload password] [app name] [version] [url] \n Example: ./gl-packager password ./SomeFolder SomeApp version1 http://localhost:8765")
}
else {
    const input = Deno.args[0];
    const pass = Deno.args[1];
    const appName = Deno.args[2];
    const version = Deno.args[3];
    const url = Deno.args[4];

    pack(input, pass, appName, version, url);
}