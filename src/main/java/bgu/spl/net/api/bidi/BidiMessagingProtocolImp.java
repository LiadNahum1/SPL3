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
            //add the connectionID of this connection
            sharedData.getUsersConnectionId().put(user,connectionID);
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
        if(!isLogedin)
            sendError(opCode);
        Integer numsecces =0;
        Queue<String> strings = new LinkedList<>();
        Queue<Byte> bytes = new LinkedList<>();
        Byte b = '\0';
        bytes.add(b);
        //check if the method asked is follow or unfollow
        //TODO check the comparation
        boolean isFollow = message.getBytes().peek().compareTo(b) == 0;
        int numOfUsers = message.getShorts().poll();
        Queue<String> users = message.getStrings();
        if(isFollow) {
            for (int i = 0; i < numOfUsers; i++) {
                String nextUserName = users.poll();
                //add the names to my followers list if not already follow
                    if(!sharedData.getUserfollowAfter().get(username).contains(nextUserName)) {
                        sharedData.getUserfollowAfter().get(username).add(nextUserName);
                        //add me as a follower to the users
                        sharedData.getfollowerOfUser().get(nextUserName).add(this.username);
                        numsecces++;
                        strings.add(username);
                        Byte bits = '\0';
                   bytes.add(bits);
                    }
            }
        }
        //unfollow
        else {
            for (int i = 0; i < numOfUsers; i++) {
                //removes from the lists
                String nextUserName = users.poll();
                if(sharedData.getfollowerOfUser().get(nextUserName).contains(username)) {
                    sharedData.getfollowerOfUser().get(nextUserName).remove(username);
                    sharedData.getUserfollowAfter().get(username).remove(nextUserName);
                    numsecces++;
                    strings.add(username);
                    Byte bits = '\0';
                    bytes.add(bits);
                }
            }
        }
        if(numsecces == 0)
            sendError(opCode);

            //send the ACK
        else {
            Queue<Short> shorts = new LinkedList<>();
            //add the OPCOde of the ACK
            Short a = 10;
            shorts.add(a);
            //add the OPCode of the follow
            Short myO = 4;
            shorts.add(myO);
            Short secces = numsecces.shortValue();
            shorts.add(secces);
            con.send(connectionID , new Message(shorts ,strings , bytes));
        }
    }
    private void PM(Message message){}
    private void sendError(short OPCode){}
    private void sendACK(Short mOPCode) {
        Queue<Short> args = new LinkedList<>();
        //add the OPCOde
        Short a = 10;
        args.add(a);
        //add the message OPCode
        args.add(mOPCode);
        Message m = new Message(args,new LinkedList<String>() ,new LinkedList<Byte>());
        con.send(this.connectionID ,m);
    }

    @Override
    public boolean shouldTerminate() {
        return false;
    }
}
