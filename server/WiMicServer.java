import java.net.InetAddress;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.util.Scanner;

public class WiMicServer implements Runnable {
    private DatagramSocket socket;

    private final int PORT = 9876;
    private final String LOCALHOST = "0.0.0.0";
    private final String DISC_MESSAGE = "WIMIC_DISCOVER_REQ";
    private final String ACK_MESSAGE = "WIMIC_DISCOVER_ACK";
    private final String JOIN_MESSAGE = "WIMIC_JOIN_PASSWORD";
    private final String JOIN_SUCCESS = "WIMIC_JOIN_SUCCESS";
    private final String JOIN_FAIL = "WIMIC_JOIN_FAILURE";

    private String name = "WiMic Server";
    private int pin;

    WiMicServer(String name, int pin) {
        // Ensure first alphabet is capital
        this.name = name.substring(0, 1).toUpperCase() + name.substring(1);
        this.pin = pin;
    }

    public void run() {
        try {
            socket = new DatagramSocket(PORT, InetAddress.getByName(LOCALHOST));
            socket.setBroadcast(true);

            System.out.println(name + " is ready. Your pin is: " + pin);

            while (true) {

                byte[] receiveBuffer = new byte[15000];
                DatagramPacket packet = new DatagramPacket(
                    receiveBuffer,
                    receiveBuffer.length
                );

                socket.receive(packet);

                String message = new String(packet.getData()).trim();
                if (message.equals(DISC_MESSAGE)) {
                    System.out.println("Packet received from " + packet.getAddress());

                    byte[] sendData = (ACK_MESSAGE + ";" + this.name).getBytes();
                    DatagramPacket sendPacket = new DatagramPacket(
                        sendData,
                        sendData.length,
                        packet.getAddress(),
                        packet.getPort()
                    );

                    socket.send(sendPacket);
                    System.out.println("Sent response");

                } else if (message.contains(JOIN_MESSAGE)) {
                    String[] request = message.split(";");
                    byte[] sendData;

                    System.out.println(request.length);

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
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

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
