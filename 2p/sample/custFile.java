import java.io.*;
import java.util.*;

public class custFile implements Serializable{
	
	private File jFile;
	private String mode;


	public RandomAccessFile raf = null;
	public boolean isDir;
	public boolean doesExist;
	public byte[] data;	
	public String pathname;
	public String error = null;
	public long length;
	public boolean modified = false;
	


	public custFile(String pathname, String newMode){
		this.pathname = pathname;
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

	public boolean isDirectory(){
		return isDir;
	}

	public boolean exists(){
		return doesExist;
	}
}
