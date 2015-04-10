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

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;


public class Speak extends ActionBarActivity {

    /**
     * Audio config
     */
    private int sampleRate = 16000;
    private int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private AudioRecord recorder;

    /**
     * Initialize minimum buffer.
     */
    private int minBufSize;

    /**
     * IP Address of destination host.
     */
    private InetAddress destination;

    /**
     * Flag to test if button is pressed.
     */
    private boolean buttonPressed = false;

    /**
     * Holds the socket object for sending voice
     */
    private DatagramSocket socket;

    /**
     * Holds the socket object for other queries like
     * if channel is available or not.
     */
    private DatagramSocket otherSocket;

    /**
     * Flag to check whether to send stop message on
     * button release.
     *
     * Hint: Don't send it if channel is available or
     * timeout has occurred.
     */
    private boolean sendStopMessageOnRelease = false;

    /**
     * Entry point of the activity, initializes required objects.
     *
     * @param savedInstanceState Instance state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speak);

        // Display back button and title
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getIntent().getExtras().getString("roomName"));
        minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) + 3800;

        try {
            destination = InetAddress.getByName(getIntent().getExtras().getString("ipAddress"));
            socket = new DatagramSocket();
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
     * Initialize touch listener on speak button.
     */
    private void addTouchListener() {
        ImageView startButton = (ImageView) findViewById(R.id.speak_button);
        startButton.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        handleButtonDown();
                        break;

                    case MotionEvent.ACTION_UP:
                        handleButtonUp();
                        break;
                }
                return true;
            }
        });
    }

    /**
     * Handles speak button down.
     */
    private void handleButtonDown() {
        System.out.println("Button pressed");
        if (!buttonPressed) {
            buttonPressed = true;
            tryStreaming(); // If channel is available start transmission
        }
    }

    /**
     * Handles speak button up.
     */
    private void handleButtonUp() {
        System.out.println("Button Released");
        if (buttonPressed) {
            buttonPressed = false;
            if (sendStopMessageOnRelease) {
                sendStopMessage();
            }

            if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
                recorder.stop();
            }
        }
    }

    /**
     * Checks if channel is available for transmission.
     * If available, then start transmissions.
     */
    private void tryStreaming() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    DatagramSocket checkStreamSocket = new DatagramSocket();
                    byte[] toSend = Config.SPEAK_MESSAGE.getBytes();
                    DatagramPacket packet = new DatagramPacket(
                            toSend,
                            toSend.length,
                            destination,
                            Config.MESSAGE_PORT
                    );
                    checkStreamSocket.send(packet);

                    byte[] receiveBuffer = new byte[15000];
                    DatagramPacket receivePacket = new DatagramPacket(
                            receiveBuffer,
                            receiveBuffer.length
                    );
                    checkStreamSocket.receive(receivePacket);
                    final String message = new String(receivePacket.getData()).trim();
                    handleStreamResponse(message);

                    checkStreamSocket.close();
                } catch (Exception e) {
                    // TODO
                    e.printStackTrace();
                }

            }
        }).start();
    }

    /**
     * Checks if response of speak request is ACK or NACK.
     * If channel is available (ACK), start streaming.
     *
     * @param message Message received in response
     */
    private void handleStreamResponse(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Ignore the response if button is no longer pressed
                // before the response comes.
                if (!buttonPressed) {
                    return;
                }

                switch (message) {
                    case Config.SPEAK_ACK:
                        // Channel is available
                        ImageView speakBtn = (ImageView) findViewById(R.id.speak_button);
                        speakBtn.setImageResource(R.drawable.button_green);
                        sendStopMessageOnRelease = true;
                        startStreaming();
                        break;

                    case Config.SPEAK_NACK:
                        // Channel is not available
                        sendStopMessageOnRelease = false;
                        Toast.makeText(
                                getApplicationContext(),
                                "Channel unavailable",
                                Toast.LENGTH_SHORT
                        ).show();
                        break;
                    default:
                        Toast.makeText(
                                getApplicationContext(),
                                "Some error occurred",
                                Toast.LENGTH_SHORT
                        ).show();
                        break;
                }
            }
        });
    }

    /**
     * Sends stop message to the server
     */
    private void sendStopMessage() {
        try {
            ImageView speakBtn = (ImageView) findViewById(R.id.speak_button);
            speakBtn.setImageResource(R.drawable.button_speak);

            sendMessage(Config.STOP_SPEAK_MESSAGE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Send message to server on MESSAGE_PORT
     *
     * @param message Message to send
     */
    private void sendMessage(final String message) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    otherSocket = new DatagramSocket();

                    byte[] toSend = message.getBytes();
                    DatagramPacket packet = new DatagramPacket(
                            toSend,
                            toSend.length,
                            destination,
                            Config.MESSAGE_PORT
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
                // Initialize min buffer size every time
                minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
                minBufSize += 3800;
                byte[] buffer = new byte[minBufSize];

                try {
                    recorder.startRecording();
                    listenTimeout();
                    while (buttonPressed) {
                        minBufSize = recorder.read(buffer, 0, buffer.length);
                        DatagramPacket packet = new DatagramPacket(
                                buffer,
                                buffer.length,
                                destination,
                                Config.SPEAK_PORT
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
     * Listens for timeout event on speaking
     */
    private void listenTimeout() {
        // Listen for timeout event
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
                    if (message.equals(Config.SPEAK_TIMEOUT) && buttonPressed) {
                        handleTimeout();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Turns the button red if channel is unavailable
     * and sets the flags.
     */
    private void handleTimeout() {
        buttonPressed = false;
        sendStopMessageOnRelease = false;

        recorder.stop();

        // Update the UI on UI Thread
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ImageView speakBtn = (ImageView) findViewById(R.id.speak_button);
                speakBtn.setImageResource(R.drawable.button_speak);
            }
        });
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
