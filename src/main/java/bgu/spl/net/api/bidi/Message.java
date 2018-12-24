package bgu.spl.net.api.bidi;

import java.util.Queue;
import java.util.Vector;

public class Message {
    short kind;
    Queue<Byte> bytes;
    Queue<String> strings;

    public Message(short kind , Queue<Byte> bytes , Queue <String> strings) {
        this.bytes = bytes;
        this.strings = strings;
        }

    public short getKind() {
        return kind;
    }

    public Queue<Byte> getBytes() {
        return bytes;
    }

    public Queue<String> getStrings() {
        return strings;
    }


}

