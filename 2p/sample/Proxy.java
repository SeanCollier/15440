/* Sample skeleton for proxy */
/*
 	The Proxy class is used to handle all incoming requests from the client, and forward them on to the server when necessary. The proxy also utilizes a cache to handle incoming requests without the need to forward them to the server.
*/

import java.io.*;
import java.util.*;
import java.rmi.*;
import java.rmi.registry.*;

class Proxy {
	
	public static String cacheDir;
	public static Cache cache;
	public static fileServerIntf server;
	private static long chunkSize = 250000;


	/*
		FileHandler class is client-specific class for handling requests
	*/

	private static class FileHandler implements FileHandling {	

		private HashMap<Integer, custFile> fdMap = new HashMap<Integer, custFile>();
		private int fdCounter = 0;


		/*
			[description]: associates a file descriptor with the file on the server at the given path
			[in]:
				path (pathname on the server which the client wants to open)
				o (OpenOption which describes the mode with which to open the file
			[out]: 
				file descriptor assigned to file on success, proper error code on error
		*/
		public int open( String path, OpenOption o ) {
			System.err.println(String.format("##### Open Called with Pathname: %s", path));

			//generate new fd for file at path
			int fd = getNewFd();

			//check if fd is already in map, with current fd policy this shouldnt happen
			if (fdMap.containsKey(fd)){
				System.err.println(String.format("WARNING: newly generated fd (%d) already exists in fdMap", fd));
			}

			//convert openOption to mode for creation of RandomAccessFile
			String mode = optionToMode(o);
			if (mode == "failure"){
				return Errors.EINVAL;
			}

			custFile newCustFile;	
			newCustFile = new custFile(path, mode);

			//query cache for file
			newCustFile = cache.query(path, mode, newCustFile);
			newCustFile.mode = mode;
			newCustFile.pathname = path;
			String pathInCache = newCustFile.returnPath;
			String newPath = cacheDir + "/" + pathInCache;
			newCustFile.cacheFilePath = pathInCache;
			
			System.err.println(String.format("New pathname after querying cache: %s", newPath));
			System.err.println(String.format("Version number after querying cache: %d", newCustFile.version));

			//handle errors based on error message given by server
			if (newCustFile.error != null){
				switch(newCustFile.error){
					case "FileNotFound":
						System.err.println("FileNotFoundException on server");
						return Errors.ENOENT;
					case "Security":
						System.err.println("SecurityException on server");
						return Errors.EPERM;
					case "IllegalArgument":
						System.err.println("IllegalArgumentException on server");
						return Errors.EINVAL;
					case "IO":
						System.err.println("IOException on server");
						return Errors.EINVAL;
					default:
						System.err.println(String.format("Unknown error caughtin cFile error: %s", newCustFile.error));
						break;
						
				}
			}
			
			//define new File object, and check if null
			File newFile = new File(newPath);
			if (newFile == null){
				System.err.println("Error: Attempt to create File object from path failed. Got null");
			}	

			newCustFile.setJFile(newFile);

			//check for errors based on openOption
			switch(o){
				case CREATE_NEW:
					if (newCustFile.isDirectory()){
						System.err.println("Error: Open called with CREATE_NEW on directory");
						return Errors.EISDIR;
					}
					if (newCustFile.doesExist){
						System.err.println("Error: Open called with CREATE_NEW on file which already exists");
						try{
							newFile.delete();
						}
						catch (Exception e){
							e.printStackTrace();
						}
						return Errors.EEXIST;
					}	
					try {
						newFile.createNewFile();
					}
					catch (IOException e){
						e.printStackTrace();
						return Errors.EINVAL;
					}
					break;
				case CREATE:
					if (newCustFile.isDirectory()){
						System.err.println("Error: Open called with CREATE on directory");
						return Errors.EISDIR;
					}
					try {
						newFile.createNewFile();
						newCustFile.doesExist = true;
					}
					catch (IOException e){
						e.printStackTrace();
						return Errors.EINVAL;
					}
					break;
				case WRITE:
					if (newCustFile.isDirectory()){
						System.err.println("Error: Open called with WRITE on directory");
						return Errors.EISDIR;
					}
					if (!newCustFile.exists()){
						System.err.println("Error: Open called with WRITE on file that doesn't exist");
						try{
							newFile.delete();
						}
						catch (Exception e){
							e.printStackTrace();
						}
						return Errors.ENOENT;
					}
					break;
				case READ:
					System.err.println(String.format("newFile path: %s", newFile.getAbsolutePath()));
					if (!newCustFile.exists()){
						System.err.println("Error: Open called with READ on file that doesn't exist");
						try{
							newFile.delete();
						}
						catch (Exception e){
							e.printStackTrace();
						}
						return Errors.ENOENT;
					}
					
					break;
				
			}

			//initialize custFile object to store in Hashmap. Handle exceptions if necessary
			if (!newCustFile.isDirectory()){

				try {
					newCustFile.setRaf(mode);
				}	 
				catch (SecurityException e){
					System.err.println("Error: SecurityException caught, permissions denied on creation of RandomAccessFile in Open");
					System.err.println(String.format("path: %s, permissions: %s", newPath, mode));
					e.printStackTrace();
					return Errors.EPERM;
				}
				catch (FileNotFoundException e){
					System.err.println(String.format("Error: FileNotFoundException caught, no such file with path: %s", newPath));
					e.printStackTrace();
					return Errors.ENOENT;
				}
				catch (Exception e){
					System.err.println(String.format("Error: Unknown Exception Caught"));
					e.printStackTrace();
					throw e;
				}
			
			}
			//associate fd with custFile object
			fdMap.put(fd, newCustFile);

			//return fd to client
			System.err.println(String.format("Returning fd (%d) to client", fd));
			return fd;
		}

