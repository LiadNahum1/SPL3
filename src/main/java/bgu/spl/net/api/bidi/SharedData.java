package bgu.spl.net.api.bidi;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SharedData {
    private ConcurrentHashMap<String,String> registeredUsers; //username, password
    private ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> userfollowAfter; //username and the users that he follows after
    private ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> messagesForNotLogged; //messges that has been sent when user is logout
    private ConcurrentHashMap<String, Integer> usersConnectionId; //username and his connectionId

    public SharedData(ConcurrentHashMap<String,String> registeredUsers,ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> userfollowAfter,
                      ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> messagesForNotLogged,ConcurrentHashMap<String, Integer> usersConnectionId){
        this.registeredUsers = registeredUsers;
        this.userfollowAfter = userfollowAfter;
        this.messagesForNotLogged = messagesForNotLogged;
        this.usersConnectionId = usersConnectionId;

    }

    public ConcurrentHashMap<String, String> getRegisteredUsers() {
        return registeredUsers;
    }

    public ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> getUserfollowAfter() {
        return userfollowAfter;
    }

    public ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> getMessagesForNotLogged() {
        return messagesForNotLogged;
    }

    public ConcurrentHashMap<String, Integer> getUsersConnectionId() {
        return usersConnectionId;
    }
}
