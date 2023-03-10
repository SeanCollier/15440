import java.io.*;
import java.util.*;
import java.nio.file.*;
import java.lang.System.*;
import java.rmi.RemoteException;
import java.rmi.*;

/*
	The Cache class is used to act as a local cache for files which are fetched from a server. This allows RPC calls on a file system to be made without having to constantly fetch directly from the server
*/

class Cache {	

	public LinkedHashMap<String, CacheFile> cacheMap = new LinkedHashMap<String, CacheFile>(16, 0.75f, true);
	
	private String root;
	private fileServerIntf server;
	private long maxSize;
	private long currSize;
	private long chunkSize;

	/*
		[description]: Constructor for Cache class
		[in]: root (root directory to write cached files to), server (server to fetch files from), maxSize (maximum cache size, enforced by LRU replacement), chunkSize (size of chunks to read files from the server in)
	*/
	public Cache (String root, fileServerIntf server, long maxSize, long chunkSize){
		this.root = root;
		this.server = server;
		this.maxSize = maxSize;
		currSize = 0;
		this.chunkSize = chunkSize;
	}

	/*
		[description]: converts currCustFile to readOnly, deletes old readOnly files, and reduces currCustFile's refCount
		[in]: currCustFile (custFile object which contains information on the file being closed
	*/
	public void close(custFile currCustFile){
		//convert cacheFile corresponding to the custFile to write-only version
		System.err.println(String.format("Close called on cache for pathname: %s", currCustFile.cacheFilePath));
		String pathname = currCustFile.cacheFilePath;
		CacheFile currCacheFile = cacheMap.get(pathname);
		long currVersion = currCacheFile.version;
		Set toDelete = new HashSet<String>();

		//remove previous read only versions from cache if refcount = 0
		if (!currCacheFile.readOnly){
			currCacheFile.readOnly = true;
			//Iterator keys = cacheMap.keySet().iterator();
			Iterator values = cacheMap.values().iterator();
			while (values.hasNext()){
				//String nextKey = (String) keys.next();
				CacheFile nextCacheFile = (CacheFile) values.next();	
				if (nextCacheFile.readOnly && nextCacheFile.pathname.startsWith(currCustFile.adjPathname) && nextCacheFile.refCount == 0 && nextCacheFile.version < currVersion){
					System.err.println(String.format("Deleting old readOnly version: %s", nextCacheFile.pathname));
					File file = new File(root + "/" + nextCacheFile.pathname);
					long fileSize = file.length();
					if (!file.delete()){
						System.err.println("Failed to delete file");
					}
					else{
						updateSize(-1*fileSize);
						values.remove();
						printHashMap();
					}
				}
			}	
		}
		currCacheFile.refCount -= 1;

		

	}

	/*
		[description]: prints all elements in the cache at a given time, used for debugging
	*/
	public void printHashMap(){
		Iterator values = cacheMap.values().iterator();
		System.err.println("-------------- PRINTING CACHE -----------------");
		while (values.hasNext()){
			CacheFile nextCacheFile = (CacheFile) values.next();
			System.err.println(String.format("--%s--", nextCacheFile.pathname));
		}
		System.err.println("-----------------------------------------------");
	}

	/*
		[description]: deletes stale files (old readOnly files which will have been made useless by a newer readOnly version of the same file)
		[in]: pathname (pathname of file for which to search for and delete stale versions)
	*/
	public void deleteStaleFiles(String pathname){
		System.err.println(String.format("Deleting stale files for %s", pathname));
		Iterator values2 = cacheMap.values().iterator();
		while(values2.hasNext()){
			CacheFile nextCacheFile = (CacheFile) values2.next();
			if (nextCacheFile.readOnly & nextCacheFile.pathname.startsWith(pathname) && nextCacheFile.refCount == 0){
				System.err.println(String.format("Deleting %s", nextCacheFile.pathname));
				File file = new File(root + "/" + nextCacheFile.pathname);
				long fileSize = file.length();
				if (!file.delete()){
					System.err.println("Failed to delete file");
				}
				else{
					updateSize(-1*fileSize);
					values2.remove();
					printHashMap();
			
				}
			}
		}
	}

