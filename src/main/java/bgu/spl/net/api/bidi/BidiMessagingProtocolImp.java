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
        this.connectionID = connectionId;
        this.con = connections;
    }

    @Override
    //Client-to-Server communication
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
            case 8:
                stat(message);
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
            sharedData.getUserfollowAfter().put(username, new ConcurrentLinkedQueue<>());
            sharedData.getMessagesForNotLogged().put(username , new ConcurrentLinkedQueue<>());
            sharedData.getUsersConnectionId().put(username, -1); // not logged in yet
            sharedData.getfollowerOfUser().put(username, new ConcurrentLinkedQueue<>());
            sharedData.getPostsUserSend().put(username, (short)0); //number of posts user sent
            sharedData.getRegistrationQueue().add(username); //add user to queue
            sendACK(message.getShorts().peek());
        }
    }

    private void login(Message message){
        if(isLogedin)
        {
            sendError(message.getShorts().peek());
        }
        String user = message.getStrings().poll();
        String password = message.getStrings().poll();
        if(sharedData.getRegisteredUsers().containsKey(user) && sharedData.getRegisteredUsers().get(user).equals(password)){
            //update statuses
            username = user;
            isLogedin = true;
            //add the connectionID of this connection
            sharedData.getUsersConnectionId().put(user,connectionID);
            //send unseen messages
            ConcurrentLinkedQueue<Message> tosend = sharedData.getMessagesForNotLogged().get(username);
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
            sharedData.getUsersConnectionId().put(username,-1); //user logged out
            sendACK(message.getShorts().peek());
            con.disconnect(connectionID);
        }

    }
    private void follow(Message message){
        Short opCode = message.getShorts().poll();
        if(!isLogedin)
            sendError(opCode);
        else {
            Integer numsecces = 0;
            Queue<String> strings = new LinkedList<>();
            Queue<Byte> bytes = new LinkedList<>();
            byte b = '\0';
            //check if the method asked is follow or unfollow
            //TODO check the comparation
            boolean isFollow = message.getBytes().peek().compareTo(b) == 0;
            int numOfUsers = message.getShorts().poll();
            Queue<String> users = message.getStrings();
            if (isFollow) {
                for (int i = 0; i < numOfUsers; i++) {
                    String nextUserName = users.poll();
                    //add the names to my followers list if not already follow
                    if (!sharedData.getUserfollowAfter().get(username).contains(nextUserName)) {
                        sharedData.getUserfollowAfter().get(username).add(nextUserName);
                        //add me as a follower to the users
                        sharedData.getfollowerOfUser().get(nextUserName).add(this.username);
                        numsecces++;
                        strings.add(username);
                    }
                }
            }
            //unfollow
            else {
                for (int i = 0; i < numOfUsers; i++) {
                    //removes from the lists
                    String nextUserName = users.poll();
                    if (sharedData.getfollowerOfUser().get(nextUserName).contains(username)) {
                        sharedData.getfollowerOfUser().get(nextUserName).remove(username);
                        sharedData.getUserfollowAfter().get(username).remove(nextUserName);
                        numsecces++;
                        strings.add(username);
                    }
                }
            }
            if (numsecces == 0)
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
                con.send(connectionID, new Message(shorts, strings, bytes));
            }
        }
    }

    private void post(Message message){
        if(!isLogedin)
            sendError(message.getShorts().peek());
        else{
            //increment num of posts
            sharedData.getPostsUserSend().put(username, (short) (sharedData.getPostsUserSend().get(username) +1) );
            //users that follows after current user
            ConcurrentLinkedQueue<String> usersToSend = sharedData.getfollowerOfUser().get(this.username); //followers of current user
            ConcurrentHashMap<String, String> registered = sharedData.getRegisteredUsers();
            //users that are tagged with @
            String content = message.getStrings().peek();
            String usernameToSend = "";
            while(content.contains("@")){
                int indexStartName = content.indexOf('@');
                content = content.substring(indexStartName+1);
                int indexEndName = content.indexOf(' ');
                usernameToSend = content.substring(0,indexEndName);
                if(registered.containsKey(usernameToSend)) { //if username is registered add it to usersToSend queue
                    usersToSend.add(usernameToSend);
                }
                content = content.substring(indexEndName+1);
            }

            Message notification = createNotification("1", message);
            for(String user : usersToSend) {
                if (sharedData.getUsersConnectionId().get(user) == -1) {//user is not logged in
                    sharedData.getMessagesForNotLogged().get(user).add(notification);
                }
                else
                    con.send(sharedData.getUsersConnectionId().get(user), notification);
            }
        }


    }
    private Message createNotification(String isPM, Message message){
        //create notification
        Queue<Short> shortParts = new LinkedList<>();
        shortParts.add((short)9); //notification opcode
        Queue<String> stringParts = new LinkedList<>();
        stringParts.add(isPM); //private message -0 or public message - 1
        stringParts.add(username); //posting user
        stringParts.add(message.getStrings().peek()); //content
        Message notification = new Message(shortParts, stringParts, new LinkedList<>());
        return notification;

    }
    private void PM(Message message){
        if(!isLogedin)
            sendError(message.getShorts().peek());
        else{
             String usernameToSendTo = message.getStrings().poll();
            if(!sharedData.getRegisteredUsers().containsKey(usernameToSendTo)) // if not registered
                sendError(message.getShorts().peek());
            else{
                Message notification = createNotification("0", message);
                if (sharedData.getUsersConnectionId().get(usernameToSendTo) == -1) {//user is not logged in
                    sharedData.getMessagesForNotLogged().get(usernameToSendTo).add(notification);
                }
                else
                    con.send(sharedData.getUsersConnectionId().get(usernameToSendTo), notification);
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
            ConcurrentLinkedQueue<String>users = new ConcurrentLinkedQueue<>(); //users according to registration order
            //TODO concurrent iterator
            for(String user: sharedData.getRegistrationQueue()){
                users.add(user);
            }
            Message ack = new Message(shortParts, users, new LinkedList<>());
            con.send(sharedData.getUsersConnectionId().get(username), ack);
        }

    }

    private void stat(Message message) {
        if(!isLogedin)
            sendError(message.getShorts().peek());
        else{
            String usernameStat = message.getStrings().peek(); //username that we want to know his status
            if(!sharedData.getRegisteredUsers().containsKey(usernameStat)){ //the username is not registered
                sendError(message.getShorts().peek());
            }
            else{
                //create ack
                Queue<Short> shortParts = new LinkedList<>();
                shortParts.add((short)10); //opcode ACK
                shortParts.add(message.getShorts().peek()); //Stat opcode
                shortParts.add(sharedData.getPostsUserSend().get(usernameStat)); //numPosts
                shortParts.add((short)sharedData.getfollowerOfUser().get(usernameStat).size()); //num followers
                shortParts.add((short)sharedData.getUserfollowAfter().get(usernameStat).size()); //num following
                Message ack = new Message(shortParts, new LinkedList<>(), new LinkedList<>());
                con.send(sharedData.getUsersConnectionId().get(username), ack);
            }
        }
    }
    private void sendError(short OPCode){
        Queue<Short> args = new LinkedList<>();
        //add the OPCOde
        Short a = 11;
        args.add(a);
        //add the message OPCode
        args.add(OPCode);
        Message m = new Message(args,new LinkedList<String>() ,new LinkedList<Byte>());
        con.send(this.connectionID ,m);
    }
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
