/* Skeleton code for Server */

import java.util.*;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class Server implements ProjectLib.CommitServing {

    public static ProjectLib PL;
	
	public void startCommit( String filename, byte[] img, String[] sources ) {
		System.out.println( "Server: Got request to commit "+filename );
        TPC tpc = new TPC(filename, img, sources, PL);
        Thread t = new Thread(tpc);
        t.start();
	}
	
	public static void main ( String args[] ) throws Exception {
		if (args.length != 1) throw new Exception("Need 1 arg: <port>");
		Server srv = new Server();
		PL = new ProjectLib( Integer.parseInt(args[0]), srv );

		
		// main loop
		/*while (true) {
			ProjectLib.Message msg = PL.getMessage();
			System.out.println( "Server: Got message from " + msg.addr );
			System.out.println( "Server: Echoing message to " + msg.addr );
			PL.sendMessage( msg );
		}*/
	}
    

    public static class TPC implements Runnable {
        private String filename;
        private byte[] img;
        private String[] sources;
        private ProjectLib PL;

        private ConcurrentHashMap<String, ArrayList<String>> userSources = new ConcurrentHashMap<String, ArrayList<String>>();

        public TPC (String filename, byte[] img, String[] sources, ProjectLib PL){
            this.filename = filename;
            this.img = img;
            this.sources = sources;
            this.PL = PL;
        }

        public void run(){
            for (int i = 0; i < sources.length; i++){
                String source = sources[i];
                String[] splitSource = source.split(":", 2);
                String user = splitSource[0];
                String file = splitSource[1];
                ArrayList val;
                if (userSources.containsKey(user)){
                    val = userSources.get(user);
                }
                else{
                    val = new ArrayList<String>();
                }
                val.add(file);
                userSources.put(user, val);
            }
            System.out.println(userSources);
            collectVotes();
        }

        public void collectVotes(){
            Iterator<String> iter = userSources.keySet().iterator();
            while (iter.hasNext()){
                String user = iter.next();
                ArrayList<String> sourceAL = userSources.get(user);
                String[] sourceArr = new String[sourceAL.size()];
                for (int i = 0; i < sourceAL.size(); i++){
                    sourceArr[i] = sourceAL.get(i);
                }
                custMessage cmsg = new custMessage(filename, sourceArr, img, 0);
                byte[] body = seriDeseri.serialize(cmsg);
                ProjectLib.Message msg = new ProjectLib.Message(user, body);
                PL.sendMessage(msg);
            }
        }

    }
}

