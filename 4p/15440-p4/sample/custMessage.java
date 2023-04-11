import java.io.Serializable;

public class custMessage implements Serializable {
    String filename;
    boolean vote;
    String[] sources;
    byte[] img;
    int type;

    public custMessage( String filename, String[] sources, byte[] img, int type){
        this.filename = filename;
        this.sources = sources;
        this.img = img;
        this.type = type;
    }
}
