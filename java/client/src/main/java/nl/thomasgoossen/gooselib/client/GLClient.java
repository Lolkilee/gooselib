package nl.thomasgoossen.gooselib.client;

import com.esotericsoftware.minlog.Log;
import static com.esotericsoftware.minlog.Log.LEVEL_ERROR;

public class GLClient {

    /*  Input args:
     *  [command] <command args...>
     *  handshake <ip> <username> <password>
     */
    public static void main(String[] args) {
        Log.set(LEVEL_ERROR);
        System.out.println(getOutput(args));
    }

    private static String getOutput(String[] args) {
        if (args.length < 1)
            return SerializationHelper.jsonError("no args found");
        
        switch (args[0].toLowerCase()) {
            case "handshake" -> {
                if (args.length == 4)
                    return Handshake.performHandshake(args[1], args[2], args[3]);
                else
                    return SerializationHelper.jsonError("invalid args count for handshake");
            }
            default -> {
                return SerializationHelper.jsonError("command switch failure");
            }
        }
    }
}
