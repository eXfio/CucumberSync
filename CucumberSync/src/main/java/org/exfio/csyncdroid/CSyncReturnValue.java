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
package org.exfio.csyncdroid;

public class CSyncReturnValue {
	private Exception exception = null;
	private String message      = null;
	
	public void setException(Exception e) {
		this.exception = e;
		this.message   = e.getMessage();
	}

	public Exception getException() {
		return exception;
	}

	public void setMessage(String message) {
		this.exception = null;
		this.message   = message;
	}

	public String getMessage() {
		if ( exception != null ) {
			return String.format("%s - %s", exception.getClass().getName(), message);
		} else {
			return message;
		}
	}
}
