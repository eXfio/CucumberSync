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

import java.util.List;

import org.exfio.weave.WeaveException;
import org.exfio.weave.client.WeaveClient;
import org.exfio.weave.storage.NotFoundException;
import org.exfio.weave.storage.WeaveBasicObject;
import org.exfio.weave.storage.WeaveCollectionInfo;

public abstract class WeaveCollection<T extends Resource> { 

	protected WeaveClient weaveClient;
	protected String collection;
	protected WeaveCollectionInfo colinfo;
	protected List<T> vobjResources;
	protected List<WeaveBasicObject> weaveResources;

	public WeaveCollection(WeaveClient weaveClient, String collection) {
		this.weaveClient = weaveClient;
		this.collection  = collection;
		this.colinfo     = null;
	}
	
	abstract protected String memberContentType();	
	
	/* collection operations */
	
	public Double getModifiedTime() throws WeaveException, NotFoundException {
		if ( colinfo == null ) {
			colinfo = weaveClient.getCollectionInfo(collection);
		}
		return colinfo.getModified();
	}

	public String[] getObjectIds() throws WeaveException, NotFoundException {
		return weaveClient.getCollectionIds(collection, null, null, null, null, null, null, null, null);
	}

	public String[] getObjectIdsModifiedSince(Double modifiedDate) throws WeaveException, NotFoundException {
		return weaveClient.getCollectionIds(collection, null, null, modifiedDate, null, null, null, null, null);
	}

	public abstract T[] multiGet(String[] ids) throws WeaveException, NotFoundException;

	public T[] multiGet(Resource[] resources) throws WeaveException, NotFoundException {
		String[] ids = new String[resources.length];
		for (int i = 0; i < resources.length; i++) {
			ids[i] = resources[i].getUid();
		}
		return multiGet(ids);
	}

	public abstract T get(String id) throws WeaveException, NotFoundException;

	public T get(Resource res) throws WeaveException, NotFoundException {
		return get(res.getUid());
	}

	public abstract void add(Resource res) throws WeaveException;
	
	public abstract void update(Resource res) throws WeaveException, NotFoundException;

	public abstract void delete(String id) throws WeaveException, NotFoundException;
	
	public void delete(Resource res) throws WeaveException, NotFoundException {
		delete(res.getUid());
	}
}