		/*
			[description]: closes file at the given fd, removing it from the fdMap
			[in]: fd (fd of file to close)
			[out]: 0 on success, proper error code on error
		*/

		public int close( int fd ) {
			System.err.println(String.format("####### Close called for fd %d", fd));
			if (!fdMap.containsKey(fd)){
				System.err.println("Error: Close called on fd which is not open");
				return Errors.EBADF;
			}

			//get file from map
			custFile currCustFile = fdMap.get(fd);
			File currFile = currCustFile.getFile();
			RandomAccessFile currRaf = currCustFile.getRaf();



			//write back to server and close RandomAccessFile
			if (!currCustFile.isDirectory() && currRaf != null){

				try{
					if (currCustFile.modified){
						currRaf.close();
						currCustFile.raf = null;
						System.out.println(String.format("Version of currCustFile is: %d", currCustFile.version));
						writeToServerInChunks(currCustFile);
						
					}
					else
					{
						currRaf.close();
						currCustFile.raf = null;
					}
					cache.close(currCustFile);
				}
				catch(IOException e){
					System.err.println("Error: IOException caught when attempting to close raf");
					e.printStackTrace();
					return Errors.EINVAL;
				}
				catch(NullPointerException e){
					e.printStackTrace();
					System.err.println("Error: NPE caught on close");
				}
				catch (Exception e){
					e.printStackTrace();
				}

				
				
			}


			//remove from map
			fdMap.remove(fd);

			return 0;
		}
		
		/*
			[description]: writes contents of file associated with cFile to the server in chunks of chunkSize
			[in]: cFile (custFile which contains information on the file which is being written to the server
		*/
		private void writeToServerInChunks(custFile cFile) throws IOException, SecurityException, RemoteException{
			System.err.println("Writing in chunks");
			String cachePathname = cacheDir + "/" + cFile.cacheFilePath;
			File file = new File(cachePathname);
			RandomAccessFile raf = new RandomAccessFile(file, "rw");
			cFile.length = file.length();
			System.err.println(String.format("Total length: %d", file.length()));
			long offset = 0;
			boolean firstIteration = true;
			while (offset < file.length()){
				long bytesToRead = Long.min(chunkSize, file.length()-offset);
				cFile.data = new byte[(int)bytesToRead];
				raf.seek(offset);
				raf.read(cFile.data, 0, (int)bytesToRead);
				server.chunkWrite(cFile, firstIteration);
				firstIteration = false;
				offset += bytesToRead;
			}
		}
		
