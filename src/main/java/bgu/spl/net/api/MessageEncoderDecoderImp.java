package bgu.spl.net.api;

import bgu.spl.net.api.bidi.Message;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;

public class MessageEncoderDecoderImp implements MessageEncoderDecoder<Message> {

    private byte[] bytes = new byte[1 << 10]; //start with 1k
    private int len = 0;

    @Override
    public Message decodeNextByte(byte nextByte) {
        //notice that the top 128 ascii characters have the same representation as their utf-8 counterparts
        //this allow us to do the following comparison
        if (nextByte == '\n') {
            return createMessage();
        }

        pushByte(nextByte);
        return null; //not a line yet
    }

    @Override
    public byte[] encode(Message message) {
        byte[] encoded = new byte[1];
        Short type = message.getShorts().peek();
        switch (type) {
            case 9:
                encoded = enMNotifications(message);
                break;
            case 10:
                encoded = enMACK(message);
                break;
            case 11:
                encoded = enMError(message);
                break;

        }
        String s = encoded.toString();
        return encoded;
    }

    private byte[] enMACK(Message message) {
        Short ACKOP = message.getShorts().poll();
        Short senderOP = message.getShorts().poll();
        byte[] result;
        byte[] re = new byte[5];
        byte[] b = shortToBytes(ACKOP);
        re[0] = b[0];
        re[1] = b[1];
        b = shortToBytes(senderOP);
        re[2] = b[0];
        re[3] = b[1];
        re[4] = '\n';
        if (senderOP == 4 | senderOP == 7) {
            result = getBytes47(senderOP, message, re);
        } else if (senderOP == 8) {
            result = new byte[11];
            for (int i = 0; i < 4; i++) {
                result[i] = re[i];
            }
            //numPosts
            b = shortToBytes(message.getShorts().poll());
            result[4] = b[0];
            result[5] = b[1];
            //numfollowers
            b = shortToBytes(message.getShorts().poll());
            result[6] = b[0];
            result[7] = b[1];
            //numfollowing
            b = shortToBytes(message.getShorts().poll());
            result[8] = b[0];
            result[9] = b[1];
            result[10] = re[4];
        } else {
            result = re;
        }
        return result;
    }

    private byte[] getBytes47(Short senderOP, Message message, byte[] re) {
        Queue<Byte> q = new LinkedList<>();
        byte[] result;
        byte t = '\0';
        //for each string encode amd add to queue
        if(message.getStrings().size() != 0){
            for (int k = 0; k < message.getStrings().size(); k++) {
                byte[] stBites = message.getStrings().poll().getBytes();
                for (int j = 0; j < stBites.length; j++) {
                    q.add(stBites[j]);
                }
                //add the zero byte
                q.add(t);
            }}
        else {
            q.add(t);
        }
        result = new byte[q.size() + 7];
        //add the op codes
        for (int i = 0; i < 4; i++) {
            result[i] = re[i];
        }
        //numusers
        byte[] b;
        b = shortToBytes(message.getShorts().poll());
        result[4] = b[0];
        result[5] = b[1];
        int conAdd = 6;
        //add the strings and byts
        while (!q.isEmpty()) {
            result[conAdd] = q.poll();
            conAdd++;
        }
        result[conAdd] = '\n';
        return result;
    }

    private byte[] enMError(Message message) {
        Short ACKOP = message.getShorts().poll();
        Short senderOP = message.getShorts().poll();
        byte[] re = new byte[5];
        byte[] b = shortToBytes(ACKOP);
        re[0] = b[0];
        re[1] = b[1];
        b = shortToBytes(senderOP);
        re[2] = b[0];
        re[3] = b[1];
        re[4] = '\n';
        return re;
    }

    private byte[] enMNotifications(Message message) {
        Short ACKOP = message.getShorts().poll();
        Queue<Byte> q = new LinkedList<>();
        byte t = '\0';
        byte e = '\n';
        //char ch = message.getStrings().poll().charAt(0);
        byte[] result;
        byte[] b = shortToBytes(ACKOP);
        q.add(b[0]);
        q.add(b[1]);
        b = message.getStrings().poll().getBytes();
        for(int i =0;i < b.length;i++){
            q.add(b[i]);
        }
        b = message.getStrings().poll().getBytes();
        for(int i =0;i < b.length;i++){
            q.add(b[i]);
        }
        q.add(t);
        for(int i =0;i < b.length;i++){
            q.add(b[i]);
        }
        q.add(t);
        q.add(e);
        result = new byte[q.size()];
        for(int i =0;i < q.size();i++){
            result[i] = q.poll();
        }
        return result;
    }

