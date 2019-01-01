package bgu.spl.net.api.bidi;

import bgu.spl.net.srv.BlockingConnectionHandler;
import bgu.spl.net.srv.bidi.ConnectionHandler;

import java.util.concurrent.ConcurrentHashMap;

public class ConnectionsImp<T> implements Connections<T> {
    private ConcurrentHashMap<Integer, ConnectionHandler<T>> connectionsHandler;

    public ConnectionsImp(){
        this.connectionsHandler = new ConcurrentHashMap<>();
    }
    @Override
    public boolean send(int connectionId, T msg) {
      synchronized (connectionsHandler) {
          if (connectionsHandler.containsKey(connectionId)) {
              connectionsHandler.get(connectionId).send(msg);
              return true;
          }

          return false;
      }
    }

    @Override
    public void broadcast(T msg) {
        for(ConnectionHandler<T> connectionHandler : connectionsHandler.values()){
            connectionHandler.send(msg);
        }
    }

    @Override
    public void disconnect(int connectionId) {
        connectionsHandler.remove(connectionId);
    }

    public void add(ConnectionHandler<T> handler, int connectionId) {
        connectionsHandler.put(connectionId, handler);
    }
}
