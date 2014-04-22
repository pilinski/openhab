/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.paradox.protocol;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openhab.binding.paradox.internal.ParadoxException;


/**
 * Class for present data parser rule.
 * 
 * @author Pauli Anttila
 * @author Maciej Piliński
 * @since 1.5.0
 */
public class ParadoxDataParserRule {
	

	/** RegEx to extract a parse a data type String <code>'(.*?)\((.*)\)'</code> */
	/** tested with http://www.regexr.com/38njp */
	private static final Pattern EXTRACT_USER_TYPE_PATTERN = Pattern.compile("^(\\d{4}\\/\\d{2}\\/\\d{2})\\s+(\\d{2}:\\d{2})\\s+Partition\\s+(\\d+)\\s+(.*)Zone\\s+(\\d+).*$");
	private static final Pattern EXTRACT_ZONE_TYPE_PATTERN = Pattern.compile("^(\\d{4}\\/\\d{2}\\/\\d{2})\\s+(\\d{2}:\\d{2})\\s+Partition\\s+(\\d+)\\s+(.*)User code\\s+(\\d+).*$");

	byte zoneNum = 0;
	ParadoxCommandType dataType = null;
	byte partNum = 0;
	byte userNum = 0;
	
			
	public ParadoxDataParserRule(String rule) throws ParadoxException {
		try {
			
			// Data type USER CODE
			Matcher matcher = EXTRACT_ZONE_TYPE_PATTERN.matcher(rule);
			
			if (matcher.matches()) {
				/* another try */
				matcher.reset();
				
				matcher.find();			
				String partNum    = matcher.group(4);
				String zoneAction = matcher.group(5);
				String zoneNum     = matcher.group(7);
				
				try {
					this.dataType = ParadoxCommandType.valueOf(zoneAction);	
				} catch(IllegalArgumentException e) {
					throw new ParadoxException("Invalid parser rule '" + rule + "', unknown data type");
				}
			}
				
			else	
			{
				throw new ParadoxException("Invalid parser rule '" + rule + "' does not follow the expected PTR1 pattern");
			}
			

			
		} catch (Exception e) {
			throw new ParadoxException("Invalid parser rule", e);
		}
	}
	
	public byte getZoneState() {
		return zoneNum;
	}

	public ParadoxCommandType getDataType() {
		return dataType;
	}

	public int getUserAction() {
		return userNum;
	}

}