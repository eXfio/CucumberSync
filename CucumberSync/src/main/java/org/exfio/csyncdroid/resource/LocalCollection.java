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

import java.util.ArrayList;
import java.util.List;

import lombok.Cleanup;
import android.accounts.Account;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentProviderOperation.Builder;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.CalendarContract;
import android.util.Log;

public abstract class LocalCollection<T extends Resource> {
	private static final String TAG = "csyncdroid.LocalCol";
	
	protected Account account;
	protected ContentProviderClient providerClient;
	protected ArrayList<ContentProviderOperation> pendingOperations = new ArrayList<ContentProviderOperation>();
	protected Double modifiedTime;
	
	// database fields
	
	abstract protected Uri entriesURI();

	abstract protected String entryColumnAccountType();
	abstract protected String entryColumnAccountName();
	
	abstract protected String entryColumnParentID();
	abstract protected String entryColumnID();
	abstract protected String entryColumnRemoteId();
	abstract protected String entryColumnETag();
	
	abstract protected String entryColumnDirty();
	abstract protected String entryColumnDeleted();
	
	abstract protected String entryColumnUID();

	abstract protected String resourceToString(Resource resource) throws LocalStorageException;

	LocalCollection(Account account, ContentProviderClient providerClient) {
		this.account = account;
		this.providerClient = providerClient;
	}
	

	// collection operations
	
	abstract public long getId();
	abstract public Double getModifiedTime();
	abstract public void setModifiedTime(Double modified);
	
	// content provider (= database) querying

	public long[] findNew() throws LocalStorageException {
		// new records are 1) dirty, and 2) don't have a remote file name yet
		String where = entryColumnDirty() + "=1 AND " + entryColumnRemoteId() + " IS NULL";
		if (entryColumnParentID() != null)
			where += " AND " + entryColumnParentID() + "=" + String.valueOf(getId());
		try {
			@Cleanup Cursor cursor = providerClient.query(entriesURI(),
					new String[] { entryColumnID() },
					where, null, null);
			if (cursor == null)
				throw new LocalStorageException("Couldn't query new records");
			
			long[] fresh = new long[cursor.getCount()];
			for (int idx = 0; cursor.moveToNext(); idx++) {
				long id = cursor.getLong(0);
				
				// new record: generate UID + remote file name so that we can upload
				T resource = findById(id, false);
				resource.initialize();
				// write generated UID + remote file name into database
				ContentValues values = new ContentValues(2);
				values.put(entryColumnUID(), resource.getUid());
				values.put(entryColumnRemoteId(), resource.getId());
				providerClient.update(ContentUris.withAppendedId(entriesURI(), id), values, null, null);
				
				fresh[idx] = id;
			}
			return fresh;
		} catch(RemoteException ex) {
			throw new LocalStorageException(ex);
		}
	}
	
	public long[] findUpdated() throws LocalStorageException {
		// updated records are 1) dirty, and 2) already have a remote file name
		String where = entryColumnDirty() + "=1 AND " + entryColumnRemoteId() + " IS NOT NULL";
		if (entryColumnParentID() != null)
			where += " AND " + entryColumnParentID() + "=" + String.valueOf(getId());
		try {
			@Cleanup Cursor cursor = providerClient.query(entriesURI(),
					new String[] { entryColumnID(), entryColumnRemoteId(), entryColumnETag() },
					where, null, null);
			if (cursor == null)
				throw new LocalStorageException("Couldn't query dirty records");
			
			long[] dirty = new long[cursor.getCount()];
			for (int idx = 0; cursor.moveToNext(); idx++)
				dirty[idx] = cursor.getLong(0);
			return dirty;
		} catch(RemoteException ex) {
			throw new LocalStorageException(ex);
		}
	}

	public long[] findDeleted() throws LocalStorageException {
		String where = entryColumnDeleted() + "=1";
		if (entryColumnParentID() != null)
			where += " AND " + entryColumnParentID() + "=" + String.valueOf(getId());
		try {
			@Cleanup Cursor cursor = providerClient.query(entriesURI(),
					new String[] { entryColumnID(), entryColumnRemoteId(), entryColumnETag() },
					where, null, null);
			if (cursor == null)
				throw new LocalStorageException("Couldn't query dirty records");
			
			long deleted[] = new long[cursor.getCount()];
			for (int idx = 0; cursor.moveToNext(); idx++)
				deleted[idx] = cursor.getLong(0);
			return deleted;
		} catch(RemoteException ex) {
			throw new LocalStorageException(ex);
		}
	}
	
	public T findById(long localID, boolean populate) throws LocalStorageException {
		try {
			@Cleanup Cursor cursor = providerClient.query(ContentUris.withAppendedId(entriesURI(), localID),
					new String[] { entryColumnRemoteId(), entryColumnETag() }, null, null, null);
			if (cursor != null && cursor.moveToNext()) {
				T resource = newResource(localID, cursor.getString(0), cursor.getString(1));
				if (populate)
					populate(resource);
				return resource;
			} else
				throw new RecordNotFoundException();
		} catch(RemoteException ex) {
			throw new LocalStorageException(ex);
		}
	}
	
