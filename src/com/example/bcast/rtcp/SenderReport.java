package com.example.bcast.rtcp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class SenderReport {
	public static final int MTU = 1500;
	
	private MulticastSocket usock;
	private DatagramPacket upack;
	
	private byte[] buffer = new byte[MTU];
	private int ssrc, port = -1;
	private int octetCount = 0, packetCount = 0;
	
	public SenderReport() throws IOException {
		buffer[0] = (byte) Integer.parseInt("10000000", 2);
		buffer[1] = (byte) 200;
		setLong(28/4-1, 2, 4);
		usock = new MulticastSocket();
		upack = new DatagramPacket(buffer, 1);
	}
	
	public void close() {
		usock.close();
	}
	
	public void send() throws IOException {
		upack.setLength(28);
		usock.send(upack);
	}
	
	public void send(long ntpts, long rtpts) throws IOException {
		long hb = ntpts/1000000000;
		long lb = ( ( ntpts - hb*1000000000 ) * 4294967296L )/1000000000;
		setLong(hb, 8, 12);
		setLong(lb, 12, 16);
		setLong(rtpts, 16, 20);
		upack.setLength(28);
		usock.send(upack);
	}
	
	public void update(int length) {
		packetCount += 1;
		octetCount += length;
		setLong(packetCount, 20, 24);
		setLong(octetCount, 24, 28);
	}
	
	public void setNtpTimestamp(long ts) {
		long hb = ts/1000000000;
		long lb = ( ( ts - hb*1000000000 ) * 4294967296L )/1000000000;
		setLong(hb, 8, 12);
		setLong(lb, 12, 16);
	}
	
	public void setSSRC(int ssrc) {
		this.ssrc = ssrc; 
		setLong(ssrc,4,8);
		packetCount = 0;
		octetCount = 0;
		setLong(packetCount, 20, 24);
		setLong(octetCount, 24, 28);
	}

	public void setDestination(InetAddress dest, int dport) {
		port = dport;
		upack.setPort(dport);
		upack.setAddress(dest);
	}

	public int getPort() {
		return port;
	}

	public int getLocalPort() {
		return usock.getLocalPort();
	}

	public int getSSRC() {
		return ssrc;
	}

	private void setLong(long n, int begin, int end) {
		for (end--; end >= begin; end--) {
			buffer[end] = (byte) (n % 256);
			n >>= 8;
		}
	}
	
}
