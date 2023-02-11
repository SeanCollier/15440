/* Sample skeleton for proxy */

import java.io.*;
import java.util.*;

class Proxy {

	
	private static class FileHandler implements FileHandling {	


		private HashMap<Integer, custFile> fdMap = new HashMap<Integer, custFile>();
		private int fdCounter = 0;

		public int open( String path, OpenOption o ) {
			System.err.println(String.format("##### Open Called with Pathname: %s", path));


			//generate new fd for file at path
			int fd = getNewFd();

			//check if fd is already in map, with current fd policy this shouldnt happen
			if (fdMap.containsKey(fd)){
				System.err.println(String.format("WARNING: newly generated fd (%d) already exists in fdMap", fd));
			}

			//define new File object, and check if null
			File newFile = new File(path);
			if (newFile == null){
				System.err.println("Error: Attempt to create File object from path failed. Got null");
			}
			custFile newCustFile;

			//convert openOption to mode for creation of RandomAccessFile
			String mode = optionToMode(o);
			if (mode == "failure"){
				return Errors.EINVAL;
			}

			//check for errors based on openOption
			switch(o){
				case CREATE_NEW:
					if (newFile.exists()){
						System.err.println("Error: Open called with CREATE_NEW on file which already exists");
						return Errors.EEXIST;
					}
					if (newFile.isDirectory()){
						System.err.println("Error: Open called with CREATE_NEW on directory");
						return Errors.EISDIR;
					}
					break;
				case CREATE:
					if (newFile.isDirectory()){
						System.err.println("Error: Open called with CREATE on directory");
						return Errors.EISDIR;
					}
				case WRITE:
					if (newFile.isDirectory()){
						System.err.println("Error: Open called with WRITE on directory");
						return Errors.EISDIR;
					}
					if (!newFile.exists()){
						System.err.println("Error: Open called with WRITE on file that doesn't exist");
						return Errors.ENOENT;
					}
					break;
				case READ:
					if (!newFile.exists()){
						System.err.println("Error: Open called with READ on file that doesn't exist");
						return Errors.ENOENT;
					}
					break;
				
			}

			//initialize custFile object to store in Hashmap. Handle exceptions if necessary
			try {
				newCustFile = new custFile(newFile, mode);
			} 
			catch (SecurityException e){
				System.err.println("Error: SecurityException caught, permissions denied on creation of RandomAccessFile in Open");
				System.err.println(String.format("path: %s, permissions: %s", path, mode));
				e.printStackTrace();
				return Errors.EPERM;
			}
			catch (FileNotFoundException e){
				System.err.println(String.format("Error: FileNotFoundException caught, no such file with path: %s", path));
				e.printStackTrace();
				return Errors.ENOENT;
			}
			catch (Exception e){
				System.err.println(String.format("Error: Unknown Exception Caught"));
				e.printStackTrace();
				throw e;
			}
			
			//given that no exceptions were thrown, set internal File object in the custFile
			newCustFile.setJFile(newFile);
			
			//associate fd with custFile object
			fdMap.put(fd, newCustFile);

			//return fd to client
			System.err.println(String.format("Returning fd (%d) to client", fd));
			return fd;
		}

		public int close( int fd ) {
			return Errors.ENOSYS;
		}

		public long write( int fd, byte[] buf ) {
			return Errors.ENOSYS;
		}

		public long read( int fd, byte[] buf ) {
			return Errors.ENOSYS;
		}

		public long lseek( int fd, long pos, LseekOption o ) {
			return Errors.ENOSYS;
		}

		public int unlink( String path ) {
			return Errors.ENOSYS;
		}

		public void clientdone() {
			return;
		}

		private int getNewFd(){
			int retval = fdCounter;
			fdCounter += 1;
			System.err.println(String.format("fdCounter is %s", fdCounter));
			return retval;
		}
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

	public static void main(String[] args) throws IOException {
		(new RPCreceiver(new FileHandlingFactory())).run();
	}
}

