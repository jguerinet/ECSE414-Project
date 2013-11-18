import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Created with IntelliJ IDEA.
 * User: Yulric
 * Date: 17/11/13
 * Time: 8:30 PM
 * To change this template use File | Settings | File Templates.
 */


public class ReceiveDatagram extends Thread
{
    DatagramSocket receiverSocket;

    public ReceiveDatagram(DatagramSocket receiverSocket)
    {
        this.receiverSocket = receiverSocket;
    }

    @Override
    public void run()
    {
        DatagramPacket receivedPacket = new DatagramPacket(new byte[1024], 1024);

        try
        {
            while(true)
            {
                receiverSocket.receive(receivedPacket);
                System.out.print("LOL");

                System.out.println(new String(receivedPacket.getData()));
            }
        }
        catch(Exception ex)
        {
            System.out.println("Receive Datagram " + ex.getMessage());
        }
    }
}
