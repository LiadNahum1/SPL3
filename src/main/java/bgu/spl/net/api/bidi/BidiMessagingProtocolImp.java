package bgu.spl.net.api.bidi;

import static com.sun.org.apache.xml.internal.security.utils.resolver.ResourceResolver.register;

public class BidiMessagingProtocolImp implements  BidiMessagingProtocol<String> {
    private int connectionID;
    private Connections con;
    private String username;
    private SharedData sharedData;
    public BidiMessagingProtocolImp(SharedData sd){
        this.sharedData = sd;
    }
    @Override
    public void start(int connectionId, Connections<String> connections) {
        connectionId = connectionID;
        con = connections;
    }

    @Override
    public void process(String message) {
        String opCode = message.substring(0,message.length()-2);
        switch(opCode){
            case "01":
                register(message);
                break;
            case "02":
                login(messgae);
                break;
            case "03":
                logout(message);
                break;
            case "04":
                follow(message);
                break;
            case "05":
                post(messgae);
                break;
            case "06":
                PM(message);
                break;
            case "07":
                userList(message);
                break;
            case "08":
                

        }


    }

    @Override
    public boolean shouldTerminate() {
        return false;
    }
}
