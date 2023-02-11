import java.io.*;
import java.util.*;

class custFile {
	
	private RandomAccessFile raf;

	public custFile(File fileObj, String mode) throws FileNotFoundException{
		raf = new RandomAccessFile(fileObj, mode);
	}

}
