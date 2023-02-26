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

	public Cache (String root, fileServerIntf server){
		this.root = root;
		this.server = server;
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
				System.err.println("Loop");
				String nextKey = (String) keys.next();
				CacheFile nextCacheFile = cacheMap.get(nextKey);
				if (nextCacheFile.readOnly && nextCacheFile.pathname.startsWith(currCustFile.adjPathname) && nextCacheFile.refCount == 0 && nextCacheFile.version < currVersion){
					System.err.println(String.format("Deleting old readOnly version: %s", nextCacheFile.pathname));
					File file = new File(root + "/" + nextCacheFile.pathname);
					if (!file.delete()){
						System.err.println("Failed to delete file");
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
		else{
			currCacheFile.refCount -= 1;
		}

		

	}

	public String query(String pathname, String mode, custFile cFile){
		System.err.println(String.format("Querying cache for with pathname: %s", pathname));
		boolean readOnly = (mode == "w");
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


		
		if (recentCacheFile == null){

			System.err.println(String.format("Cache miss on pathname: %s. Checking server", pathname));
			try{
				cFile = server.open(cFile);
			}
			catch (RemoteException e){
				e.printStackTrace();
			}


			if (cFile.error != null){
				return null;
			}

			if (!cFile.exists() || cFile.isDirectory()){
				if (!cFile.exists()){
					System.err.println("file does not exist on server side");
				}
				if (cFile.isDirectory()){
					System.err.println("file is directory on server side");
				}
				return newPathname;
			}

			recentCacheFile = new CacheFile(newPathname + "_FETCHED", openedTime-1, true);
			File file = new File(root + "/" + newPathname+"_FETCHED");
			cacheMap.put(newPathname+"_FETCHED",recentCacheFile);
			try {
				file.createNewFile();
				RandomAccessFile raf = new RandomAccessFile(file, "rw");
				raf.write(cFile.data);
			}
			catch (IOException e){
				e.printStackTrace();
				return null;
			}
			catch (SecurityException e){
				e.printStackTrace();
				return null;
			}
		}



		//file was in cache, or was fetched from server
		//now have found the most recent read-only version. If mode is not readOnly, then must make a copy
		if (readOnly){
			recentCacheFile.refCount += 1;
			System.err.println("Is read only, returning");
			return recentCacheFile.pathname;
		}

		//custFile is not read only. Must copy data from most recent read-only file to new writable file

		CacheFile newCacheFile = new CacheFile(newPathname, openedTime, false);
		newCacheFile.refCount += 1;
		
		//Copy recentCacheFile's file to new file, return path to that new file
		Path origPath = Paths.get(root + "/" + recentCacheFile.pathname);
		Path copyPath = Paths.get(root + "/" + newPathname);
		try{
			Files.copy(origPath, copyPath, StandardCopyOption.REPLACE_EXISTING);
		}
		catch (IOException e){
			e.printStackTrace();
			cFile.error="IO";
			return null;
		}
		
		//Place newCacheFile in cache
		cacheMap.put(newPathname, newCacheFile);


		return newPathname;
	}

}

