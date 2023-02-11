/* Sample skeleton for proxy */

import java.io.*;
import java.util.*;

class Proxy {

	
	private static class FileHandler implements FileHandling {

		private HashMap<Integer, custFile> fdMap = new HashMap<Integer, custFile>();
		private int fdCounter = 0;

		public int open( String path, OpenOption o ) {
			System.err.println(String.format("##### Open Called with Pathname: %s", path));
			int fd = getNewFd();
			File newFile = new File(path);
			if (newFile == null){
				System.err.println("Error: Attempt to create File object from path failed. Got null");
			}
			custFile newCustFile;
			String mode = optionToMode(o);
			if (mode == "failure"){
				return Errors.EINVAL;
			}
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

			return Errors.ENOSYS;
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
		System.out.println("Hello World");
		(new RPCreceiver(new FileHandlingFactory())).run();
	}
}

