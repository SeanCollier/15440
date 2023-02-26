import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.rmi.registry.*;
import java.rmi.Naming;
import java.io.*;
import java.util.*;

public class Server extends UnicastRemoteObject implements fileServerIntf {

	public String root;

	public Server(int port) throws RemoteException {
		super(port);
	}

	public void close(custFile cFile) throws RemoteException{
		String newPathname = root + cFile.pathname;
		File file = new File (newPathname);
		System.err.println(String.format("Close called for file with path: %s", newPathname));
		try {
			file.delete();
		}
		catch (SecurityException e){
			e.printStackTrace();
		}

		try {
			file.createNewFile();
		}
		catch (IOException e){
			e.printStackTrace();
		}
		
		
		if (cFile.data == null || cFile.data.length == 0){
			System.err.println("data is null or has length 0");
			return;
		}

		try {
			RandomAccessFile raf = new RandomAccessFile(file, "rw");
			raf.write(cFile.data);
			raf.close();
		}
		catch (FileNotFoundException e){
			e.printStackTrace();
		}
		catch (IOException e){
			e.printStackTrace();
		}

		return;
		
		
		
	}

	public custFile open(custFile cFile) throws RemoteException{
		File file = new File(root + cFile.pathname);
		System.err.println(String.format("Open called for file with path: %s", root + cFile.pathname));
		cFile.doesExist = file.exists();
		cFile.isDir = file.isDirectory();

		if (cFile.isDir || !cFile.doesExist){
			if (!file.exists()){
				System.err.println("File does not exist");
			}
			if (file.isDirectory()){
				System.err.println("File is Directory");
			}
			return cFile;
		}

		//read contents of file into custFile data buffer

		try{
			RandomAccessFile raf = new RandomAccessFile(file, "rw");
			cFile.length = raf.length();
			byte[] data = new byte[(int) raf.length()];
			raf.read(data);
			cFile.data = data;
		}
		catch (FileNotFoundException e){
			cFile.error = "FileNotFound";
			e.printStackTrace();
			return cFile;
		}
		catch (SecurityException e){
			cFile.error = "Security";
			e.printStackTrace();
			return cFile;
		}
		catch (IllegalArgumentException e){
			cFile.error = "IllegalArgument";
			e.printStackTrace();
			return cFile;
		}
		catch (IOException e){
			cFile.error = "IO";
			e.printStackTrace();
			return cFile;
		}
		

		return cFile;

	}

	public static void main (String[] args) throws IOException {
		int port = Integer.parseInt(args[0]);

		try{
			Server server = new Server(port);
			server.root = args[1] + "/";
			Registry registry = LocateRegistry.createRegistry(port);
			registry.bind("Server:"+args[0], server);	
		}
		catch (Exception e){
			e.printStackTrace();
		}
	}
	
}
