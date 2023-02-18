import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.rmi.registry.*;
import java.rmi.Naming;
import java.io.*;
import java.util.*;

public class Server extends UnicastRemoteObject implements fileServerIntf {
	public Server() throws RemoteException {
		super(0);
	}

	public custFile open(custFile cFile){
		File file = new File(cFile.pathname);
		cFile.doesExist = file.exists();
		cFile.isDir = file.isDirectory();

		if (cFile.isDir || !cFile.doesExist){
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
	
}
