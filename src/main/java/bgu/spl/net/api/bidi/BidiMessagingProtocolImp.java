package bgu.spl.net.api.bidi;


import java.util.concurrent.ConcurrentHashMap;

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
        String messageData = message.substring(2);
        switch(opCode){
            case "01":
                register(messageData);
                break;
            case "02":
                login(messageData);
                break;
            case "03":
                logout(messageData);
                break;
            case "04":
                follow(messageData);
                break;
            case "05":
                post(messageData);
                break;
            case "06":
                PM(messageData);
                break;
            case "07":
                userList(messageData);
                break;

        }


    }

    private void register(String message){
        int index = message.indexOf('\0');
        String username = message.substring(0,index);
        message = message.substring(index +1);
        index = message.indexOf('\0');
        String password = message.substring(0,index);
        ConcurrentHashMap<String, String> registerd = sharedData.getRegisteredUsers();
        String response="";
        if(!registerd.containsKey(username)){
            registerd.put(username,password);
            response = "ACK 1";
        }
        else{
            //already registered
            response = "EROOR 1";
        }
        con.send(connectionID, response);
    }

    private void logout(String message){

    }
    private void post(String message){
        message = message.substring(0, message.length()-1);
        

    }
    @Override
    public boolean shouldTerminate() {
        return false;
    }
}
