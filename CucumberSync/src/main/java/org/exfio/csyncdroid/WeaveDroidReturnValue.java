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
package org.exfio.csyncdroid;

public class WeaveDroidReturnValue {
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
