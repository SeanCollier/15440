/* Sample code for basic Server */
import java.rmi.*;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.Naming;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.BlockingQueue;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.concurrent.LinkedBlockingQueue;


public class Server extends UnicastRemoteObject implements serverIntf {
    public static BlockingQueue<Cloud.FrontEndOps.Request> requestQueue = new LinkedBlockingQueue<Cloud.FrontEndOps.Request>();
    public static HashSet<Integer> frontEndIds = new HashSet<Integer>();
    public static HashSet<Integer> backEndIds = new HashSet<Integer>();

    public static int numFrontEnds = 0;
    public static int numBackEnds = 0;

       

    protected Server() throws RemoteException{
        super(0);
    }

	public static void main ( String args[] ) throws Exception {
		// Cloud class will start one instance of this Server intially [runs as separate process]
		// It starts another for every startVM call [each a seperate process]
		// Server will be provided 3 command line arguments
		if (args.length != 3) throw new Exception("Need 3 args: <cloud_ip> <cloud_port> <VM id>");
		
		// Initialize ServerLib.  Almost all server and cloud operations are done 
		// through the methods of this class.  Please refer to the html documentation in ../doc
		ServerLib SL = new ServerLib( args[0], Integer.parseInt(args[1]) );
		// get the VM id for this instance of Server in case we need it
		int myVMid = Integer.parseInt(args[2]);
		int port = Integer.parseInt(args[1]);
        Registry registry = LocateRegistry.getRegistry(port);
        serverIntf master;


		if (myVMid == 1){
            try {
                master = new Server();
                registry.bind("Master", master);
            }
            catch (Exception e){
                e.printStackTrace();
            }
            masterMain(SL);
		}

        else if (frontEndIds.contains(myVMid)){
            master = (serverIntf) Naming.lookup("//localHost:"+args[1]+"/Master");
            frontEndMain(SL, master);
        }
        else{
            assert backEndIds.contains(myVMid);
            master = (serverIntf) Naming.lookup("//localHost:"+args[1]+"/Master");
            backEndMain(SL, master);
        }
	
    /*	
		// main loop
		while (true) {
			// wait for and accept next client connection, returns a connection handle
			ServerLib.Handle h = SL.acceptConnection();
			// read and parse request from client connection at the given handle
			Cloud.FrontEndOps.Request r = SL.parseRequest( h );
			// Note: can use the single SL.getNextRequest() call instead of the prior two
			
			// actually process request and send any reply 
			// (this should be a middle tier operation in checkpoints 2 and 3)
			SL.processRequest( r );
		}
     */
	}

    public static void masterMain(ServerLib SL){
        SL.register_frontend();
        launchBackEnd(SL);

        while (true){
            Cloud.FrontEndOps.Request r = SL.getNextRequest();
            requestQueue.add(r);
            if (SL.getQueueLength() > 6 && numFrontEnds < 1){
                numFrontEnds = 1;
                launchFrontEnd(SL);
            }
            if (backEndIds.size() * 4 < requestQueue.size()){
                launchBackEnd(SL);
            }
        }
    }

    public static void frontEndMain(ServerLib SL, serverIntf master){
        SL.register_frontend();
        while (true){
            Cloud.FrontEndOps.Request r = SL.getNextRequest();
            try{
                master.enqueueRequest(r);
            }
            catch (Exception e){
                e.printStackTrace();
                return;
            }
        }
    }

    public static void backEndMain(ServerLib SL, serverIntf master){
        while(true){
            Cloud.FrontEndOps.Request job = null;
            try {
                job = master.getJob();
            }
            catch (Exception e){
                return;
            }
            SL.processRequest(job);
        }
    }

    public static void launchFrontEnd(ServerLib SL){
        int id = SL.startVM();
        frontEndIds.add(id);
    }

    public static void launchBackEnd(ServerLib SL){
        int id = SL.startVM();
        backEndIds.add(id);
    }

    public void enqueueRequest(Cloud.FrontEndOps.Request r){
        requestQueue.add(r);
    }

    public Cloud.FrontEndOps.Request getJob() throws NoSuchElementException{
        Cloud.FrontEndOps.Request r;
        try {
            r = requestQueue.remove();
        }
        catch (Exception e){
            return null;
        }
        return r;
    }

	public static void launchNewServers(ServerLib SL){
		int numServers = getNumServers(SL);
		for (int i = 0; i < numServers-1; i ++){
			SL.startVM();
		}
	}
	public static int getNumServers(ServerLib SL){
        double time = SL.getTime();
        if (time <= 2.0){
            return 2;
        }
        if (time <= 4.0){
            return 1;
        }
        if (time <= 6.0){
            return 2;
        }
        if (time <= 7){
            return 5;
        }
        if (time <= 11){
            return 4;
        }
        if (time <= 13){
            return 5;
        }
        if (time <= 16){
            return 4;
        }
        if (time <= 20){
            return 5;
        }
        return 3;
	}
}

