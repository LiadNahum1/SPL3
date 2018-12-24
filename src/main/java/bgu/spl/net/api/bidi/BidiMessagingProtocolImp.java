package bgu.spl.net.api.bidi;

public class BidiMessagingProtocolImp implements  BidiMessagingProtocol<String> {
private int connectionID;
private Connections con;
    @Override
    public void start(int connectionId, Connections<String> connections) {
connectionId = connectionID;
con = connections;
    }

    @Override
    public void process(String message) {

    }

    @Override
    public boolean shouldTerminate() {
        return false;
    }
}
