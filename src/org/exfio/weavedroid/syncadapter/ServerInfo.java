/*******************************************************************************
 * Copyright (c) 2014 Gerry Healy <nickel_chrome@mac.com>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Based on DavDroid:
 *     Richard Hirner (bitfire web engineering)
 * 
 * Contributors:
 *     Gerry Healy <nickel_chrome@mac.com> - Initial implementation
 ******************************************************************************/
package org.exfio.weavedroid.syncadapter;

import java.io.Serializable;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import org.exfio.weavedroid.Constants.ResourceType;

@RequiredArgsConstructor(suppressConstructorProperties=true)
@Data
public class ServerInfo implements Serializable {
	private static final long serialVersionUID = 238330408340527325L;
	
	final private String  guid;
	final private String  baseURL;
	final private String  user;
	final private String  password;
	final private String  syncKey;
	final private boolean authPreemptive;
	
	private ResourceInfo addressBook;
	
	private String errorMessage;
		
	@RequiredArgsConstructor(suppressConstructorProperties=true)
	@Data
	public static class ResourceInfo implements Serializable {
		private static final long serialVersionUID = 4962153552085743L;
		
		boolean enabled = false;
		
		final ResourceType type;
		final boolean      readOnly;
		final String       collection;
		final String       title;
		final String       description;
		final String       color;		
	}
}
