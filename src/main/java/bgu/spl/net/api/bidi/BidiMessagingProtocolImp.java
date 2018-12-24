package bgu.spl.net.api.bidi;

public class BidiMessagingProtocolImp implements  BidiMessagingProtocol<String> {

    @Override
    public void start(int connectionId, Connections<String> connections) {

    }

    @Override
    public void process(String message) {

    }

    @Override
    public boolean shouldTerminate() {
        return false;
    }
}
