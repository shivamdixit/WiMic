package in.ac.lnmiit.wimic;

import android.app.Activity;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

/**
 * class Network
 *
 * Responsible for server discovery
 */
public class Scanner extends AsyncTask<InetAddress, Room, List<Room>> {

    /**
     * Time duration for which scan rooms available
     */
    private final int timeout = 10;     // In seconds, i.e 1 min

    /**
     * Create a list of rooms to cache for later use
     */
    private List<Room> rooms = new ArrayList<>();

    /**
     * To update the view
     */
    private RecyclerView recyclerView;

    /**
     * Object of main activity to update view
     */
    private Activity mainActivity;

    /**
     * Constructor
     *
     * @param recyclerView RecyclerView object
     */
    Scanner(RecyclerView recyclerView, Activity activity) {
        super();
        this.recyclerView = recyclerView;
        mainActivity = activity;
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
            DatagramSocket socket = new DatagramSocket();
            socket.setBroadcast(true);

            byte[] sendData = Config.DISC_MESSAGE.getBytes();
            DatagramPacket packet = new DatagramPacket(
                    sendData,
                    sendData.length,
                    ip[0],
                    Config.MESSAGE_PORT
            );

            socket.send(packet);
            System.out.println("Sent discovery packets");

            // Wait and fetch ACKs from server
            waitForResponse(socket);

            // Close the socket
            socket.close();

        } catch (IOException e) {
            showToast("Try again later");
        }

        return rooms;
    }

    /**
     * Show toast message on UI thread if search
     * is already in progress
     *
     * @param message Message to show on Toast
     */
    private void showToast(final String message) {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(
                        mainActivity.getApplicationContext(),
                        message,
                        Toast.LENGTH_LONG
                ).show();
            }
        });
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
        socket.setSoTimeout(timeout * 1000);
        while ((System.currentTimeMillis() - startTime) < timeout * 1000) {
            try {
                socket.receive(receivePacket);

                String message = new String(receivePacket.getData()).trim();
                String[] serverDetails;

                if (message.contains(Config.ACK_MESSAGE)) {
                    serverDetails = message.split(";");
                    Room newRoom = new Room(serverDetails[1], receivePacket.getAddress().toString());
                    rooms.add(newRoom);

                    publishProgress(newRoom);
                }
            } catch (SocketTimeoutException e) {
                // Timeout has reached. Do nothing
            } catch (Exception e) {
                e.printStackTrace();
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
        ((MainActivity) mainActivity).resetRefresh();   // Stops the refresh button animation

    }
}
