/*******************************************************************************
 * Copyright (c) 2014 Richard Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Richard Hirner (bitfire web engineering) - initial API and implementation
 ******************************************************************************/
package org.exfio.weavedroid.resource;

public class LocalStorageException extends Exception {
	private static final long serialVersionUID = -7787658815291629529L;
	
	private static final String detailMessage = "Couldn't access local content provider";
	

	public LocalStorageException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

	public LocalStorageException(String detailMessage) {
		super(detailMessage);
	}
	
	public LocalStorageException(Throwable throwable) {
		super(detailMessage, throwable);
	}

	public LocalStorageException() {
		super(detailMessage);
	}
}
