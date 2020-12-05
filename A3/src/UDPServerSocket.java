import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;

class UDPServerSocket {
    private long totalSequenceNumber;
    private DatagramChannel channel;
    private int serverPort;
    private long initialSeqNum;
    private SocketAddress router;
    private InetAddress clientAddress;
    private int clientPort;
    private SocketAddress routerAddress;
    private long sendSeqNum;
    private boolean debugMessage;

    UDPServerSocket(int serverRootPort) {
        totalSequenceNumber = 12121212L;
        serverPort = serverRootPort;
        routerAddress = new InetSocketAddress("localhost", 3000);
        debugMessage = true;

        try {
            channel = DatagramChannel.open();
            channel.bind(new InetSocketAddress(serverRootPort));
        } catch (IOException exception) {
            System.out.println("MyServerSocket exception: " + exception.getMessage());
        }
    }

    UDPServerSocket accept() {
        if (debugMessage) System.out.println("\nServer is accepting");
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

                if (1 == packet.getType()) {
                    if (debugMessage) System.out.println(serverPort + " has received: " + packet);
                    long initialSeqNum = packet.getSequenceNumber();
                    String payLoad = "SYN-ACK " + (serverPort+1);
                    Packet resp = packet.toBuilder()
                            .setType(2)
                            .setPayload(payLoad.getBytes())
                            .create();
                    channel.send(resp.toBuffer(), router);
                    if (debugMessage) System.out.println(serverPort + " has sent    : " + resp);
                    UDPServerSocket newServerSocket =  new UDPServerSocket(++serverPort);
                    newServerSocket.setInitialSeqNum(initialSeqNum, router, packet.getPeerAddress(), packet.getPeerPort());
                    return newServerSocket;
                }
            }
        } catch (IOException exception) {
            System.out.println("MyServerSocket receive exception: " + exception.getMessage());
            return null;
        }
    }

    private void setInitialSeqNum(long initialSeqNum, SocketAddress router, InetAddress clientAddress, int clientPort) {
        this.initialSeqNum = initialSeqNum;
        this.router = router;
        this.clientAddress = clientAddress;
        this.clientPort = clientPort;
    }

    String receiveData() {
        if (debugMessage) System.out.println("\nReceiving at port number " + serverPort);
        SelectiveRepeat selectiveRepeatReceiver = new SelectiveRepeat(channel, clientAddress, clientPort, router);
        sendSeqNum = selectiveRepeatReceiver.receive(initialSeqNum, totalSequenceNumber, serverPort);
        System.out.println("receive: " + selectiveRepeatReceiver.getData());
        return selectiveRepeatReceiver.getData();
    }

    void sendData(String data) {
        SelectiveRepeat selectiveRepeatSender = new SelectiveRepeat(channel, new InetSocketAddress("localhost", clientPort), routerAddress);
        selectiveRepeatSender.send(data, sendSeqNum, totalSequenceNumber);
    }

    int getServerPort() { return serverPort; }

    void close() throws Exception { channel.close(); }
}