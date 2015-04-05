package in.ac.lnmiit.wimic;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;


public class Speak extends ActionBarActivity {

    private AudioRecord recorder;
    private int speakPort = 9898;    // SPEAK_PORT on server

    private int sampleRate = 16000;
    private int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    int minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);

    private InetAddress destination;
    private String LOCALHOST = "0.0.0.0";

    private boolean buttonPressed = false;

    private DatagramSocket socket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speak);

        try {
            minBufSize += 3800;
            // TODO: Extract the key outside in a common file
            destination = InetAddress.getByName(getIntent().getExtras().getString("ipAddress"));
            socket = new DatagramSocket(speakPort, InetAddress.getByName(LOCALHOST));
            recorder = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    minBufSize * 10
            );

            addTouchListener();
        } catch (Exception e) {
            // TODO
        }
    }

    private void addTouchListener() {
        Button startButton = (Button) findViewById(R.id.speak_button);
        startButton.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        System.out.println("Button pressed");
                        buttonPressed = true;
                        startStreaming();
                        break;

                    case MotionEvent.ACTION_UP:
                        System.out.println("Button Released");
                        buttonPressed = false;
                        recorder.stop();
                        break;
                }
                return true;
            }
        });
    }

    public void startStreaming() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] buffer = new byte[minBufSize];

                    recorder.startRecording();
                    while (buttonPressed) {
                        minBufSize = recorder.read(buffer, 0, buffer.length);
                        DatagramPacket packet = new DatagramPacket(
                                buffer,
                                buffer.length,
                                destination,
                                speakPort
                        );

                        socket.send(packet);
                    }
                } catch (Exception e) {
                    // TODO
                    System.out.println(e);
                    recorder.release();
                }
            }
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_speak, menu);
        return true;
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
}
