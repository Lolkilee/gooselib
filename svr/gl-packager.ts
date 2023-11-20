// Packager script for folder to .app conversion
// Input ./gl-packager [input folder] [upload password] [app name] [version] [url]

import { existsSync } from "https://deno.land/std@0.198.0/fs/exists.ts";
import { tar } from "https://deno.land/x/compress@v0.4.4/mod.ts";

const tmpFile = "./tmp";
const tmpCmpFile = "./tmp.gz";

async function pack(input: string, passServer: string, appName: string, version: string, url: string) {
    try {
        // Compress to tar
        console.log("Compressing (1/2)");
        await tar.compress(input, tmpFile);
        console.log("Compressing (2/2)")
        const uncFile = await Deno.open(tmpFile, { read: true });
        const cmpFile = await Deno.open(tmpCmpFile, { create: true, write: true });
        await uncFile.readable.pipeThrough(new CompressionStream("gzip")).pipeTo(cmpFile.writable);

        // Cleanup (1)
        if (existsSync(tmpFile))
            await Deno.remove(tmpFile);
        
        // Send post request to server
        const file = await Deno.open(tmpCmpFile, { read: true });
        const body = file.readable;
        console.log("Uploading...");
        const resp = await fetch(url + "/upload", {
            method: "POST",
            headers: {
                "app-name": appName,
                "app-version": version,
                "pw": passServer
            },
            body: body
        });

        // Cleanup (2)
        if (existsSync(tmpCmpFile))
            await Deno.remove(tmpCmpFile);
        
        // Server refresh
        await fetch(url + "/refresh", {
            headers: {
                "pw": passServer
            }
        });
        console.log(await resp.text());
    } catch (err) {
        console.log(err);
    }
}

if (Deno.args[0] == "help" || Deno.args.length != 5) {
    console.log("Command format: [input folder] [upload password] [app name] [version] [url] \n Example: ./gl-packager ./SomeFolder password SomeApp version1 http://localhost:8765")
}
else {
    const input = Deno.args[0];
    const pass = Deno.args[1];
    const appName = Deno.args[2];
    const version = Deno.args[3];
    const url = Deno.args[4];

    pack(input, pass, appName, version, url);
}