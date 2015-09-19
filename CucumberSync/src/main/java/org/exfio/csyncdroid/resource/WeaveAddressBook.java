/*
 * Copyright (C) 2015 Gerry Healy <nickel_chrome@exfio.org> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * This program is derived from DavDroid, Copyright (C) 2014 Richard Hirner, bitfire web engineering
 * DavDroid is distributed under the terms of the GNU Public License v3.0, https://github.com/bitfireAT/davdroid
 */
package org.exfio.csyncdroid.resource;

import java.util.List;
import java.util.LinkedList;

import org.exfio.weave.WeaveException;
import org.exfio.weave.client.WeaveClient;
import org.exfio.weave.storage.NotFoundException;
import org.exfio.weave.storage.WeaveBasicObject;

public class WeaveAddressBook extends WeaveCollection<Contact> {
	
	public WeaveAddressBook(WeaveClient weaveClient, String collection) {
		super(weaveClient, collection);
	}
	protected String memberContentType() {
		return "application/vcard+json";
	}
	
	
	/* internal member operations */
	public Contact[] multiGet(String[] ids) throws WeaveException, NotFoundException {		
		List<Contact> colContact = new LinkedList<Contact>();

		WeaveBasicObject[] colWbo = this.weaveClient.getCollection(collection, ids, null, null, null, null, null, null, null, null); 

		for (int i = 0; i < colWbo.length; i++) {
			Contact con = Contact.fromWeaveBasicObject(colWbo[i]);
			colContact.add(con);
		}
		
		return colContact.toArray(new Contact[0]);
	}

	public Contact get(String id) throws WeaveException, NotFoundException {
		WeaveBasicObject wbo = this.weaveClient.get(this.collection, id);
		return Contact.fromWeaveBasicObject(wbo);
	}

	public void add(Resource res) throws WeaveException {
		add((Contact)res);
	}

	public void add(Contact res) throws WeaveException {
		WeaveBasicObject wbo = Contact.toWeaveBasicObject(res);
		this.weaveClient.put(collection, wbo.getId(), wbo);
	}
	
	public void update(Resource res) throws WeaveException {
		update((Contact)res);	
	}

	public void update(Contact res) throws WeaveException {
		WeaveBasicObject wbo = Contact.toWeaveBasicObject(res);		

		//TODO confirm resource exists
		
		this.weaveClient.put(collection, wbo.getId(), wbo);
	}
	
	public void delete(String id) throws WeaveException, NotFoundException {
		this.weaveClient.delete(collection, id);
	}
}
