import org.ice4j.Transport;
import org.ice4j.TransportAddress;
import org.ice4j.socket.IceUdpSocketWrapper;
import org.ice4j.stunclient.SimpleAddressDetector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.URL;


public class ClientCode {
	public static final String SERVER_URL = "http://stun.l.google.com";
	public static final int SERVER_PORT = 19302;
    public static final int LOCAL_PORT = 56145;
	
	static DatagramSocket socket; 
	
	public static void main(String[] args) throws IOException{
		//Set up the socket
        socket = new DatagramSocket(LOCAL_PORT, InetAddress.getLocalHost());

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

        System.out.print("Client IP Address: ");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in,"UTF-8"));
        String destinationIPAddress = br.readLine();

        System.out.print("Client Port: ");
        String destinationPort = br.readLine();

        socket =  new DatagramSocket(LOCAL_PORT, InetAddress.getLocalHost());

        String sentData = "SNEDER";

        DatagramPacket sentPacket = new DatagramPacket(sentData.getBytes(), sentData.getBytes().length,
                InetAddress.getByName(destinationIPAddress.trim()), Integer.valueOf(destinationPort.trim()));

        ReceiveDatagram receiver = new ReceiveDatagram(socket);
        receiver.start();

        while (true)
        {
            socket.send(sentPacket);
            System.out.println("Sending packet...");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
		
	}
}
