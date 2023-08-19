import * as path from "https://deno.land/std@0.188.0/path/mod.ts";
import {existsSync} from "https://deno.land/std@0.198.0/fs/mod.ts";

const __dirname = path.dirname(path.fromFileUrl(import.meta.url));
const FILES_FOLDER = "/files";

class AppDefinition {
    name: string;
    versions: string[];

    constructor(name: string, versions: string[]) {
        this.name = name;
        this.versions = versions;
    }
}

class Library {
    apps: AppDefinition[];

    constructor(apps: AppDefinition[]) {
        this.apps = apps;
    }

    addDefinition(def: AppDefinition) {
        this.apps.push(def);
    }

    removeDefinition(name: string) {
        let index = -1;
        for (let i = 0; i < this.apps.length; i++) {
            if (this.apps[i].name == name)
                index = i;
        }

        if (index >= 0)
            this.apps.splice(index, 1);
    }

    async rebuild() {
        const arr: AppDefinition[] = [];
        let appCount = 0;
        let vCount = 0;
        
        for await (const dirEntry of Deno.readDir(__dirname + FILES_FOLDER + "/")) {
            if (dirEntry.isDirectory) {
                const versions: string[] = []
                appCount++;
                
                for await (const vEntry of Deno.readDir(__dirname + FILES_FOLDER + "/" + dirEntry.name)) {
                    if (vEntry.name.includes(".app") && vEntry.isFile) {
                        versions.push(vEntry.name.replace(".app", ""));
                        vCount++;
                    }
                }

                arr.push(new AppDefinition(dirEntry.name, versions));
            }
        }

        console.log("Rebuilt app library with " + appCount + " apps and " + vCount + " total versions.");
        this.apps = arr;
    }
}

async function updateMetaData() {
    // Create the file to write to
    if (!existsSync(__dirname + FILES_FOLDER + "/apps.json")) {
        const metaFile = await Deno.open(__dirname + FILES_FOLDER + "/apps.json", { createNew: true, write: true });
        metaFile.close();
    }
    
    // Rebuild the app library
    const arr: AppDefinition[] = []
    const lib: Library = new Library(arr);
    await lib.rebuild();

    // Write the json data to the meta file
    const data = JSON.stringify(lib);
    await Deno.writeTextFile(__dirname + FILES_FOLDER + "/apps.json", data);
}

async function startServer() {
    const PORT = 8765;
    const server = Deno.listen({ port: PORT });
    console.log("Started http server on port " + PORT);

    for await (const conn of server) {
        serveHttp(conn);
    }
}

async function serveHttp(conn: Deno.Conn) {
    const httpConn = Deno.serveHttp(conn);
    for await (const requestEvent of httpConn) {
        
        //url is filepath
        const url = new URL(requestEvent.request.url);
        
        // Base request returns app.json file
        if (url.pathname == "/") {
            const res = new Response(Deno.readTextFileSync(__dirname + FILES_FOLDER + "/apps.json"), {
                status: 200,
                headers: {
                    "content-type": "application/json; charset=utf-8"
                }
            });
            await requestEvent.respondWith(res);
        }
        
        // Refresh the meta data
        else if (url.pathname == "/refresh") {
            const res = new Response("Refreshing meta data", { status: 202 });
            await requestEvent.respondWith(res);
            updateMetaData();
        }
        
        // TODO: UPLOAD POST REQUEST
        /*
        else if (url.pathname == "/upload" && requestEvent.request.method == "POST") {

        } */
        
        // Else serve file if exists on that path
        else {
            const filepath = decodeURIComponent(url.pathname);

            // Check if file exists and open it
            let file;
            try {
                file = await Deno.open(__dirname + FILES_FOLDER + filepath, { read: true });
            } catch {
                const notFoundResponse = new Response("404 Not Found", { status: 404 });
                await requestEvent.respondWith(notFoundResponse);
                continue;
            }

            // Send file back to request
            const readableStream = file.readable;
            const response = new Response(readableStream);
            await requestEvent.respondWith(response);
        }
    }
}

updateMetaData();
startServer();