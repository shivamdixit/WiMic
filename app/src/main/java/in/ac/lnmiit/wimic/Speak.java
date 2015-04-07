package in.ac.lnmiit.wimic;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOError;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;


public class Speak extends ActionBarActivity {

    /**
     * Port to send data
     */
    private int speakPort = 9898;    // SPEAK_PORT on server

    // TODO: Refactor into single config file
    private final int PORT = 9876;

    private final String SPEAK_MESSAGE = "WIMIC_SPEAK_REQ";
    private final String STOP_SPEAK_MESSAGE = "WIMIC_SPEAK_END";
    private final String SPEAK_ACK = "WIMIC_SPEAK_ACK";
    private final String SPEAK_NACK = "WIMIC_SPEAK_NACK";
    private final String SPEAK_TIMEOUT = "WIMIC_SPEAK_TIMEOUT";

    /**
     * Audio config
     */
    private AudioRecord recorder;
    private int sampleRate = 16000;
    private int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

    /**
     * Initialize minimum buffer
     */
    int minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);

    /**
     * IP Address of destination host
     */
    private InetAddress destination;

    /**
     * Localhost IP Address
     */
    private String LOCALHOST = "0.0.0.0";

    /**
     * Flag to test if button is pressed
     */
    private boolean buttonPressed = false;

    /**
     * Holds the socket object for sending voice
     */
    private DatagramSocket socket;

    /**
     * Holds the socket object for other queries like
     * if channel is available or not
     */
    private DatagramSocket otherSocket;

    /**
     * Entry point of the activity. Initializes required objects.
     *
     * @param savedInstanceState Instance state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speak);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

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
            e.printStackTrace();
        }
    }

    /**
     * Initialize touch listener on speak button
     */
    private void addTouchListener() {
        ImageView startButton = (ImageView) findViewById(R.id.speak_button);
        startButton.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        System.out.println("Button pressed");
                        buttonPressed = true;
                        tryStreaming();
                        break;

                    case MotionEvent.ACTION_UP:
                        System.out.println("Button Released");
                        if (buttonPressed) {
                            buttonPressed = false;
                            sendStopMessage();
                            recorder.stop();
                        }

                        break;
                }
                return true;
            }
        });
    }

    private void tryStreaming() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    DatagramSocket checkStreamSocket = new DatagramSocket(
                            12345,
                            InetAddress.getByName(LOCALHOST)
                    );

                    byte[] toSend = SPEAK_MESSAGE.getBytes();
                    DatagramPacket packet = new DatagramPacket(
                            toSend,
                            toSend.length,
                            destination,
                            PORT
                    );

                    checkStreamSocket.send(packet);

                    byte[] receiveBuffer = new byte[15000];
                    DatagramPacket receivePacket = new DatagramPacket(
                            receiveBuffer,
                            receiveBuffer.length
                    );

                    checkStreamSocket.receive(receivePacket);
                    final String message = new String(receivePacket.getData()).trim();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (message.equals(SPEAK_ACK)) {
                                // Channel is available
                                ImageView speakBtn = (ImageView) findViewById(R.id.speak_button);
                                speakBtn.setImageResource(R.drawable.button_green);
                                startStreaming();
                            } else {
                                // Channel is not available
                                Toast.makeText(
                                        getApplicationContext(),
                                        "Channel unavailable",
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }
                    });

                    checkStreamSocket.close();
                } catch (Exception e) {
                    // TODO
                    e.printStackTrace();
                }

            }
        }).start();
    }

    private void sendStopMessage() {
        try {
            ImageView speakBtn = (ImageView) findViewById(R.id.speak_button);
            speakBtn.setImageResource(R.drawable.button_speak);

            sendMessage(STOP_SPEAK_MESSAGE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(final String message) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    otherSocket = new DatagramSocket(
                            12345,
                            InetAddress.getByName(LOCALHOST)
                    );

                    byte[] toSend = message.getBytes();
                    DatagramPacket packet = new DatagramPacket(
                            toSend,
                            toSend.length,
                            destination,
                            PORT
                    );

                    otherSocket.send(packet);
                    otherSocket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Transmit voice to server through UDP
     */
    public void startStreaming() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] buffer = new byte[minBufSize];

                    recorder.startRecording();

                    // Listen for timeout
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                byte[] receiveBuffer = new byte[15000];
                                DatagramPacket receivePacket = new DatagramPacket(
                                        receiveBuffer,
                                        receiveBuffer.length
                                );

                                socket.receive(receivePacket);

                                final String message = new String(receivePacket.getData()).trim();
                                if (message.equals(SPEAK_TIMEOUT)) {
                                    buttonPressed = false;
                                    recorder.stop();

                                    // Change the UI
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            ImageView speakBtn = (ImageView) findViewById(R.id.speak_button);
                                            speakBtn.setImageResource(R.drawable.button_speak);
                                        }
                                    });
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();

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
                    System.out.println("Exception");
                    recorder.release();
                }
            }
        }).start();
    }

    /**
     * Auto-generated method
     *
     * @param menu Menu
     * @return boolean
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_speak, menu);
        return true;
    }

    /**
     * Auto-generated method
     *
     * @param item Item
     * @return boolean
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
        } else if (id == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
