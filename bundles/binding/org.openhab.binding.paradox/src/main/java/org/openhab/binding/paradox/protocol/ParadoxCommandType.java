/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.paradox.protocol;

import java.io.InvalidClassException;

import org.openhab.core.items.Item;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.items.StringItem;
import org.openhab.core.library.items.SwitchItem;

/**
 * Represents all valid command types which could be processed by this
 * binding.
 * 
 * @author Pauli Anttila
 * @author Maciej Piliński
 * @since 1.5.0
 */
public enum ParadoxCommandType {

	REQ_AREA_STAT ("RAS", NumberItem.class),
	REQ_ZONE_STAT ("RZS", NumberItem.class),
//	ZONE1 ("Zone 1", StringItem.class),
//	ZONE2 ("Zone 2", NumberItem.class),
//	ZONE3 ("Zone 3", NumberItem.class),
//	ZONE10 ("Zone 10", StringItem.class),
	ZONE1 ("Zone 1", SwitchItem.class),
	ZONE2 ("Zone 2", SwitchItem.class),
	ZONE3 ("Zone 3", SwitchItem.class),
	ZONE10 ("Zone 10", SwitchItem.class),
	USER1 ("User 1", StringItem.class),
	USER2 ("User 2", StringItem.class),
	POWER_STATE ("PowerState", SwitchItem.class),
	ERR_CODE ("Err_code", NumberItem.class),
	ERR_MSG	("Err_msg", StringItem.class), 
	;

	private final String text;
	private Class<? extends Item> itemClass;

	private ParadoxCommandType(final String text, Class<? extends Item> itemClass) {
		this.text = text;
		this.itemClass = itemClass;
	}

	@Override
	public String toString() {
		return text;
	}

	public Class<? extends Item> getItemClass() {
		return itemClass;
	}

	/**
	 * Procedure to validate command type string.
	 * 
	 * @param commandTypeText
	 *            command string e.g. Zone1, User2
	 * @return true if item is valid.
	 * @throws IllegalArgumentException
	 *             Not valid command type.
	 * @throws InvalidClassException
	 *             Not valid class for command type.
	 */
	public static boolean validateBinding(String commandTypeText,
			Class<? extends Item> itemClass) throws IllegalArgumentException,
			InvalidClassException {

		for (ParadoxCommandType c : ParadoxCommandType.values()) {
			if (c.text.equals(commandTypeText)) {

				if (c.getItemClass().equals(itemClass)) {
					return true;
				} else {
					throw new InvalidClassException(
							"Not valid class for command type");
				}
			}
		}

		throw new IllegalArgumentException("Not valid command type");

	}

	/**
	 * Procedure to convert command type string to command type class.
	 * 
	 * @param commandTypeText
	 *            command string e.g. Zone1, User2
	 * @return corresponding command type.
	 * @throws InvalidClassException
	 *             Not valid class for command type.
	 */
	public static ParadoxCommandType getCommandType(String commandTypeText)
			throws IllegalArgumentException {

		for (ParadoxCommandType c : ParadoxCommandType.values()) {
			if (c.text.equals(commandTypeText)) {
				return c;
			}
		}

		throw new IllegalArgumentException("Not valid command type");
	}

}
