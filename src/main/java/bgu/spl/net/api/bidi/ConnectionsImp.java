package bgu.spl.net.api.bidi;

import bgu.spl.net.srv.BlockingConnectionHandler;

import java.util.concurrent.ConcurrentHashMap;

public class ConnectionsImp<T> implements Connections<T> {
    private ConcurrentHashMap<Integer,BlockingConnectionHandler<T>> connectionsHandler;

    public ConnectionsImp(){
        this.connectionsHandler = new ConcurrentHashMap<>();
    }
    @Override
    public boolean send(int connectionId, T msg) {
      synchronized (connectionsHandler) {
          if (connectionsHandler.containsKey(connectionId)) {
              System.out.println("send" + msg);
              System.out.println(connectionId);
              connectionsHandler.get(connectionId).send(msg);
              return true;
          }

          return false;
      }
    }

    @Override
    public void broadcast(T msg) {
        for(BlockingConnectionHandler<T> connectionHandler : connectionsHandler.values()){
            connectionHandler.send(msg);
        }
    }

    @Override
    public void disconnect(int connectionId) {
        connectionsHandler.remove(connectionId);
    }

    public void add(BlockingConnectionHandler<T> handler, int connectionId) {
        connectionsHandler.put(connectionId, handler);
    }
}