		/*
			[description]: writes the data from buf to the file associated with the given fd
			[in]: fd (file descriptor of file to write to), buf (array of bytes to write to file)
			[out]: # of bytes written on success, proper error code on error
		*/
		public long write( int fd, byte[] buf ) {
			System.err.println(String.format("####### Write called for fd %d", fd)); 
			if (!fdMap.containsKey(fd)){
				System.err.println(String.format("Error: fd %d not currently open"));
				return Errors.EBADF;
			}

			//get custFile associated with fd
			custFile currCustFile = fdMap.get(fd);
			currCustFile.modified = true;
			File currFile = currCustFile.getFile();

			//check permissions
			if (currCustFile.getMode() != "rw"){
				System.err.println("Error: bad permissions for write");
				return Errors.EBADF;
			}

			//check if is directory
			
			if (currCustFile.isDirectory()){
				System.err.println("Error: write called on directory");
				return Errors.EISDIR;
			}
			if (!currFile.exists()){
				System.err.println("Error: write called with file that doesnt exist");
				return Errors.ENOENT;
			}

			long origSize = currFile.length();
			RandomAccessFile currRaf = currCustFile.getRaf();
			try {	
				currRaf.write(buf);
			}
			catch (IOException e){
				System.err.println("Error: IOException caught on write");
				e.printStackTrace();
				return Errors.EINVAL;
			}

			System.err.println(String.format("Wrote %d bytes", buf.length));
			cache.updateSize(currFile.length() - origSize);
			currCustFile.doesExist = true;

			return buf.length;
		}
	
		/*
			[description]: reads data from file associated with fd to buf
			[in]: fd (file descriptor of file to read from), buf (buffer to write to)
			[out]: # of bytes read on success, proper error code on error
		*/
		public long read( int fd, byte[] buf ) {
			System.err.println(String.format("####### Read called on fd %d", fd));
			if (!fdMap.containsKey(fd)){
				System.err.println(String.format("Error: fd %d not currently open", fd));
				return Errors.EBADF;
			}

			//get custFile associated with fd
			custFile currCustFile = fdMap.get(fd);
			File currFile = currCustFile.getFile();

			System.err.println(String.format("Pathname for read: %s", currFile.getAbsolutePath()));
			//check if is directory
			if (currCustFile.isDirectory()){
				System.err.println("Error: read called on directory");
				return Errors.EISDIR;
			}
			if (!currCustFile.exists()){
				System.err.println(String.format("Error: Read called on File that does not exist with pathname %s", currFile.getAbsolutePath()));
				try {
					currFile.delete();	
				}
				catch (Exception e){
					e.printStackTrace();
				}
				return Errors.ENOENT;
			}
			RandomAccessFile currRaf = currCustFile.getRaf();
			


			
			//read file contents

			long bytesRead = 0;
			try{
				bytesRead = (long) currRaf.read(buf);
			}
			catch (IOException e){
				System.err.println("Error: IOException on read");
				e.printStackTrace();
				return Errors.EINVAL;
			}
			if (bytesRead == -1){
				System.err.println("Warning: Read returned -1");
				return 0;
			}
			return bytesRead;
		}
		
