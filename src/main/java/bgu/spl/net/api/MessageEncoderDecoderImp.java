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

    private short opCode = 0;
    private int indexOfZero= 0; //index of \0
    private  Queue<Short> shortParts;
    private Queue<String> stringParts;
    private Queue<Byte> byteParts;
    private short numOfUsers = -1;
    private byte[] bytes = new byte[1 << 10]; //start with 1k
    private int len = 0;

    @Override
    public Message decodeNextByte(byte nextByte) {
        pushByte(nextByte);
        Message msg = null;
        if(len == 2 & opCode == 0) {
            byte[] opCodeArr = new byte[2];
            opCodeArr[0] = bytes[0];
            opCodeArr[1] = bytes[1];
            this.opCode = bytesToShort(opCodeArr);
            shortParts = new LinkedBlockingQueue<>();
            stringParts = new LinkedBlockingQueue<>();
            byteParts = new LinkedBlockingQueue<>();
            shortParts.add(opCode);
        }
        if(opCode !=0) {

            switch (opCode) {
                case 1:
                    msg = createRegisterOrLogin(nextByte);
                    break;
                case 2:
                    msg = createRegisterOrLogin(nextByte);
                    break;
                case 3:
                    msg = createLogoutOrUserList(nextByte);
                    break;
                case 4:
                    msg = createFollow(nextByte);
                    break;
                case 5:
                    msg = createPostOrStat(nextByte);
                    break;
                case 6:
                    msg = createPm(nextByte);
                    break;
                case 7:
                    msg = createLogoutOrUserList(nextByte);
                    break;
                case 8:
                    msg = createPostOrStat(nextByte);
                default:
                    break;
            }
        }
        if(msg != null){
            opCode = 0;
            indexOfZero = 0;
            numOfUsers = -1;
            len = 0;
            bytes = new byte[1 << 10];
        }
        return msg;
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
        byte[] re = new byte[4];
        byte[] b = shortToBytes(ACKOP);
        re[0] = b[0];
        re[1] = b[1];
        b = shortToBytes(senderOP);
        re[2] = b[0];
        re[3] = b[1];
        if (senderOP == 4 | senderOP == 7) {
            result = getBytes47(senderOP, message, re);
        } else if (senderOP == 8) {
            result = new byte[10];
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
            int s = message.getStrings().size();
            for (int k = 0; k < s; k++) {
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
        result = new byte[q.size() + 6];
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
        return result;
    }

    private byte[] enMError(Message message) {
        Short ACKOP = message.getShorts().poll();
        Short senderOP = message.getShorts().poll();
        byte[] re = new byte[4];
        byte[] b = shortToBytes(ACKOP);
        re[0] = b[0];
        re[1] = b[1];
        b = shortToBytes(senderOP);
        re[2] = b[0];
        re[3] = b[1];
        return re;
    }

    private byte[] enMNotifications(Message message) {
        Short ACKOP = message.getShorts().poll();
        Queue<Byte> q = new LinkedList<>();
        byte t = '\0';
        //char ch = message.getStrings().poll().charAt(0);
        byte[] result;
        byte[] b = shortToBytes(ACKOP);
        q.add(b[0]);
        q.add(b[1]);
        b = message.getStrings().poll().getBytes();
        //get the char
        for(int i =0;i < b.length;i++){
            q.add(b[i]);
        }
        b = message.getStrings().poll().getBytes();
        //get the user
        for(int i =0;i < b.length;i++){
            q.add(b[i]);
        }
        //add the bye
        q.add(t);
        b = message.getStrings().poll().getBytes();
        //get the content
        for(int i =0;i < b.length;i++){
            q.add(b[i]);
        }
        q.add(t);
        int z = q.size();
        result = new byte[z];
        for(int i =0;i < z;i++){
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



    private Message createRegisterOrLogin(byte b) {
        if (b == '\0') {
            indexOfZero++;
        }
        if (indexOfZero == 2) {
            String message = new String(bytes, 2, len - 2, StandardCharsets.UTF_8);
            int index = message.indexOf('\0');
            String username = message.substring(0, index);
            message = message.substring(index + 1);
            index = message.indexOf('\0');
            String password = message.substring(0, index);
            stringParts.add(username);
            stringParts.add(password);
            return new Message(shortParts, stringParts, byteParts);
        }
        return null;
    }

    private Message createLogoutOrUserList(byte b) {
        return new Message(shortParts, stringParts, byteParts);

    }

    private Message createFollow(byte b) {
        //num of users to follow
        if(len==5){
            byte[] numOfUsersbytes = new byte[2]; //numberOfUsers
            numOfUsersbytes[0] = bytes[3];
            numOfUsersbytes[1] = bytes[4];
            numOfUsers = bytesToShort(numOfUsersbytes);
        }
        if(b=='\0' & len > 5)
            indexOfZero++;
        if(indexOfZero == numOfUsers){
            byteParts.add(bytes[2]); //follow/unfollow
            shortParts.add(numOfUsers);
            String users = new String(bytes, 5, len-5, StandardCharsets.UTF_8);
            int indexOfNextChar = 0;
            for (int i = 0; i < users.length(); i = i + 1) {
                if (users.charAt(i) == '\0') {
                    stringParts.add(users.substring(indexOfNextChar, i));
                    indexOfNextChar = i + 1;
                }
            }
            return new Message(shortParts, stringParts, byteParts);
        }
        return null;
    }

    private Message createPostOrStat(byte b) {
        if(b=='\0') {
            Queue<String> stringParts = new LinkedBlockingQueue<>();
            String s = new String(bytes, 2, len - 2, StandardCharsets.UTF_8);
            stringParts.add(s.substring(0, s.indexOf('\0')));
            return new Message(shortParts, stringParts, new LinkedBlockingQueue<Byte>());
        }
        return null;
    }

    private Message createPm(byte b) {
        if(b=='\0')
            indexOfZero++;
        if(indexOfZero == 2) {
            String message = new String(bytes, 2, len - 2, StandardCharsets.UTF_8);
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
        return null;
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