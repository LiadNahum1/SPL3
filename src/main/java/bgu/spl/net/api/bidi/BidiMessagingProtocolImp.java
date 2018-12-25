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
    private boolean shouldTerminate = false;
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
            case 6:
                PM(message);
                break;
            case 7:
                userList(message);
                break;

        }


    }



    private void register(Message message){
        String username = message.getStrings().poll();
        ConcurrentHashMap<String,String> registered = sharedData.getRegisteredUsers();
        if(registered.containsKey(username)){
            sendError(message.getShorts().peek());
        }
        else {
            String password = message.getStrings().poll();
            registered.put(username, password);
            sharedData.getPostsUserSend().put(username, 0); //number of posts user sent
            sendACK(message.getShorts().peek());

        }


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
        if(!isLogedin)
            sendError(message.getShorts().peek());
        else{
            isLogedin = false;
            shouldTerminate = true;
            sendACK(message.getShorts().peek());
            sharedData.getUsersConnectionId().put(username,-1); //user logged out
            con.disconnect(connectionID);
        }

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

    private void post(Message message){
        if(!isLogedin)
            sendError(message.getShorts().peek());
        else{
            sharedData.getPostsUserSend().put(username,  sharedData.getPostsUserSend().get(username) +1 );
            //users that follows afte rcurrent user
            ConcurrentLinkedQueue<String> usersToSend = sharedData.getfollowerOfUser().get(this.username); //followers of current user
            ConcurrentHashMap<String, String> registered = sharedData.getRegisteredUsers();
            //users that are tagged with @
            String content = message.getStrings().peek();
            String username = "";
            while(content.contains("@")){
                int indexStartName = content.indexOf('@');
                content = content.substring(indexStartName+1);
                int indexEndName = content.indexOf(' ');
                username = content.substring(0,indexEndName);
                if(registered.containsKey(username)) { //if username is registered add it to usersToSend queue
                    usersToSend.add(username);
                }
                content = content.substring(indexEndName+1);
            }
            for(String user : usersToSend) {
                if (sharedData.getUsersConnectionId().get(user) == -1) {//user is not logged in
                    sharedData.getMessagesForNotLogged().get(user).add(message);
                }
                else
                    con.send(sharedData.getUsersConnectionId().get(user), message);
            }
        }


    }
    private void PM(Message message){
        if(!isLogedin)
            sendError(message.getShorts().peek());
        else{
             String usernameToSendTo = message.getStrings().peek();
            if(!sharedData.getRegisteredUsers().containsKey(usernameToSendTo))
                sendError(message.getShorts().peek());
            else{
                if (sharedData.getUsersConnectionId().get(usernameToSendTo) == -1) {//user is not logged in
                    sharedData.getMessagesForNotLogged().get(usernameToSendTo).add(message);
                }
                else
                    con.send(sharedData.getUsersConnectionId().get(usernameToSendTo), message);
            }
        }
    }

    private void userList(Message message) {
        if(!isLogedin)
            sendError(message.getShorts().peek());
        else{
            Queue<Short> shortParts = new LinkedList<>();
            shortParts.add((short)10); //opcode ACK
            shortParts.add(message.getShorts().peek()); //opcode userList message
            shortParts.add((short)sharedData.getRegisteredUsers().size()); //numOfUsers
            Queue<String>users = new LinkedList<>();
            for(String user: sharedData.getRegisteredUsers().keySet()){


            }
        }

    }
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
        return shouldTerminate;
    }
}
