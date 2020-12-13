import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.charset.StandardCharsets.UTF_8;

class SelectiveRepeat {
    private int maxDataLength;
    private int currentPacket;
    private int endCounter;
    private HashMap<Long, Packet> currentWindowPackets;
    private boolean completedFlag;

    private DatagramChannel channel;
    private InetSocketAddress receiverAddress;
    private SocketAddress routerAddress;
    private int serverPort;
    private StringBuilder data;

    private boolean printDebug;
	private InetAddress clientAddress;
	private int clientPort;
	private boolean inputData;

    SelectiveRepeat(DatagramChannel channel, InetSocketAddress receiverAddress, SocketAddress routerAddress, boolean printDebug) {
        this.channel = channel;
        this.receiverAddress = receiverAddress;
        this.routerAddress = routerAddress;

        serverPort = receiverAddress.getPort();
        maxDataLength = 1013;
        currentPacket = 0;
        endCounter = 3;
        currentWindowPackets = new HashMap<>();
        completedFlag = false;
        this.printDebug = printDebug;
    }

    SelectiveRepeat(DatagramChannel channel, InetAddress clientAddress, int clientPort, SocketAddress routerAddress,boolean printDebug) {
        this.channel = channel;
        this.clientAddress = clientAddress;
        this.clientPort = clientPort;
        this.routerAddress = routerAddress;
        inputData = false;
        this.printDebug = printDebug;
    }

    long sendData(String data, long windowBeginSeqNum, long totalSequenceNumber) {
        byte[] byteData = data.getBytes();
        long windowSize = totalSequenceNumber / 2;
        long totalPacket = byteData.length / maxDataLength;
        if (0 != byteData.length % maxDataLength)
            ++totalPacket;

        while (true) {
            try {
                if (!completedFlag) {
                    for (int i = 0; i < windowSize; ++i) {
                        long currentSeqNum = (windowBeginSeqNum + i) % totalSequenceNumber;
                        if (!currentWindowPackets.containsKey(currentSeqNum)) {
                            byte[] packetData;
                            if (currentPacket < totalPacket - 1) {
                                packetData = Arrays.copyOfRange(byteData, (currentPacket) * maxDataLength,
                                        (currentPacket + 1) * maxDataLength);

                            } else if (currentPacket == totalPacket - 1) {
                                packetData = Arrays.copyOfRange(byteData, (currentPacket) * maxDataLength,
                                        byteData.length);
                            } else {
                                break;
                            }
                            ++currentPacket;
                            Packet p = new Packet.Builder().setType(0).setSequenceNumber(currentSeqNum)
                                    .setPortNumber(receiverAddress.getPort())
                                    .setPeerAddress(receiverAddress.getAddress()).setPayload(packetData).create();
                            currentWindowPackets.put(currentSeqNum, p);
                            channel.send(currentWindowPackets.get(currentSeqNum).toBuffer(), routerAddress);
                            //if (printDebug)
                               // System.out.println(
                                //        "Sent to " + serverPort + ": " + currentWindowPackets.get(currentSeqNum));
                        }
                    }
                }

                channel.configureBlocking(false);
                Selector selector = Selector.open();
                channel.register(selector, OP_READ);
                selector.select(2000);

                Set<SelectionKey> keys = selector.selectedKeys();
                if (keys.isEmpty()) {
                   // if (printDebug)
                     //   System.out.println("TIME OUT");
                    if (completedFlag) {
                        if (--endCounter < 0) {
                            
                            return ++windowBeginSeqNum;
                        }
                        channel.send(currentWindowPackets.get(windowBeginSeqNum).toBuffer(), routerAddress);
                       // if (printDebug)
                         //   System.out.println(
                           //         "Sent to " + serverPort + ": " + currentWindowPackets.get(windowBeginSeqNum));
                    } else {
                        for (long i = 0; i < windowSize; ++i) {
                            long seqNum = windowBeginSeqNum + i;
                            if (currentWindowPackets.containsKey(seqNum)) {
                                channel.send(currentWindowPackets.get(seqNum).toBuffer(), routerAddress);
                               // if (printDebug)
                                 //   System.out
                                   //         .println("Sent to " + serverPort + ": " + currentWindowPackets.get(seqNum));
                            }
                        }
                    }
                } else {
                    ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
                    channel.receive(buf);
                    buf.flip();
                    Packet resp = Packet.fromBuffer(buf);
                    if (completedFlag) {
                        if (resp.getType() == 3) {
                            channel.send(currentWindowPackets.get(windowBeginSeqNum).toBuffer(), routerAddress);
                           // if (printDebug)
                             //   System.out.println(
                               //         "Sent to " + serverPort + ": " + currentWindowPackets.get(windowBeginSeqNum) + " type-"+currentWindowPackets.get(windowBeginSeqNum).getType());
                        } else if (5 == resp.getType()) {
                            //if (printDebug)
                             //   System.out.println("Completed sending data");
                            return ++windowBeginSeqNum;
                        }
                    } else if ( resp.getType() == 3) {
                        if (printDebug)
                            System.out.println("Ok Received: " + resp);
                        long missedSeqNum = resp.getSequenceNumber();
                        if (currentWindowPackets.containsKey(missedSeqNum)) {
                            // send missed Packet
                            channel.send(currentWindowPackets.get(missedSeqNum).toBuffer(), routerAddress);
                           // if (printDebug)
                            //    System.out.println(
                              //          "Sent to " + serverPort + ": " + currentWindowPackets.get(missedSeqNum) + " type-"+currentWindowPackets.get(missedSeqNum));

                            long numACKed = missedSeqNum - windowBeginSeqNum;
                            for (int i = 0; i < numACKed; ++i) {
                                // remove ACKed Packets
                                currentWindowPackets.remove(windowBeginSeqNum);
                                // shift window
                                windowBeginSeqNum = (windowBeginSeqNum + 1) % totalSequenceNumber;
                            }
                        } else if (missedSeqNum == (windowBeginSeqNum + currentWindowPackets.size())
                                % totalSequenceNumber) {
                            // all received
                            currentWindowPackets.clear();
                            windowBeginSeqNum = missedSeqNum;
                            if (currentPacket == totalPacket) {
                                // finish sending all Packets
                                Packet p = new Packet.Builder().setType(4).setSequenceNumber(windowBeginSeqNum)
                                        .setPortNumber(receiverAddress.getPort())
                                        .setPeerAddress(receiverAddress.getAddress()).setPayload("FIN".getBytes())
                                        .create();
                                currentWindowPackets.put(windowBeginSeqNum, p);
                                channel.send(currentWindowPackets.get(windowBeginSeqNum).toBuffer(), routerAddress);
                             //   if (printDebug)
                               //     System.out.println("Sent to " + serverPort + ": "
                                 //           + currentWindowPackets.get(windowBeginSeqNum) + " type-"+currentWindowPackets.get(windowBeginSeqNum).getType());
                                completedFlag = true;
                            }
                        }
                    }
                }
                keys.clear();
                selector.close();
            } catch (IOException exception) {
                System.out.println("ClientSocket selectiveRepeat exception: " + exception.getMessage());
            }
        }
    }

