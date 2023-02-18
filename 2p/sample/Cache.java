import java.io.*;
import java.util.*;
import java.nio.file.*;

class Cache {	

	public LinkedHashMap<String, CacheFile> fileMap = new LinkedHashMap<String, CacheFile>();

	public String query(String pathname, String mode, custFile cFile){
		boolean readOnly = (mode == "w");
		Path path = Paths.get(pathname);
		Path normalPath = path.normalize();
		System.err.println(normalPath);
		return "a";
	}

}

