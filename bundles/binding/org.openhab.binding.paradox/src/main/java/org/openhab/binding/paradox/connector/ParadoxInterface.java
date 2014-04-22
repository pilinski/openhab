/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.paradox.connector;

import java.util.HashMap;
import java.util.Map;

import org.openhab.binding.paradox.connector.ParadoxInterface.ZoneStatus;
import org.openhab.binding.paradox.internal.ParadoxException;
import org.openhab.binding.paradox.ptr1.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Provide high level interface to Paradox alarm system.
 * 
 * @author Pauli Anttila
 * @author Maciej Piliński
 * @since 1.5.0
 */
public class ParadoxInterface {

	public enum ZoneStatus {
		CLOSE(0x00), OPEN(0x01), ALARM(0x80), OTHER1(0x40), OTHER2(0x20), ERROR(0xFF);

		private int value;
	    private static final Map<Integer, ZoneStatus> typesByValue = new HashMap<Integer, ZoneStatus>();

	    static {
	        for (ZoneStatus type : ZoneStatus.values()) {
	            typesByValue.put(type.value, type);
	        }
	    }
	    
		private ZoneStatus(int value) {
			this.value = value;
		}

	    public static ZoneStatus forValue(int value) {
	        return typesByValue.get(value);
	    }

		public int toInt() {
			return value;
		}
	}

	public enum AreaStatus {
		NORMAL(0x00), ECO(0x01), ERROR(0xFF);

		private int value;
	    private static final Map<Integer, AreaStatus> typesByValue = new HashMap<Integer, AreaStatus>();

	    static {
	        for (AreaStatus type : AreaStatus.values()) {
	            typesByValue.put(type.value, type);
	        }
	    }
	    
		private AreaStatus(int value) {
			this.value = value;
		}

		public static AreaStatus forValue(int value) {
	        return typesByValue.get(value);
	    }

		public int toInt() {
			return value;
		}

	}

	
	
	
	
	private final String OK = "&OK";
	private final String FAIL = "&FAIL";
	private final String COMMUNICATION = "COMM";

	/**
	###### COMMANDS SENT TO THE MODULE FROM US
	*/
	private final String VIRTUAL_INPUT_OPEN = "VO%03d";
	private final String VIRTUAL_INPUT_CLOSED = "VC%03d";

	private final String REQUEST_AREA_STATUS = "RA%03d";
/*
  	private final String REQUEST_AREA_STATUS_REPLY = compile("RA%03d(\w\w\w\w\w\w\w)");
	  REQUEST_AREA_STATUS_BYTE_6 = {"D": "disarmed", "A": "armed", "F": "force armed", "S": "stay armed", "I": "instant armed"}
REQUEST_AREA_STATUS_BYTE_7 = {"M": "zone in memory", "O": "OK"}
REQUEST_AREA_STATUS_BYTE_8 = {"T": "trouble", "O": "OK"}
REQUEST_AREA_STATUS_BYTE_9 = {"N": "not ready", "O": "OK"}
REQUEST_AREA_STATUS_BYTE_10 = {"P": "in programming", "O": "OK"}
REQUEST_AREA_STATUS_BYTE_11 = {"A": "in alarm", "O": "OK"}
REQUEST_AREA_STATUS_BYTE_12 = {"S": "strobe", "O": "OK"}

*/

	public enum ZoneLabel {
		STANDBY("ale"), ON("To"), NIEZNANY("Blad");

		/*
		REQUEST_ZONE_STATUS_BYTE_6 = {("C", "closed"), ("O", "open"), ("T": "tampered"), ("F": "fire loop trouble")};
		REQUEST_ZONE_STATUS_BYTE_7 = {"A": "in alarm", "O": "OK"}
		REQUEST_ZONE_STATUS_BYTE_8 = {"F": "fire alarm", "O": "OK"}
		REQUEST_ZONE_STATUS_BYTE_9 = {"S": "supervision lost", "O": "OK"}
		REQUEST_ZONE_STATUS_BYTE_10 = {"L": "low battery", "O": "OK"}
*/
		private String value;
	    private static final Map<String, ZoneLabel> typesByValue = new HashMap<String, ZoneLabel>();

