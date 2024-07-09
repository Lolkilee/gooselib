package nl.thomasgoossen.gooselib.client;

import java.nio.file.Files;
import java.nio.file.Paths;

import com.esotericsoftware.minlog.Log;
import static com.esotericsoftware.minlog.Log.LEVEL_ERROR;

import io.javalin.Javalin;
import nl.thomasgoossen.gooselib.shared.messages.HandshakeResp;

public class GLClient {

    private static final int PORT = 7123;
    private static final String UPLOAD_FILE = "temp.tar.gz";

    private static String username;
    private static String password;

    private static ConnectionInstance connection;

    public static void main(String[] args) {
        Log.set(LEVEL_ERROR);
        Javalin app = Javalin.create();
        app = statusHandler(app);
        app = stopHandler(app);
        app = setUsernameHandler(app);
        app = setPasswordHandler(app);
        app = handshakeHandler(app);
        app = uploadHandler(app);
        app.start(PORT);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.out.println("running shutdown hook");
                if (connection != null)
                    connection.stop();
            }
        });
    }

    private static Javalin statusHandler(Javalin app) {
        return app.get("/", ctx -> ctx.result("http server is running"));
    }

    private static Javalin stopHandler(Javalin app) {
        return app.get("/stop", ctx -> app.stop());
    }

    private static Javalin setUsernameHandler(Javalin app) {
        return app.post("/set-user/{name}", ctx -> {
            username = ctx.pathParam("name");
            ctx.result("Set username to: " + username);
        });
    }

    private static Javalin setPasswordHandler(Javalin app) {
        return app.post("/set-password/{password}", ctx -> {
            password = ctx.pathParam("password");
            ctx.result("Set password to: " + password);
        });
    }

    private static Javalin handshakeHandler(Javalin app) {
        return app.post("/handshake/{ip}", ctx -> {
            if (connection != null) {
                connection.stop();
            }

            String ip = ctx.pathParam("ip");
            HandshakeResp resp = Handshake.performHandshake(ip, username, password);
            connection = new ConnectionInstance(ip, resp);
            ctx.json(resp);
        });
    }

    // Path should be formatted between [] brackets, and ! instead of /
    private static Javalin uploadHandler(Javalin app) {
        return app.post("/upload/{path}", ctx -> {
            if (connection != null) {
                String path = ctx.pathParam("path");
                path = path.replace("[", "");
                path = path.replace("]", "");
                path = path.replace("!", "/");
                System.out.println(Paths.get(path).toAbsolutePath());
                ctx.result("path exists: " + Files.isDirectory(Paths.get(path)));
            } else {
                ctx.result("connection not initialized");
            }
        });
    }
}
