import com.fasterxml.jackson.databind.ObjectMapper;
import org.ice4j.Transport;
import org.ice4j.TransportAddress;
import org.ice4j.socket.IceUdpSocketWrapper;
import org.ice4j.stunclient.SimpleAddressDetector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;


public class Chat {
    public static final String SERVER_URL = "http://chat-sigvaria.rhcloud.com/peers/";
	public static final String STUN_SERVER_URL = "http://stun.l.google.com";
	public static final int STUN_SERVER_PORT = 19302;
    public static final int LOCAL_PORT = 56144;

    public static final String DISCONNECT = "EXIT";

    static BufferedReader reader;
	static DatagramSocket socket;
    static ObjectMapper mapper;

    static String name;
    static Peer[] peers;
    static Peer user;

    static InetAddress destinationAddress, hostInternalAddress, hostExternalAddress;
    static int destinationPort, hostInternalPort, hostExternalPort;
	
	public static void main(String[] args) throws IOException{
		//Set up the internal info
        hostInternalAddress = InetAddress.getLocalHost();
        hostInternalPort = LOCAL_PORT;

		//Set up the socket
        socket = new DatagramSocket(hostInternalPort, hostInternalAddress);

        //Set up the CLI reader
        reader = new BufferedReader(new InputStreamReader(System.in,"UTF-8"));

        //Set up the JSON mapper
        mapper = new ObjectMapper();

        //Get peers
        getPeers();

        boolean validName = false;

        while(!validName){
            validName = true;
            //Get the user's name
            System.out.print("Please enter your name: ");
            name = reader.readLine();
            for(Peer peer : peers){
                if(name.equals(peer.getName())){
                    System.out.println("Error : name already taken, please try again.");
                    validName = false;
                }
            }
        }

        /* STUN SERVER CONNECTION */

		//Set up the wrapper
        IceUdpSocketWrapper socketWrapper = new IceUdpSocketWrapper(socket);

        //Set up the server
		TransportAddress socketTransportAddress = new TransportAddress(new URL(STUN_SERVER_URL).getHost(), STUN_SERVER_PORT, Transport.UDP);

		//Set up the Address Discoverer
		SimpleAddressDetector detector = new SimpleAddressDetector(socketTransportAddress);

		//Start it up
		detector.start();

        //Get the public info
		TransportAddress publicServerAddress = detector.getMappingFor(socketWrapper);
        hostExternalAddress = publicServerAddress.getAddress();
        hostExternalPort = publicServerAddress.getPort();

        //TODO : DELETE
        System.out.println("Public Address : " + hostExternalAddress);
        System.out.println("Public Port: " + hostExternalPort);

        /* SERVER CONNECTION */

        connectToServer();

        //Get the chosen peer
        Peer chosenPeer = null;
        while(chosenPeer == null){
            chosenPeer = choosePeer();
        }

        /* DESTINATION ADDRESS SET-UP */

        //Check if you are behind the same NAT
        if(hostExternalAddress.equals(InetAddress.getByName(chosenPeer.getExternalAddress()))){
            //Use internal address if you are behind the same NAT
            destinationAddress = InetAddress.getByName(chosenPeer.getInternalAddress());
            destinationPort = Integer.valueOf(chosenPeer.getInternalPort());
        }
        else{
            //Use external address if  not.
            destinationAddress = InetAddress.getByName(chosenPeer.getExternalAddress());
            destinationPort = Integer.valueOf(chosenPeer.getExternalPort());
        }

        /* CHAT SET-UP */

        //Reconnect the socket (for some reason the getMappingFor() method closes the socket
        if(socket.isClosed()){
            socket =  new DatagramSocket(hostInternalPort, hostInternalAddress);
        }

        //Set up the packet communicator
        PacketCommunicator communicator = new PacketCommunicator(socket, destinationAddress, destinationPort);

        //Connect to the peer
        boolean connected = false;
        //Keep on trying to connect to the peer
        while(!connected){
            connected = communicator.connectToClient(chosenPeer.getName());
            if(!connected){
                System.out.println("Connection Failed. Retrying...");
            }
        }

        System.out.println("Connected to " + chosenPeer.getName());
        System.out.println("Type 'Exit' to exit the program");

        //Send the messages
        while(true){
            String message = reader.readLine();
            //If user chooses to disconnect
            if(message.equalsIgnoreCase(DISCONNECT)){
                communicator.disconnect();
                System.out.println("Disconnecting...");
                //Disconnect from the server
                disconnectFromServer();
                System.exit(0);
            }
            //If not, just send the message
            else{
                communicator.sendMessage(message);
            }
        }
	}

    //Get list of peers from server
    public static void getPeers() throws IOException {
        //Get the URL
        URL url = new URL(SERVER_URL);

        peers = mapper.readValue(url, Peer[].class);

        //Find out which peer maps to the current user
        for(Peer peer : peers){
            if(peer.getName().equals(name)){
                user = peer;
                break;
            }
        }
    }

    //Get a peer from the list of peers
    public static Peer choosePeer() throws IOException{
        //Show list of peers
        System.out.println("Type the name of the peer you want to connect with or 'refresh' to refresh the list: ");
        System.out.println("Type 'Exit' to exit the program");
        for(Peer peer : peers){
            if(peer.getName() != null && !peer.getName().equals(user.getName())){
                System.out.println("- " + peer.getName());
            }
        }

        System.out.print("Chosen Peer: ");
        String chosenPeerName = reader.readLine().trim();

        if(chosenPeerName.equalsIgnoreCase("refresh")){
            System.out.println("Refreshing...");
            getPeers();
            return null;
        }
        //If user chooses to disconnect
        if(chosenPeerName.equalsIgnoreCase(DISCONNECT)){
            System.out.println("Disconnecting...");
            //Disconnect from the server
            disconnectFromServer();
            System.exit(0);
        }

        //Find the right peer
        for(Peer peer : peers){
            if(chosenPeerName.equals(peer.getName())){
                return peer;
            }
        }

        System.out.println("Name not found. Please try again.");
        return null;
    }

    //Update the status on the server
    public static void connectToServer() throws IOException {
        //Get the string
        String userString = "{\"name\": \"" + name + "\", \"externalAddress\": \"" + hostExternalAddress.getHostAddress()
                            +  "\", \"externalPort\": \"" + hostExternalPort
                            +  "\", \"internalAddress\": \"" + hostInternalAddress.getHostAddress()
                            +  "\", \"internalPort\": \"" + hostInternalPort
                            +  "\"}";

        //Set up the URL connection
        URL url = new URL(SERVER_URL);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");

        //Write the data to the server
        OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
        writer.write(userString);
        writer.flush();
        writer.close();

        //Call this to actually do the transaction
        connection.getResponseCode();
    }

    public static void disconnectFromServer() throws IOException{
        URL url = new URL(SERVER_URL + user.getId());
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("DELETE");
        connection.setRequestProperty("Content-Type", "application/json");

        connection.getResponseCode();
    }
}
