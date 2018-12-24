package bgu.spl.net.api;

import bgu.spl.net.api.bidi.Message;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

//TODO: CHECK WHAT TO DO
public class MessageEncoderDecoderImp implements MessageEncoderDecoder<Message>{

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
        return new byte[0];
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
        byte[]opCodeArr = new byte[2];
        opCodeArr[0] = bytes[0];
        opCodeArr[1] = bytes[1];
        short opCode = bytesToShort(opCodeArr);
        Queue<Short> shortParts = new LinkedBlockingQueue<>();
        shortParts.add(opCode);
        switch(opCode){
            case 1:
                msg = createRegisterOrLogin(shortParts);
                break;
            case 2:
                msg = createRegisterOrLogin(shortParts);
                break;
            case 3:
                msg= createLogoutOrUserList(shortParts);
                break;
            case 4:
                msg= createFollow(shortParts);
                break;
            case 5:
                msg= createPostOrStat(shortParts);
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
       return msg;
    }



    private Message createRegisterOrLogin(Queue<Short> shortParts) {
        String message = new String(bytes, 2, bytes.length-2, StandardCharsets.UTF_8);
        int index = message.indexOf('\0');
        String username = message.substring(0,index);
        message = message.substring(index+1);
        index = message.indexOf('\0');
        String password = message.substring(0,index);
        Queue<String> stringParts = new LinkedBlockingQueue<>();
        stringParts.add(username);
        stringParts.add(password);
        return new Message(shortParts, stringParts, new LinkedBlockingQueue<Byte>());

    }
    private Message createLogoutOrUserList(Queue<Short> shortParts) {
        return new Message(shortParts, new LinkedBlockingQueue<>(), new LinkedBlockingQueue<>());

    }
    private Message createFollow(Queue<Short> shortParts) {
        Queue<Byte>bytesParts = new LinkedBlockingQueue<>();
        Queue<String> stringParts = new LinkedBlockingQueue<>();
        bytesParts.add(bytes[2]); //follow/unfollow

        byte[] numOfUsers = new byte[2]; //numberOfUsers
        numOfUsers[0] = bytes[3];
        numOfUsers[1] = bytes[4];
        shortParts.add(bytesToShort(numOfUsers));

        String users = new String(bytes, 5, bytes.length-1, StandardCharsets.UTF_8);
        int indexOfNextChar =0;
        for(int i = 0; i<users.length(); i = i+1){
            if(users.charAt(i)== '\0'){
                stringParts.add(users.substring(indexOfNextChar, i));
                indexOfNextChar = i+1;
            }
        }
        return new Message(shortParts, stringParts, bytesParts);

    }
    private Message createPostOrStat(Queue<Short> shortParts) {
        Queue<String> stringParts = new LinkedBlockingQueue<>();
        stringParts.add(new String(bytes, 2, bytes.length-1, StandardCharsets.UTF_8));
        return new Message(shortParts, stringParts, new LinkedBlockingQueue<Byte>());
    }

    private Message createPm(Queue<Short> shortParts) {
        String message = new String(bytes, 2, bytes.length-2, StandardCharsets.UTF_8);
        int index = message.indexOf('\0');
        String username = message.substring(0,index);
        message = message.substring(index+1);
        index = message.indexOf('\0');
        String content = message.substring(0,index);
        Queue<String> stringParts = new LinkedBlockingQueue<>();
        stringParts.add(username);
        stringParts.add(content);
        return new Message(shortParts, stringParts, new LinkedBlockingQueue<Byte>());
    }



    public short bytesToShort(byte[] byteArr)
    {
        short result = (short)((byteArr[0] & 0xff) << 8);
        result += (short)(byteArr[1] & 0xff);
        return result;
    }

    public byte[] shortToBytes(short num)
    {
        byte[] bytesArr = new byte[2];
        bytesArr[0] = (byte)((num >> 8) & 0xFF);
        bytesArr[1] = (byte)(num & 0xFF);
        return bytesArr;
    }
}
