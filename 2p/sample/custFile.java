import java.io.*;
import java.util.*;


/*
	The custFile class is the object which contains metadata about a file. custFiles are serializable to allow transmission between client and server
*/
public class custFile implements Serializable{
	
	private File jFile;

	public String mode;
	public RandomAccessFile raf = null;
	public boolean isDir;
	public boolean doesExist;
	public byte[] data;	
	public String pathname;
	public String error = null;
	public long length;
	public boolean modified = false;
	public String cacheFilePath;
	public String adjPathname;
	public String returnPath;
	public long version;


	/*
		[description]: constructor for custFile class
		[in]: pathname (pathname of associated file), newMode (mode used for opening file)
		[out]:
	*/
	public custFile(String pathname, String newMode){
		this.pathname = pathname;
		mode = newMode;
	}
	
	/*
		[description]: sets the RandomAccessFile associated with this custFile to the given RandomAccessFile
		[in]: RandomAcccessFile to set raf to
	*/
	public void setRaf(String mode) throws FileNotFoundException{
		raf = new RandomAccessFile(jFile, mode);
	}
	
	/*
		[description]: setter for JFIle field
		[in]: fileObj to set jFile to
	*/
	public void setJFile(File fileObj){
		jFile = fileObj;
	}
		
	/*
		[description]: getter for jFile field
		[out]: jFile (File object)
	*/
	public File getFile(){
		return jFile;
	}

	/*
		[description]: getter for mode
		[out]: mode
	*/
	public String getMode(){
		return mode;
	}

	/*
		[description]: getter for raf 
		[out]: raf (RandomAccessFile)
	*/
	public RandomAccessFile getRaf(){
		return raf;
	}

	/*
		[description]: getter for isDir, attribute which describes if this custFile is a directory or not
		[out]: isDir
	*/
	public boolean isDirectory(){
		return isDir;
	}

	/*
		[description]: getter for doesExist, which indicates if the associated file exists
		[out]: doesExist
	*/
	public boolean exists(){
		return doesExist;
	}
}
