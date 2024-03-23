package br.com.j1scorpii.ffmda.util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class HostChecker {
	public static String reason;
	
	public static boolean check( String node, int port, int timeout, String network) {
		boolean result = false;
        Socket s = null;
        reason = "Trying...";
        try {
            s = new Socket();
            s.setReuseAddress(true);
            s.connect( new InetSocketAddress(node, port), timeout * 1000);
        } catch (IOException e) {
        	
            if ( e.getMessage().equals("Connection refused")) {
                reason = "Port " + port + " closed.";
            };
            if ( e instanceof UnknownHostException ) {
                reason = "Host " + network+"/"+node + " not found.";
            }
            if ( e instanceof SocketTimeoutException ) {
                reason = "Timeout " + network+"/"+node+":"+port;
            }
        } 

        if (s != null) {
            if ( s.isConnected()) {
                //System.out.println("Port " + port + " on " + node + " is reachable!");
                result = true;
            } else {
                reason = "Error: " + reason;
            }
        }
        
        try {s.close();} catch (IOException e) {}
        
        return result;
	}
	
	
}
