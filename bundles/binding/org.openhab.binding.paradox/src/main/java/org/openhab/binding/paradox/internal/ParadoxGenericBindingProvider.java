/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.paradox.internal;

import java.io.InvalidClassException;
import java.util.ArrayList;
import java.util.List;

import org.openhab.binding.paradox.ParadoxBindingProvider;
import org.openhab.binding.paradox.protocol.ParadoxCommandType;
import org.openhab.core.binding.BindingConfig;
import org.openhab.core.items.Item;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.items.StringItem;
import org.openhab.core.library.items.ContactItem;
import org.openhab.model.item.binding.AbstractGenericBindingProvider;
import org.openhab.model.item.binding.BindingConfigParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * This class can parse information from the generic binding format and provides
 * <b>Paradox alarm device</b> binding information from it.
 * <p>
 * The connection to the Paradox system is via RS-232 port on so called 'printer port': either APR-PTR1 or APR-PTR3. 
 * Currently the software is verified with the PTR1 only (older, read only, cheaper).
 * <i>If the Paradox would be so kind to rent the PTR3 board for tests, I would be more than happy to develop this
 * version too :) </i>  
 * <p>
 * For the <b>APR-PTR1</b> device there are only two valid binding configuration strings:
 * <ul>
 * <li><code>Numeric Zone1  { paradox="zone1" }</code></li>
 * <li><code>String	 door   { paradox="zone5" }</code></li>
 * <li><code>Contact window { paradox="zone8" }</code></li>
 * <li><code>String  User2  { paradox="user2" }</code></li>
 * <li><code>Switch  AlarmReset { paradox="reset" }</code></li>
 * </ul>
 * 
 * In the above example 'zone1' is the zone in the alarm system. depending on the Paradox system version there is 
 * 48 (EVO48) up to 128 (EVO12) zones named as 'zone1' up to 'zone128'.
 * The ItemType determines the scope of the returned values:
 * <h3><u>Contact</u><h3>
 * <ul>
 * <li><code>OFF = closed</code></li>
 * <li><code>ON  = opened</code></li>
 * </ul>
 * <h3><u>Number</u><h3>
 * <ul>
 * <li><code>00 = closed</code></li>
 * <li><code>01 = opened</code></li>
 * <li><code>80 = burglary alarm</code></li>
 * <li><code>.. = ......</code></li>
 *  </ul>
 *  <h3><u>String</u><h3>
 * <ul>
 * <li><code>ABCDE</code></li>
 * where:
 * A - 
 * B - 
 * C - 
 * D - 
 * E - 
 * </ul>
 *  If there is the burglary alarm on the line(s), it remains until we do not clear it via <code>sendCommand(AlarmReset, ON)</code> or <code>postUpdate(AlarmReset, ON)</code>
 *  <p> 
 * 'user2' defines the actions 
 * 
 * <p>
 * For the <b>APR-PTR3</b> device there are valid following binding configuration strings:
 * 
 * <ul>
 * <li><code>paradox="zone1[:REFRESH]"</code></li>
 * <li><code>paradox="zone2"</code></li>
 * <li><code>paradox="zone3"</code></li>
 * <li><code>paradox="user2"</code></li>
 * </ul>
 * where: 'zone' is the zone name in the range 'zone1' up to 'zone128'. 'user' is the user ('user1' to 'user8')
 * Optional 'REFRESH' is the refresh time in miliseconds.
 * <p>
 * 
 * @author Pauli Anttila
 * @author Maciej Piliński
 * @since 1.5.0
 */

