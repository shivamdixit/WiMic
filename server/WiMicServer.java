import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.lang.System;
import java.net.InetAddress;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;


/**
 * class WiMicServer
 *
 * Creates a WiMic server and provides interface between
 * client and speakers
 */
public class WiMicServer implements Runnable {

    private final int PORT = 9876;

    private final String LOCALHOST = "0.0.0.0";
    private final String DISC_MESSAGE = "WIMIC_DISCOVER_REQ";
    private final String ACK_MESSAGE = "WIMIC_DISCOVER_ACK";
    private final String JOIN_MESSAGE = "WIMIC_JOIN_PASSWORD";
    private final String JOIN_SUCCESS = "WIMIC_JOIN_SUCCESS";
    private final String JOIN_FAIL = "WIMIC_JOIN_FAILURE";
    private final String SPEAK_MESSAGE = "WIMIC_SPEAK_REQ";
    private final String STOP_SPEAK_MESSAGE = "WIMIC_SPEAK_END";
    private final String SPEAK_ACK = "WIMIC_SPEAK_ACK";
    private final String SPEAK_NACK = "WIMIC_SPEAK_NACK";
    private final String SPEAK_TIMEOUT = "WIMIC_SPEAK_TIMEOUT";

    /**
     * Room name
     */
    private String name = "WiMic Server";

    /**
     * Room PIN
     */
    private int pin;

    /**
     * Channel availibity flag
     */
    private boolean isChannelAvailable = true;

    /**
     * Time per request in ms
     */
    private int timeout = 60 * 1000;

    private Timer timer;

    /**
     * Variables used for transmitting voice
     */
    AudioInputStream audioInputStream;
    static AudioInputStream ais;
    static AudioFormat format;
    static boolean status = true;
    static int sampleRate = 16000;
    static DataLine.Info dataLineInfo;
    static SourceDataLine sourceDataLine;
    static int SPEAK_PORT = 9898;
    private DatagramSocket voiceSocket;

    /**
     * Constructor
     *
     * @param name Name of the room
     * @param pin PIN of the room
     */
    WiMicServer(String name, int pin) {
        // Ensure first alphabet is capital
        this.name = name.substring(0, 1).toUpperCase() + name.substring(1);
        this.pin = pin;
    }

    /**
     * Creates socket and listen for connection
     */
    public void run() {
        try {
            DatagramSocket socket = new DatagramSocket(PORT, InetAddress.getByName(LOCALHOST));
            socket.setBroadcast(true);
            System.out.println(name + " is ready. Your pin is: " + pin);

            receiveVoicePackets();
            while (true) {
                receiveOtherPackets(socket);
            }
        } catch (Exception e) {
            // TODO
            System.out.println(e);
        }
    }

    /**
     * Receive packets from clients (other than voice)
     *
     * @param socket DatagramSocket object which is binded to port
     * @throws IOException if cannot receive packets
     */
    private void receiveOtherPackets(DatagramSocket socket) throws IOException {
        byte[] receiveBuffer = new byte[15000];
        DatagramPacket packet = new DatagramPacket(
                receiveBuffer,
                receiveBuffer.length
        );

        socket.receive(packet);
        handleReceivedPacket(socket, packet);
    }

    /**
     * Checks if received packet is discovery packet or join packet
     *
     * @param socket DatagramSocket object
     * @param packet DatagramPacket object
     * @throws IOException thrown when can't send message
     */
    private void handleReceivedPacket(
            DatagramSocket socket,
            DatagramPacket packet
    ) throws IOException {
        String message = new String(packet.getData()).trim();
        if (message.equals(DISC_MESSAGE)) {
            System.out.println("Discovery packet received from " + packet.getAddress());
            sendDiscoveryAck(socket, packet);

        } else if (message.contains(JOIN_MESSAGE)) {
            System.out.println("Join packet received from " + packet.getAddress());
            sendJoinACK(socket, packet);
        } else if (message.equals(SPEAK_MESSAGE)) {
            System.out.println("Speak message received");
            sendSpeakACK(socket, packet);
        } else if (message.equals(STOP_SPEAK_MESSAGE)) {
            System.out.println("Stop speak message received");
            isChannelAvailable = true;

            // Cancel the timer
            timer.cancel();
            timer.purge();
        }
    }

    /**
     * Send ACK of discovery packet
     *
     * @param socket DatagramSocket object
     * @param packet DatagramPacket object
     * @throws IOException thrown when can't send message
     */
    private void sendDiscoveryAck(DatagramSocket socket, DatagramPacket packet) throws IOException {
        byte[] sendData = (ACK_MESSAGE + ";" + this.name).getBytes();
        DatagramPacket sendPacket = new DatagramPacket(
                sendData,
                sendData.length,
                packet.getAddress(),
                packet.getPort()
        );

        socket.send(sendPacket);
        System.out.println("Sent discovery ACK");
    }

