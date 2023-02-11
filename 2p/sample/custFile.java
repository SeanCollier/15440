import java.io.*;
import java.util.*;

class custFile {
	
	private RandomAccessFile raf;
	private File jFile;

	public custFile(File fileObj, String mode) throws FileNotFoundException{
		raf = new RandomAccessFile(fileObj, mode);
	}
	
	public void setJFile(File fileObj){
		jFile = fileObj;
	}

}
