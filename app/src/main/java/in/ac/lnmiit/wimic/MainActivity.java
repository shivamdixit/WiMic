package in.ac.lnmiit.wimic;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

/**
 * class MainActivity
 *
 * Main activity of the app. Controls the UI as well as other events
 */
public class MainActivity extends ActionBarActivity {

    /**
     * Stores WiFi system service object
     */
    private WifiManager wifi;

    /**
     * Stores RecyclerView object for list population
     */
    protected RecyclerView recyclerView;

    /**
     *
     */
    private MenuItem refreshItem;

    /**
     * Entry point of the application. Draws the UI and initiate
     * server discovery
     *
     * @param savedInstanceState Instance State
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize WiFi Manager for later use
        wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

        // Will work only after setting contentView
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        // Set layout of recyclerView
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);

        List<Room> rooms = new ArrayList<>();   // Initialize empty adapter
        RVAdapter adapter = new RVAdapter(rooms);
        recyclerView.setAdapter(adapter);
    }

    /**
     * Creates ActionBar
     *
     * @param menu Menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);

        refreshItem = menu.findItem(R.id.action_refresh);
        checkWifi();    // Placed here so that refreshItem is initialized

        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Auto-generated method
     *
     * @param item Item
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        } else if (id == R.id.action_refresh) {
            checkWifi();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Checks if WiFi is enabled or not.
     *
     * If enabled, search for available servers else prompt
     * user to enable it.
     */
    private void checkWifi() {
        if (!wifi.isWifiEnabled()) {
            System.out.println("WiFI is disabled");
            showWifiPrompt();
        } else if (!wifiConnected()) {
            System.out.println("WiFi not connected");
            showToast("Please connect to a network");
        } else {
            try {
                System.out.println("WiFI is enabled");
                sendBroadcastPackets();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Checks if wifi is connected to a network or not
     *
     * @return true if connected else false
     */
    private boolean wifiConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return wifiInfo.isConnected();
    }

    /**
     * Displays the WiFi prompt to enable WiFi
     */
    private void showWifiPrompt() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("WiMic")
            .setCancelable(false)
            .setMessage("The WiFi is not enabled. Do you want to turn it on?")
            .setPositiveButton("Enable", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(final DialogInterface dialogInterface, final int i) {
                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                }
            })
            .setNegativeButton("Quit", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(final DialogInterface dialogInterface, final int i) {
                    finish();
                }
            })
            .create()
            .show();
    }

    /**
     * Generates broadcast address from DHCP info of Wifi
     *
     * @return the broadcast address of the network
     * @throws IOException if it is not able to get DHCP info
     */
    InetAddress getBroadcastAddress() throws IOException {
        DhcpInfo dhcpInfo = wifi.getDhcpInfo();
        if (dhcpInfo == null) {
            throw new IOException("Cannot get DHCP information");
        }

        int broadcast = (dhcpInfo.ipAddress & dhcpInfo.netmask) | ~dhcpInfo.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++) {
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        }

        return InetAddress.getByAddress(quads);
    }

    /**
     * Sends broadcast message on given broadcast address
     *
     * @throws IOException if it is not able to send packets
     */
    private void sendBroadcastPackets() throws IOException {
        animateRefresh();

        // Start a new Async Task
        new Scanner(recyclerView, MainActivity.this).executeOnExecutor(
                AsyncTask.THREAD_POOL_EXECUTOR,
                getBroadcastAddress()
        );
    }

    /**
     * Starts animation on refresh button
     */
    private void animateRefresh() {
        LayoutInflater inflater = (LayoutInflater) MainActivity.this.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE
        );
        ImageView imageView = (ImageView) inflater.inflate(R.layout.refresh_action_view, null);
        Animation rotation = AnimationUtils.loadAnimation(
                MainActivity.this,
                R.anim.clockwise_refresh
        );
        rotation.setRepeatCount(Animation.INFINITE);
        imageView.startAnimation(rotation);

        refreshItem.setActionView(imageView);
    }

    /**
     * Stops animation on refresh button
     */
    public void resetRefresh() {
        if (refreshItem.getActionView() != null) {
            refreshItem.getActionView().clearAnimation();
            refreshItem.setActionView(null);
        }
    }

    /**
     * Prompts user for PIN when he/she clicks on room
     *
     * @param v View clicked
     */
    public void onClick(View v) {
        final TextView addressView = (TextView) v.findViewById(R.id.ip_addr);
        final TextView nameView = (TextView) v.findViewById(R.id.room_name);
        final EditText passField = new EditText(this);

        passField.setInputType(InputType.TYPE_CLASS_NUMBER);
        passField.setLayoutParams(new ActionBar.LayoutParams(50, 50));

        // Prompt user for password
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter PIN")
            .setCancelable(false)
            .setView(passField)
            .setPositiveButton("Join", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(final DialogInterface dialogInterface, final int i) {
                    ProgressDialog dialog = ProgressDialog.show(
                            MainActivity.this,
                            "Loading",
                            "Please wait...",
                            true
                    );

                    String password = passField.getText().toString();
                    String ipAddress = addressView.getText().toString(); // Ex "/192.168.xxx.xxx"
                    String roomName = nameView.getText().toString();

                    // Strip the '/' if present
                    if (ipAddress.charAt(0) == '/') {
                        ipAddress = ipAddress.substring(1);
                    }

                    joinRoom(ipAddress, password, roomName, dialog);
                }
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(final DialogInterface dialogInterface, final int i) {
                    // Do nothing
                }
            })
            .create()
            .show();
    }

    /**
     * Validates PIN and open next activity
     *
     * @param ipAddress Receiver's IP
     * @param pin Pin entered by user
     * @param dialog ProgressDialog
     */
    private void joinRoom(
            final String ipAddress,
            final String pin,
            final String roomName,
            final ProgressDialog dialog
    ) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                DatagramSocket socket = null;
                try {
                    socket = new DatagramSocket();
                    sendMessage(
                            socket,
                            Config.JOIN_MESSAGE + ";" + pin,
                            ipAddress,
                            Config.MESSAGE_PORT
                    );

                    // Receive the response
                    byte[] receiveBuffer = new byte[15000];
                    DatagramPacket packet = new DatagramPacket(
                            receiveBuffer,
                            receiveBuffer.length
                    );

                    socket.setSoTimeout(Config.RESPONSE_TIMEOUT);
                    socket.receive(packet);
                    handleResponse(packet, ipAddress, roomName);

                    dialog.dismiss();
                    socket.close();
                } catch (SocketTimeoutException e) {
                    // Do nothing
                    showToast("Cannot reach server");
                } catch (Exception e) {
                    // TODO
                    e.printStackTrace();
                    showToast("Some error occurred");
                } finally {
                    dialog.dismiss();

                    if (socket != null) {
                        socket.close();
                    }
                }
            }
        }).start();
    }

    /**
     * Show toast message on UI thread if search
     * is already in progress
     *
     * @param message Message to show on Toast
     */
    private void showToast(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(
                        MainActivity.this,
                        message,
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    /**
     * Sends a message to a given IP and port
     *
     * @param socket Socket object
     * @param message Message to send
     * @param ipAddress Receiver's IP
     * @param port Receiver's Port
     *
     * @throws Exception if not able to send message
     */
    private void sendMessage(
            DatagramSocket socket,
            String message,
            String ipAddress,
            int port
    ) throws Exception {
        byte[] sendData = message.getBytes();

        DatagramPacket sendPacket = new DatagramPacket(
                sendData,
                sendData.length,
                InetAddress.getByName(ipAddress),
                port
        );

        socket.send(sendPacket);
    }

    /**
     * Handles response from Join Room message
     *
     * @param packet Response
     * @param ipAddress Receiver's IP
     */
    private void handleResponse(DatagramPacket packet, String ipAddress, String roomName) {
        String message = new String(packet.getData()).trim();

        if (message.equals(Config.JOIN_SUCCESS)) {
            Intent myIntent = new Intent(MainActivity.this, Speak.class);
            myIntent.putExtra("ipAddress", ipAddress);
            myIntent.putExtra("roomName", roomName);
            MainActivity.this.startActivity(myIntent);

        } else if (message.equals(Config.JOIN_FAIL)) {
            showToast("Invalid PIN");
        }
    }
}