	    static {
	        for (ZoneLabel type : ZoneLabel.values()) {
	            typesByValue.put(type.value, type);
	        }
	    }
	    
		private ZoneLabel(String value) {
			this.value = value;
		}

	    public static ZoneLabel forValue(String value) {
	        return typesByValue.get(value);
	    }

	    public int toInt() {
			return 0;
		}
	}


	
/*
REQUEST_ZONE_STATUS = "RZ%03d"
REQUEST_ZONE_STATUS_REPLY = re.compile(r"RZ\d\d\d(\w\w\w\w\w)")

# XXX: TODO
REQUEST_ZONE_LABEL = "ZL%03d"
REQUEST_AREA_LABEL = "AL%03d"
REQUEST_USER_LABEL = "UL%03d"

# If incorrect code - we get an &fail back.
AREA_ARM = "AA%03d%s%s"
ARAE_ARM_BYTE6 = {"regular arm": "A", "force arm": "F", "stay arm": "S", "instant arm": "I"}

AREA_DISARM = "AD%03d%s"

EMERGENCY_PANIC = "PE%03d"
MEDICAL_PANIC = "PM%03d"
FIRE_PANIC = "PF%03d"
SMOKE_RESET = "SR%03d"

###### COMMANDS SENT TO US FROM THE MODULE
VIRTUAL_PGM_ON = re.compile(r"PGM(\d\d)ON")
VIRTUAL_PGM_OFF = re.compile(r"PGM(\d\d)OFF")

SYSTEM_EVENT = re.compile(r"G(\d\d\d)N(\d\d\d)A(\d\d\d)")

# NB: Pad to 3
SYSTEM_EVENT_GROUP = {0: "zone ok", 1: "zone open", 2: "zone tampered", 3: "zone fire loop",
                      10: "armed with usercode",
                      14: "disarmed with usercode",
                      17: "disarmed after alarm with usercode",
                      20: "arming canceled with usercode",
                      23: "zone bypassed",
                      24: "zone in alarm",
                      25: "fire alarm",
                      26: "zone alarm restore",
                      27: "fire alarm restore",
                      30: "special alarm",
                      31: "duress alarm by user",
                      32: "zone shutdown",
                      33: "zone tamper",
                      34: "zone tamper restore",
                      36: "trouble event",
                      37: "trouble restore",
                      38: "module trouble",
                      39: "module trouble restore",
                      41: "low battery on zone",
                      42: "zone supervision trouble",
                      43: "low battery on zone restore",
                      44: "zone supervision trouble restore",
                      64: "status 1",
                      65: "status 2",
                      66: "status 3"                      
                      }

SYSTEM_EVENT_NUMBER_SIGNIFICANCE = {0: "zone in question", 1: "zone in question", 2: "zone in question", 3: "zone in question",
                      10: "usercode in question",
                      14: "usercode in question",
                      17: "usercode in question",
                      20: "usercode in question",
                      23: "zone in question",
                      24: "zone in question",
                      25: "zone in question",
                      26: "zone in question",
                      27: "zone in question",
                      30: {0: "emergency panic", 1: "medical panic", 2: "fire panic"},
                      31: "usercode in question",
                      32: "zone in question",
                      33: "zone in question",
                      34: "zone in question",
                      36: {1: "ac failure", 2: "battery failure", 5: "bell absent"},
                      37: {1: "ac failure", 2: "battery failure", 5: "bell absent"},
                      38: "dont care",
                      39: "dont care",
                      41: "zone in question",
                      42: "zone in question",
                      43: "zone in question",
                      44: "zone in question",
                      64: "area in question",
                      65: "area in question",
                      66: "area in question"                      
                      }

SYSTEM_AREA_NUMBER_SIGNIFICANCE = {64: {0: "armed", 1: "force armed", 2: "stay armed", 3: "instant armed", 4: "strobe alarm", 5: "silent alarm", 6: "audible alarm", 7: "fire alarm"},
                                 65: {0: "ready", 1: "exit delay", 2: "entry delay", 3: "system in trouble", 5: "zones bypassed"},
                                 66: {5: "zone low battery"}}

class Event(object):
    group = None
    number = None
    area = None
    
    def __str__(self):
        return "Group '%s' Number '%s' Area '%s'" % (self.group, self.number, self.area)
    
def interprete(line, zone_labels):
    event_to_return = Event()
    
    match = SYSTEM_EVENT.match(line)
    if not match: return None
    
    group, number, area = match.groups()
    group = int(group)
    number = int(number)
    area = int(area)
    
    if group not in SYSTEM_EVENT_GROUP:
        return None
    else:
        event_to_return.group = SYSTEM_EVENT_GROUP[group]
    
    if isinstance(SYSTEM_EVENT_NUMBER_SIGNIFICANCE[group], dict):
        event_to_return.number = SYSTEM_EVENT_NUMBER_SIGNIFICANCE[group][number]
    else:
        if SYSTEM_EVENT_NUMBER_SIGNIFICANCE[group] == "zone in question":
            event_to_return.number = zone_labels.get(number, number)
        else:
            event_to_return.number = number
    
    if group in SYSTEM_AREA_NUMBER_SIGNIFICANCE:
        event_to_return.area = SYSTEM_AREA_NUMBER_SIGNIFICANCE[group][area]
    else:
        event_to_return.area = area
    
    return event_to_return
    	
	
	
	
	
	
	*/
	
// @TODO: CHANGE THE defaultTimeout to lower value (5000) 
	final private int defaultTimeout = 50000;
	

