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

import org.exfio.weave.WeaveException;
import org.exfio.weave.client.WeaveClient;
import org.exfio.weave.storage.NotFoundException;
import org.exfio.weave.storage.WeaveBasicObject;

import java.util.LinkedList;
import java.util.List;

public class WeaveCalendar extends WeaveCollection<Event> {

	public WeaveCalendar(WeaveClient weaveClient, String collection) {
		super(weaveClient, collection);
	}
	protected String memberContentType() {
		return "application/calendar+json";
	}
	
	
	/* internal member operations */
	public Event[] multiGet(String[] ids) throws WeaveException, NotFoundException {
		List<Event> colEvent = new LinkedList<Event>();

		WeaveBasicObject[] colWbo = this.weaveClient.getCollection(collection, ids, null, null, null, null, null, null, null, null); 

		for (int i = 0; i < colWbo.length; i++) {
			Event con = Event.fromWeaveBasicObject(colWbo[i]);
			colEvent.add(con);
		}
		
		return colEvent.toArray(new Event[0]);
	}

	public Event get(String id) throws WeaveException, NotFoundException {
		WeaveBasicObject wbo = this.weaveClient.get(this.collection, id);
		return Event.fromWeaveBasicObject(wbo);
	}

	public void add(Resource res) throws WeaveException {
		add((Event)res);
	}

	public void add(Event res) throws WeaveException {
		WeaveBasicObject wbo = Event.toWeaveBasicObject(res);
		this.weaveClient.put(collection, wbo.getId(), wbo);
	}
	
	public void update(Resource res) throws WeaveException {
		update((Event)res);
	}

	public void update(Event res) throws WeaveException {
		WeaveBasicObject wbo = Event.toWeaveBasicObject(res);

		//TODO confirm resource exists
		
		this.weaveClient.put(collection, wbo.getId(), wbo);
	}
	
	public void delete(String id) throws WeaveException, NotFoundException {
		this.weaveClient.delete(collection, id);
	}
}
