package bgu.spl.net.api.bidi;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SharedData {
    private ConcurrentHashMap<String,String> registeredUsers; //username, password
    private ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> userfollowAfter; //username and the users that he follows after
    private ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> messagesForNotLogged; //messges that has been sent when user is logout
    private ConcurrentHashMap<String, Integer> usersConnectionId; //username and his connectionId
    private ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> followerOfUser; //username and the users that follows him
    public SharedData(){
        this.registeredUsers = new ConcurrentHashMap<>();
        this.userfollowAfter = new ConcurrentHashMap<>();
        this.messagesForNotLogged = new ConcurrentHashMap<>();
        this.usersConnectionId = new ConcurrentHashMap<>();
        this.followerOfUser = new ConcurrentHashMap<>();
    }

    public ConcurrentHashMap<String, String> getRegisteredUsers() {
        return registeredUsers;
    }

    public ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> getUserfollowAfter() {
        return userfollowAfter;
    }

    public ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> getfollowerOfUser() {
        return followerOfUser;
    }

    public ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> getMessagesForNotLogged() {
        return messagesForNotLogged;
    }

    public ConcurrentHashMap<String, Integer> getUsersConnectionId() {
        return usersConnectionId;
    }
}