    private void pushByte(byte nextByte) {
        if (len >= bytes.length) {
            bytes = Arrays.copyOf(bytes, len * 2);
        }

        bytes[len++] = nextByte;
    }

    private Message createMessage() {
        //notice that we explicitly requesting that the string will be decoded from UTF-8
        //this is not actually required as it is the default encoding in java.
        Message msg = null;
        byte[] opCodeArr = new byte[2];
        opCodeArr[0] = bytes[0];
        opCodeArr[1] = bytes[1];
        short opCode = bytesToShort(opCodeArr);
        Queue<Short> shortParts = new LinkedBlockingQueue<>();
        shortParts.add(opCode);
        switch (opCode) {
            case 1:
                msg = createRegisterOrLogin(shortParts);
                break;
            case 2:
                msg = createRegisterOrLogin(shortParts);
                break;
            case 3:
                msg = createLogoutOrUserList(shortParts);
                break;
            case 4:
                msg = createFollow(shortParts);
                break;
            case 5:
                msg = createPostOrStat(shortParts);
                break;
            case 6:
                msg = createPm(shortParts);
                break;
            case 7:
                msg = createLogoutOrUserList(shortParts);
                break;
            case 8:
                msg = createPostOrStat(shortParts);
            default:
                break;
        }
        len = 0;
        bytes = new byte[1 << 10];
        return msg;
    }


    private Message createRegisterOrLogin(Queue<Short> shortParts) {
        String message = new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_8);
        int index = message.indexOf('\0');
        String username = message.substring(0, index);
        message = message.substring(index + 1);
        index = message.indexOf('\0');
        String password = message.substring(0, index);
        Queue<String> stringParts = new LinkedBlockingQueue<>();
        stringParts.add(username);
        stringParts.add(password);
        return new Message(shortParts, stringParts, new LinkedBlockingQueue<Byte>());

    }

    private Message createLogoutOrUserList(Queue<Short> shortParts) {
        return new Message(shortParts, new LinkedBlockingQueue<>(), new LinkedBlockingQueue<>());

    }

    private Message createFollow(Queue<Short> shortParts) {
        Queue<Byte> bytesParts = new LinkedBlockingQueue<>();
        Queue<String> stringParts = new LinkedBlockingQueue<>();
        bytesParts.add(bytes[2]); //follow/unfollow

        byte[] numOfUsers = new byte[2]; //numberOfUsers
        numOfUsers[0] = bytes[3];
        numOfUsers[1] = bytes[4];
        shortParts.add(bytesToShort(numOfUsers));

        String users = new String(bytes, 5, bytes.length-5, StandardCharsets.UTF_8);
        int indexOfNextChar = 0;
        for (int i = 0; i < users.length(); i = i + 1) {
            if (users.charAt(i) == '\0') {
                stringParts.add(users.substring(indexOfNextChar, i));
                indexOfNextChar = i + 1;
            }
        }
        return new Message(shortParts, stringParts, bytesParts);

    }

    private Message createPostOrStat(Queue<Short> shortParts) {
        Queue<String> stringParts = new LinkedBlockingQueue<>();
        stringParts.add(new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_8));
        return new Message(shortParts, stringParts, new LinkedBlockingQueue<Byte>());
    }

    private Message createPm(Queue<Short> shortParts) {
        String message = new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_8);
        int index = message.indexOf('\0');
        String username = message.substring(0, index);
        message = message.substring(index + 1);
        index = message.indexOf('\0');
        String content = message.substring(0, index);
        Queue<String> stringParts = new LinkedBlockingQueue<>();
        stringParts.add(username);
        stringParts.add(content);
        return new Message(shortParts, stringParts, new LinkedBlockingQueue<Byte>());
    }


    public short bytesToShort(byte[] byteArr) {
        short result = (short) ((byteArr[0] & 0xff) << 8);
        result += (short) (byteArr[1] & 0xff);
        return result;
    }

    public byte[] shortToBytes(short num) {
        byte[] bytesArr = new byte[2];
        bytesArr[0] = (byte) ((num >> 8) & 0xFF);
        bytesArr[1] = (byte) (num & 0xFF);
        return bytesArr;
    }
}