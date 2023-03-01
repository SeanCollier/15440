import java.io.*;
import java.util.*;


/*
	The CacheFile class is used to store relevant metadata relating to a field which is being cached in a Cache object.
*/
class CacheFile {
	public long version;
	public boolean readOnly;
	public String pathname;
	public int refCount;
	public long size;

	/*
		[description]: constructor for CacheFile class
		[in]: pathname (pathname of file), version (version number of file), readOnly (indicates whether this file can be written to or not)
	*/
	public CacheFile(String pathname, long version, boolean readOnly){
			this.pathname = pathname;
			this.version = version;
			this.readOnly = readOnly;
			this.refCount = 0;
	}
}
