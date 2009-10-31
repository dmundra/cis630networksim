package network.impl;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import network.Message;
import network.OperatingSystem;
import network.Process;
import network.routing.RIPEntry;

public class OperatingSystemImpl_Simple implements OperatingSystem {

	/**this is our routing table, it contains address/entry pairs*/
	private HashMap<Integer, RIPEntry> routingTable = new HashMap<Integer, RIPEntry>();
	
	@Override
	public Logger logger() {
		return Logger.getLogger("OS_Logger");
	}

	@Override
	public Message<?> receive() throws InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Message<?> receive(long timeout, TimeUnit unit)
			throws InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void replaceProcess(Process process) {
		// TODO Auto-generated method stub

	}

	@Override
	public void send(Message<?> message) throws InterruptedException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean send(Message<?> message, long timeout, TimeUnit unit)
			throws InterruptedException {
		// TODO Auto-generated method stub
		return false;
	}

}
