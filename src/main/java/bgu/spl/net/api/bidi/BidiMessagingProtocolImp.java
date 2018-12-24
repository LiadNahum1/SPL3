package bgu.spl.net.api.bidi;


import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BidiMessagingProtocolImp implements  BidiMessagingProtocol<Message> {
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
    public void start(int connectionId, Connections<Message> connections) {
        connectionId = connectionID;
        con = connections;
    }

    @Override
    public void process(Message message) {
        short opCode = message.getShorts().peek();
        switch(opCode){
            case 1:
                register(message);
                break;
            case 2:
                login(message);
                break;
            case 3:
                logout(message);
                break;
            case 4:
                follow(message);
                break;
            case 5:
                post(message);
                break;
            case 7:
                PM(message);
                break;
            case 8:
                userList(message);
                break;

        }


    }

    private void register(Message message){
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
    private void login(Message message){
        if(isLogedin)
        {
            sendError(message.getShorts().peek());
        }
        String user = message.strings.poll();
        String password = message.strings.poll();
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
                sendACK(message.getShorts().peek());
        }
        else {
            sendError(message.getShorts().poll());
        }
    }

    private void logout(Message message){

    }
    private void post(Message message){
        message = message.substring(0, message.length()-1);


    }
    private void follow(Message message){
        Short opCode = message.getShorts().poll();
        //check if the method asked is follow or unfollow
        //TODO check the comparation
        Byte b = 0;
        boolean isFollow = message.getBytes().peek().compareTo(b) == 0;
        int numOfUsers = message.getShorts().poll();
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
    private void PM(Message message){}
    private void sendError(short OPCode){}
    private void sendACK(Short mOPCode) {
        Queue<Short> args = new LinkedList<>();
        //add the OPCOde
        Short a = 11;
        args.add(a);
        //add the message OPCode
        args.add(mOPCode);
        Message m = new Message(args,new LinkedList<Byte>(),new LinkedList<String>());
        con.send(this.connectionID ,m);
    }

    @Override
    public boolean shouldTerminate() {
        return false;
    }
}
