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

public class InvalidAccountException extends Exception {
	private static final long serialVersionUID = 7225373793851520256L;
	
	public InvalidAccountException(String message) {
		super(message);
	}

	public InvalidAccountException(Throwable throwable) {
		super(throwable);
	}

	public InvalidAccountException(String message, Throwable throwable) {
		super(message, throwable);
	}
}
