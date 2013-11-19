import java.io.IOException;
import java.net.*;

/**
 * Created with IntelliJ IDEA.
 * User: Julien
 * Date: 18/11/13
 * Time: 4:40 PM
 */
public class PacketCommunicator {
    private DatagramSocket socket;
    public InetAddress destinationAddress;
    public int destinationPort;
    private String peerName;
    private boolean connected, peerConnected, bothConnected;

    private static byte[] packetBuffer = new byte[1024];
    private static String ACK_NOT_CONNECTED = "#!#DISCONNECTED";
    private static String ACK_CONNECTED = "#!#CONNECTED";
    private static String ACK_BOTH_CONNECTED = "#!#BOTHCONNECTED";

    public PacketCommunicator(DatagramSocket socket, InetAddress address, int port){
        this.socket = socket;
        this.destinationAddress = address;
        this.destinationPort = port;
        this.connected = false;
        this.peerConnected = false;
        this.bothConnected = false;

        //Initialize the receiver
        new PacketReceiver().start();
    }

    //This would send acknowledgment packets to hole punch your NAT
    public boolean connectToClient(String name){
        //Let the user know we are trying to connect
        System.out.println("Connecting to peer...");

        //Send 10 of these (gives us a better to chance to connect to the client)
        //With Connected if we are connected of not connected if we are not connected
        String ackMessage;
        if(connected && peerConnected){
            ackMessage = ACK_BOTH_CONNECTED;
        }
        else if(connected){
            ackMessage = ACK_CONNECTED;
        }
        else{
            ackMessage = ACK_NOT_CONNECTED + name;
        }
        for(int i = 0; i < 10; i++){
            sendMessage(ackMessage);
        }

        //Wait 2 seconds
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //Check if both people are connected
        return bothConnected;
    }

    public void sendMessage(String message){
        try {
            socket.send(new DatagramPacket(message.getBytes(),message.getBytes().length,
                    destinationAddress, destinationPort));
        } catch (IOException e) {
            System.out.println("Error sending packet to peer");
            e.printStackTrace();
        }
    }

    //To receive packets we have a separate thread.
    public class PacketReceiver extends Thread
    {
        @Override
        public void run()
        {
            DatagramPacket receivedPacket = new DatagramPacket(packetBuffer, packetBuffer.length);
            try
            {
                while(true)
                {
                    socket.receive(receivedPacket);
                    String packetData = new String(receivedPacket.getData());

                    //You are now connected to the peer
                    if(packetData.startsWith(ACK_NOT_CONNECTED)){
                        connected = true;
                        peerName = packetData.replace(ACK_NOT_CONNECTED, "");
                    }
                    //Peer is connected to you
                    else if(packetData.startsWith(ACK_CONNECTED)){
                        connected = true;
                        peerConnected = true;
                    }
                    else if(packetData.startsWith(ACK_BOTH_CONNECTED)){
                        bothConnected = true;
                    }
                    //Normal message
                    else{
                        System.out.println(peerName + ": " + packetData);
                    }
                }
            }
            catch(Exception ex)
            {
                System.out.println("Error receiving message.");
                ex.printStackTrace();
            }
        }
    }

}

