package in.ac.lnmiit.wimic;

import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * class Network
 *
 * Responsible for server discovery
 */
public class Network extends AsyncTask<InetAddress, Room, List<Room>> {

    /**
     * Port to bind on
     */
    private final int PORT = 9876;

    /**
     * Time duration for which scan rooms available
     */
    private final int timeout = 60;     // In seconds, i.e 1 min

    /**
     * Message format:
     * From server->
     * ACK_MESSAGE;serverName
     *
     * From client->
     * DISC_MESSAGE
     */
    private final String DISC_MESSAGE = "WIMIC_DISCOVER_REQ";
    private final String ACK_MESSAGE = "WIMIC_DISCOVER_ACK";

    /**
     * Create a list of rooms to cache for later use
     */
    private List<Room> rooms = new ArrayList<>();

    /**
     * To update the view
     */
    private RecyclerView recyclerView;

    /**
     * Constructor
     *
     * @param recyclerView RecyclerView object
     */
    Network(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
    }

    /**
     * Sends discovery message and wait for responses till 'timeout' period
     *
     * @param ip Ip Address
     * @return List of rooms
     */
    @Override
    protected List<Room> doInBackground(InetAddress... ip) {
        try {
            DatagramSocket socket = new DatagramSocket(PORT);
            socket.setBroadcast(true);

            byte[] sendData = DISC_MESSAGE.getBytes();
            DatagramPacket packet = new DatagramPacket(
                    sendData,
                    sendData.length,
                    ip[0],
                    PORT
            );

            socket.send(packet);
            System.out.println("Sent discovery packets");

            // Wait and fetch ACKs from server
            waitForResponse(socket);

            // Close the socket
            socket.close();

        } catch (Exception e) {
            // TODO
        }

        return rooms;
    }

    /**
     * Fetches ACK of discovery packets and publishes progress
     *
     * @param socket Socket to listen on
     * @throws IOException if cannot receive packets
     */
    private void waitForResponse(DatagramSocket socket) throws IOException {
        byte[] buffer = new byte[15000];
        DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);

        // Discover server only for 'timeout' seconds.
        long startTime = System.currentTimeMillis();
        while ((System.currentTimeMillis() - startTime) < timeout * 1000) {
            socket.receive(receivePacket);

            String message = new String(receivePacket.getData()).trim();
            String[] serverDetails;

            if (message.contains(ACK_MESSAGE)) {
                serverDetails = message.split(";");
                Room newRoom = new Room(serverDetails[1], receivePacket.getAddress().toString());
                rooms.add(newRoom);

                publishProgress(newRoom);
            }
        }
    }

    /**
     * When a room is discovered update the view
     *
     * @param progress Room object
     */
    @Override
    protected void onProgressUpdate(Room... progress) {
        // TODO: Currently every card is redrawn each time a room is found
        // modify code to draw only new card found
        System.out.println("Server Discovered " + progress[0].getName());

        if (!rooms.isEmpty()) {
            RVAdapter adapter = new RVAdapter(rooms);
            recyclerView.setAdapter(adapter);
        }
    }

    /**
     * When rooms discovery is finished, cache the rooms
     *
     * @param rooms List of rooms discovered
     */
    @Override
    protected void onPostExecute(List<Room> rooms) {
        // TODO Add caching of rooms
    }
}
