import java.io.*;
import java.util.*;

class custFile {
	
	private RandomAccessFile raf = null;
	private File jFile;
	private String mode;

	public custFile(File fileObj, String newMode){
		jFile = fileObj;
		mode = newMode;
	}
	public void setRaf(String mode) throws FileNotFoundException{
		raf = new RandomAccessFile(jFile, mode);
	}
	
	public void setJFile(File fileObj){
		jFile = fileObj;
	}
		
	public File getFile(){
		return jFile;
	}

	public String getMode(){
		return mode;
	}

	public RandomAccessFile getRaf(){
		return raf;
	}
}
