package com.example.bcast.stream;

import java.io.IOException;
import java.net.InetAddress;

public interface Stream {
	public void start() throws IllegalStateException, IOException;
	public void stop();
//	public void setTimeToLive(int ttl);
	public void setDestinationAddress(InetAddress dest);
	public void setDestinationPorts(int dport);
	public int[] getLocalPorts();
	public int[] getDestinationPorts();
	public int getSSRC();
	public long getBitrate();
	public boolean isStreaming();
	
//	public void sendSessionDetails();
}
