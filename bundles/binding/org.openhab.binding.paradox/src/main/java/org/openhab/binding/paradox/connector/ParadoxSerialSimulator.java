/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.paradox.connector;

import gnu.io.SerialPort;

import org.openhab.binding.paradox.internal.ParadoxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Connector simulator for testing purposes.
 * 
 * @author Pauli Anttila
 * @author Maciej Pilinski
 * @since 1.5.0
 */
public class ParadoxSerialSimulator implements ParadoxConnector {

	public static final Logger logger = LoggerFactory
			.getLogger(ParadoxSerialSimulator.class);

	String serialPortName = null;
	SerialPort serialPort = null;
	
	static final int BAUDRATE = 9600;
	int counter = 0;

	public ParadoxSerialSimulator(String serialPort) {
		serialPortName = serialPort;
	}


	/**
	 * {@inheritDoc}
	 */
	public void connect() throws ParadoxException {
		logger.debug("Paradox APT-PRT1 simulator started");
	}

	/**	
	 * {@inheritDoc}
	 */
	public void disconnect() throws ParadoxException {
		logger.debug("Paradox APT-PRT3 simulator stopped");
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

		try {

			Thread.sleep(5000);

			String testData1 = new String("2014/01/14 17:01  Partition 2    Zone close               Zone 3          ");
			String testData2 = new String("2014/02/14 17:02  Partition 2    Zone close               Zone 10         ");
			String testData3 = new String("2014/03/14 17:03  Partition 2    Stay arming             ");
			String testData4 = new String("2014/04/14 17:04  Partition 2    Partial arming          ");
			String testData5 = new String("2014/05/14 17:05  Partition 2    Arming with master code  User Code 2     ");
			String testData6 = new String("2014/06/14 17:06  Partition 2    Zone open                Zone 10         ");
			String testData7 = new String("2014/07/14 17:07  Partition 2    Zone open                Zone 3          "); 
			String testData8 = new String("2014/08/14 17:08  Partition 2    Burglary alarm           Zone 1          ");
			String testData9 = new String("2014/09/14 17:09  System Area    User code entered        User Code 2     ");
			String testData0 = new String("2014/10/14 17:10  Partition 4    Duress alarm on code     User Code 2     ");
			
			final String[] messages = new String[] { testData1, testData2, testData3, testData4, testData5, testData6, testData7, testData8, testData9, testData0 };

			/*
			 * loop the messages
			 */
			if (++counter >= messages.length)
				counter = 0;

			String resp = messages[counter] + "\\r\\n";
			
			return resp;

		} catch (InterruptedException e) {

			throw new ParadoxException(e);
		}

	}

	
}
