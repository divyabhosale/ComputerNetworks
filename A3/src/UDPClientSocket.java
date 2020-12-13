import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Set;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.charset.StandardCharsets.UTF_8;

public class UDPClientSocket {

    private SocketAddress routerAddress;
    private InetSocketAddress serverAddress;
    private DatagramChannel channel;
    private final long SeqNumber = 12121212L;
    private long ranSenderSequence;
    private long receiverSeqNumber;
    private int serverPortNumber;
    private boolean printDebug;
    
   
    UDPClientSocket(int routerPortNumber, int serverPortNumber,boolean printDebug) {
        this.serverPortNumber = serverPortNumber;
        this.routerAddress = new InetSocketAddress("localhost", routerPortNumber);
        this.serverAddress = new InetSocketAddress("localhost", serverPortNumber);
        this.printDebug = printDebug;
        try {
        	channel = DatagramChannel.open();
        } catch (IOException exception) {
            System.out.println("client socket exception " + exception.getMessage());
        }
        makeHandshake();
    }
    
    private void makeHandshake() {
        if (printDebug) System.out.println("\nConnecting to " + serverPortNumber );
        int handShakeStep = 1;
        boolean isconnected = false;
        while (!isconnected) {
            try {
                switch (handShakeStep) {
                    case 1:
                        ranSenderSequence = (long) (Math.random() * SeqNumber);
                        Packet p1 = new Packet.Builder().setType(1).setSequenceNumber(ranSenderSequence).setPortNumber(serverPortNumber).setPeerAddress(serverAddress.getAddress()).setPayload("SYN".getBytes()).create();
                        channel.send(p1.toBuffer(), routerAddress);
                        if (printDebug) System.out.println("Sending to " + serverPortNumber + ": " + p1 +" type-"+p1.getType());

                        // Try to receive a packet within timeout.
                        channel.configureBlocking(false);
                        Selector selector = Selector.open();
                        channel.register(selector, OP_READ);
                        selector.select(5000); // block for up to timeout milliseconds

                        Set<SelectionKey> keys = selector.selectedKeys();
                      if (keys.isEmpty()) {
                            if (printDebug) System.out.println("TIME OUT");
                        } else {
                            ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
                            channel.receive(buf);
                            buf.flip();
                            Packet resp = Packet.fromBuffer(buf);
                            if (printDebug) System.out.println("Client Received From: " + resp.getPeerPort() + ": " + resp +" type-"+resp.getType());
                            if (resp.getType() == 2 && ranSenderSequence == resp.getSequenceNumber()) {
                                String payLoad = new String(resp.getPayload(), UTF_8);
                                serverPortNumber = new Integer(payLoad.substring(8));
                                this.serverAddress = new InetSocketAddress("localhost", serverPortNumber);
                                handShakeStep = 3;
                            }
                            keys.clear();
                            selector.close();
                			}
                        break;
                    case 3:
                        Packet p2 = new Packet.Builder().setType(3).setSequenceNumber(ranSenderSequence).setPortNumber(serverPortNumber).setPeerAddress(serverAddress.getAddress()).setPayload("ACK".getBytes()).create();
                        channel.send(p2.toBuffer(), routerAddress);
                        if (printDebug) System.out.println("Sending to " + serverPortNumber + ": " + p2 +" type-"+p2.getType());
                        isconnected = true;
                        break;
                }
            } catch (IOException exception) {
                System.out.println("client socket exception" + handShakeStep + " " + exception.getMessage());
            }
        }
    }


    public String receiveData(boolean printDebug) {
        SelectiveRepeat selectiveRepeatReceiver = new SelectiveRepeat(channel, serverAddress.getAddress(), serverPortNumber, routerAddress,printDebug);
        selectiveRepeatReceiver.receiveData(receiverSeqNumber, SeqNumber, serverPortNumber);
        return selectiveRepeatReceiver.getData();
    }
    
    public void sendData(String data,boolean printDebug) {
        if (printDebug) 
        	System.out.println("\nHandshake done - Sending to server port :" + serverPortNumber );
        SelectiveRepeat selectiveRepeatSender = new SelectiveRepeat(channel, serverAddress, routerAddress, printDebug);
        receiverSeqNumber = selectiveRepeatSender.sendData(data, ranSenderSequence, SeqNumber);
    }

	

}
