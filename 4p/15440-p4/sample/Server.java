/* Skeleton code for Server */

import java.util.*;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.io.*;
import java.nio.file.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Server implements ProjectLib.CommitServing {

    public static ProjectLib PL;
    public static syncHandler sh;
    
    public static int VOTE_REQUEST = 0;
    public static int DISTRIBUTE_DECISION = 1;
    public static int ACK = 2;

    public static int TIMEOUT = 6000;
	
	public void startCommit( String filename, byte[] img, String[] sources ) {
		System.out.println( "Server: Got request to commit "+filename );
        TPC tpc = new TPC(filename, img, sources, PL);
        Thread t = new Thread(tpc);
        t.start();
	}
	
	public static void main ( String args[] ) throws Exception {
		if (args.length != 1) throw new Exception("Need 1 arg: <port>");
		Server srv = new Server();
        sh = new syncHandler();
		PL = new ProjectLib( Integer.parseInt(args[0]), srv, sh );

		
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
        private int numTotalVotes;

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
            numTotalVotes = userSources.size();
            boolean shouldCommit = collectVotes();
            System.out.println(String.format("shouldCommit for filename %s is %b", filename, shouldCommit));

            if (shouldCommit){
                commit();
            }

            notifyUsers(userSources, shouldCommit);
            System.out.println(String.format("All users successfully notified for %s", filename));

        }

        public void notifyUsers(ConcurrentHashMap<String, ArrayList<String>> userSources, boolean committed) {
            while (userSources.size() != 0){
                //send messages to users notifying them of decision
                System.out.println("Notifying users");
                Iterator<String> users = userSources.keySet().iterator();
                while (users.hasNext()){
                    String user = users.next();
                    ArrayList<String> sourceAL = userSources.get(user);
                    String[] sourceArr = new String[sourceAL.size()];
                    for (int i = 0; i < sourceAL.size(); i++){
                        sourceArr[i] = sourceAL.get(i);
                    }

                    custMessage postCommitMsg = new custMessage(filename, sourceArr, null, DISTRIBUTE_DECISION);
                    postCommitMsg.vote = committed;
                    byte[] pcBody = seriDeseri.serialize(postCommitMsg);
                    ProjectLib.Message msg = new ProjectLib.Message(user, pcBody);
                    PL.sendMessage(msg);
                }
                
                long startTime = System.currentTimeMillis();
                long endTime = startTime;
                boolean timedOut = false;

                while (userSources.size() != 0 && !timedOut){
                    custMessage nextMessage = sh.getNextElement(filename);
                    endTime = System.currentTimeMillis();
                    if (nextMessage != null && nextMessage.type == ACK){
                        System.out.println(String.format("Got ack from %s for filename %s", nextMessage.addr, filename));
                        userSources.remove(nextMessage.addr);
                    }
                    else if (endTime - startTime > TIMEOUT && userSources.size() != 0){
                        System.out.println("Timed out while notifying users. Redistributing decision");
                        timedOut = true;
                    }
                }

            }

        }

        public void commit(){
            try{
                FileOutputStream fos = new FileOutputStream(filename);
                fos.write(img);
                fos.close();
                PL.fsync();
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }

        public boolean collectVotes(){
            Iterator<String> iter = userSources.keySet().iterator();
            while (iter.hasNext()){
                String user = iter.next();
                ArrayList<String> sourceAL = userSources.get(user);
                String[] sourceArr = new String[sourceAL.size()];
                for (int i = 0; i < sourceAL.size(); i++){
                    sourceArr[i] = sourceAL.get(i);
                }
                custMessage cmsg = new custMessage(filename, sourceArr, img, VOTE_REQUEST);
                byte[] body = seriDeseri.serialize(cmsg);
                ProjectLib.Message msg = new ProjectLib.Message(user, body);
                PL.sendMessage(msg);
            }

            System.out.println(sources.length);
            int collectedVotes = 0;
            boolean shouldCommit = true;

            long startTime = System.currentTimeMillis();
            long currTime = startTime;
            boolean timedOut = false;

            while (collectedVotes < numTotalVotes){
                custMessage nextMessage = sh.getNextElement(filename);
                currTime = System.currentTimeMillis();
                if (nextMessage != null){
                    System.out.println(String.format("nextMessage vote is %b", nextMessage.vote));
                    collectedVotes+=1;
                    if (!nextMessage.vote){
                        shouldCommit = false;
                        break;
                    }
                }
                else if (currTime - startTime > TIMEOUT && collectedVotes < numTotalVotes){
                    shouldCommit = false;
                    timedOut = true;
                    break;
                }
            }
            if (timedOut){
                System.out.println(String.format("Timed out when collecting responses, defaulting to shouldCommit = %b", shouldCommit));
            }
            System.out.println("Here");
            return shouldCommit;
        }

    }
}

