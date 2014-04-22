/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.paradox.internal;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.lang.StringUtils;
import org.openhab.binding.paradox.ParadoxBindingProvider;
import org.openhab.binding.paradox.connector.ParadoxConnector;
import org.openhab.binding.paradox.connector.ParadoxInterface;
import org.openhab.binding.paradox.connector.ParadoxSerialConnector;
import org.openhab.binding.paradox.connector.ParadoxSerialReader;
import org.openhab.binding.paradox.connector.ParadoxSerialSimulator;
import org.openhab.binding.paradox.connector.ParadoxInterface.ZoneStatus;
import org.openhab.binding.paradox.protocol.ParadoxCommandType;
import org.openhab.binding.paradox.protocol.ParadoxDataParser;
import org.openhab.binding.paradox.protocol.ParadoxDataParserRule;

import org.openhab.core.binding.AbstractActiveBinding;
import org.openhab.core.binding.BindingConfig;

import org.openhab.core.items.Item;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.transform.TransformationHelper;
import org.openhab.core.transform.TransformationService;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Binding which communicates with Paradox alarm systems.
 * 
 * 
 * @author Pauli Anttila
 * @author Maciej Piliński
 * @since 1.5.0
 */
public class ParadoxBinding extends
		AbstractActiveBinding<ParadoxBindingProvider> implements ManagedService {

	private static final Logger logger = 
		LoggerFactory.getLogger(ParadoxBinding.class);

	/**
	 * configuration for communication:
	 * PTR - Paradox printer port, older (PTR1) version with messaging only or 
	 * newer (PTR3) with full home automation support
	 * serial port - port to which PTR is attached to (eg. COM1 on Windows, /dev/ttyS0 on Linux, etc.)  
	 */
	public enum PTRtype {
		PTR1, PTR3, SIMULATE;
	}
	private final static PTRtype DEFAULT_PTR = PTRtype.SIMULATE;
	private PTRtype deviceType = DEFAULT_PTR;
	private String serialPort = null;
	/**
	 * granularity - the interval to find new refresh candidates (defaults to 6000 milliseconds)
	 */
	private final static int DEFAULT_GRAN = 6000;
	private int granularity = DEFAULT_GRAN;


	/** Thread to parse data from Paradox devices */
	private ParadoxDataParser dataParser = null;

	/** Thread to handle messages from Paradox devices */
	private MessageListener messageListener = null;

	private Map<String, Item> paradoxMap = new HashMap<String, Item>();
	private Map<String, Long> lastUpdateMap = new HashMap<String, Long>();

	protected Map<String, DeviceConfig> deviceConfigCache = null;

	/**
	 * RegEx to validate a config
	 * <code>'^(.*?)\\.(type|serialPort|refresh)$'</code>
	 */
	private static final Pattern EXTRACT_CONFIG_PATTERN = 
		Pattern.compile("^(.*?)\\.(type|serialPort|refresh)$");

	
	public void activate() {
		logger.debug("Activate");
	}

	public void deactivate() {
		logger.debug("Deactivate");
		messageListener.setInterrupted(true);
//		closeConnection();
	}

/*	private void closeConnection() {
		if (deviceConfigCache != null) {
			// close all connections
			for (Entry<String, DeviceConfig> entry : deviceConfigCache.entrySet()) {
				DeviceConfig deviceCfg = entry.getValue();
				if (deviceCfg != null) {
					ParadoxInterface device = deviceCfg.getConnection();

					if (device != null) {
						try {
							logger.debug("Closing connection to device '{}' ", deviceCfg.deviceId);
							device.disconnect();
						} catch (ParadoxException e) {
							logger.error(
									"Error occured when closing connection to device '{}'",
									deviceCfg.deviceId);
						}
					}
				}
			}

			deviceConfigCache = null;
		}
	}
*/	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected long getRefreshInterval() {
		return granularity;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getName() {
		return "Paradox alarm Refresh Service";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void execute() {

		/*
		 * 
		 */
		for (ParadoxBindingProvider provider : providers) {
			/*
			 * 
			 */
			for (String itemName : provider.getInBindingItemNames()) {

				int refreshInterval = provider.getRefreshInterval(itemName);

				Long lastUpdateTimeStamp = lastUpdateMap.get(itemName);
				if (lastUpdateTimeStamp == null) {
					lastUpdateTimeStamp = 0L;
				}

				long age = System.currentTimeMillis() - lastUpdateTimeStamp;
				boolean needsUpdate = age >= refreshInterval;

				if (needsUpdate) {
					String deviceId = provider.getDeviceId(itemName);
					
					logger.debug("item '{}' is about to be refreshed now", itemName);

					ParadoxCommandType commmandType = provider.getCommandType(itemName);
					Class<? extends Item> itemType = provider.getItemType(itemName);

					State state = queryDataFromDevice(deviceId, commmandType, itemType);

					if (state != null) {
						eventPublisher.postUpdate(itemName, state);
					} else {
						logger.error("No response received from command '{}'", commmandType);
					}

					lastUpdateMap.put(itemName, System.currentTimeMillis());
				}
			}
		}
	}

	private State queryDataFromDevice(String deviceId,
		ParadoxCommandType commmandType, Class<? extends Item> itemType) {

		DeviceConfig device = deviceConfigCache.get(deviceId);

		if (device == null) {
			logger.error("Could not find device '{}'", deviceId);
			return null;
		}

		ParadoxInterface remoteController = device.getConnection();

		if (remoteController == null) {
			logger.error("Could not find device '{}'", deviceId);
			return null;
		}

		try {
			if (remoteController.isConnected() == false)
				remoteController.connect();

			switch (commmandType) {
			case ZONE1:
				ZoneStatus zoneState1 = remoteController.getZoneStatus(1);
				return new DecimalType(zoneState1.toInt());
			case ZONE2:
				ZoneStatus zoneState2 = remoteController.getZoneStatus(2);
				return new DecimalType(zoneState2.toInt());
			case ZONE3:
				ZoneStatus zoneState3 = remoteController.getZoneStatus(3);
				return new DecimalType(zoneState3.toInt());
			case ZONE10:
				ZoneStatus zoneState10 = remoteController.getZoneStatus(10);
				return new DecimalType(zoneState10.toInt());
/*			case USER1:
				ZoneLabel userState1 = remoteController.getUserStatus();
				return new StringType(userState1.toString());
			case USER2:
				ZoneLabel userState2 = remoteController.getUserStatus();
				return new StringType(userState2.toString());
*/
			case ERR_CODE:
				int err = remoteController.getError();
				logger.warn("Get '{}' not implemented!",
						commmandType.toString());
				return new DecimalType(err);
			case ERR_MSG:
				String errString = remoteController.getErrorString();
				logger.warn("Get '{}' not implemented!",
						commmandType.toString());
				return new StringType(errString);
			case POWER_STATE:
				int pwr = remoteController.getPowerState();
				logger.warn("Get '{}' not implemented!",
						commmandType.toString());
				return new DecimalType(pwr);
			default:
				logger.warn("Unknown '{}' command!", commmandType);
				return null;
			}

		} catch (ParadoxException e) {
			logger.warn("Couldn't execute command '{}', {}",
					commmandType.toString(), e);

		} catch (Exception e) {
			logger.warn("Couldn't create state of type '{}'", itemType.toString());
			return null;
		}

		return null;
	}


	/**
	 * @{inheritDoc
	 */
	/*
	@Override
	public void internalReceiveCommand(String itemName, Command command) {
		ParadoxBindingProvider provider = findFirstMatchingBindingProvider( itemName, command);

		if (provider == null) {
			logger.warn(
					"doesn't find matching binding provider [itemName={}, command={}]",
					itemName, command);
			return;
		}

		if (provider.isOutBinding(itemName)) {
			ParadoxCommandType commmandType = provider.getCommandType(itemName);
			String deviceId = provider.getDeviceId(itemName);
			if (commmandType != null) {
				sendDataToDevice(deviceId, commmandType, command);
			}
		} else {
			logger.warn("itemName={} is not out binding", itemName);
		}
	}

	private void sendDataToDevice(String deviceId, ParadoxCommandType commmandType, Command command) {
		DeviceConfig device = deviceConfigCache.get(deviceId);

		if (device == null) {
			logger.error("Could not find device '{}'", deviceId);
			return;
		}

		ParadoxInterface remoteController = device.getConnection();

		if (remoteController == null) {
			logger.error("Could not find device '{}'", deviceId);
			return;
		}

		try {

			if (remoteController.isConnected() == false)
				remoteController.connect();

			switch (commmandType) {
	/*		case ZONE3:
				remoteController.setZoneStatus(((DecimalType) command).intValue());
				break;
			case ZONE10:
				remoteController.setZoneStatus(10);
				break;
			case USER1:
				remoteController.setUserStatus(((DecimalType) command).intValue());
				break;
			case USER2:
				remoteController.setUserStatus(((DecimalType) command).intValue());
				break;
	*		case ERR_CODE:
				logger.error("'{}' is read only parameter", commmandType);
				break;
			case ERR_MSG:
				logger.error("'{}' is read only parameter", commmandType);
				break;
			case POWER_STATE:
				remoteController.setPowerState((command == OnOffType.ON ? 0 : 1));
				logger.error("'{}' is read only parameter", commmandType);
				break;
			default:
				logger.warn("Unknown '{}' command!", commmandType);
				break;
			}

		} catch (ParadoxException e) {
			logger.error("Couldn't execute command '{}', {}", commmandType, e);

		}
	}

/**
	 * Find the first matching {@link ExecBindingProvider} according to
	 * <code>itemName</code> and <code>command</code>. If no direct match is
	 * found, a second match is issued with wilcard-command '*'.
	 * 
	 * @param itemName
	 * @param command
	 * 
	 * @return the matching binding provider or <code>null</code> if no binding
	 *         provider could be found
	 *
	private ParadoxBindingProvider findFirstMatchingBindingProvider(
			String itemName, Command command) {

		ParadoxBindingProvider firstMatchingProvider = null;

		for (ParadoxBindingProvider provider : this.providers) {
			ParadoxCommandType commmandType = provider.getCommandType(itemName);

			if (commmandType != null) {
				firstMatchingProvider = provider;
				break;
			}
		}

		return firstMatchingProvider;
	}
*/
	/**
	 * @{inheritDoc
	 */
	@Override
	public void updated(Dictionary<String, ?> config) throws ConfigurationException {

		logger.debug("Configuration updated, config {}", config != null ? true : false);
		
		if (config != null) {
/*			if (deviceConfigCache == null) {
				deviceConfigCache = new HashMap<String, DeviceConfig>();
			}
*/
			HashMap<String, ParadoxDataParserRule> parsingRules = new HashMap<String, ParadoxDataParserRule>();
			
			String granularityString = (String) config.get("refresh");
			if (StringUtils.isNotBlank(granularityString)) {
				granularity = Integer.parseInt(granularityString);
			}

			Enumeration<String> keys = config.keys();

			while (keys.hasMoreElements()) {
				String key = (String) keys.nextElement();

				// the config-key enumeration contains additional keys that we
				// don't want to process here ...
				if ("service.pid".equals(key)) {
					continue;
				}

// better - more Paradox devices...
/*				Matcher matcher = EXTRACT_CONFIG_PATTERN.matcher(key);

				if (!matcher.matches()) {
					logger.warn("given config key '"
							+ key
							+ "' does not follow the expected pattern '<deviceId>.<type|serialPort|refresh>'");
					continue;
				}

				matcher.reset();
				matcher.find();

				String deviceId = matcher.group(1);

				DeviceConfig deviceConfig = deviceConfigCache.get(deviceId);

				if (deviceConfig == null) {
					logger.debug("Added new device {}", deviceId);
					deviceConfig = new DeviceConfig(deviceId);
					deviceConfigCache.put(deviceId, deviceConfig);
				}

				String configKey = matcher.group(2);
				String value = (String) config.get(key);

				/**
				 * parsing of valid config:
				 * 
############################## Paradox Alarm Binding ##################################
#
# Serial port of the first Paradox alarm system to control: 
# paradox:<devId1>.serialPort=
#
# Type of the Paradox printer port, to which we are connecting via serial cable
# valid options are: PTR1, PTR3, simulate (required)
# paradox:<devId1>.type=
#
# Refresh rate in miliseconds of the in-directional items (optional, defaults to 6000)
# valid for PTR3 only
# paradox:<devId1>.refresh=

# example of valid settings:
paradox:homealarm.serialPort=COM10
paradox:homealarm.type=simulate
paradox:homealarm.refresh=6000
 
				 *
				if ("serialPort".equals(configKey)) {
					deviceConfig.serialPort = value;
					
				} else if ("type".equals(configKey)) {
					if ("PTR1".equals(value))     deviceConfig.type = PTRtype.PTR1;
					else if("PTR3".equals(value)) deviceConfig.type = PTRtype.PTR3;
					else deviceConfig.type = PTRtype.SIMULATE;
					
				} else if ("simulate".equals(configKey)) {
					if (StringUtils.isNotBlank(value)) {
						deviceConfig.type = PTRtype.SIMULATE;
					}
				} else if ("refresh".equals(configKey)) {
					deviceConfig.granularity = Integer.parseInt(value);
				}
				  else
					throw new ConfigurationException(configKey,
						"the given configKey '" + configKey + "' is unknown");
				}
			
				
			setProperlyConfigured(true);

*/
/**
				############################## Paradox Alarm Binding ##################################
				#
				# Serial port of the Paradox alarm system to control (required): 
				# paradox.serialPort=
				#
				# Type of the Paradox printer port, to which we are connecting via serial cable
				# valid options are: PTR1, PTR3 (required)  // use 'simulate' for tests only
				# paradox.type=
				#
				# Refresh rate in milliseconds of the in-directional items (optional, defaults to 6000)
				# valid for PTR3 only
				# paradox.refresh=

				# example of valid settings:
				paradox.serialPort=COM10
				paradox.type=PTR1
				paradox.refresh=6000
*/
				String value = (String) config.get(key);

			if ("serialPort".equals(key)) {
				serialPort = value;
				
			} else if ("type".equals(key)) {
				if ("PTR1".equals(value))     deviceType = PTRtype.PTR1;
				else if("PTR3".equals(value)) deviceType = PTRtype.PTR3;
				else deviceType = PTRtype.SIMULATE;
				
			} else if ("simulate".equals(key)) {
				if (StringUtils.isNotBlank(value)) {
					deviceType = PTRtype.SIMULATE;
				}
			} else if ("refresh".equals(key)) {
				granularity = Integer.parseInt(value);
			}
			  else
				throw new ConfigurationException(key,
					"the given configKey '" + key + "' is unknown");
			}
		
			if (parsingRules != null) {

				dataParser = new ParadoxDataParser(parsingRules);
			}
			
			messageListener = new MessageListener();
			messageListener.start();
		}
	}

	/**
	 * Internal data structure which carries the connection details of Paradox device
	 */
	static class DeviceConfig {

		String deviceId;
		String serialPort = null;
		PTRtype type = null;
		int granularity = DEFAULT_GRAN;

		ParadoxInterface device = null;

		public DeviceConfig(String deviceId) {
			this.deviceId = deviceId;
		}

		@Override
		public String toString() {
			return "Device [id=" + deviceId + ", type=" + type + "]";
		}

		ParadoxInterface getConnection() {
			if (device == null) {
				if (serialPort != null) {
						device = new ParadoxInterface(serialPort);
				}	
			}
			return device;
		}

	}

	

	/**
	 * The MessageListener runs as a separate thread.
	 * 
	 * Thread listening message from Paradox devices and send
	 * updates to openHAB bus.
	 * 
	 */
	private class MessageListener extends Thread {

		private boolean interrupted = false;

		MessageListener() {
		}

		public void setInterrupted(boolean interrupted) {
			this.interrupted = interrupted;
			messageListener.interrupt();
		}

		@Override
		public void run() {

			logger.debug("Paradox message listener started");

			// how to get deviceConfig?????
//			ParadoxInterface deviceConfig = deviceConfigCache.get("paradox");
			ParadoxConnector connector = null;

			if (serialPort != null && deviceType != null) {
			
			switch (deviceType) { 
				case SIMULATE: 	
					connector = new ParadoxSerialSimulator(serialPort);
					break;
				case PTR1:		
					connector = new ParadoxSerialReader(serialPort);
					break;
				case PTR3:		
					connector = new ParadoxSerialConnector(serialPort);
					break;
			}
							
			try {
				connector.connect();
			} catch (ParadoxException e) {
				logger.error(
						"Error occured when connecting to Paradox device",
						e);
			}

			// as long as no interrupt is requested, continue running
			while (!interrupted) {

				try {
					// Wait a packet (blocking)
					String data = connector.receiveData();
//					byte[] bdata = data.getBytes();

					logger.debug("Received data (len={}): {}", data.length(), data);
					logger.trace("Received data (len={}): {}", data.length(), data);

					HashMap<String, Number> vals = dataParser.parseData(data);

					for (ParadoxBindingProvider provider : providers) {
						for (String itemName : provider.getItemNames()) {

							for (Entry<String, Number> entry : vals.entrySet()) {
								String key = entry.getKey();
								Number value = entry.getValue();

								if (key != null && value != null) {

									boolean found = false;

									org.openhab.core.types.State state = null;

									String variable = provider.getVariable(itemName);

									if (variable.equals(key)) {
										state = new DecimalType(value.doubleValue());
										found = true;

									} else if (variable.contains(key) && variable.matches(".*[+-/*^%].*")) {
										logger.debug("Eval key={}, variable={}", key, variable);

/*										String tmp = replaceVariables(vals, variable);
										
										try {
											double result = new DoubleEvaluator().evaluate(tmp);
											logger.debug("Eval '{}={}={}'", variable, tmp, result);
											state = new DecimalType(result);
											found = true;

										} catch (Exception e) {
											logger.error(
													"Error occured during data evaluation",
													e);
										}
*/									}

									if (found) {

										state = transformData(
												provider.getTransformationType(itemName),
												provider.getTransformationFunction(itemName),
												state);

										if (state != null) {
											eventPublisher.postUpdate(itemName, state);
											break;
										}

									}

								}
							}
						}

					}

				} catch (ParadoxException e) {

					logger.error(
							"Error occured when received data from Paradox device",
							e);
				}
			}

			try {
				connector.disconnect();
			} catch (ParadoxException e) {
				logger.error(
						"Error occured when disconnecting form Paradox device",
						e);
			}

		}
	}
		
				
		public void end() {

			logger.debug("Paradox message listener stopped");

		}
	}

	private String replaceVariables(HashMap<String, Number> vals,
			String variable) {
		for (Entry<String, Number> entry : vals.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();

			variable = variable.replace(key, String.valueOf(value));

		}

		return variable;
	}

	/**
	 * Transform received data by Transformation service.
	 * 
	 */
	protected org.openhab.core.types.State transformData(
			String transformationType, String transformationFunction,
			org.openhab.core.types.State data) {
		
		if (transformationType != null && transformationFunction != null) {
			String transformedResponse = null;

			/* try { */
				TransformationService transformationService = TransformationHelper
						.getTransformationService(
								ParadoxActivator.getContext(),
								transformationType);
				if (transformationService != null) {
//					transformedResponse = transformationService.transform(
//							transformationFunction, String.valueOf(data));
				} else {
					logger.warn(
							"couldn't transform response because transformationService of type '{}' is unavailable",
							transformationType);
				}
			
			
			/* } catch (ParadoxException e) {
				logger.error(
						"transformation throws exception [transformation type="
								+ transformationType
								+ ", transformation function="
								+ transformationFunction + ", response=" + data
								+ "]", e);
			} */
			
			logger.debug("transformed response is '{}'", transformedResponse);

			if (transformedResponse != null) {
				return new DecimalType(transformedResponse);
			}
		}

		return data;
	}
	
	public void addParadoxConfig(Item item, String elementName) {
			paradoxMap.put(elementName, item);

	}



}
