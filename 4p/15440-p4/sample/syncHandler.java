import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class syncHandler implements ProjectLib.MessageHandling {
    public ConcurrentHashMap<String, ConcurrentLinkedQueue<custMessage>> messageQueues = new ConcurrentHashMap<String, ConcurrentLinkedQueue<custMessage>>();

    public boolean deliverMessage (ProjectLib.Message msg){
        custMessage cmsg = seriDeseri.deserialize(msg.body);
        cmsg.addr = msg.addr;
        System.out.println(String.format("syncHandler received message from %s about collage %s with vote %b", cmsg.addr, cmsg.filename, cmsg.vote));
        updateQueue(cmsg);
        return true;
    } 

    private void updateQueue(custMessage cmsg){       
        if (!messageQueues.containsKey(cmsg.filename)){
            messageQueues.put(cmsg.filename, new ConcurrentLinkedQueue<custMessage>());
        }
        ConcurrentLinkedQueue<custMessage> clq = messageQueues.get(cmsg.filename);
        clq.add(cmsg);
    }

    public custMessage getNextElement(String filename){
        if (!messageQueues.containsKey(filename)){
            return null;
        }
        ConcurrentLinkedQueue clq = messageQueues.get(filename);
        return (custMessage) clq.poll();
    }
}
