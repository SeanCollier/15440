/* Skeleton code for UserNode */
import java.util.HashSet;


public class UserNode implements ProjectLib.MessageHandling {
	public final String myId;
    public HashSet<String> lockedFiles = new HashSet<String>();
	public UserNode( String id ) {
		myId = id;
	}

	public boolean deliverMessage( ProjectLib.Message msg ) {
		System.out.println( myId + ": Got message from " + msg.addr );
        custMessage cmsg = seriDeseri.deserialize(msg.body);
        System.out.println(String.format("Message type: %d", cmsg.type));
        System.out.println(String.format("Collage filename: %s", cmsg.filename));
		return true;
	}
	
	public static void main ( String args[] ) throws Exception {
		if (args.length != 2) throw new Exception("Need 2 args: <port> <id>");
		UserNode UN = new UserNode(args[1]);
		ProjectLib PL = new ProjectLib( Integer.parseInt(args[0]), args[1], UN );
		
		ProjectLib.Message msg = new ProjectLib.Message( "Server", "hello".getBytes() );
		System.out.println( args[1] + ": Sending message to " + msg.addr );
		PL.sendMessage( msg );
	}
}

