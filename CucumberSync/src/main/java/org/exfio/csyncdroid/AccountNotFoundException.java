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

public class AccountNotFoundException extends Exception {
	private static final long serialVersionUID = 8925373748071527486L;
	
	public AccountNotFoundException(String message) {
		super(message);
	}

	public AccountNotFoundException(Throwable throwable) {
		super(throwable);
	}

	public AccountNotFoundException(String message, Throwable throwable) {
		super(message, throwable);
	}
}
