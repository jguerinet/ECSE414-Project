import org.ice4j.Transport;
import org.ice4j.TransportAddress;
import org.ice4j.socket.IceUdpSocketWrapper;
import org.ice4j.stunclient.SimpleAddressDetector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.URL;


public class Client {
	public static final String SERVER_URL = "http://stun.l.google.com";
	public static final int SERVER_PORT = 19302;
    public static final int LOCAL_PORT = 56144;

    public static final String DISCONNECT = "EXIT";

    static BufferedReader reader;
	static DatagramSocket socket;

    static String name;
	
	public static void main(String[] args) throws IOException{
		//Set up the socket
        socket = new DatagramSocket(LOCAL_PORT, InetAddress.getLocalHost());

        //Set up the CLI reader
        reader = new BufferedReader(new InputStreamReader(System.in,"UTF-8"));

        //Get the user's name
        System.out.print("Please enter your name: ");
        name = reader.readLine();

        /* Connect to a STUN Server to get Public IP Address and Public Port */

		//Set up the wrapper
        IceUdpSocketWrapper socketWrapper = new IceUdpSocketWrapper(socket);

        //Set up the server
		TransportAddress socketTransportAddress = new TransportAddress(new URL(SERVER_URL).getHost(), SERVER_PORT, Transport.UDP);

		//Set up the Address Discoverer
		SimpleAddressDetector detector = new SimpleAddressDetector(socketTransportAddress);

		//Start it up
		detector.start();

        //Get the public info
		TransportAddress publicServerAddress = detector.getMappingFor(socketWrapper);

        //Print
        System.out.println("Public Address : " + publicServerAddress.getAddress());
        System.out.println("Public Port: " + publicServerAddress.getPort());

        //TODO Connect to the server

        //TODO Take this out after.

        System.out.print("Client IP Address: ");
        InetAddress destinationAddress = InetAddress.getByName(reader.readLine());
        System.out.print("Client Port: ");
        String destinationPort = reader.readLine();



        //Reconnect the socket (for some reason the getMappingFor() method closes the socket
        if(socket.isClosed()){
            socket =  new DatagramSocket(LOCAL_PORT, InetAddress.getLocalHost());
        }

        PacketCommunicator sender = new PacketCommunicator(socket, destinationAddress, Integer.valueOf(destinationPort));

        //Connect to the peer
        boolean connected = false;

        //Keep on trying to connect to the peer
        while(!connected){
            connected = sender.connectToClient(name);
            if(!connected){
                System.out.println("Connection Failed. Retrying...");
            }
        }

        System.out.println("Connected");

        //Send the messages
        while(true){
            String message = reader.readLine();
            if(message.equals(DISCONNECT)){
                sender.disconnect();
                System.out.println("Disconnecting...");
                System.exit(0);
            }
            else{
                sender.sendMessage(message);
            }
        }
	}
}
