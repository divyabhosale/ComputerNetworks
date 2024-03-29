import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;

class UDPServerSocket {
    private long SeqNumber;
    private DatagramChannel channel;
    private int serverPort;
    private long initialSeqNum;
    private SocketAddress router;
    private InetAddress clientAddress;
    private int clientPort;
    private SocketAddress routerAddress;
    private long sendSeqNum;
    private boolean printDebug;

    UDPServerSocket(int serverRootPort,boolean printDebug) {
        SeqNumber = 12121212L;
        serverPort = serverRootPort;
        routerAddress = new InetSocketAddress("localhost", 3000);
        this.printDebug = printDebug;

        try {
            channel = DatagramChannel.open();
            channel.bind(new InetSocketAddress(serverRootPort));
        } catch (IOException exception) {
            System.out.println("UDPServerSocket exception: " + exception.getMessage());
        }
    }

    UDPServerSocket accept() {
        if (printDebug) System.out.println("\nServer is accepting");
        try {
            ByteBuffer buf = ByteBuffer
                    .allocate(Packet.MAX_LEN)
                    .order(ByteOrder.BIG_ENDIAN);
            while(true) {
                buf.clear();
                SocketAddress router = channel.receive(buf);

                // Parse a packet from the received raw data.
                buf.flip();
                Packet packet = Packet.fromBuffer(buf);
                buf.flip();

                if (packet.getType() == 1) {
                    if (printDebug) System.out.println(serverPort + " has received: " + packet + " type-"+packet.getType());
                    long initialSeqNum = packet.getSequenceNumber();
                    String payLoad = "SYN-ACK " + (serverPort+1);
                    Packet resp = packet.toBuilder()
                            .setType(2)
                            .setPayload(payLoad.getBytes())
                            .create();
                    channel.send(resp.toBuffer(), router);
                    if (printDebug) System.out.println(serverPort + " has sent    : " + resp + " type-"+resp.getType());
                    UDPServerSocket newServerSocket =  new UDPServerSocket(++serverPort,true);
                    newServerSocket.setInitialSeqNum(initialSeqNum, router, packet.getPeerAddress(), packet.getPeerPort());
                    return newServerSocket;
                }
            }
        } catch (IOException exception) {
            System.out.println("UDPServerSocket receive exception: " + exception.getMessage());
            return null;
        }
    }

    private void setInitialSeqNum(long initialSeqNum, SocketAddress router, InetAddress clientAddress, int clientPort) {
        this.initialSeqNum = initialSeqNum;
        this.router = router;
        this.clientAddress = clientAddress;
        this.clientPort = clientPort;
    }

    String receiveData(boolean printDebug) {
        if (printDebug) System.out.println("\nReceiving at port number " + serverPort);
        SelectiveRepeat selectiveRepeatReceiver = new SelectiveRepeat(channel, clientAddress, clientPort, router, printDebug);
        sendSeqNum = selectiveRepeatReceiver.receiveData(initialSeqNum, SeqNumber, serverPort);
        System.out.println("Server Received: " + selectiveRepeatReceiver.getData());
        return selectiveRepeatReceiver.getData();
    }

    void sendData(String data, boolean printDebug) {
        SelectiveRepeat selectiveRepeatSender = new SelectiveRepeat(channel, new InetSocketAddress("localhost", clientPort), routerAddress, printDebug);
        selectiveRepeatSender.sendData(data, sendSeqNum, SeqNumber);
    }

    int getServerPort() { return serverPort; }

    void close() throws Exception { channel.close(); }
}