public class ParadoxGenericBindingProvider extends
		AbstractGenericBindingProvider implements ParadoxBindingProvider {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory
			.getLogger(ParadoxGenericBindingProvider.class);

	/**
	 * {@inheritDoc}
	 */
	public String getBindingType() {
		return "paradox";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void validateItemType(Item item, String bindingConfig)
			throws BindingConfigParseException {
		if (!(item instanceof NumberItem || item instanceof StringItem || item instanceof ContactItem)) {
			throw new BindingConfigParseException(
					"item '"
							+ item.getName()
							+ "' is of type '"
							+ item.getClass().getSimpleName()
							+ "', only ContactItem, NumberItem and StringItem are allowed - please check your *.items configuration");
		}

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void processBindingConfiguration(String context, Item item, String bindingConfig)
			throws BindingConfigParseException {
		super.processBindingConfiguration(context, item, bindingConfig);

		ParadoxBindingConfig config = new ParadoxBindingConfig();

		String[] configParts = bindingConfig.trim().split(":");

		config.inBinding = false;
		config.outBinding = false;
//		config.PTR3 = false; 
		config.itemType = item.getClass();

/*		if (bindingConfig.startsWith("<")) {

			if (configParts.length != 2 && configParts.length != 3) {
				throw new BindingConfigParseException(
						"Paradox alarm <in binding must contain 2 or 3 parts separated by ':'");
			}

			config.inBinding = true;
			config.outBinding = false;
			config.deviceID = configParts[0].trim().replace("<", "");
			
			if (configParts.length == 3) {
				parseRefreshPeriod(configParts[2], config);				
			}
		} 
		else 
		if (bindingConfig.startsWith(">")) {
			
			if ( configParts.length != 2) {
					throw new BindingConfigParseException(
						"Paradox alarm out> binding must contain 2 parts separated by ':'");
			}
			
			config.inBinding = false;
			config.outBinding = true;
			config.deviceID = configParts[0].trim().replace(">", "");
	
		} 
		else
		{

			if (configParts.length != 2 && configParts.length != 3) {
				throw new BindingConfigParseException(
						"Paradox alarm bi-directional binding must contain 2 or 3 parts separated by ':'");
			}

			config.inBinding = true;
			config.outBinding = true;
			config.deviceID = configParts[0].trim();
			if (configParts.length == 3) {
				parseRefreshPeriod(configParts[2], config);				
			}
		}
	
*/
		if (configParts.length != 1 && configParts.length != 2) {
			throw new BindingConfigParseException(
					"Paradox alarm binding must contain max 2 parts separated by ':'");
		}

		config.inBinding = true;
		config.outBinding = true;
		config.deviceID = configParts[0].trim();
		
		if (configParts.length == 2) {
			parseRefreshPeriod(configParts[1], config);				
		}
		config.commandType = getCommandTypeFromString(configParts[1].trim().toLowerCase(), item);

		addBindingConfig(item, config);
//		addParadoxConfig(item, config.deviceID);
		
	}


	
	private void parseRefreshPeriod(String refreshPeriodString, ParadoxBindingConfig config) throws BindingConfigParseException {
		
			config.refreshInterval = Integer.valueOf(refreshPeriodString);

	}

	
	
	private ParadoxCommandType getCommandTypeFromString(String commandTypeString, Item item) throws BindingConfigParseException {
		
		ParadoxCommandType commandType = null;
		
		try {
			ParadoxCommandType.validateBinding(commandTypeString, item.getClass());

			commandType = ParadoxCommandType.getCommandType(commandTypeString);

		} catch (IllegalArgumentException e) {
			throw new BindingConfigParseException("Invalid command type '"
					+ commandTypeString + "'!");

		} catch (InvalidClassException e) {
			throw new BindingConfigParseException(
					"Invalid item type for command type '" + commandTypeString
							+ "'!");

		}
		
		return commandType;
	}
	
	/**
	 * @{inheritDoc
	 */
	@Override
	public Class<? extends Item> getItemType(String itemName) {
		ParadoxBindingConfig config = (ParadoxBindingConfig) bindingConfigs
				.get(itemName);
		return config != null ? config.itemType : null;
	}

	@Override
	public String getDeviceId(String itemName) {
		ParadoxBindingConfig config = (ParadoxBindingConfig) bindingConfigs
				.get(itemName);
		return config != null ? config.deviceID : null;
	}

	@Override
	public ParadoxCommandType getCommandType(String itemName) {
		ParadoxBindingConfig config = (ParadoxBindingConfig) bindingConfigs
				.get(itemName);
		return config != null ? config.commandType : null;
	}

	/**
	 * {@inheritDoc}
	 */
	public int getRefreshInterval(String itemName) {
		ParadoxBindingConfig config = (ParadoxBindingConfig) bindingConfigs
				.get(itemName);
		return config != null && config.inBinding == true ? config.refreshInterval : 0;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isOutBinding(String itemName) {
		ParadoxBindingConfig config = (ParadoxBindingConfig) bindingConfigs
				.get(itemName);
		return config != null ? config.outBinding: null;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isInBinding(String itemName) {
		ParadoxBindingConfig config = (ParadoxBindingConfig) bindingConfigs
				.get(itemName);
		return config != null ? config.inBinding: null;
	}

	/**
	 * {@inheritDoc}
	 */
/*	public boolean PTR3(String itemName) {
		ParadoxBindingConfig config = (ParadoxBindingConfig) bindingConfigs
				.get(itemName);
		return config != null && config.PTR3 == true ? config.PTR3: null;
	}
*/
/**
	 * {@inheritDoc}
	 */
	public List<String> getInBindingItemNames() {
		List<String> inBindings = new ArrayList<String>();
		for (String itemName : bindingConfigs.keySet()) {
			ParadoxBindingConfig config = (ParadoxBindingConfig) bindingConfigs
					.get(itemName);
			if (config.inBinding == true) {
				inBindings.add(itemName);
			}
		}
		return inBindings;
	}

	/**
	 * This is an internal data structure to store information from the binding
	 * config strings and use it to answer the requests to the Paradox alarm
	 * binding provider.
	 */
	static class ParadoxBindingConfig implements BindingConfig {

		public Class<? extends Item> itemType = null;
		public String deviceID = null;
		public ParadoxCommandType commandType = null;

		public int refreshInterval = 0;
		public boolean inBinding = true;
		public boolean outBinding = false;
//		public boolean PTR3 = true;
		
		@Override
		public String toString() {
			return "ExecBindingConfigElement ["
					+ ", itemType=" + itemType
					+ ", deviceID=" + deviceID
					+ ", commandType=" + commandType
					+ ", inBinding=" + inBinding + "]";
		}

	}

	@Override
	public String getItemName(String itemName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getVariable(String itemName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getTransformationType(String itemName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getTransformationFunction(String itemName) {
		// TODO Auto-generated method stub
		return null;
	}

}	