		/*	
			[description]: sets the file offset for the given file
			[in]: fd (file descriptor of file), pos (position to set offset to), o (LseekOption which describes how to set offset based on pos)
			[out]: new file offset on success, proper error code on error
		*/
		public long lseek( int fd, long pos, LseekOption o ) {
			System.err.println(String.format("####### lseek called on fd %d", fd));
			if (!fdMap.containsKey(fd)){
				System.err.println(String.format("Error: fd %d not currently open", fd));
				return Errors.EBADF;
			}

			//get custFile associated with fd
			custFile currCustFile = fdMap.get(fd);
			File currFile = currCustFile.getFile();

			//check if is directory
			if (currCustFile.isDirectory()){
				System.err.println("Error: lseek called on directory");
				return Errors.EISDIR;
			}

			if (!currFile.exists()){
				System.err.println("Error: lseek called on file that does not exist");
				return Errors.ENOENT;
			}

			RandomAccessFile raf = currCustFile.getRaf();
			if (pos < 0){
				System.err.println("Error: lseek called with pos < 0");
				return Errors.EINVAL;
			}

			switch(o){
				case FROM_CURRENT:
					try{
						pos = pos + raf.getFilePointer();
					}
					catch (IOException e){
						e.printStackTrace();
						return Errors.EINVAL;
					}
					break;
				case FROM_START:
					break;
				case FROM_END:
					try{	
						pos = pos + raf.length();
					}
					catch (IOException e){
						e.printStackTrace();
						return Errors.EINVAL;
					}
					break;
				default:
					System.err.println("Error: lseek called with unknown option");
					return Errors.EINVAL;
			}
			try {
				raf.seek(pos);
			}
			catch (IOException e){
				e.printStackTrace();
				return Errors.EINVAL;
			}
			return pos;

		}


		/*
			[description]: removes the file on the server with the given path, if it exists
			[in]: path (name of path on server to delete)
			[out]: 0 on success, proper error code on error
		*/
		public int unlink( String path ) {
			System.err.println(String.format("###### Unlink called for path %s", path));

			int response = -4;

			try{
				response = server.unlink(path);
			}
			catch (Exception e){
				e.printStackTrace();
			}
			
			switch (response){
				case 0:
					return 0;
				case -1:
					return Errors.EPERM;
				case -2:
					return Errors.ENOENT;
				case -3:
					return Errors.EINVAL;
				default:
					System.err.println(String.format("retval on unlink: %d", response));
					return -4;
			}

						
			
		}

		public void clientdone() {
			return;
		}


		/*
			[description]: returns a distinct file descriptor to associate with file called with open
			[out]: fd
		*/
		private int getNewFd(){
			int retval = fdCounter;
			fdCounter += 1;
			System.err.println(String.format("fdCounter is %s", fdCounter));
			return retval;
		}
		
		/*
			[description]: converts OpenOption to string representing mode
			[in]: o (OpenOption open was called with)
			[out]: mode (String representing mode)
		*/
		private String optionToMode(OpenOption o){
			String mode;
			System.err.print("Calling optionToMode for ");
			switch (o){
				case READ:
					mode = "r";
					System.err.println("READ");
					break;
				case WRITE:
					mode = "rw";	
					System.err.println("WRITE");
					break;
				case CREATE:
					mode = "rw";
					System.err.println("CREATE");
					break;
				case CREATE_NEW:
					mode = "rw";
					System.err.println("CREATE_NEW");
					break;
				default:	
					System.err.println("Error: Unknown open option");
					mode = "failure";
					break;
			}
			return mode;
		}

	}
	
	private static class FileHandlingFactory implements FileHandlingMaking {
		public FileHandling newclient() {
			return new FileHandler();
		}
	}
	
		/*
			description: initializes cache and looks up server
			[in]: args (arguments passed in on command line)
		*/
	public static void main(String[] args) throws IOException {
		cacheDir = args[2];
		int cacheSize = Integer.parseInt(args[3]);

		try {
			Registry registry = LocateRegistry.getRegistry(args[0], Integer.parseInt(args[1]));
			server = (fileServerIntf) registry.lookup("Server:"+args[1]);
		}
		catch (Exception e){
			e.printStackTrace();
		}

		cache = new Cache(cacheDir, server, cacheSize, chunkSize);
		(new RPCreceiver(new FileHandlingFactory())).run();
	}
}

