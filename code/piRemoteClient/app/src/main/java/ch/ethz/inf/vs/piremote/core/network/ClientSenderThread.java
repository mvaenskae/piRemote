package ch.ethz.inf.vs.piremote.core.network;

import MessageObject.Message;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * created by fabian on 13.11.15
 */

public class ClientSenderThread implements Runnable {

    public final static BlockingQueue<Message> sendingQueue = new LinkedBlockingQueue<Message>();
    private Thread senderThread;
    private Socket socket;
    private ObjectOutputStream outputStream;

    public ClientSenderThread(Socket socket) {
        this.socket = socket;

        try {
            // get the output stream of the socket
            outputStream = new ObjectOutputStream(this.socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        senderThread = new Thread(this);
        senderThread.start();
    }


    @Override
    public void run() {

        try {
            Message messageToSend = sendingQueue.take(); // or poll?
            outputStream.writeObject(messageToSend);

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}