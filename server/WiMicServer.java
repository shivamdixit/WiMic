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

    private String name = "WiMic Server";

    WiMicServer(String name) {
        this.name = name;
    }

    public void run() {
        try {
            socket = new DatagramSocket(PORT, InetAddress.getByName(LOCALHOST));
            socket.setBroadcast(true);

            while (true) {
                System.out.println(name + " is ready");

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
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static void main(String[] args) {
        Scanner sc;
        sc = new Scanner(System.in);
        String name;

        System.out.println("Enter room name:");
        name = sc.next();

        Thread serverThread = new Thread(new WiMicServer(name));
        serverThread.start();
    }
}