	public T findByRemoteId(String remoteName, boolean populate) throws LocalStorageException {
		try {
			@Cleanup Cursor cursor = providerClient.query(entriesURI(),
					new String[] { entryColumnID(), entryColumnRemoteId(), entryColumnETag() },
					entryColumnRemoteId() + "=?", new String[] { remoteName }, null);
			if (cursor != null && cursor.moveToNext()) {
				T resource = newResource(cursor.getLong(0), cursor.getString(1), cursor.getString(2));
				if (populate)
					populate(resource);
				return resource;
			} else
				throw new RecordNotFoundException();
		} catch(RemoteException ex) {
			throw new LocalStorageException(ex);
		}
	}

	public T findByUID(String uid, boolean populate) throws RecordNotFoundException, LocalStorageException {
		try {
			@Cleanup Cursor cursor = providerClient.query(entriesURI(),
					new String[] { entryColumnID(), entryColumnRemoteId(), entryColumnETag() },
					this.entryColumnUID() + "=?", new String[] { uid }, null);
			if (cursor != null && cursor.moveToNext()) {
				T resource = newResource(cursor.getLong(0), cursor.getString(1), cursor.getString(2));
				if (populate)
					populate(resource);
				return resource;
			} else
				throw new RecordNotFoundException();
		} catch(RemoteException ex) {
			throw new LocalStorageException(ex);
		}
	}


	public abstract void populate(Resource record) throws LocalStorageException;
	
	protected void queueOperation(Builder builder) {
		if (builder != null)
			pendingOperations.add(builder.build());
	}

	
	// create/update/delete
	
	abstract public T newResource(long localID, String resourceName, String eTag);
	
	public void add(Resource resource) {
		Log.d(TAG, "add()");
		try {
			Log.d(TAG, "Before update");
			Log.d(TAG, resourceToString(resource));
		} catch (LocalStorageException e) {
			//Fail quietly
		}

		int idx = pendingOperations.size();
		pendingOperations.add(
				buildEntry(ContentProviderOperation.newInsert(entriesURI()), resource)
				.withYieldAllowed(true)
				.build());
		
		addDataRows(resource, -1, idx);
	}
	
	public void updateByRemoteId(Resource remoteResource) throws LocalStorageException {
		Log.d(TAG, "updateByRemoteId()");

		T localResource = findByRemoteId(remoteResource.getId(), false);

		Log.d(TAG, "Before update");
		Log.d(TAG, resourceToString(localResource));

		pendingOperations.add(
				buildEntry(ContentProviderOperation.newUpdate(ContentUris.withAppendedId(entriesURI(), localResource.getLocalID())), remoteResource)
						.withValue(entryColumnETag(), remoteResource.getETag())
						.withYieldAllowed(true)
						.build());

		removeDataRows(localResource);
		addDataRows(remoteResource, localResource.getLocalID(), -1);
	}

	public void delete(Resource resource) {
		Log.d(TAG, "delete()");
		try {
			Log.d(TAG, "Before update");
			Log.d(TAG, resourceToString(resource));
		} catch (LocalStorageException e) {
			//Fail quietly
		}

		pendingOperations.add(ContentProviderOperation
				.newDelete(ContentUris.withAppendedId(entriesURI(), resource.getLocalID()))
				.withYieldAllowed(true)
				.build());
	}

	public void deleteAllExceptRemoteIds(Resource[] remoteResources) {
		List<String> ids = new ArrayList<String>();
		for (Resource res: remoteResources) {
			ids.add(res.getId());
		}
		deleteAllExceptRemoteIds(ids.toArray(new String[0]));
	}
	public abstract void deleteAllExceptRemoteIds(String[] ids);
	public abstract void deleteAllExceptUIDs(String[] uids);
	
	public void clearDirty(Resource resource) {
		Log.d(TAG, "clearDirty()");
		try {
			Log.d(TAG, "Before update");
			Log.d(TAG, resourceToString(resource));
		} catch (LocalStorageException e) {
			//Fail quietly
		}

		pendingOperations.add(ContentProviderOperation
				.newUpdate(ContentUris.withAppendedId(entriesURI(), resource.getLocalID()))
				.withValue(entryColumnDirty(), 0)
				.build());
	}

	public void commit() throws LocalStorageException {
		if (!pendingOperations.isEmpty())
			try {
				Log.d(TAG, "Committing " + pendingOperations.size() + " operations");
				for (ContentProviderOperation op: pendingOperations) {
					Log.d(TAG, String.format("%s: %s", (op.isWriteOperation() ? "Write" : "Read"), op.getUri().toString()));
				}
				providerClient.applyBatch(pendingOperations);
				pendingOperations.clear();
			} catch (RemoteException ex) {
				throw new LocalStorageException(ex);
			} catch(OperationApplicationException ex) {
				throw new LocalStorageException(ex);
			}
	}

	
	// helpers
	
	protected Uri syncAdapterURI(Uri baseURI) {
		return baseURI.buildUpon()
				.appendQueryParameter(entryColumnAccountType(), account.type)
				.appendQueryParameter(entryColumnAccountName(), account.name)
				.appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
				.build();
	}
	
	protected Builder newDataInsertBuilder(Uri dataUri, String refFieldName, long raw_ref_id, Integer backrefIdx) {
		Builder builder = ContentProviderOperation.newInsert(syncAdapterURI(dataUri));
		if (backrefIdx != -1)
			return builder.withValueBackReference(refFieldName, backrefIdx);
		else
			return builder.withValue(refFieldName, raw_ref_id);
	}
	
	
	// content builders

	protected abstract Builder buildEntry(Builder builder, Resource resource);
	
	protected abstract void addDataRows(Resource resource, long localID, int backrefIdx);
	protected abstract void removeDataRows(Resource resource);
}
