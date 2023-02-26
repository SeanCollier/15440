import java.rmi.Remote;
import java.rmi.RemoteException;

public interface fileServerIntf extends Remote{
	custFile open(custFile cFile) throws RemoteException;
	void close(custFile cFile) throws RemoteException;
	long checkVersion(String pathname) throws RemoteException;
}
