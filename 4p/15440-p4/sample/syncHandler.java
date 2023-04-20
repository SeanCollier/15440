import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class syncHandler implements ProjectLib.MessageHandling {
    public ConcurrentHashMap<String, ConcurrentLinkedQueue<custMessage>> messageQueues = new ConcurrentHashMap<String, ConcurrentLinkedQueue<custMessage>>();

    /*
    * [description]: called upon receiving a message fromt he server, deserializes message and adds it to a queue based on which collage the message refers to
    * [in]: msg (Message received from server)
    * [out]: true
    */
    public boolean deliverMessage (ProjectLib.Message msg){
        custMessage cmsg = seriDeseri.deserialize(msg.body);
        cmsg.addr = msg.addr;
        System.out.println(String.format("syncHandler received message from %s about collage %s with vote %b", cmsg.addr, cmsg.filename, cmsg.vote));
        updateQueue(cmsg);
        return true;
    } 

    /*
    * [description]: adds cmsg to its corresponding queue based on the collage it refers to
    * [in]: cmsg (custMessage object to add to a queue)
    */
    private void updateQueue(custMessage cmsg){       
        if (!messageQueues.containsKey(cmsg.filename)){
            messageQueues.put(cmsg.filename, new ConcurrentLinkedQueue<custMessage>());
        }
        ConcurrentLinkedQueue<custMessage> clq = messageQueues.get(cmsg.filename);
        clq.add(cmsg);
    }

    /*
    * [description]: gets the next element in the queue which corresponds to the given filename
    * [in]: filename of collage corresponding to desired queue
    * [out]: custMessage object representing the next message on the queue
    */
    public custMessage getNextElement(String filename){
        if (!messageQueues.containsKey(filename)){
            return null;
        }
        ConcurrentLinkedQueue clq = messageQueues.get(filename);
        return (custMessage) clq.poll();
    }
}
