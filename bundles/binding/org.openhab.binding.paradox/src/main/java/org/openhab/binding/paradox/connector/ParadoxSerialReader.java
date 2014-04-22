/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.paradox.connector;

import java.io.InputStream;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

import org.apache.commons.io.IOUtils;

import org.openhab.binding.paradox.internal.ParadoxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Connector for serial port communication.
 * 
 * @author Pauli Anttila
 * @author Maciej Piliński
 * @since 1.5.0
 */
public class ParadoxSerialReader implements ParadoxConnector {

	private static final Logger logger = 
		LoggerFactory.getLogger(ParadoxSerialReader.class);

	String serialPortName = null;
//	String paradoxPTRType = null;
	InputStream in 		  = null;
	SerialPort serialPort = null;
	
	static final int MAX_PACKET_SIZE = 100;
	static final int BAUDRATE = 9600;

	public ParadoxSerialReader(String serialPort) {
		serialPortName = serialPort;
	}

	/**
	 * {@inheritDoc}
	 */
	public void connect() throws ParadoxException {

		try {
			logger.debug("Open connection to serial port '{}'", serialPortName);
			CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(serialPortName);

			CommPort commPort = portIdentifier.open(this.getClass().getName(),2000);

			serialPort = (SerialPort) commPort;
			serialPort.setSerialPortParams(BAUDRATE, SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

			// TODO: verify functions of these two lines:
			serialPort.enableReceiveThreshold(1);
			serialPort.disableReceiveTimeout();

			in = serialPort.getInputStream();
			logger.debug("Paradox Serial Port message listener started");

			if (in.markSupported()) {
				in.reset();
			}

		} catch (Exception e) {
			throw new ParadoxException(e);
		}

	}

	/**
	 * {@inheritDoc}
	 */
	public void disconnect() throws ParadoxException {
		if (in != null) {
			logger.debug("Close serial in stream");
			IOUtils.closeQuietly(in);
		}
		if (serialPort != null) {
			logger.debug("Close serial port");
			serialPort.close();
		}
		
		serialPort = null;
		in = null;

		logger.debug("Closed");
	}

	/**
	 * {@inheritDoc}
	 */
	public String sendMessage(String data, int timeout) throws ParadoxException {
			throw new ParadoxException("Not implemented");
	}

	
	/**
	 * {@inheritDoc}
	 */
	public String receiveData() throws ParadoxException {

		if (in == null) {
			connect();
		}

		try {
			
			/*
			 * reading the serial port as long as the complete data with CR+LF arrive
			 */
			String resp = "";
			 
			while (true) {										
				int availableBytes = in.available();
				if (availableBytes > 0) {
					byte[] tmpData = new byte[availableBytes];
					int readBytes = in.read(tmpData, 0, availableBytes);
					resp = resp.concat(new String(tmpData, 0, readBytes));
					
					if (resp.contains("\\r\\n")) {  			// CR+LF = End of Message
						resp = resp.replaceAll("\\r\\n", "");	// remove CR+LF
						return resp;
					}
				} else {
					Thread.sleep(100);
				}

			}
		} catch (Exception e) {
			throw new ParadoxException(
					"Error occured while receiving data", e);
		}


	}

}
