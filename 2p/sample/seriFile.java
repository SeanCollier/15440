import java.io.*;

public class seriFile implements Serializable{

	public seriFile(byte[] newData, String newPathname, long newLen){
		data = newData;
		pathname = newPathname;
		len = newLen;
	}
}
