import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.rmi.registry.*;
import java.rmi.Naming;
import java.io.*;
import java.util.*;

public class Server extends UnicastRemoteObject implements fileServerIntf {

	public String root;
	public long defaultVersionOnMiss = 1;
	public HashMap<String, Long> versionMap = new HashMap<String, Long>();

	public Server(int port) throws RemoteException {
		super(port);
	}

	public long checkVersion(String pathname) throws RemoteException{
		pathname = root + pathname;
		return versionMap.getOrDefault(pathname, defaultVersionOnMiss);
	}

	public void close(custFile cFile){
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
		
		//update version in versionMap

		System.err.println(String.format("Current version of file: %d", versionMap.getOrDefault(newPathname, defaultVersionOnMiss)));
		System.err.println(String.format("cFile version is: %d", cFile.version));
		versionMap.put(newPathname, cFile.version);

		System.err.println(String.format("New version of file: %d", versionMap.get(newPathname)));	


		return;		
		
	}

	public custFile open(custFile cFile, long chunkSize) throws RemoteException{
		File file = new File(root + cFile.pathname);
		System.err.println(String.format("Open called for file with path: %s", root + cFile.pathname));
		cFile.doesExist = file.exists();
		cFile.isDir = file.isDirectory();
		cFile.version = versionMap.getOrDefault(root+cFile.pathname, defaultVersionOnMiss);
		cFile.length = file.length();

		if (cFile.isDir || !cFile.doesExist){
			if (!file.exists()){
				System.err.println("File does not exist");
			}
			if (file.isDirectory()){
				System.err.println("File is Directory");
			}
			return cFile;
		}

		chunkRead(cFile, 0, chunkSize);

		return cFile;
	}

	public void chunkWrite(custFile cFile, boolean firstIteration) {
	//	System.err.println(String.format("Writing in chunks to pathname: %s", cFile.pathname));
		if (firstIteration){
			close(cFile);
		}
		try {
			File file = new File(root + "/" + cFile.pathname);
			RandomAccessFile raf = new RandomAccessFile(file, "rw");
			raf.seek(file.length());	
			raf.write(cFile.data);
			raf.close();
			
		}
		catch (Exception e){
			e.printStackTrace();
			return;
		}	
	}
	
	public custFile chunkRead(custFile cFile,long offset, long chunkSize) throws RemoteException {

	//	System.err.println(String.format("chunkRead called with pathname: %s and offset: %d", cFile.pathname, offset));
		
		File file = new File(root + cFile.pathname);
		
		if (offset >= file.length()){
			System.err.println(String.format("Offset (%d) greater than or equal to file length (%d). Returning", offset, file.length()));
			return cFile;
		}

		try{
			RandomAccessFile raf = new RandomAccessFile(file, "rw");
			long bytesToRead = Long.min(chunkSize, file.length()-offset);
			cFile.data = new byte[(int)bytesToRead];
	//		System.err.println(String.format("Reading %d bytes in this chunk", bytesToRead));
			raf.seek(offset);
			raf.read(cFile.data, 0, (int)bytesToRead);
			if (cFile.data == null){
				System.err.println("cFile.data is null");
			}
		}
		catch (FileNotFoundException e){
			System.err.println("FileNotFoundException");
			cFile.error = "FileNotFound";
			e.printStackTrace();
			return cFile;
		}
		catch (SecurityException e){
			System.err.println("SecurityException");
			cFile.error = "Security";
			e.printStackTrace();
			return cFile;
		}
		catch (IllegalArgumentException e){
			System.err.println("IllegalArgumentException");
			cFile.error = "IllegalArgument";
			e.printStackTrace();
			return cFile;
		}
		catch (IOException e){
			System.err.println("IOException");
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
