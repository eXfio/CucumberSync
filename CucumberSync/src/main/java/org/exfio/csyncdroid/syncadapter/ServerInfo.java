/*
 * Copyright (C) 2015 Gerry Healy <nickel_chrome@exfio.org> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * This program is derived from DavDroid, Copyright (C) 2014 Richard Hirner, bitfire web engineering
 * DavDroid is distributed under the terms of the GNU Public License v3.0, https://github.com/bitfireAT/davdroid
 */
package org.exfio.csyncdroid.syncadapter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import org.exfio.csyncdroid.Constants.ResourceType;

@RequiredArgsConstructor(suppressConstructorProperties=true)
@Data
public class ServerInfo implements Serializable {
	private static final long serialVersionUID = 238330408340529125L;
	
	private String accountType;
	private String guid;
	
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private String accountParams;
	
	private ResourceInfo addressBook;
	private ResourceInfo calendar;
	private String errorMessage;
	
	public void setAccountParams(Properties prop) throws IOException {
		//Write properties to string
		StringWriter sw = new StringWriter();
		BufferedWriter bf = new BufferedWriter(sw);
		prop.store(bf, "Serialised Weave Account Params");
		accountParams = sw.toString();
	}

	public Properties getAccountParamsAsProperties() throws IOException {
		//Read properties from string
		BufferedReader br = new BufferedReader(new StringReader(accountParams));
		Properties prop = new Properties();
		prop.load(br);
		return prop;
	}
	
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

		//Calendar specific properties
		final String       color;
		final String       timezone;
	}
}
