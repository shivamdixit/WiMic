package in.ac.lnmiit.wimic;

import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class Network extends AsyncTask<InetAddress, Room, List<Room>> {

    private final int PORT = 9876;
    private final int timeout = 60;     // In seconds, i.e 1 min

    /**
     * Message format
     *
     * ACK_MESSAGE;serverName
     * DISC_MESSAGE
     */
    private final String DISC_MESSAGE = "WIMIC_DISCOVER_REQ";
    private final String ACK_MESSAGE = "WIMIC_DISCOVER_ACK";

    /**
     * Create a list of rooms to cache
     */
    private List<Room> rooms = new ArrayList<>();

    private RecyclerView recyclerView;

    Network(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
    }

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

            byte[] buffer = new byte[15000];
            DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);

            // Discover server only for 1 minute.
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

            // Close the socket
            socket.close();

        } catch (Exception e) {
            // TODO
        }

        return rooms;
    }

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

    protected void onPostExecute(List<Room> rooms) {
        // TODO Add caching of rooms
    }
}
