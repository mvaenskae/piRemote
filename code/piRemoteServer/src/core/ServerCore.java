package core;

import MessageObject.Message;
import MessageObject.PayloadObject.Offer;
import MessageObject.PayloadObject.Payload;
import MessageObject.PayloadObject.Pick;
import MessageObject.PayloadObject.ServerStateChange;
import SharedConstants.CoreCsts;
import StateObject.State;
import com.sun.istack.internal.NotNull;
import core.network.ServerNetwork;
import core.test.ServerCoreTester;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by sandro on 11.11.15.
 * Main class for the server. This will start all other threads.
 * Once started, ServerCore blocks on the main queue
 */
public class ServerCore{

    // The networking component shall deliver incoming messages to the server by putting them into the following queue:
    public static final BlockingQueue<Message> mainQueue = new LinkedBlockingQueue<>();
    public static final String portPath = System.getProperty("user.home")+"/.piremote-port";

    protected static ServerNetwork serverNetwork;
    protected static CoreCsts.ServerState serverState;
    protected static AbstractApplication application;
    protected static boolean running;
    protected static String filePickerBasePath;

    //public static int round=0; // TEST ONLY

    public static void main(String [ ] args) throws InterruptedException {
        System.out.println("PiRemote: Starting up.");
        try {
            run();
        }finally { // This doesn't happen when the program gets killed
            try {
                FileOutputStream portFile = new FileOutputStream(portPath);
                try{
                    String txt = "PiRemote: (not running)";
                    portFile.write(txt.getBytes());
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }finally {
                    portFile.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("PiRemote: Exiting normally."); // Note: Server Shutdown is not implemented at this stage.
    }

    public static void run() throws InterruptedException {
        // Initialize state
        serverState = CoreCsts.ServerState.NONE;
        application = null;
        running = true;
        filePickerBasePath = "";

        // Init Network component
        serverNetwork = new ServerNetwork(8015);
        serverNetwork.startNetwork();

        // Wait for network to be available
        while(serverNetwork.getPort() <= 0);
        String portString = Integer.toString(serverNetwork.getPort());
        System.out.println("PiRemote: Listening on port "+portString);
        try {
            FileOutputStream portFile = new FileOutputStream(portPath);
            try{
                portFile.write(portString.getBytes());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }finally {
                portFile.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // TEST ONLY
        //ServerCoreTester st = new ServerCoreTester(mainQueue);
        //st.phase1();
        // END TEST

        // Main loop
        while(running){

            // Blocking wait for messages to arrive:
            Message msg = mainQueue.take();

            // TEST ONLY
            /*round++;
            System.out.println("\n("+round+") Incoming:");
            System.out.println(msg);
            if(mainQueue.isEmpty()){
                running = false;
            }*/
            // END TEST

            if(!checkServerState(msg)){
                // Server State mismatch! Send ss to whoever sent this to us
                sendMessage(makeMessage(msg.getUuid()));
                continue;
            }

            // Server state is consistent. Is the message destined to us or to the Application?
            if(msg.hasPayload()){
                if(msg.getPayload() instanceof ServerStateChange){
                    CoreCsts.ServerState newServerState = ((ServerStateChange) msg.getPayload()).newServerState;
                    if(serverState == CoreCsts.ServerState.NONE){
                        // Application shall start
                        application = ApplicationFactory.makeApplication(newServerState);
                        if(application != null) {
                            serverState = newServerState;
                            application.onApplicationStart();
                        }else{
                            System.out.println("OMG - a client that knew that no application is running asked for stopping it, what is this client doing??");
                        }
                    }else{
                        // Application shall change or stop
                        if(!newServerState.equals(serverState)){
                            application.onApplicationStop();
                            application = ApplicationFactory.makeApplication(newServerState);
                            if(application != null) {
                                serverState = newServerState;
                                application.onApplicationStart();
                            }else{
                                // Failed to run the new application => this is an application stop
                                serverState = CoreCsts.ServerState.NONE;
                                sendMessage(makeMessage());
                            }
                        }else{
                            System.out.println("OMG - client asked to go into the state everybody is already in, what is this client doing??");
                        }
                    }
                    continue;
                }else if(msg.getPayload() instanceof Pick){
                    String path = ((Pick) msg.getPayload()).path;
                    File f = new File(path);
                    if(f.exists()){
                        if(f.isDirectory()){
                            // Directory picked
                            sendMessage(makeOffer(msg.getUuid(), f, false, false));
                        }else{
                            // File picked
                            if(application != null) application.onFilePicked(f,msg.getUuid());
                        }
                    }else{
                        System.out.println("WARNING: Picked path <"+path+"> is not valid!");
                    }
                    continue;
                }
            }// otherwise it was just an ss, do nothing

            // If we land here, core is not responsible for handling this message.
            // Forward the message to the application anyway so that it can check the state.
            if(application != null) {
                application.processMessage(msg);
            }else{
                assert serverState == CoreCsts.ServerState.NONE;
                if(msg.getServerState() != CoreCsts.ServerState.NONE)
                    System.out.println("OMG what is this client doin'??? It knows I have no app running and asks me to talk to it?!");
            }
        }
        // TEST ONLY
        /*System.out.println("\n\n--- Results ---");
        st.phase2();*/
        // END TEST
    }

    public static State getState(){
        // Use this to read the current state (including server and application state) from the server.
        if(application != null) {
            return new State(serverState, application.getApplicationState());
        }else{
            assert serverState.equals(CoreCsts.ServerState.NONE);
            return new State(CoreCsts.ServerState.NONE, null);
        }
    }

    public static void setFilePickerBasePath(String newBasePath){
        // Setter for filePickerBasePath
        filePickerBasePath=newBasePath;
    }

    protected static boolean checkServerState(Message msg){
        // This returns whether or not the ServerState in the message corresponds to the actual ServerState.
        if(msg==null || msg.getServerState() == null) return false;
        return msg.getServerState().equals(serverState);
    }

    protected static void sendMessage(@NotNull Message msg){
        // This will deliver msg to the networking component
        if(msg== null) return;
        try {
            //SenderService.sendingQueue.put(msg);
            // TEST ONLY
            /*System.out.println("Outgoing:");
            System.out.println(msg);
            ServerCoreTester.sendingQueue.put(msg);*/
            // END TEST
            if(serverNetwork.getSendingQueue() != null) serverNetwork.getSendingQueue().put(msg);
        } catch (InterruptedException e) {
            System.out.println("Failed to enqueue message for sending!");
            e.printStackTrace();
        }
    }

    protected static Message makeOffer(@NotNull UUID recipient, @NotNull File dir, boolean firstTime, boolean showHidden){
        // This takes a recipient and a File (must be a directory!) and returns a message with an Offer Payload
        //    containing a list of the contents in the specified directory.
        Offer offerPayload = new Offer();
        if(dir == null || !dir.isDirectory()) return null;
        File[] contents = dir.listFiles();
        if(contents != null) {
            if(firstTime) offerPayload.paths.add("/"+filePickerBasePath+"/");
            else offerPayload.paths.add(dir.getAbsolutePath()+"/");
            Arrays.sort(contents);
            for (File path : contents) {
                String pathtoSend=path.getName();
                if(path.isDirectory()) pathtoSend+="/";
                if (showHidden || !pathtoSend.startsWith(".")) { // depending on flag showHidden, shows hidden files and dirs
                    offerPayload.paths.add(pathtoSend);
                }
            }
            return makeMessage(recipient, offerPayload);
        }else{
            System.out.println("WARNING: "+dir.getName()+" should be a directory, gut listFiles() returns null!");
            return null;
        }
    }

    protected static Message makeMessage(){
        // Convenience function for building an ss broadcast
        return new Message(getState());
    }

    protected static Message makeMessage(@NotNull Payload payload){
        // Convenience function for building a broadcast message with given payload
        if(payload==null) return null;
        return new Message(getState(), payload);
    }

    protected static Message makeMessage(@NotNull UUID recipient){
        // Convenience function for building an ss message for a specific recipient
        if(recipient==null) return null;
        return new Message(recipient, getState());
    }

    protected static Message makeMessage(@NotNull UUID recipient, @NotNull Payload payload){
        // Convenience function for building a message with given payload for a specific recipient
        if(recipient==null || payload==null) return null;
        return new Message(recipient, getState(), payload);
    }
}