	private static Logger logger = LoggerFactory
			.getLogger(ParadoxInterface.class);

	private ParadoxConnector connection = null;
	private boolean connected = false;
	
	
	public ParadoxInterface(String serialPort) {
		
		connection = (ParadoxConnector) new ParadoxSerialConnector(serialPort);

	}
	
	private String sendQuery(String query, int timeout) throws ParadoxException {
		
		logger.debug("Query: '{}'", query);
		String response = connection.sendMessage(query, timeout);
		response = response.replace("\r:", "");
		logger.debug("Response: '{}'", response);
		
		if (response.length() == 0)
			throw new ParadoxException("No response received");

		if (response.equals("ERR"))
			throw new ParadoxException("Error response received");

		return response;
	}
	
	@SuppressWarnings("unused")
	private String sendQuery(String query) throws ParadoxException {
		return sendQuery(query, defaultTimeout);
	}

	protected void sendCommand(String command, int timeout) throws ParadoxException {
		sendQuery(command, timeout);
	}

	protected void sendCommand(String command) throws ParadoxException {
		sendCommand(command, defaultTimeout);
	}

	protected int queryInt(String query, int timeout, int radix) throws ParadoxException {
		
		String response = sendQuery(query, timeout);
		
		if (response != null && !response.equals("")) {
			
			try {
				String[] pieces = response.split("=");
				String str = pieces[1].trim();

				return Integer.parseInt(str, radix);	
				
			} catch (Exception e) {
				 throw new ParadoxException("Illegal response");
			}
		} else {
			throw new ParadoxException("No response received");	
		}
		
	}

	protected int queryInt(String query, int timeout) throws ParadoxException {
		return queryInt(query, timeout, 10);
	}

	protected int queryInt(String query) throws ParadoxException {
		return queryInt(query, defaultTimeout, 10);
	}
	
	
	protected ZoneStatus queryZone(String query) throws ParadoxException {
		int val = queryHexInt(query);
		ZoneStatus retval = ZoneStatus.forValue(val);
		if (retval != null) {
			return retval;
		} else {
			throw new ParadoxException("Can't convert value" + val + " to ZoneState");
		}
	}

