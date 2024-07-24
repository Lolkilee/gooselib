package nl.thomasgoossen.gooselib.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.esotericsoftware.minlog.Log;
import static com.esotericsoftware.minlog.Log.LEVEL_ERROR;

import io.javalin.Javalin;
import nl.thomasgoossen.gooselib.shared.AppMetaData;
import nl.thomasgoossen.gooselib.shared.messages.HandshakeResp;
import nl.thomasgoossen.gooselib.shared.messages.LibInfoReq;
import nl.thomasgoossen.gooselib.shared.messages.SetExecPathReq;

public class GLClient {
    private static final int PORT = 7123;
    private static final int TIMEOUT = 2000;

    private static AppMetaData[] metaData = null;
    private static volatile boolean metaSignal = false;

    private static String username;
    private static String password;

    private static volatile ConnectionInstance connection;
    private static volatile long lastReq;

    public static void main(String[] args) {
        Log.set(LEVEL_ERROR);
        Javalin app = Javalin.create();
        app = lastReqUpdater(app);
        app = statusHandler(app);
        app = stopHandler(app);
        app = setUsernameHandler(app);
        app = setPasswordHandler(app);
        app = handshakeHandler(app);
        app = downloadHandler(app);
        app = downloadProgressHandler(app);
        app = uploadHandler(app);
        app = uploadStatusHandler(app);
        app = metaDataHandler(app);
        app = connectionStatusHandler(app);
        app = setExecPathHandler(app);
        app.start(PORT);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.out.println("running shutdown hook");
                if (connection != null)
                    connection.stop();

                Download.kill();
            }
        });

        if (!checkFlag(args, "notimeout")) {
            Runnable checkTimeout = () -> {
                if (System.currentTimeMillis() - lastReq > TIMEOUT) {
                    System.out.println("Timeout, exiting..");
                    System.exit(0);
                }
            };
            lastReq = System.currentTimeMillis();
            ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
            executor.scheduleAtFixedRate(checkTimeout, 0, 100, TimeUnit.MILLISECONDS);
        }
    }

    private static boolean checkFlag(String[] args, String flag) {
        for (String a : args) {
            if (a.toLowerCase().replace("-", "").equals(flag.toLowerCase()))
                return true;
        }

        return false;
    }

    private static Javalin lastReqUpdater(Javalin app) {
        return app.before(ctx -> {
            lastReq = System.currentTimeMillis();
        });
    }

    private static Javalin statusHandler(Javalin app) {
        return app.get("/", ctx -> ctx.result("http server is running"));
    }

    private static Javalin stopHandler(Javalin app) {
        return app.get("/stop", ctx -> {
            ctx.result("Ok, stopping..");
            new java.util.Timer().schedule( 
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        app.stop();
                        System.exit(0);
                    }
                }, 
                100 
            );
        });
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
            if (!Handshake.isHandshaking && username != null && password != null) {
                if (connection != null) {
                    connection.stop();
                }

                String ip = ctx.pathParam("ip");
                HandshakeResp resp = null;
                try {
                    resp = Handshake.performHandshake(ip, username, password);
                } catch (IOException | InterruptedException e) {
                    ctx.json(new ErrorWrapper(e.getMessage()));
                }

                if (resp != null) {
                    connection = new ConnectionInstance(ip, resp);
                    ctx.json(resp);
                } else {
                    ctx.json(new ErrorWrapper(Handshake.getError()));
                }
            } else {
                ctx.json(new ErrorWrapper("already handshaking..."));
            }
        });
    }

    // Path should be formatted between [] brackets, and ! instead of /
    private static Javalin downloadHandler(Javalin app) {
        return app.post("/download/{appName}/{dst}", ctx -> {
            if (connection != null && metaData != null) {
                String dst = ctx.pathParam("dst");
                String appName = ctx.pathParam("appName");

                int appIndex = -1;
                int i = 0;
                for (AppMetaData meta : metaData) {
                    if (meta.name.equals(appName)) {
                        appIndex = i;
                        break;
                    }
                    i++;
                }

                dst = dst.replace("[", "");
                dst = dst.replace("]", "");
                dst = dst.replace("!", "/");

                System.out.println("downloading to: " + Paths.get(dst).toAbsolutePath());
                if (appIndex >= 0 && appIndex < metaData.length) {
                    AppMetaData meta = metaData[appIndex];
                    Download d = new Download(meta, dst);
                    System.out.println("started download instance with name: "
                            + d.getName());
                    ctx.result("started download with name " + meta.name);
                } else {
                    ctx.result("could not start download");
                }
            }
        });
    }

    private static Javalin downloadProgressHandler(Javalin app) {
        return app.get("/download-progress", ctx -> {
            ctx.json(Download.getDownloadInfos());
        });
    }

    // Path should be formatted between [] brackets, and ! instead of /, * instead of :
    private static Javalin uploadHandler(Javalin app) {
        return app.post("/upload/{path}/{name}", ctx -> {
            if (connection != null) {
                String path = ctx.pathParam("path");
                String name = ctx.pathParam("name");
                String version = new SimpleDateFormat("dd-MM HH:mm:ss").format(new Date());

                path = path.replace("[", "");
                path = path.replace("]", "");
                path = path.replace("!", "/");
                path = path.replace("*", ":");
                System.out.println("starting upload with vars:");
                System.out.println("path: " + Paths.get(path).toAbsolutePath());
                System.out.println("name: " + name);
                System.out.println("version: " + version);

                final String lPath = path;

                if (Files.isDirectory(Paths.get(path))) {
                    ctx.result("starting upload");
                    new Thread(() -> {
                        Upload.upload(password, lPath, name, version);
                    }).start();
                } else {
                    String absPath = Paths.get(lPath).toAbsolutePath().toString();
                    ctx.result("path " + absPath + " is not a folder");
                }
            } else {
                ctx.result("connection not initialized");
            }
        });
    }

    private static Javalin uploadStatusHandler(Javalin app) {
        return app.get("/upload-status", ctx -> {
            ctx.result(Upload.getStatus());
        });
    }

    private static Javalin metaDataHandler(Javalin app) {
        return app.get("/meta", ctx -> {
            if (connection != null) {
                LibInfoReq req = new LibInfoReq(username, password);
                sendPacketTCP(req);

                long startTime = System.currentTimeMillis();
                while (!metaSignal) {
                    if (System.currentTimeMillis() - startTime > 5000)
                        break;
                }
            }

            if (metaData != null)
                ctx.json(metaData);
            else
                ctx.json(new AppMetaData[0]);
        });
    }

    private static Javalin connectionStatusHandler(Javalin app) {
        return app.get("/connection", ctx -> {
            boolean b = (connection != null);
            if (!b)
                ctx.json(new ConnectionStatus(b));
            else
                ctx.json(new ConnectionStatus(connection.isConnected()));
        });
    }
    
    private static Javalin setExecPathHandler(Javalin app) {
        return app.post("/set-exec-path/{appName}/{path}/{adminPass}", ctx -> {
            String appName = ctx.pathParam("appName");
            String path = ctx.pathParam("path");
            String adminPass = ctx.pathParam("adminPass");
            path = path.replace("[", "");
            path = path.replace("]", "");
            path = path.replace("!", "/");
            System.out.println(appName);
            System.out.println(path);
            System.out.println(adminPass);
            SetExecPathReq req = new SetExecPathReq(appName, path, adminPass);
            sendPacketTCP(req);
            ctx.result("sent request");
        });
    }

    public static void sendPlainPacketTCP(Object data) {
        if (connection != null) {
            connection.sendPlainPacketTCP(data);
        }
    }
    public static void sendPlainPacketUDP(Object data) {
        if (connection != null) {
            connection.sendPlainPacketUDP(data);
        }
    }

    public static void sendPacketTCP(Object data) {
        if (connection != null) {
            connection.sendPacketTCP(data);
        }
    }

    public static void sendPacketUDP(Object data) {
        if (connection != null) {
            connection.sendPacketUDP(data);
        }
    }

    public static String getPassword() {
        return password;
    }

    public static String getUsername() {
        return username;
    }

    public static void setMetaSignal() {
        metaSignal = true;
    }

    public static void setMetaData(AppMetaData[] metaData) {
        GLClient.metaData = metaData;
    }
}
