async function startServer() {
    const PORT = 8765;
    const server = Deno.listen({ port: PORT });
    console.log("Started http server on port " + PORT);

    for await (const conn of server) {
        serveHttp(conn);
    }
}

async function serveHttp(conn: Deno.Conn) {
    
}



startServer();