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
package org.exfio.weavedroid.resource;

public class RecordNotFoundException extends LocalStorageException {
	private static final long serialVersionUID = 4961024282198632578L;
	
	private static final String detailMessage = "Record not found in local content provider"; 
	
	
	RecordNotFoundException(Throwable ex) {
		super(detailMessage, ex);
	}
	
	RecordNotFoundException() {
		super(detailMessage);
	}

}
