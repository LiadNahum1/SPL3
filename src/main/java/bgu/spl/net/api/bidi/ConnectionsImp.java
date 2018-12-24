package bgu.spl.net.api.bidi;

public class ConnectionsImp<T> implements Connections<T> {


    @Override
    public boolean send(int connectionId, T msg) {
        return false;
    }

    @Override
    public void broadcast(T msg) {

    }

    @Override
    public void disconnect(int connectionId) {

    }
}
