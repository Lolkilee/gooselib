import { existsSync } from "https://deno.land/std@0.198.0/fs/mod.ts";

// Default upload & download password
// Becomes a hash string on startup
let password = "password";
let uploadPassword = password;
const FILES_FOLDER = "./files";

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
        
        for await (const dirEntry of Deno.readDir(FILES_FOLDER + "/")) {
            if (dirEntry.isDirectory) {
                const versions: string[] = []
                appCount++;
                
                for await (const vEntry of Deno.readDir(FILES_FOLDER + "/" + dirEntry.name)) {
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
    if (!existsSync(FILES_FOLDER + "/apps.json")) {
        const metaFile = await Deno.open(FILES_FOLDER + "/apps.json", { createNew: true, write: true });
        metaFile.close();
    }
    
    // Rebuild the app library
    const arr: AppDefinition[] = []
    const lib: Library = new Library(arr);
    await lib.rebuild();

    // Write the json data to the meta file
    const data = JSON.stringify(lib);
    await Deno.writeTextFile(FILES_FOLDER + "/apps.json", data);
}

async function startServer() {
    if (!existsSync(FILES_FOLDER))
        await Deno.mkdir(FILES_FOLDER);

    const PORT = 8765;
    const server = Deno.listen({ port: PORT });
    console.log("Started http server on port " + PORT);

    for await (const conn of server) {
        try {
            serveHttp(conn);
        } catch (err) {
            console.log(err);
        }
    }
}

async function serveHttp(conn: Deno.Conn) {
    try {
        const httpConn = Deno.serveHttp(conn);
        for await (const requestEvent of httpConn) {
            try {
                //url is filepath
                const url = new URL(requestEvent.request.url);
        
                if (requestEvent.request.headers.get("pw")! == password || requestEvent.request.headers.get("pw")! == uploadPassword) {
                    // Base request returns app.json file
                    if (url.pathname == "/") {
                        const res = new Response(Deno.readTextFileSync(FILES_FOLDER + "/apps.json"), {
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
        
                    // Upload file
                    else if (url.pathname == "/upload" && requestEvent.request.method == "POST"
                        && requestEvent.request.headers.has("app-name") && requestEvent.request.headers.has("app-version")
                        && requestEvent.request.headers.has("pw")) {
            
                        const badReqResp = new Response("400 Bad Request", { status: 400 });
                        const pw = requestEvent.request.headers.get("pw");

                        if (pw != null) {
                            if (pw == uploadPassword) {
                                const appName = requestEvent.request.headers.get("app-name");
                                const verName = requestEvent.request.headers.get("app-version");

                                if (appName != null && verName != null) {
                                

                                    if (!existsSync(FILES_FOLDER + "/" + appName))
                                        await Deno.mkdir(FILES_FOLDER + "/" + appName);

                                    if (!existsSync(FILES_FOLDER + "/" + appName + "/" + verName + ".app")) {
                                        const tF = await (Deno.open(FILES_FOLDER + "/" + appName + "/" + verName + ".app", { write: true, createNew: true }));
                                        tF.close();
                                    }
                                
                                    const file = await Deno.open(FILES_FOLDER + "/" + appName + "/" + verName + ".app", { write: true });
                                    await requestEvent.request.body?.pipeTo(file.writable);
                                    await requestEvent.respondWith(new Response("Upload accepted", { status: 200 }));
                                } else {
                                    await requestEvent.respondWith(badReqResp);
                                }
                            } else {
                                const forbiddenResp = new Response("403 Forbidden", { status: 403 });
                                await requestEvent.respondWith(forbiddenResp);
                            }
                        } else {
                            await requestEvent.respondWith(badReqResp);
                        }
                    }
        
                    // Else serve file if exists on that path
                    else {
                        const filepath = decodeURIComponent(url.pathname);

                        // Check if file exists and open it
                        let file: Deno.FsFile;
                        try {
                            file = await Deno.open(FILES_FOLDER + filepath, { read: true });
                        } catch {
                            const notFoundResponse = new Response("404 Not Found", { status: 404 });
                            await requestEvent.respondWith(notFoundResponse);
                            continue;
                        }

                        // Calculate length of file
                        const content_length = file.statSync().size.toString();

                        // Send file back to request
                        const readableStream = file.readable;
                        const response = new Response(readableStream, { headers: { "Content-Length": content_length } });
                        await requestEvent.respondWith(response);
                    }
                } else {
                    const response = new Response("403 Forbidden", { status: 403 });
                    await requestEvent.respondWith(response);
                }
            } catch (err) {
                requestEvent.respondWith(new Response("500 Internal Server Error; \n" + err, { status: 500 }));
            }
        }
    } catch (err) {
        console.log(err);
    }
}

if (Deno.args.length != 2) {
    console.log("Starting server with default passwords (`password`) \nTo start the server with a password pass the download password as the first argument and the upload password as the second");
} else {
    password = Deno.args[0];
    uploadPassword = Deno.args[1];
}

updateMetaData();
startServer();