	/*
		[description]: queries the cache for the file with the given pathname. Fetches file from server if need be
		[in]: pathname (name of file which the user wants), mode (mode used to open file), cFile (custFile object which contains information about the requested file)
		[out]: custFile containing extra information about the requested file, including the path where it exists in the cache
	*/
	public custFile query(String pathname, String mode, custFile cFile){
		System.err.println(String.format("Querying cache for with pathname: %s", pathname));
		boolean readOnly = (mode == "r");
		Path path = Paths.get(pathname);
		Path normalPath = path.normalize();
		String adjPath = normalPath.toString().replaceAll("/", "#@#");
		cFile.adjPathname = adjPath;
		long openedTime = System.nanoTime();
		String newPathname = adjPath + "_" + Long.toString(openedTime);

		//check to see if file already in cache
		CacheFile recentCacheFile = null;
		long mostRecentVersion = 0;
		Iterator values = cacheMap.values().iterator();
		CacheFile currFile;
		while (values.hasNext()){
			currFile = (CacheFile) values.next();
			if (currFile.readOnly && currFile.version > mostRecentVersion && currFile.pathname.startsWith(adjPath)){
				mostRecentVersion = currFile.version;
				recentCacheFile = currFile;

			}
		}

		long serverVersion = -100;

		try {
			serverVersion = server.checkVersion(cFile.pathname);
		}
		catch (Exception e){
			e.printStackTrace();
			return null;
		}	

		System.err.println(String.format("mostRecentVersion: %d, serverVersion: %d", mostRecentVersion, serverVersion));
		
		if (recentCacheFile == null || serverVersion != mostRecentVersion){
			// File is not in cache or cached version is out of date
			if (recentCacheFile == null){
				System.err.println(String.format("Cache miss on pathname: %s. Checking server", pathname));
			}
			
			else if (serverVersion != mostRecentVersion)  {
				System.err.println(String.format("Cache version (%d) is not up to date with server version (%d).", mostRecentVersion, serverVersion));
				mostRecentVersion = serverVersion;
			}

			try{
				cFile = server.open(cFile, chunkSize);
			}
			catch (Exception e){
				e.printStackTrace();
			}	

			if (!cFile.exists()){
				// File doesn't exist on server side, must create a new one and add to the cache
				System.err.println("file does not exist on server side");
				String finalPath = newPathname + "_NEW";
				CacheFile newCacheFile = new CacheFile(finalPath, openedTime, readOnly);
				newCacheFile.refCount += 1;
				File file = new File(root + "/" + finalPath);
				try{
					file.createNewFile();
				}
				catch (Exception e){
					e.printStackTrace();
					return null;
				}
				deleteStaleFiles(adjPath);
				cacheMap.put(finalPath, newCacheFile);
				cFile.version = openedTime;
				System.err.println(String.format("cFile version is %d", openedTime));
				cFile.returnPath = finalPath;
				return cFile;
			}


			if (cFile.isDirectory()){
				if (cFile.isDirectory()){
					System.err.println("file is directory on server side");
				}
				cFile.returnPath = newPathname;
				return cFile;
			}
			if (recentCacheFile != null){
				//local cached version was out of date, must delete stale versions
				deleteStaleFiles(adjPath);
			}

			recentCacheFile = new CacheFile(newPathname + "_FETCHED", serverVersion, true);
			File file = new File(root + "/" + newPathname+"_FETCHED");
			cacheMap.put(newPathname+"_FETCHED",recentCacheFile);
			try {
				file.createNewFile();
				getFileInChunks(cFile, file);	
				if (cFile.error != null){
					return null;
				}
				updateSize(file.length());
			}
			catch (IOException e){
				e.printStackTrace();
				return null;
			}
			catch (SecurityException e){
				e.printStackTrace();
				return null;
			}
			catch (Exception e){
				e.printStackTrace();
				return null;
			}
		}
		else{
			System.err.println("Here");
			cFile.doesExist = true;
		}





		//file was in cache, or was fetched from server
		//now have found the most recent read-only version. If mode is not readOnly, then must make a copy
		if (readOnly){
			cFile.version = recentCacheFile.version;
			recentCacheFile.refCount += 1;
			System.err.println("Is read only, returning");
			cFile.returnPath = recentCacheFile.pathname;
			return cFile;
		}

		//custFile is not read only. Must copy data from most recent read-only file to new writable file

		CacheFile newCacheFile = new CacheFile(newPathname, openedTime, false);
		cFile.version = openedTime;
		System.err.println(String.format("cFile.version has been set to: %d", cFile.version));
		newCacheFile.refCount += 1;
		
		//Copy recentCacheFile's file to new file, return path to that new file
		Path origPath = Paths.get(root + "/" + recentCacheFile.pathname);
		Path copyPath = Paths.get(root + "/" + newPathname);
		File newFile = new File(root + "/" + newPathname);
		try{
			Files.copy(origPath, copyPath, StandardCopyOption.REPLACE_EXISTING);
			updateSize(newFile.length());
		}
		catch (IOException e){
			e.printStackTrace();
			cFile.error="IO";
			return null;
		}
		
		//Place newCacheFile in cache
		cacheMap.put(newPathname, newCacheFile);

		System.err.println(String.format("cFile version on return: %d", cFile.version));
		cFile.returnPath = newPathname;

		return cFile;
	}

