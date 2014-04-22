/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.paradox.connector;

import org.openhab.binding.paradox.internal.ParadoxException;

/**
 * Base class for Paradox alarm communication.
 * 
 * @author Pauli Anttila
 * @author Maciej Pilinski
 * @since 1.5.0
 */
public interface ParadoxConnector {

	/**
	 * Procedure for connecting to PTR1/PTR3 module.
	 * 
	 * @throws ParadoxException
	 */
	void connect() throws ParadoxException;

	/**
	 * Procedure for disconnecting to alarm controller.
	 * 
	 * @throws ParadoxException
	 */
	void disconnect() throws ParadoxException;

	/**
	 * Procedure for send raw data to alarm.
	 * 
	 * @param data
	 *            Message to send.
	 * 
	 * @param timeout
	 *            timeout to wait response in milliseconds.
	 * 
	 * @throws ParadoxException
	 */
	String sendMessage(String data, int timeout) throws ParadoxException;
	
	
	/**
	 * Procedure for receiving data from Paradox system
	 * 
	 * @throws ParadoxException
	 */
	 String receiveData() throws ParadoxException;



}
