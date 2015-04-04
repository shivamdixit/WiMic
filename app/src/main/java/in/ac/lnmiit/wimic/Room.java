package in.ac.lnmiit.wimic;

/**
 * @author Shivam Dixit <shivamd001 at gmail.com>
 */
public class Room {

    private String name;
    private String ipAddress;

    Room(String name, String ipAddress) {
        this.name = name;
        this.ipAddress = ipAddress;
    }

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
