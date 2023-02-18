import java.io.*;
import java.util.*;
import java.nio.file.*;
import java.lang.System.*;

class Cache {	

	public LinkedHashMap<String, CacheFile> cacheMap = new LinkedHashMap<String, CacheFile>();

	public String query(String pathname, String mode, custFile cFile){
		boolean readOnly = (mode == "w");
		Path path = Paths.get(pathname);
		Path normalPath = path.normalize();
		String adjPath = normalPath.toString().replaceAll("/", "#@#");
		long openedTime = System.nanoTime();
		String newPath = adjPath + "_" + Long.toString(openedTime);

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

		//TODO: if file is not in cache, fetch from server, and create read only and written version, add (both?) to cache, make sure to handle case where file doesnt exist, or is directory
		//For now, assume exists, is in cache, and is not directory. In future, this will be set by the custFile returned by server
		cFile.doesExist = true;
		cFile.isDir = false;
		if (recentCacheFile == null){
			recentCacheFile = new CacheFile(newPath, openedTime, true);
			File file = new File(newPath);
			try {
				file.createNewFile();
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
			return recentCacheFile.pathname;
		}

		//custFile is not read only. Must copy data from most recent read-only file to new writable file

		CacheFile newCacheFile = new CacheFile(newPath, openedTime, false);
		File file = new File(newPath);
		try {
			file.createNewFile();
		}
		catch (IOException e){
			e.printStackTrace();
			cFile.error = "IO";
			return null;
		}
		catch (SecurityException e){
			e.printStackTrace();
			cFile.error = "Security";
			return null;
		}
		//TODO: Copy recentCacheFile's file to new file, return path to that new file

		return newPath;
	}

}

