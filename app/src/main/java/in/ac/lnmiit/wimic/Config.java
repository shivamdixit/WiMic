package in.ac.lnmiit.wimic;

public class Config {
    public final static int MESSAGE_PORT = 9876;
    public final static int SPEAK_PORT = 9898;
    public final static String JOIN_MESSAGE = "WIMIC_JOIN_PASSWORD";
    public final static String JOIN_SUCCESS = "WIMIC_JOIN_SUCCESS";
    public final static String JOIN_FAIL = "WIMIC_JOIN_FAILURE";

    public final static String SPEAK_MESSAGE = "WIMIC_SPEAK_REQ";
    public final static String STOP_SPEAK_MESSAGE = "WIMIC_SPEAK_END";
    public final static String SPEAK_ACK = "WIMIC_SPEAK_ACK";
    public final static String SPEAK_NACK = "WIMIC_SPEAK_NACK";
    public final static String SPEAK_TIMEOUT = "WIMIC_SPEAK_TIMEOUT";

    // From server: ACK_MESSAGE;name
    public final static String DISC_MESSAGE = "WIMIC_DISCOVER_REQ";
    public final static String ACK_MESSAGE = "WIMIC_DISCOVER_ACK";
}
