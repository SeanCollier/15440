/* Skeleton code for UserNode */
import java.util.HashSet;
import java.io.File;
import java.nio.file.Paths;
import java.nio.file.*;

public class UserNode implements ProjectLib.MessageHandling {
	public final String myId;
    public HashSet<String> lockedFiles = new HashSet<String>();
    private static ProjectLib PL;

    public static int VOTE_REQUEST = 0;
    public static int DISTRIBUTE_DECISION = 1;
    public static int ACK = 2;

	public UserNode( String id ) {
		myId = id;
	}

	public boolean deliverMessage( ProjectLib.Message msg ) {
		System.out.println( myId + ": Got message from " + msg.addr );
        custMessage cmsg = seriDeseri.deserialize(msg.body);
        System.out.println(String.format(myId + ": Message type: %d", cmsg.type));
        System.out.println(String.format(myId + ": Collage filename: %s", cmsg.filename));
        if (cmsg.type == VOTE_REQUEST){
            boolean vote = getVote(cmsg);
            custMessage response = new custMessage(cmsg.filename, null, null, cmsg.type);
            response.vote = vote;
            byte[] body = seriDeseri.serialize(response);
            ProjectLib.Message responseMsg = new ProjectLib.Message(msg.addr, body);
            PL.sendMessage(responseMsg);
        }
        else if (cmsg.type == DISTRIBUTE_DECISION){
            ackReply(cmsg, msg.addr);
        }
        return true;
	}

    public void ackReply(custMessage cmsg, String addr){
        System.out.println(String.format("%s: reply received about filename %s, which committed is %b", myId, cmsg.filename, cmsg.vote));
        for (int i = 0; i < cmsg.sources.length; i ++){
            String currSource = cmsg.sources[i];
            System.out.println(currSource);
            lockedFiles.remove(currSource);
            if (cmsg.vote){
                try {
                    Path path = Paths.get(currSource);
                    Files.deleteIfExists(path);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        }

        //send back ack 
        custMessage response = new custMessage(cmsg.filename, null, null, ACK);
        byte[] body = seriDeseri.serialize(response);
        ProjectLib.Message responseMsg = new ProjectLib.Message(addr, body);
        PL.sendMessage(responseMsg);

    }

    public boolean getVote(custMessage cmsg){
        boolean vote = true;
        for (int i = 0; i < cmsg.sources.length; i ++){
            String name = cmsg.sources[i];
            if (lockedFiles.contains(name)){
                vote = false;
                break;
            }
            File file = new File(name);
            if (!file.exists()){
                vote = false;
                break;
            }
        }
        if (!vote){
            return false;
        }
        for (int i = 0; i < cmsg.sources.length; i ++){
            String name = cmsg.sources[i];
            lockedFiles.add(name);
        }
        vote = PL.askUser(cmsg.img, cmsg.sources);
        System.out.println(String.format("My vote is: %b", vote));
        return vote;
    }
	
	public static void main ( String args[] ) throws Exception {
		if (args.length != 2) throw new Exception("Need 2 args: <port> <id>");
		UserNode UN = new UserNode(args[1]);
	    PL = new ProjectLib( Integer.parseInt(args[0]), args[1], UN );
		
        /*
		ProjectLib.Message msg = new ProjectLib.Message( "Server", "hello".getBytes() );
		System.out.println( args[1] + ": Sending message to " + msg.addr );
		PL.sendMessage( msg );
        */
	}
}

