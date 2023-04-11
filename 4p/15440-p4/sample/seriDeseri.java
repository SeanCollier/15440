import java.io.*;

public class seriDeseri {
    public static byte[] serialize(custMessage cmsg){
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(cmsg);
            return baos.toByteArray();
        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public static custMessage deserialize(byte[] bytes){
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(bais);
            return (custMessage) ois.readObject();
        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }
}