    long receiveData(long windowBeginSeqNum, long totalSequenceNumber, int serverPort) {
        long windowSize = totalSequenceNumber / 2;
        data = new StringBuilder();

        HashMap<Long, Packet> currentWindowPackets = new HashMap<>();

        ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN).order(ByteOrder.BIG_ENDIAN);
        try {
            channel.configureBlocking(false);
            while(true) {
                Selector selector = Selector.open();
                channel.register(selector, OP_READ);
                selector.select(500);
                Set<SelectionKey> keys = selector.selectedKeys();

                if (keys.isEmpty()) {
                    if (inputData) {
                        //if (printDebug)
                            //System.out.println("TIME OUT");
                        Packet resp = new Packet.Builder().setType(3).setSequenceNumber(windowBeginSeqNum)
                                .setPeerAddress(clientAddress).setPortNumber(clientPort).setPayload("ACK".getBytes())
                                .create();
                        channel.send(resp.toBuffer(), routerAddress);
                        //if (printDebug)
                          //  System.out.println("    " + serverPort + " sent    : " + resp);
                    }
                } else {
                    inputData = true;
                    buf.clear();
                    channel.receive(buf);
                    buf.flip();
                    Packet packet = Packet.fromBuffer(buf);
                    buf.flip();
                    long seqNum = packet.getSequenceNumber();
                    if (packet.getType() == 4) {
                        Packet resp = packet.toBuilder().setType(5).setSequenceNumber(windowBeginSeqNum)
                                .setPayload("FIN_ACK".getBytes()).create();
                        channel.send(resp.toBuffer(), routerAddress);
                       // if (printDebug)
                         //   System.out.println("    " + serverPort + " sent    : " + resp);
                        return ++windowBeginSeqNum;
                    }
                    if (0 != packet.getType())
                        continue;
                    if (printDebug)
                        System.out.println(serverPort + " received: " + packet);
                    boolean outOfOrderButWithinRange = false;
                    if (windowBeginSeqNum == seqNum) {
                       
                        data.append(new String(packet.getPayload(), UTF_8));
                        windowBeginSeqNum = (windowBeginSeqNum + 1) % totalSequenceNumber;
                        for (long i = 0; i < windowSize - 1; ++i) {
                            long bufferSeqNum = windowBeginSeqNum;
                            if (currentWindowPackets.containsKey(bufferSeqNum)) {
                              
                                data.append(new String(currentWindowPackets.get(bufferSeqNum).getPayload(), UTF_8));
                                windowBeginSeqNum = (windowBeginSeqNum + 1) % totalSequenceNumber;
                                currentWindowPackets.remove(bufferSeqNum);
                            } else {
                                break;
                            }
                        }
                        if (printDebug)
                            System.out.println();
                    } else if (windowBeginSeqNum + windowSize <= totalSequenceNumber) {
                        if (windowBeginSeqNum < seqNum && seqNum < windowBeginSeqNum + windowSize) {                          
                            outOfOrderButWithinRange = true;
                        } 
                    } else {
                        if (windowBeginSeqNum < seqNum && seqNum < totalSequenceNumber
                                || 0 <= seqNum && seqNum < (windowSize - (totalSequenceNumber - windowBeginSeqNum))) {
                            outOfOrderButWithinRange = true;
                        } 
                    }

                    if (outOfOrderButWithinRange) {
                        if (currentWindowPackets.containsKey(seqNum)) {    
                            continue;
                        }
                        currentWindowPackets.put(seqNum, packet);
                        Packet resp = packet.toBuilder().setType(3).setSequenceNumber(windowBeginSeqNum).setPayload("ACK".getBytes()).create();
                        channel.send(resp.toBuffer(), routerAddress);
                        if (printDebug)
                            System.out.println("    " + serverPort + " sent    : " + resp);
                    }
                }

                keys.clear();
                selector.close();
            }
        } catch (IOException exception) {
            System.out.println("server socket exception " + exception.getMessage());
            return -1;
        }
    }

    String getData() {
        return data.toString();
    }
}
