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

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
public abstract class Resource {
	@Getter @Setter protected String id;
	@Getter @Setter protected String uid;
	@Getter @Setter protected String ETag;
	@Getter @Setter protected long   localID;

	/*
	public Resource() {
		this.id      = null;
		this.uid     = null;
		this.ETag    = null;
		this.localID = 0;
	}
	*/

	public Resource(Resource resource) {
		this.localID = resource.localID;
		this.id      = resource.id;
		this.ETag    = resource.ETag;
	}

	public Resource(String id, String ETag) {
		this.id = id;
		this.ETag = ETag;
	}
	
	public Resource(long localID, String id, String ETag) {
		this(id, ETag);
		this.localID = localID;
	}
	
	public void initialize() {
		generateUID();
		id = uid;
	}

	protected void generateUID() {
		uid = UUID.randomUUID().toString();
	}
	
}
