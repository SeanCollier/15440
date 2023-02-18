import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.rmi.registry.*;
import java.rmi.Naming;
public class Server extends UnicastRemoteObject implements fileServerIntf {
	public Server() throws RemoteException {
		super(0);
	}
	
}
