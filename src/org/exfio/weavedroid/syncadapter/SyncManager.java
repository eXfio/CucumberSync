/*******************************************************************************
 * Copyright (c) 2013 Richard Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package org.exfio.weavedroid.syncadapter;

import java.util.HashSet;
import java.util.Set;

import android.content.SyncResult;
import android.util.Log;

import org.exfio.weave.WeaveException;
import org.exfio.weave.client.NotFoundException;
import org.exfio.weavedroid.ArrayUtils;
import org.exfio.weavedroid.resource.LocalCollection;
import org.exfio.weavedroid.resource.LocalStorageException;
import org.exfio.weavedroid.resource.RecordNotFoundException;
import org.exfio.weavedroid.resource.Resource;
import org.exfio.weavedroid.resource.WeaveCollection;

public class SyncManager {
	private static final String TAG = "exfio.SyncManager";
	
	private static final int MAX_MULTIGET_RESOURCES = 35;
	
	protected LocalCollection<? extends Resource> local;
	protected WeaveCollection<? extends Resource> remote;
	
	
	public SyncManager(LocalCollection<? extends Resource> local, WeaveCollection<? extends Resource> remote) {
		this.local = local;
		this.remote = remote;
	}

	//TODO - Adapt for use with Weave Sync
	public void synchronize(boolean manualSync, SyncResult syncResult) throws LocalStorageException, WeaveException {
		// PHASE 1: push local changes to server
		int	deletedRemotely = pushDeleted();
		int addedRemotely   = pushNew();
		int updatedRemotely = pushDirty();
		
		syncResult.stats.numEntries = deletedRemotely + addedRemotely + updatedRemotely;
		
		// PHASE 2A: check if there's a reason to do a sync with remote (= forced sync or remote CTag changed)
		boolean fetchCollection = false;
		
		if (manualSync) {
			Log.i(TAG, "Synchronization forced");
			fetchCollection = true;
		} else if (syncResult.stats.numEntries > 0) {
			Log.i(TAG, "Local changes found");
			fetchCollection = true;
		} else if (remote.getModifiedTime() == null || remote.getModifiedTime() != local.getModifiedTime()) {
			Log.i(TAG, "Remote changes found");
			fetchCollection = true;
		}

		Log.d(TAG, String.format("local mod time: %.02f, remote mod time: %.02f", local.getModifiedTime(), remote.getModifiedTime()));
		
		if (!fetchCollection) {
			Log.i(TAG, "No local or remote changes, no need to sync");
			return;
		}
		
		// PHASE 2B: detect details of remote changes
		Log.i(TAG, "Fetching remote resource list");
		Set<String> remotelyAdded   = new HashSet<String>();
		Set<String> remotelyUpdated = new HashSet<String>();
		
		String [] remoteResourceIds = null;
		try {
			remoteResourceIds = remote.getObjectIdsModifiedSince(local.getModifiedTime());
		} catch (NotFoundException e) {
			throw new WeaveException("Couldn't get modified objects", e);
		}
		
		for (String id: remoteResourceIds) {
			try {
				Resource localResource = local.findByUID(id, false);
				if (localResource != null) {
					remotelyUpdated.add(id);
				}
			} catch (RecordNotFoundException e) {
				remotelyAdded.add(id);
			}
		}

		try { Thread.sleep(2000); } catch (InterruptedException e) { }

		// PHASE 3: pull remote changes from server
		syncResult.stats.numInserts = pullNew(remotelyAdded.toArray(new String[0]));
		syncResult.stats.numUpdates = pullChanged(remotelyUpdated.toArray(new String[0]));
		syncResult.stats.numEntries += syncResult.stats.numInserts + syncResult.stats.numUpdates;
		
		Log.i(TAG, "Removing non-dirty resources that are not present remotely anymore");
		local.deleteAllExceptUIDs(remoteResourceIds);
		local.commit();

		// update collection CTag
		Log.i(TAG, "Sync complete, fetching new modified time");
		local.setModifiedTime(remote.getModifiedTime());
		local.commit();
	}
	
	
	private int pushDeleted() throws LocalStorageException, WeaveException {
		int count = 0;
		long[] deletedIDs = local.findDeleted();
		
		try {
			Log.i(TAG, "Remotely removing " + deletedIDs.length + " deleted resource(s) (if not changed)");
			for (long id : deletedIDs)
				try {
					Resource res = local.findById(id, false);
					// is this resource even present remotely?
					if (res.getId() != null)	{
						try {
							remote.delete(res.getUid());
						} catch (NotFoundException e) {
							throw new WeaveException(e);
						} catch (WeaveException e) {
							throw new WeaveException(e);
						}
					}
					
					// always delete locally so that the record with the DELETED flag doesn't cause another deletion attempt
					local.delete(res);
					
					count++;
					
				} catch (RecordNotFoundException e) {
					Log.wtf(TAG, "Couldn't read locally-deleted record", e);
				}
		} finally {
			local.commit();
		}
		return count;
	}
	
	private int pushNew() throws LocalStorageException, WeaveException {
		int count = 0;
		long[] newIDs = local.findNew();
		Log.i(TAG, "Uploading " + newIDs.length + " new resource(s) (if not existing)");
		try {
			for (long id : newIDs)
				try {
					Resource res = local.findById(id, true);
					remote.add(res);
					local.clearDirty(res);
					count++;
				} catch (RecordNotFoundException e) {
					Log.wtf(TAG, "Couldn't read new record", e);
				}
		} finally {
			local.commit();
		}
		return count;
	}
	
	private int pushDirty() throws LocalStorageException, WeaveException {
		int count = 0;
		long[] dirtyIDs = local.findUpdated();
		Log.i(TAG, "Uploading " + dirtyIDs.length + " modified resource(s) (if not changed)");
		try {
			for (long id : dirtyIDs) {
				try {
					Resource res = local.findById(id, true);
					remote.update(res);
					local.clearDirty(res);
					count++;
				} catch (NotFoundException e) {
					Log.e(TAG, "Couldn't update remote record", e);
					continue;
				} catch (RecordNotFoundException e) {
					Log.e(TAG, "Couldn't read dirty record", e);
					continue;
				}
			}
		} finally {
			local.commit();
		}
		return count;
	}

	private int pullNew(Resource[] resources) throws LocalStorageException, WeaveException {
		String[] ids = new String[resources.length];
		for (int i = 0; i < resources.length; i++) {
			ids[i] = resources[i].getUid();
		}
		return pullNew(ids);
	}

	private int pullNew(String[] resourcesToAdd) throws LocalStorageException, WeaveException {
		int count = 0;
		Log.i(TAG, "Fetching " + resourcesToAdd.length + " new remote resource(s)");
		
		for (String[] resources : ArrayUtils.partition(resourcesToAdd, MAX_MULTIGET_RESOURCES)) {
			try {
				for (Resource res : remote.multiGet(resources)) {
					Log.d(TAG, "Adding " + res.getId());
					local.add(res);
					local.commit();
					count++;
				}
			} catch (NotFoundException e) {
				Log.e(TAG, String.format("Couldn't get remote resources - %s", e.getMessage()));
				continue;
			}
		}
		return count;
	}
	
	private int pullChanged(Resource[] resources) throws LocalStorageException, WeaveException {
		String[] ids = new String[resources.length];
		for (int i = 0; i < resources.length; i++) {
			ids[i] = resources[i].getUid();
		}
		return pullChanged(ids);
	}
	
	private int pullChanged(String[] resourcesToUpdate) throws LocalStorageException, WeaveException {
		int count = 0;
		Log.i(TAG, "Fetching " + resourcesToUpdate.length + " updated remote resource(s)");
		
		for (String[] resources : ArrayUtils.partition(resourcesToUpdate, MAX_MULTIGET_RESOURCES)) {
			try {
				for (Resource res : remote.multiGet(resources)) {
					Log.i(TAG, "Updating " + res.getId());
					local.updateByRemoteId(res);
					local.commit();
					count++;
				}
			} catch (NotFoundException e) {
				Log.e(TAG, String.format("Couldn't get remote resources - %s", e.getMessage()));
				continue;
			}
		}	
		return count;
	}

}
