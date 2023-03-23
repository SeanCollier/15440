import java.rmi.*;
import java.util.concurrent.BlockingQueue;
import java.util.HashSet;
import java.util.NoSuchElementException;

public interface serverIntf extends Remote {
    public void enqueueRequest(Cloud.FrontEndOps.Request r) throws RemoteException;
    public Cloud.FrontEndOps.Request getJob() throws NoSuchElementException, RemoteException;
}