	/*
		[description]: reads the file associated with cFile in chunks from the server
		[in]: cFile (custFile containing information about the file which is needed), file (File object which references the file to be read)
	*/
	public void getFileInChunks(custFile cFile, File file) throws IOException, SecurityException, RemoteException{
		System.err.println("Getting file in chunks");
		long offset = chunkSize;
		RandomAccessFile raf = new RandomAccessFile(file, "rw");
		raf.write(cFile.data);
		while (offset < cFile.length){
			System.err.println(String.format("offset: %d, file length: %d", offset, cFile.length));
			cFile.data = null;
			cFile = server.chunkRead(cFile, offset, chunkSize);
			if (cFile.error != null){
				return;
			}
			raf.seek(file.length());
			raf.write(cFile.data);
			offset = file.length();
		}
		raf.close();
	}

	/*
		[description]: updates the current size of the files stored in the cache
		[in]: delta (value to add to the current size)
	*/
	public void updateSize(long delta){
		System.err.println(String.format("Updating current size of cache (%d) by %d", currSize, delta));
		currSize += delta;

		System.err.println(String.format("New size: %d", currSize));
		
		//add LRU eviction logic here
		if (currSize > maxSize){
			LRUEvict();
		}

	}

	/*
		[description]: evicts an element from the cache according to LRU
	*/
	//evicts element according to LRU
	public void LRUEvict(){
		System.err.println("LRU Evicting an element");
		Iterator values = cacheMap.values().iterator();
		long fileSize = 0;
		while (values.hasNext()){
			CacheFile nextCacheFile = (CacheFile) values.next();
			System.err.println(nextCacheFile.pathname);
			System.err.println(nextCacheFile.readOnly);
			System.err.println(nextCacheFile.refCount);
			if (nextCacheFile.readOnly && nextCacheFile.refCount == 0){
				System.err.println(String.format("Evicting file version: %s", nextCacheFile.pathname));
				File file = new File(root + "/" + nextCacheFile.pathname);
				fileSize = file.length();
				if (!file.delete()){
					System.err.println("Failed to delete file");
				}
				else{
					values.remove();
					break;
				}
			}
		}
		printHashMap();
		updateSize(-1*fileSize);
	}

}

