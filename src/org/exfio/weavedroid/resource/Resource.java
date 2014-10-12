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
