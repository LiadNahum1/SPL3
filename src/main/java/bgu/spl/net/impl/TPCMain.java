package bgu.spl.net.impl;

import bgu.spl.net.api.MessageEncoderDecoderImp;
import bgu.spl.net.api.bidi.BidiMessagingProtocolImp;
import bgu.spl.net.api.bidi.SharedData;
import bgu.spl.net.srv.Server;

public class TPCMain {
    public static void main(String[] args) {
        SharedData shared = new SharedData();
        Server.threadPerClient(Integer.parseInt(args[0]), ()-> new BidiMessagingProtocolImp(shared), ()-> new MessageEncoderDecoderImp()).serve();
    }
}
