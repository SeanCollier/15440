import java.io.*;


public class seriDeseri {
    
    /*
    * [description]: serializes a custmessage object into a byte array so it can be sent in a message
    * [in]: cmsg (custmessage to serialize)
    * [out]: byte array to send in a message body
    */
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

     /*
    * [description]: deserializes a message body byte array into a custMessage object
    * [in]: byte array to deserialize
    * [out]: custMessage object containing data sent in message body
    */
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
