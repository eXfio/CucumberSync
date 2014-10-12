package org.exfio.weavedroid.resource;

import java.util.List;
import java.util.LinkedList;

import org.exfio.weave.client.WeaveClient;
import org.exfio.weave.client.WeaveBasicObject;
import org.exfio.weave.WeaveException;

public class WeaveAddressBook extends WeaveCollection<Contact> {
	
	public WeaveAddressBook(WeaveClient weaveClient, String collection) {
		super(weaveClient, collection);
	}
	protected String memberContentType() {
		return "application/vcard+json";
	}
	
	
	/* internal member operations */
	public Contact[] multiGet(String[] ids) throws WeaveException {		
		List<Contact> colContact = new LinkedList<Contact>();

		WeaveBasicObject[] colWbo = this.weaveClient.getCollection(collection, ids, null, null, null, null, null, null, null, null); 

		for (int i = 0; i < colWbo.length; i++) {
			Contact con = Contact.fromWeaveBasicObject(colWbo[i]);
			colContact.add(con);
		}
		
		return colContact.toArray(new Contact[0]);
	}

	public Contact get(String id) throws WeaveException {
		WeaveBasicObject wbo = this.weaveClient.getItem(this.collection, id);
		return Contact.fromWeaveBasicObject(wbo);
	}

	public void add(Resource res) throws WeaveException {
		add((Contact)res);
	}

	public void add(Contact res) throws WeaveException {
		WeaveBasicObject wbo = Contact.toWeaveBasicObject(res);
		this.weaveClient.putItem(collection, wbo.getId(), wbo);
	}
	
	public void update(Resource res) throws WeaveException {
		update((Contact)res);	
	}

	public void update(Contact res) throws WeaveException {
		WeaveBasicObject wbo = Contact.toWeaveBasicObject(res);		

		//TODO confirm resource exists
		
		this.weaveClient.putItem(collection, wbo.getId(), wbo);
	}
	
	public void delete(String id) throws WeaveException {
		this.weaveClient.deleteItem(collection, id);
	}
}
