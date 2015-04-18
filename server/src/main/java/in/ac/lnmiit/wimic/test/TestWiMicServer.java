package test;

import org.junit.*;
import static org.junit.Assert.*;
import in.ac.lnmiit.wimic.*;

public class TestWiMicServer
{
    private WiMicServer server;

    private Thread serverThread;

    private boolean firstRun = true;

    @Before
    public void setUp()
    {
        if (firstRun) {
            server = new WiMicServer("Server", 1234);
            serverThread = new Thread(server);
            serverThread.start();
            firstRun = false;
        }
    }

    @Test
    public void testPin()
    {
        assertEquals(1234, server.getPin());
        assertNotEquals("", server.getPin());
        assertNotEquals(4564, server.getPin());

        server.setPin(4567);
        assertNotEquals("1234", server.getPin());
        assertNotEquals(1234, server.getPin());
        assertEquals(4567, server.getPin());
    }

    @Test
    public void testName()
    {
        assertEquals("Server", server.getName());
        assertNotEquals("", server.getName());
        assertNotEquals("1234", server.getName());

        server.setName("Test");
        assertNotEquals("Hello", server.getName());
        assertNotEquals("1234", server.getName());
        assertEquals("Test", server.getName());
        assertNotEquals("", server.getName());
    }

    @Test
    public void testValidatePin()
    {
        assertTrue(server.validatePin("1234"));
        assertFalse(server.validatePin(""));
        assertFalse(server.validatePin("9872"));
    }
}
