/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.paradox.internal;

/**
 * Exception for Paradox alarm errors.
 * 
 * @author Pauli Anttila
 * @author Maciej Piliński
 * @since 1.5.0
 */
public class ParadoxException extends Exception {

	private static final long serialVersionUID = -6252349495804841519L;

	public ParadoxException() {
		super();
	}

	public ParadoxException(String message) {
		super(message);
	}

	public ParadoxException(String message, Throwable cause) {
		super(message, cause);
	}

	public ParadoxException(Throwable cause) {
		super(cause);
	}

}
