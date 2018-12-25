package bgu.spl.net.api.bidi;

import java.util.Queue;
import java.util.Vector;

public class Message {
    private Queue<Short> shorts;
    private Queue<Byte> bytes;
    private Queue<String> strings;

    public Message(Queue<Short> shorts , Queue <String> strings, Queue<Byte> bytes) {
        this.bytes = bytes;
        this.strings = strings;
        this.shorts = shorts;
        }

    public Queue<Short> getShorts() {
        return shorts;
    }

    public Queue<Byte> getBytes() {
        return bytes;
    }

    public Queue<String> getStrings() {
        return strings;
    }


}

