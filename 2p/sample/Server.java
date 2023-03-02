import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.rmi.registry.*;
import java.rmi.Naming;
import java.io.*;
import java.util.*;


/*
	The Server class implements the fileServerIntf to act as a file server in conjunction with Proxy
*/

public class Server extends UnicastRemoteObject implements fileServerIntf {

	public String root;
	public long defaultVersionOnMiss = 1;
	public HashMap<String, Long> versionMap = new HashMap<String, Long>();

	/*
		[description]: class constructor for server
		[in]: port (port for tcp connection)
	*/

	public Server(int port) throws RemoteException {
		super(port);
	}

	/*
		[description]: checks the version of a given file existing on the servers file system
		[in]: pathname (pathname of file to check the version of)
		[out]: version number of file if it exists on the server, -1 if it was deleted
	*/
	public long checkVersion(String pathname) throws RemoteException{
		pathname = root + pathname;
		System.err.println(String.format("Checking version for pathname: %s", pathname));
		System.err.println(String.format("Giving back version: %d", versionMap.getOrDefault(pathname, defaultVersionOnMiss)));
		return versionMap.getOrDefault(pathname, defaultVersionOnMiss);
	}

	/*
		[description]: deletes the current version of the given file from the server, and replaces it with a fresh
		[in]: cFile (custFile object containing metadata about file to delete and recreate)
	*/
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


	/*
		[description]: gives metadata about file specified by cFile, along with first chunkSize bytes from the file in a data buffer
		[in]: cFile (custFile describing the file to get data from), chunkSize (number of bytes to read from file)
		[out]: custFile containing metadata and data array of file data
	*/
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

	/*
		[description]: appends the data fromn data buffer in cFile to the file specified by cFile
		[in]: cFile (custFile describing file to write to, and containing data to write), firstIteration (boolean denoting if this is the first time this function is called in the Proxy's while loop)
	*/
	public void chunkWrite(custFile cFile, boolean firstIteration) {
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
	
	/*
		[description]: deletes file at specified pathname
		[in]: path (pathname of file to delete)
		[out]: 0 on success, -1 on SecurityException, -2 if file does not exist, -3 if file is not deleted properly
	*/
	public int unlink (String path) throws RemoteException{
		
		path = root + path;

		System.err.println(String.format("#### Unlink called for path %s", path));
		File currFile = new File(path);
		if (!currFile.exists()){
			System.err.println("Error: unlink called on file which does not exist");
			return -2;
		}
		boolean deleted = false;
		try{
			deleted = currFile.delete();
		}
		catch (SecurityException e){
			e.printStackTrace();
			return -1;
		}
		if (deleted){
			System.err.println("Deleted properly");
			versionMap.put(path, (long)-1);
			System.err.println(String.format("New version: %d", versionMap.get(path)));
			return 0;
		}
		System.out.println("Error: file not properly deleted");
		return -3;
	
	}
	
	/*
		[description]: reads chunkSize number of bytes into data buffer from the specified file in server's file system
		[in]: cFile (custFile containing metadata about file to read from), offset (offset from beginning of file to read from), chunkSize (number of bytes to read)
		[out]: custFile containing data buffer filled with file data
	*/
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

	/*
		[description]: places server object in registry
		[in]: args (command line arguments, args[0]: port number, args[1]: root directory)
	*/
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
