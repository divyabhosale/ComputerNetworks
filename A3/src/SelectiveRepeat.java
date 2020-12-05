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
    private boolean done;

    private DatagramChannel channel;
    private InetSocketAddress receiverAddress;
    private SocketAddress routerAddress;
    private int serverPort;
    private StringBuilder data;

    private boolean debug;
	private InetAddress clientAddress;
	private int clientPort;
	private boolean inputData;

    SelectiveRepeat(DatagramChannel channel, InetSocketAddress receiverAddress, SocketAddress routerAddress) {
        this.channel = channel;
        this.receiverAddress = receiverAddress;
        this.routerAddress = routerAddress;

        serverPort = receiverAddress.getPort();
        maxDataLength = 1013;
        currentPacket = 0;
        endCounter = 3;
        currentWindowPackets = new HashMap<>();
        done = false;
        debug = true;
    }

    SelectiveRepeat(DatagramChannel channel, InetAddress clientAddress, int clientPort, SocketAddress routerAddress) {
        this.channel = channel;
        this.clientAddress = clientAddress;
        this.clientPort = clientPort;
        this.routerAddress = routerAddress;
        inputData = false;
        debug = true;
    }

    long send(String data, long windowBeginSeqNum, long totalSequenceNumber) {
        byte[] byteData = data.getBytes();
        long windowSize = totalSequenceNumber / 2;
        long totalPacket = byteData.length / maxDataLength;
        if (0 != byteData.length % maxDataLength)
            ++totalPacket;

        while (true) {
            try {
                // fill / create window size Packets
                if (!done) {
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
                                // no more Packets to create
                                break;
                            }
                            ++currentPacket;
                            Packet p = new Packet.Builder().setType(0).setSequenceNumber(currentSeqNum)
                                    .setPortNumber(receiverAddress.getPort())
                                    .setPeerAddress(receiverAddress.getAddress()).setPayload(packetData).create();
                            currentWindowPackets.put(currentSeqNum, p);
                            channel.send(currentWindowPackets.get(currentSeqNum).toBuffer(), routerAddress);
                            if (debug)
                                System.out.println(
                                        "Sent to " + serverPort + ": " + currentWindowPackets.get(currentSeqNum));
                        }
                    }
                }

                // Try to receive a packet within timeout.
                channel.configureBlocking(false);
                Selector selector = Selector.open();
                channel.register(selector, OP_READ);
                selector.select(2000);

                Set<SelectionKey> keys = selector.selectedKeys();
                if (keys.isEmpty()) {
                    if (debug)
                        System.out.println("TIME OUT");
                    if (done) {
                        if (--endCounter < 0) {
                            if (debug)
                                System.out.println("Completed sending data");
                            return ++windowBeginSeqNum;
                        }
                        channel.send(currentWindowPackets.get(windowBeginSeqNum).toBuffer(), routerAddress);
                        if (debug)
                            System.out.println(
                                    "Sent to " + serverPort + ": " + currentWindowPackets.get(windowBeginSeqNum));
                    } else {
                        for (long i = 0; i < windowSize; ++i) {
                            long seqNum = windowBeginSeqNum + i;
                            if (currentWindowPackets.containsKey(seqNum)) {
                                channel.send(currentWindowPackets.get(seqNum).toBuffer(), routerAddress);
                                if (debug)
                                    System.out
                                            .println("Sent to " + serverPort + ": " + currentWindowPackets.get(seqNum));
                            }
                        }
                    }
                } else {
                    ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
                    channel.receive(buf);
                    buf.flip();
                    Packet resp = Packet.fromBuffer(buf);
                    if (done) {
                        if (3 == resp.getType()) {
                            channel.send(currentWindowPackets.get(windowBeginSeqNum).toBuffer(), routerAddress);
                            if (debug)
                                System.out.println(
                                        "Sent to " + serverPort + ": " + currentWindowPackets.get(windowBeginSeqNum));
                        } else if (5 == resp.getType()) {
                            if (debug)
                                System.out.println("Completed sending request");
                            return ++windowBeginSeqNum;
                        }
                    } else if (3 == resp.getType()) {
                        if (debug)
                            System.out.println("    Received: " + resp);
                        long missedSeqNum = resp.getSequenceNumber();
                        if (currentWindowPackets.containsKey(missedSeqNum)) {
                            // send missed Packet
                            channel.send(currentWindowPackets.get(missedSeqNum).toBuffer(), routerAddress);
                            if (debug)
                                System.out.println(
                                        "Sent to " + serverPort + ": " + currentWindowPackets.get(missedSeqNum));

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
                                if (debug)
                                    System.out.println("Sent to " + serverPort + ": "
                                            + currentWindowPackets.get(windowBeginSeqNum));
                                done = true;
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

    long receive(long windowBeginSeqNum, long totalSequenceNumber, int serverPort) {
        long windowSize = totalSequenceNumber / 2;
        data = new StringBuilder();

        HashMap<Long, Packet> currentWindowPackets = new HashMap<>();

        ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN).order(ByteOrder.BIG_ENDIAN);
        try {
            channel.configureBlocking(false);
            for (;;) {
                Selector selector = Selector.open();
                channel.register(selector, OP_READ);
                selector.select(500);
                Set<SelectionKey> keys = selector.selectedKeys();

                if (keys.isEmpty()) {
                    if (inputData) {
                        if (debug)
                            System.out.println("TIME OUT");
                        Packet resp = new Packet.Builder().setType(3).setSequenceNumber(windowBeginSeqNum)
                                .setPeerAddress(clientAddress).setPortNumber(clientPort).setPayload("ACK".getBytes())
                                .create();
                        channel.send(resp.toBuffer(), routerAddress);
                        if (debug)
                            System.out.println("    " + serverPort + " sent    : " + resp);
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
                        if (debug)
                            System.out.println("    " + serverPort + " sent    : " + resp);
                        return ++windowBeginSeqNum;
                    }
                    if (0 != packet.getType())
                        continue;
                    if (debug)
                        System.out.print(serverPort + " received: " + packet);
                    boolean outOfOrderButWithinRange = false;
                    if (windowBeginSeqNum == seqNum) {
                        // in order
                        if (debug)
                            System.out.print(", in order, deliver #" + seqNum);
                        data.append(new String(packet.getPayload(), UTF_8));
                        windowBeginSeqNum = (windowBeginSeqNum + 1) % totalSequenceNumber;
                        // check buffer
                        for (long i = 0; i < windowSize - 1; ++i) {
                            long bufferSeqNum = windowBeginSeqNum;
                            if (currentWindowPackets.containsKey(bufferSeqNum)) {
                                if (debug)
                                    System.out.print(", #" + bufferSeqNum);
                                data.append(new String(currentWindowPackets.get(bufferSeqNum).getPayload(), UTF_8));
                                windowBeginSeqNum = (windowBeginSeqNum + 1) % totalSequenceNumber;
                                currentWindowPackets.remove(bufferSeqNum);
                            } else {
                                break;
                            }
                        }
                        if (debug)
                            System.out.println();
                    } else if (windowBeginSeqNum + windowSize <= totalSequenceNumber) {
                        // out of order
                        if (windowBeginSeqNum < seqNum && seqNum < windowBeginSeqNum + windowSize) {
                            // within window range
                            if (debug)
                                System.out.print(", out of order, within range");
                            outOfOrderButWithinRange = true;
                        } else {
                            if (debug)
                                System.out.println(", out of order, out of range, discard it");
                        }
                    } else {
                        // out of order
                        if (debug)
                            System.out.print(", out of order, within range");
                        if (windowBeginSeqNum < seqNum && seqNum < totalSequenceNumber
                                || 0 <= seqNum && seqNum < (windowSize - (totalSequenceNumber - windowBeginSeqNum))) {
                            // within window range
                            outOfOrderButWithinRange = true;
                        } else {
                            if (debug)
                                System.out.println(", out of order, out of range, discard it");
                        }
                    }

                    if (outOfOrderButWithinRange) {
                        // check duplicate
                        if (currentWindowPackets.containsKey(seqNum)) {
                            if (debug)
                                System.out.println(", duplicate, discard it");
                            continue;
                        }
                        if (debug)
                            System.out.println(", not duplicate, buffer it");
                        // buffer it
                        currentWindowPackets.put(seqNum, packet);
                        // send ACK
                        Packet resp = packet.toBuilder().setType(3).setSequenceNumber(windowBeginSeqNum)
                                .setPayload("ACK".getBytes()).create();
                        channel.send(resp.toBuffer(), routerAddress);
                        if (debug)
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