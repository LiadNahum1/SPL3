package bgu.spl.net.srv;

import bgu.spl.net.api.MessageEncoderDecoderImp;
import bgu.spl.net.api.bidi.BidiMessagingProtocol;
import bgu.spl.net.api.bidi.BidiMessagingProtocolImp;
import bgu.spl.net.api.bidi.Message;
import bgu.spl.net.api.bidi.SharedData;

public class ServerMain {
    public static void main(String[] args) {
        SharedData shared = new SharedData();
        Server.threadPerClient(9000, ()-> new BidiMessagingProtocolImp(shared), ()-> new MessageEncoderDecoderImp()).serve();

    }
}
