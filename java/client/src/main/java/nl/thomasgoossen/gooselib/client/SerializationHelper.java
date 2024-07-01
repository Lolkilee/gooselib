package nl.thomasgoossen.gooselib.client;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;

public class SerializationHelper {
    public static String seriliazeToString(Object obj) {
        String ret;
        try {
            ObjectMapper mapper = new ObjectMapper();
            ret = mapper.writeValueAsString(obj);
        } catch (IOException e) {
            ret = "{\"error\": \"" + e.toString() + "\"}";
        }

        return ret;
    }

    public static String jsonError(Exception e) {
        String s = "{\"error\": \"" + e.toString() + "\"}";
        return s;
    }

    public static String jsonError(String msg) {
        String s = "{\"error\": \"" + msg + "\"}";
        return s;
    }
}
