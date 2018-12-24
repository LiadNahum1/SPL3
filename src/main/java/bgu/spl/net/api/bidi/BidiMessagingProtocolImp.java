package bgu.spl.net.api.bidi;


import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BidiMessagingProtocolImp implements  BidiMessagingProtocol<String> {
    private int connectionID;
    private Connections con;
    private String username;
    private SharedData sharedData;
    private boolean isLogedin;
    public BidiMessagingProtocolImp(SharedData sd){
        this.sharedData = sd;
        isLogedin = false;
        username = "";
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
            //TODO add values to all other hashmaps
            response = "ACK 1";
        }
        else{
            //already registered
            response = "EROOR 1";
        }
        con.send(connectionID, response);
    }
    private void login(String message){
        int parser =message.indexOf('\0');
        String user = message.substring(0,parser-1);
        String password = message.substring(parser+1,message.lastIndexOf('\0')-1);
        if(sharedData.getRegisteredUsers().containsKey(user) && sharedData.getRegisteredUsers().get(user).equals(password)){
            //update statuses
            username = user;
            isLogedin = true;
            Integer lastconnectionID = sharedData.getUsersConnectionId().get(user);
            lastconnectionID = this.connectionID;
            //send unseen messages
            ConcurrentLinkedQueue tosend = sharedData.getMessagesForNotLogged().get(username);
                 while(!tosend.isEmpty()) {
                     con.send(this.connectionID, tosend.poll());
                 }
                 //TODO understand how to send the aka
            con.send(this.connectionID , "AKA");
        }
        else {
            //TODO hoe to send error;
        }
    }
    private void logout(String message){

    }
    private void post(String message){
        message = message.substring(0, message.length()-1);


    }
    private void follow(String message){
        boolean isFollow = true;
        int numOfUsers = 0;
        String users = "";
        String[] names = users.split("\0");
        if(isFollow) {
            for (int i = 0; i < numOfUsers; i++) {
                //add the names to my followers list
                sharedData.getUserfollowAfter().get(username).add(names[i]);
                //add me as a follower to the users
                sharedData.getfollowerOfUser().get(names[i]).add(this.username);
            }
        }
        //unfollow
        else {
            for (int i = 0; i < numOfUsers; i++) {
                //removes from the lists
                sharedData.getfollowerOfUser().get(names[i]).remove(username);
                sharedData.getUserfollowAfter().get(username).remove(names[i]);
            }
        }
    }
    @Override
    public boolean shouldTerminate() {
        return false;
    }
}
