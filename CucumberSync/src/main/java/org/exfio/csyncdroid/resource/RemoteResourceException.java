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

public class RemoteResourceException extends Exception {
	private static final long serialVersionUID = 1593264832655590320L;
	
	public RemoteResourceException(String message) {
		super(message);
	}

	public RemoteResourceException(Throwable throwable) {
		super(throwable);
	}

	public RemoteResourceException(String message, Throwable throwable) {
		super(message, throwable);
	}
}
