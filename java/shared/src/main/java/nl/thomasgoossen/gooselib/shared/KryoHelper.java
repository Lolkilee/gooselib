package nl.thomasgoossen.gooselib.shared;

import com.esotericsoftware.kryo.Kryo;

public class KryoHelper {
    public static void addRegisters(Kryo kryo) {
        kryo.register(EncryptedPacket.class, new EncryptedPacketSerializer());
    }
}
