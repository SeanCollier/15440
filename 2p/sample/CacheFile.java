import java.io.*;
import java.util.*;

class CacheFile {
	public long version;
	public boolean readOnly;
	public String pathname;
	public int refCount;
	public long size;

	public CacheFile(String pathname, long version, boolean readOnly){
			this.pathname = pathname;
			this.version = version;
			this.readOnly = readOnly;
			this.refCount = 0;
	}
}
