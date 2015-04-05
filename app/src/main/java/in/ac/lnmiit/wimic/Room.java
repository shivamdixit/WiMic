package in.ac.lnmiit.wimic;

/**
 * class Room
 *
 * Represents a server
 */
public class Room {

    /**
     * Name of the room
     */
    private String name;

    /**
     * IP Address of the room
     */
    private String ipAddress;

    /**
     * Constructor
     *
     * @param name Name of the room
     * @param ipAddress IP address of the room
     */
    Room(String name, String ipAddress) {
        this.name = name;
        this.ipAddress = ipAddress;
    }

    /**
     * Getters and setters
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
}
