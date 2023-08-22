// Packager script for folder to .app conversion
// Input ./gl-packager [input folder] [file password] [upload password] [app name] [version] [url]

import { tar } from "https://deno.land/x/compress@v0.4.4/mod.ts";
import { lock } from "https://cdn.jsdelivr.net/gh/hexagon/lock@0.9.9/mod.ts";
import { existsSync } from "https://deno.land/std@0.198.0/fs/mod.ts";

const tmpFile = "./tmp";
const pFile = "./enc-part";
const encFile = "./enc";

export async function pack(pass: string, input: string, appName: string, version: string, url: string, passServer: string) {
    try {
        // Compress
        await tar.compress(input, tmpFile);
        
        // Check if encFile does not exists and if it does delete
        if (existsSync(encFile))
            await Deno.remove(encFile);

        // Encrypt
        const cmpFile = await Deno.open(tmpFile, { read: true });
        const eFile = await Deno.open(encFile, {write: true, createNew: true})

        for await (const chunk of cmpFile.readable) {  
            // Write to part file
            const partFile = await Deno.open(pFile, { write: true, createNew: true });
            await partFile.write(chunk);
            partFile.close();

            await lock(pFile, false, true, true, false, pass);
            const partFileRead = await Deno.open(pFile + ".lock", { read: true });
            for await (const pChunk of partFileRead.readable) {
                await eFile.write(pChunk);
            }
            //partFileRead.close();
            Deno.remove(pFile + ".lock");
        }
        eFile.close();

        // Rename file so tmpFile can be reused
        await Deno.remove(tmpFile);
        await Deno.rename(encFile, tmpFile);

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
        await Deno.remove(tmpFile);
        await fetch(url + "/refresh");
    } catch (err) {
        console.log(err);
    }
}

if (Deno.args[0] == "help" || Deno.args.length != 6) {
    console.log("Command format: [input folder] [file password] [upload password] [app name] [version] [url] \n Example: ./gl-packager passwordFile passwordServer ./SomeFolder SomeApp version1 http://localhost:8765")
}
else {
    const input = Deno.args[0];
    const pass = Deno.args[1];
    const passServer = Deno.args[2];
    const appName = Deno.args[3];
    const version = Deno.args[4];
    const url = Deno.args[5];

    pack(pass, input, appName, version, url, passServer);
}