	protected int queryHexInt(String query, int timeout) throws ParadoxException {
		return queryInt(query, timeout, 16);
	}

	protected int queryHexInt(String query) throws ParadoxException {
		return queryInt(query, defaultTimeout, 16);
	}

	public void connect() throws ParadoxException {
		connection.connect();
		 connected = true;
	}

	public void disconnect() throws ParadoxException {
		connection.disconnect();
		 connected = false;
	}
	
	public boolean isConnected() {
		return connected;
	}

	/*
	 * Power
	 */
	public ZoneLabel getZoneLabel() throws ParadoxException {
		String val = queryString("ZL001");
		ZoneLabel retval = ZoneLabel.forValue(val);
		if (retval != null) {
			return retval;
		} else {
			throw new ParadoxException("Can't convert value" + val + " to UserStatus");
		}
	}


	public void setAreaArm(int value) throws ParadoxException {
		sendCommand(String.format("AA001", value));
	}

	private String queryString(String string) {
		// TODO Auto-generated method stub
		return string;
	}

	/*
	 * get Zone Status
	 */
	public ZoneStatus getZoneStatus(int zone) throws ParadoxException {
		return queryZone(String.format("RZ%03X", zone));
	}


	/*
	 * Reset all
	 */
	public void ResetAll() throws ParadoxException {
		sendCommand("INITALL");
	}


	/*
	 * Power
	 */
	public int getPowerState() throws ParadoxException {
		return queryInt("PWR?");
	}

	public void setPowerState(int value) throws ParadoxException {
		sendCommand(String.format("POWER %d", value));
	}

	
	/*
	 * Error
	 */
	public int getError() throws ParadoxException {
		return queryHexInt("ERR?");
	}

	/*
	 * Error
	 */
	public String getErrorString() throws ParadoxException {
		String errString = null;

		int err = queryInt("ERR?");

		switch (err) {
		case 0:
			errString = Messages.EpsonProjectorBinding_NO_ERROR;
			break;
		case 1:
			errString = Messages.EpsonProjectorBinding_ERROR1;
			break;
		case 3:
			errString = Messages.EpsonProjectorBinding_ERROR3;
			break;
		case 4:
			errString = Messages.EpsonProjectorBinding_ERROR4;
			break;
		case 6:
			errString = Messages.EpsonProjectorBinding_ERROR6;
			break;
		case 7:
			errString = Messages.EpsonProjectorBinding_ERROR7;
			break;
		case 8:
			errString = Messages.EpsonProjectorBinding_ERROR8;
			break;
		case 9:
			errString = Messages.EpsonProjectorBinding_ERROR9;
			break;
		case 10:
			errString = Messages.EpsonProjectorBinding_ERROR10;
			break;
		case 11:
			errString = Messages.EpsonProjectorBinding_ERROR11;;
			break;
		case 12:
			errString = Messages.EpsonProjectorBinding_ERROR12;
			break;
		case 13:
			errString = Messages.EpsonProjectorBinding_ERROR13;
			break;
		case 14:
			errString = Messages.EpsonProjectorBinding_ERROR14;
			break;
		case 15:
			errString = Messages.EpsonProjectorBinding_ERROR15;
			break;
		case 16:
			errString = Messages.EpsonProjectorBinding_ERROR16;
			break;
		case 17:
			errString = Messages.EpsonProjectorBinding_ERROR17;
			break;
		case 18:
			errString = Messages.EpsonProjectorBinding_ERROR18;
			break;
		case 19:
			errString = Messages.EpsonProjectorBinding_ERROR19;
			break;
		case 20:
			errString = Messages.EpsonProjectorBinding_ERROR20;
			break;
		case 21:
			errString = Messages.EpsonProjectorBinding_ERROR21;
			break;
		case 22:
			errString = Messages.EpsonProjectorBinding_ERROR22;
			break;
		default:
			errString = String.format(Messages.EpsonProjectorBinding_UNKNOWN_ERROR + " %d", err);
		}

		return errString;
	}


}
