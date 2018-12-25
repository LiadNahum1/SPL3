package bgu.spl.net.api.bidi;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SharedData {
    private ConcurrentHashMap<String,String> registeredUsers; //username, password
    private ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> userfollowAfter; //username and the users that he follows after
    private ConcurrentHashMap<String, ConcurrentLinkedQueue<Message>> messagesForNotLogged; //messges that has been sent when user is logout
    private ConcurrentHashMap<String, Integer> usersConnectionId; //username and his connectionId
    private ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> followerOfUser; //username and the users that follows him
    private ConcurrentHashMap<String,Integer> postsUserSend;
    private Queue<String> registrationQueue;
    public SharedData(){
        this.registeredUsers = new ConcurrentHashMap<>();
        this.userfollowAfter = new ConcurrentHashMap<>();
        this.messagesForNotLogged = new ConcurrentHashMap<>();
        this.usersConnectionId = new ConcurrentHashMap<>();
        this.followerOfUser = new ConcurrentHashMap<>();
        this.postsUserSend = new ConcurrentHashMap<>();
        this.registrationQueue = new LinkedList<>();
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

    public ConcurrentHashMap<String, ConcurrentLinkedQueue<Message>> getMessagesForNotLogged() {
        return messagesForNotLogged;
    }

    public ConcurrentHashMap<String, Integer> getUsersConnectionId() {
        return usersConnectionId;
    }
    public ConcurrentHashMap<String, Integer> getPostsUserSend() {
        return postsUserSend;
    }

    public Queue<String> getRegistrationQueue() {
        return registrationQueue;
    }

}
