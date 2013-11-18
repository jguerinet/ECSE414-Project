import java.io.IOException;
import java.net.*;

/**
 * Created with IntelliJ IDEA.
 * User: Julien
 * Date: 18/11/13
 * Time: 4:40 PM
 */
public class PacketSender {
    private DatagramSocket socket;
    public InetAddress destinationAddress;
    public int destinationPort;

    private static byte[] packetBuffer = new byte[1024];
    private static String ACK = "ACK";

    public PacketSender(DatagramSocket socket, InetAddress address, int port){
        this.socket = socket;
        this.destinationAddress = address;
        this.destinationPort = port;
    }

    //This would send acknowledgment packets to hole punch your NAT
    public String connectToClient(String name){
        //Let the user know we are trying to connect
        System.out.println("Connecting to peer...");

        //Send 10 of these (gives us a better to chance to connect to the client)
        for(int i = 0; i < 10; i++){
            try {
                sendMessage(ACK + name);
            } catch (IOException e) {
                System.out.println("Error sending packet to peer.");
                e.printStackTrace();
                return null;
            }
        }

        //Receive a connection packet from your peer
        DatagramPacket peerAckPacket;
        try {
            peerAckPacket = receiveAckPacket();
        } catch (IOException e) {
            System.out.println("Error receiving packet from peer.");
            e.printStackTrace();
            return null;
        }

        if(peerAckPacket != null){
            String receivedData;
            try {
                receivedData = new String(receiveAckPacket().getData());
            } catch (IOException e) {
                System.out.println("Could not read data from packet");
                return null;
            }
            //Check that it's an ACK packet
            if(receivedData.contains(ACK)){
                //Return the name of the peer (in the ACK packet)
                return receivedData.replace(ACK, "");
            }
            else{
                //If not, return false
                return null;
            }
        }
        else{
            //We did not receive a packet
            return null;
        }
    }

    public void sendMessage(String message) throws IOException {
        socket.send(new DatagramPacket(message.getBytes(),message.getBytes().length,
                destinationAddress, destinationPort));
    }

    public DatagramPacket receiveAckPacket() throws IOException {
        //4 second timeout ?
        socket.setSoTimeout(4000);
        DatagramPacket receivedPacket = new DatagramPacket(packetBuffer, packetBuffer.length);
        try{
            socket.receive(receivedPacket);
        }catch(SocketTimeoutException e){
            System.out.println("Timed out");
            return null;
        }
        return receivedPacket;
    }
}

