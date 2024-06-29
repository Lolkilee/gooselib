package nl.thomasgoossen.gooselib.util;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class EncryptedPacketSerializer extends Serializer<EncryptedPacket> {
    @Override
    public void write(Kryo kryo, Output output, EncryptedPacket object) {
        output.writeInt(object.getDataLength());
        output.writeBytes(object.getData());
    }

    @Override
    public EncryptedPacket read(Kryo kryo, Input input, Class<EncryptedPacket> type) {
        int l = input.readInt();
        byte[] data = input.readBytes(l);
        return new EncryptedPacket(data);
    }
    
}
