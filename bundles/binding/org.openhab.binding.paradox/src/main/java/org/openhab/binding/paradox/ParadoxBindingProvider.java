/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.paradox;

import java.util.List;

import org.openhab.binding.paradox.protocol.ParadoxCommandType;
import org.openhab.core.binding.BindingProvider;
import org.openhab.core.items.Item;

/**
 * This interface is implemented by classes that can provide mapping information
 * between openHAB items and Paradox alarm items.
 * 
 * @author Pauli Anttila
 * @author Maciej Piliński
 * @since 1.5.0
 */
public interface ParadoxBindingProvider extends BindingProvider {

	/**
	 * @return the corresponding item name to the given <code>itemName</code>
	 */
	public String getItemName(String itemName);
	
	/**
	 * Returns the Type of the Item identified by {@code itemName}
	 * 
	 * @param itemName
	 *            the name of the item to find the type for
	 * @return the type of the Item identified by {@code itemName}
	 */
	public Class<? extends Item> getItemType(String itemName);

	/**
	 * Returns the device id to execute according to <code>itemName</code>.
	 * 
	 * @param itemName
	 *            the item for which to find a command
	 * 
	 * @return the matching device id or <code>null</code> if no matching device
	 *         id could be found.
	 */
	public String getDeviceId(String itemName);

	/**
	 * Returns the command type to the given <code>itemName</code>.
	 * 
	 * @param itemName
	 *            the item for which to find a unit code.
	 * 
	 * @return the corresponding command type to the given <code>itemName</code>
	 *         .
	 */
	ParadoxCommandType getCommandType(String itemName);

	/**
	 * Returns the refresh interval to use according to <code>itemName</code>.
	 * Is used by In-Binding.
	 * 
	 * @param itemName
	 *            the item for which to find a refresh interval
	 * 
	 * @return the matching refresh interval or <code>null</code> if no matching
	 *         refresh interval could be found.
	 */
	int getRefreshInterval(String itemName);

	/**
	 * Check if <code>itemName</code> is In-binding.
	 * 
	 * @param itemName
	 *            the item for which to find binding direction
	 * 
	 * @return true if binding is In-binding.
	 */
	boolean isInBinding(String itemName);

	/**
	 * Returns all items which are mapped to a Paradox-In-Binding
	 * 
	 * @return item which are mapped to a Paradox-In-Binding
	 */
	List<String> getInBindingItemNames();

	/**
	 * Check if <code>itemName</code> is Out-binding.
	 * 
	 * @param itemName
	 *            the item for which to find binding direction
	 * 
	 * @return true if binding is Out-binding.
	 */
	boolean isOutBinding(String itemName);


	/**
	 * Returns the variable type to the given <code>itemName</code>.
	 * 
	 * @param itemName
	 *            the item for which to find a variable type.
	 * 
	 * @return the corresponding variable type to the given
	 *         <code>itemName</code> .
	 */
	public String getVariable(String itemName);

	/**
	 * Returns the transformation type to the given <code>itemName</code>.
	 * 
	 * @param itemName
	 *            the item for which to find a transformation type
	 * 
	 * @return the matching transformation type or <code>null</code> if no
	 *         matching transformation rule could be found.
	 */
	String getTransformationType(String itemName);

	/**
	 * Returns the transformation function to the given <code>itemName</code>.
	 * 
	 * @param itemName
	 *            the item for which to find a transformation function
	 * 
	 * @return the matching transformation function or <code>null</code> if no
	 *         matching transformation function could be found.
	 */
	String getTransformationFunction(String itemName);


}
