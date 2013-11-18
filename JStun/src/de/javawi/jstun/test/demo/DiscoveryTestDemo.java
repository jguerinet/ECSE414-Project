/*
 * This file is part of JSTUN. 
 * 
 * Copyright (c) 2005 Thomas King <king@t-king.de> - All rights
 * reserved.
 * 
 * This software is licensed under either the GNU Public License (GPL),
 * or the Apache 2.0 license. Copies of both license agreements are
 * included in this distribution.
 */

package de.javawi.jstun.test.demo;

import de.javawi.jstun.test.DiscoveryInfo;
import de.javawi.jstun.test.DiscoveryTest;
import de.javawi.jstun.test.demo.ice.ReceiveDatagram;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.util.Enumeration;
import java.util.logging.*;

public class DiscoveryTestDemo implements Runnable {
	public static final int LOCAL_PORT = 0;

    InetAddress iaddress;
	int port;
	
	public DiscoveryTestDemo(InetAddress iaddress, int port) {
		this.iaddress = iaddress;
		this.port = port;
	}
	
	public DiscoveryTestDemo(InetAddress iaddress) {
		this.iaddress = iaddress;
		this.port = 0;
	}
	
	public void run() {		
		try {
			DiscoveryTest test = new DiscoveryTest(iaddress, port, "jstun.javawi.de", 3478);
			//DiscoveryTest test = new DiscoveryTest(iaddress, "stun.sipgate.net", 10000);
			// iphone-stun.freenet.de:3478
			// larry.gloo.net:3478
			// stun.xten.net:3478
			// stun.sipgate.net:10000
            DiscoveryInfo externalInfo = test.test();
            System.out.println(externalInfo );

            System.out.println("My External IP: " + externalInfo.getPublicIP());
            System.out.println("My External Port: " + Integer.toString(externalInfo.getPublicPort()));

            System.out.print("Client IP Address: ");
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in,"UTF-8"));
            String destinationIPAddress = br.readLine();

            System.out.print("Client Port: ");
            String destinationPort = br.readLine();

            DatagramSocket senderSocket = new DatagramSocket(externalInfo.getPublicPort(), InetAddress.getLocalHost());

            String sentData = "SNEDER";

            DatagramPacket sentPacket = new DatagramPacket(sentData.getBytes(), sentData.getBytes().length,
                    InetAddress.getByName(destinationIPAddress.trim()), Integer.valueOf(destinationPort.trim()));

            ReceiveDatagram receiver = new ReceiveDatagram(senderSocket);
            receiver.start();

            while (true)
            {
                senderSocket.send(sentPacket);
                System.out.println("Sending packet...");
                Thread.sleep(1000);
            }


		} catch (BindException be) {
			System.out.println(iaddress.toString() + ": " + be.getMessage());
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static void main(String args[]) {
		try {
			Handler fh = new FileHandler("logging.txt");
			fh.setFormatter(new SimpleFormatter());
			Logger.getLogger("de.javawi.jstun").addHandler(fh);
			Logger.getLogger("de.javawi.jstun").setLevel(Level.ALL);
			
			Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
			while (ifaces.hasMoreElements()) {
				NetworkInterface iface = ifaces.nextElement();
				Enumeration<InetAddress> iaddresses = iface.getInetAddresses();
				while (iaddresses.hasMoreElements()) {
					InetAddress iaddress = iaddresses.nextElement();
					if (Class.forName("java.net.Inet4Address").isInstance(iaddress)) {
						if ((!iaddress.isLoopbackAddress()) && (!iaddress.isLinkLocalAddress())) {
							Thread thread = new Thread(new DiscoveryTestDemo(iaddress));
							thread.start();
						}
					}
				}
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
}