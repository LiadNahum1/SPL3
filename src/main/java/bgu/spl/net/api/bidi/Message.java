package bgu.spl.net.api.bidi;

import java.util.Queue;
import java.util.Vector;

public class Message {
    Queue<Short> shorts;
    Queue<Byte> bytes;
    Queue<String> strings;

    public Message(Queue<Short> shorts , Queue<Byte> bytes , Queue <String> strings) {
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