    /**
     * Send ACK of join packet
     *
     * @param socket DatagramSocket object
     * @param packet DatagramPacket object
     * @throws IOException thrown when can't send message
     */
    private void sendJoinACK(DatagramSocket socket, DatagramPacket packet) throws IOException {
        String message = new String(packet.getData()).trim();
        String[] request = message.split(";");

        byte[] sendData;
        if (request.length >= 2 && validatePin(request[1])) {
            sendData = JOIN_SUCCESS.getBytes();
            System.out.println("PIN success!");
        } else {
            sendData = JOIN_FAIL.getBytes();
            System.out.println("Invalid PIN");
        }

        DatagramPacket sendPacket = new DatagramPacket(
                sendData,
                sendData.length,
                packet.getAddress(),
                packet.getPort()
        );

        socket.send(sendPacket);
    }

    private void sendSpeakACK(DatagramSocket socket, DatagramPacket packet) throws IOException {
        if (isChannelAvailable) {
            isChannelAvailable = false;
            setTimeout();  // Inform client after timeout
            System.out.println("Sending ACK of channel available");
            sendMessage(socket, packet, SPEAK_ACK);
        } else {
            System.out.println("Sending NACK of channel available");
            sendMessage(socket, packet, SPEAK_NACK);
        }
    }

    private void sendMessage(
        DatagramSocket socket,
        DatagramPacket packet,
        String message
    ) throws IOException {

        byte[] sendData = message.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(
                sendData,
                sendData.length,
                packet.getAddress(),
                packet.getPort()
        );

        socket.send(sendPacket);
    }

    private void setTimeout() {
        timer = new Timer();
        timer.schedule(new TimerTask(){
            @Override
            public void run() {
                System.out.println("Timeout has occurred");
                isChannelAvailable = true;  // Assuming client respects our requests
            }

        }, timeout);
    }

    /**
     * Checks if a pin is valid
     *
     * @param pin Given pin as String
     * @return true if valid, else false
     */
    private boolean validatePin(String pin) {
        String regex = "\\d+";
        int pinInt;

        if (pin.matches(regex)) {
            pinInt = Integer.parseInt(pin);

            if (this.pin == pinInt) {
                return true;
            }
        }

        return false;
    }

    private void receiveVoicePackets() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    voiceSocket = new DatagramSocket(
                        SPEAK_PORT,
                        InetAddress.getByName(LOCALHOST)
                    );

                    byte[] receiveData = new byte[3800];
                    initialize();

                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    ByteArrayInputStream baiss = new ByteArrayInputStream(receivePacket.getData());

                    while (status == true) {
                        voiceSocket.receive(receivePacket);

                        if (isChannelAvailable) {
                            sendMessage(voiceSocket, receivePacket, SPEAK_TIMEOUT);
                        }

                        ais = new AudioInputStream(baiss, format, receivePacket.getLength());
                        toSpeaker(receivePacket.getData());
                    }

                    sourceDataLine.drain();
                    sourceDataLine.close();
                } catch (Exception e) {
                    System.out.println("Exception!");
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Initializes audio
     */
    public static void initialize() throws Exception {
        format = new AudioFormat(sampleRate, 16, 1, true, false);
        dataLineInfo = new DataLine.Info(SourceDataLine.class, format);
        sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
        sourceDataLine.open(format);
        sourceDataLine.start();

        FloatControl volumeControl = (FloatControl) sourceDataLine.getControl(FloatControl.Type.MASTER_GAIN);
        volumeControl.setValue(1.00f);
    }

    /**
     * Transmits data to speakers
     *
     * @param soundBytes bytes to write to speakers
     */
    public static void toSpeaker(byte[] soundBytes) {
        try {
            sourceDataLine.write(soundBytes, 0, soundBytes.length);
        } catch (Exception e) {
            System.out.println("Cannot send data to speakers");
            e.printStackTrace();
        }
    }

    /**
     * Creates new server instance
     *
     * @param args Command line args
     */
    public static void main(String[] args) {
        Scanner sc;
        sc = new Scanner(System.in);
        String name;

        System.out.println("Enter room name:");
        name = sc.next();

        // Generate a random pin
        double rand = Math.random() * 8999 + 1000;

        Thread serverThread = new Thread(new WiMicServer(name, (int) rand));
        serverThread.start();
    }
}
