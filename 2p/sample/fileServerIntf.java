import java.rmi.Remote;
import java.rmi.RemoteException;


/*
	The fileServerIntf class defines an interface for which a file server must be faithful to in order for proper remote interaction with a Proxy.
*/
public interface fileServerIntf extends Remote{
	custFile open(custFile cFile, long chunkSize) throws RemoteException;
	long checkVersion(String pathname) throws RemoteException;
	custFile chunkRead(custFile cFile, long offset, long chunkSize) throws RemoteException;
	void chunkWrite(custFile cFile, boolean firstIteration) throws RemoteException;
	int unlink(String path) throws RemoteException;
}
