package bgu.spl.net.api.bidi;

import bgu.spl.net.srv.BlockingConnectionHandler;

import java.io.IOException;

public interface Connections<T> {

    boolean send(int connectionId, T msg);

    void broadcast(T msg);

    void disconnect(int connectionId);

    void add(BlockingConnectionHandler<T> handler);
}
