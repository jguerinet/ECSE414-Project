import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Created with IntelliJ IDEA.
 * User: Yulric
 * Date: 17/11/13
 * Time: 8:30 PM
 */


public class PacketReceiver extends Thread
{
    DatagramSocket receiverSocket;
    String peerName;
    private static byte[] packetBuffer = new byte[1024];

    public PacketReceiver(DatagramSocket receiverSocket, String peerName)
    {
        this.receiverSocket = receiverSocket;
        this.peerName = peerName;
    }

    @Override
    public void run()
    {
        DatagramPacket receivedPacket = new DatagramPacket(packetBuffer, packetBuffer.length);
        try
        {
            while(true)
            {
                receiverSocket.receive(receivedPacket);
                System.out.println(peerName + ": " + new String(receivedPacket.getData()));
            }
        }
        catch(Exception ex)
        {
            System.out.println("Error receiving message.");
            ex.printStackTrace();
        }
    }
}
