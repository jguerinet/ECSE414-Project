import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

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

    private static String SIGNAL_PACKET = "#!#DISCONNECTED";
    private static String ACK_SIGNAL_PACKET = "#!#CONNECTED";
    private static String ACK_PACKET = "#!#BOTHCONNECTED";
    private static String DISCONNECT = "#!#DISCONNECT";

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
    public boolean connectToClient(String peerName){
        //Set the peer name
        this.peerName = peerName;

        //Let the user know we are trying to connect
        System.out.println("Connecting to " + peerName + " at address " + destinationAddress.getHostAddress() + " on port " + destinationPort);

        //Send 5 of these (gives us a better to chance to connect to the client)
        //With Connected if we are connected of not connected if we are not connected
        String ackMessage;
        if(connected && peerConnected){
            ackMessage = ACK_PACKET;
            bothConnected = true;
        }
        else if(connected){
            ackMessage = ACK_SIGNAL_PACKET;
        }
        else{
            ackMessage = SIGNAL_PACKET;
        }

        for(int i = 0; i < 5; i++){
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

    public void disconnect(){
        sendMessage(DISCONNECT);
    }

    //To receive packets we have a separate thread.
    public class PacketReceiver extends Thread
    {
        @Override
        public void run()
        {
            try
            {
                while(true)
                {
                    byte[] packetBuffer = new byte[1024];
                    DatagramPacket receivedPacket = new DatagramPacket(packetBuffer, packetBuffer.length);
                    socket.receive(receivedPacket);
                    String packetData = new String(receivedPacket.getData()).trim();

                    //You are now connected to the peer
                    if(packetData.startsWith(SIGNAL_PACKET)){
                        connected = true;
                    }
                    //Peer is connected to you
                    else if(packetData.startsWith(ACK_SIGNAL_PACKET)){
                        connected = true;
                        peerConnected = true;
                    }
                    else if(packetData.startsWith(ACK_PACKET)){
                        bothConnected = true;
                    }
                    else if(packetData.startsWith(DISCONNECT)){
                        System.out.println(peerName + " has disconnected.");
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

