import java.io.*;
import java.util.*;
import java.nio.file.*;
import java.lang.System.*;
import java.rmi.RemoteException;
import java.rmi.*;

class Cache {	

	public LinkedHashMap<String, CacheFile> cacheMap = new LinkedHashMap<String, CacheFile>();
	
	private String root;
	private fileServerIntf server;
	private long maxSize;
	private long currSize;
	private long chunkSize;

	public Cache (String root, fileServerIntf server, long maxSize, long chunkSize){
		this.root = root;
		this.server = server;
		this.maxSize = maxSize;
		currSize = 0;
		this.chunkSize = chunkSize;
	}

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
			Iterator keys = cacheMap.keySet().iterator();
			while (keys.hasNext()){
				String nextKey = (String) keys.next();
				CacheFile nextCacheFile = cacheMap.get(nextKey);
				/*
				System.err.println("____________________________");
				System.err.println(nextCacheFile.pathname);
				System.err.println(nextCacheFile.readOnly);
				System.err.println(nextCacheFile.pathname.startsWith(currCustFile.adjPathname));
				System.err.println(nextCacheFile.refCount);
				System.err.println(nextCacheFile.version < currVersion);
				System.err.println(nextCacheFile.version);
				System.err.println(currVersion);
				*/
				if (nextCacheFile.readOnly && nextCacheFile.pathname.startsWith(currCustFile.adjPathname) && nextCacheFile.refCount == 0 && nextCacheFile.version < currVersion){
					System.err.println(String.format("Deleting old readOnly version: %s", nextCacheFile.pathname));
					File file = new File(root + "/" + nextCacheFile.pathname);
					long fileSize = file.length();
					if (!file.delete()){
						System.err.println("Failed to delete file");
					}
					else{
						updateSize(-1*fileSize);
					}
					//collect hash set of keys which need to be deleted from cache
					toDelete.add(nextKey);
				}
			}

			//delete keys from cache
			Iterator keysToDelete = toDelete.iterator();
			while (keysToDelete.hasNext()){
				String key = (String) keysToDelete.next();
				cacheMap.remove(key);
			}
		}
		currCacheFile.refCount -= 1;

		

	}

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


		System.err.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");



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

	public void getFileInChunks(custFile cFile, File file) throws IOException, SecurityException, RemoteException{
		System.err.println("Getting file in chunks");
		long offset = chunkSize;
		RandomAccessFile raf = new RandomAccessFile(file, "rw");
		raf.write(cFile.data);
		while (offset <= cFile.length){
			System.err.println(String.format("offset: %d, file length: %d", offset, cFile.length));
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

	public void updateSize(long delta){
		System.err.println(String.format("Updating current size of cache (%d) by %d", currSize, delta));
		currSize += delta;

		System.err.println(String.format("New size: %d", currSize));
		
		//add LRU eviction logic here

	}

}

