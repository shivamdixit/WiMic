package in.ac.lnmiit.wimic;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * class MainActivity
 *
 * Main activity of the app. Controls the UI as well as other events
 */
public class MainActivity extends ActionBarActivity {

    private WifiManager wifi;

    protected RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize WiFi Manager for later use
        wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Will work only after setting contentView
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);

        // TODO: Can be improved
        // Initialize empty adapter
        List<Room> rooms = new ArrayList<>();
        RVAdapter adapter = new RVAdapter(rooms);
        recyclerView.setAdapter(adapter);

        checkWifi();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void checkWifi() {
        if (!wifi.isWifiEnabled()) {
            System.out.println("WiFI is disabled");
            showWifiPrompt();
        } else {
            try {
                System.out.println("WiFI is enabled");
                sendBroadcastPackets();
            } catch (Exception e) {
                System.out.println("Caught Exception!");
            }
        }
    }

    private void showWifiPrompt() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("WiMic")
                .setCancelable(false)
                .setMessage("The WiFi is not enabled. Do you want to turn it on?")
                .setPositiveButton("Enable", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialogInterface, final int i) {
                        startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
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

    private void sendBroadcastPackets() throws IOException {
        // Start a new Async Task
        new Network(recyclerView).execute(getBroadcastAddress());
    }
}
