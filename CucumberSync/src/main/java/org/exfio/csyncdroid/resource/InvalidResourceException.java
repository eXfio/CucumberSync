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
package org.exfio.csyncdroid.resource;

public class InvalidResourceException extends Exception {
	private static final long serialVersionUID = 1593585432655578220L;
	
	public InvalidResourceException(String message) {
		super(message);
	}

	public InvalidResourceException(Throwable throwable) {
		super(throwable);
	}
}
