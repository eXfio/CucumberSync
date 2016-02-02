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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import biweekly.ICalDataType;
import biweekly.ICalVersion;
import biweekly.component.VAlarm;
import biweekly.io.ParseContext;
import biweekly.io.TimezoneInfo;
import biweekly.io.WriteContext;
import biweekly.io.scribe.property.DurationPropertyScribe;
import biweekly.io.scribe.property.ExceptionDatesScribe;
import biweekly.io.scribe.property.ExceptionRuleScribe;
import biweekly.io.scribe.property.RecurrenceDatesScribe;
import biweekly.io.scribe.property.RecurrenceRuleScribe;
import biweekly.parameter.CalendarUserType;
import biweekly.parameter.ICalParameters;
import biweekly.parameter.ParticipationStatus;
import biweekly.parameter.Related;
import biweekly.parameter.Role;
import biweekly.property.Attendee;
import biweekly.property.DurationProperty;
import biweekly.property.ExceptionDates;
import biweekly.property.ExceptionRule;
import biweekly.property.Organizer;
import biweekly.property.RecurrenceDates;
import biweekly.property.RecurrenceRule;
import biweekly.property.Status;
import biweekly.property.Trigger;
import biweekly.util.Duration;
import biweekly.util.Recurrence;

import lombok.Cleanup;
import lombok.Getter;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import android.accounts.Account;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentProviderOperation.Builder;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Build;
import android.os.RemoteException;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Reminders;
import android.provider.ContactsContract;
import android.util.Log;

import com.google.ical.values.DateValue;
import com.google.ical.values.RDateList;
import com.google.ical.values.RRule;
import com.google.ical.values.WeekdayNum;

import org.exfio.csyncdroid.syncadapter.AccountSettings;
import org.exfio.csyncdroid.syncadapter.ServerInfo;
import org.exfio.weave.util.SQLUtils;

public class LocalCalendar extends LocalCollection<Event> {
	private static final String TAG = "csyncdroid.LocalCal";

	//TODO - use CTAG or other content provider field for modified date?
	protected static String COLLECTION_COLUMN_CTAG = Calendars.CAL_SYNC1;

	protected String contentAuthority = "com.android.calendar";

	@Getter protected long id;
	@Getter protected String timeZone;
	protected AccountSettings accountSettings;

	/* database fields */
	
	@Override
	protected Uri entriesURI() {
		return syncAdapterURI(Events.CONTENT_URI);
	}

	protected String entryColumnAccountType()	{ return Events.ACCOUNT_TYPE; }
	protected String entryColumnAccountName()	{ return Events.ACCOUNT_NAME; }
	
	protected String entryColumnParentID()		{ return Events.CALENDAR_ID; }
	protected String entryColumnID()			{ return Events._ID; }
	protected String entryColumnRemoteId()		{ return Events._SYNC_ID; }
	protected String entryColumnETag()			{ return Events.SYNC_DATA1; }

	protected String entryColumnDirty()			{ return Events.DIRTY; }
	protected String entryColumnDeleted()		{ return Events.DELETED; }
	
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	protected String entryColumnUID() {
		return (android.os.Build.VERSION.SDK_INT >= 17) ?
			Events.UID_2445 : Events.SYNC_DATA2;
	}

	public LocalCalendar(Account account, ContentProviderClient providerClient, AccountSettings accountSettings, int id, String timeZone) {
		super(account, providerClient);
		this.accountSettings = accountSettings;
		this.id = id;
		this.timeZone = timeZone;
	}


	/* class methods, constructor */

	@SuppressLint("InlinedApi")
	public static void create(Account account, ContentResolver resolver, ServerInfo.ResourceInfo info) throws RemoteException {
		ContentProviderClient client = resolver.acquireContentProviderClient(CalendarContract.AUTHORITY);

		//FIXME - change default colour
		int color = 0xFFC3EA6E;		// fallback: "DAVdroid green"
		if (info.getColor() != null) {
			Pattern p = Pattern.compile("#(\\p{XDigit}{6})(\\p{XDigit}{2})?");
			Matcher m = p.matcher(info.getColor());
			if (m.find()) {
				int color_rgb = Integer.parseInt(m.group(1), 16);
				int color_alpha = m.group(2) != null ? (Integer.parseInt(m.group(2), 16) & 0xFF) : 0xFF;
				color = (color_alpha << 24) | color_rgb;
			}
		}
		
		ContentValues values = new ContentValues();
		values.put(Calendars.ACCOUNT_NAME, account.name);
		values.put(Calendars.ACCOUNT_TYPE, account.type);
		values.put(Calendars.NAME, info.getCollection());
		values.put(Calendars.CALENDAR_DISPLAY_NAME, info.getTitle());
		values.put(Calendars.CALENDAR_COLOR, color);
		values.put(Calendars.OWNER_ACCOUNT, account.name);
		values.put(Calendars.SYNC_EVENTS, 1);
		values.put(Calendars.VISIBLE, 1);
		values.put(Calendars.ALLOWED_REMINDERS, Reminders.METHOD_ALERT);
		
		if (info.isReadOnly())
			values.put(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.CAL_ACCESS_READ);
		else {
			values.put(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.CAL_ACCESS_OWNER);
			values.put(Calendars.CAN_ORGANIZER_RESPOND, 1);
			values.put(Calendars.CAN_MODIFY_TIME_ZONE, 1);
		}
		
		if (android.os.Build.VERSION.SDK_INT >= 15) {
			values.put(Calendars.ALLOWED_AVAILABILITY, Events.AVAILABILITY_BUSY + "," + Events.AVAILABILITY_FREE + "," + Events.AVAILABILITY_TENTATIVE);
			values.put(Calendars.ALLOWED_ATTENDEE_TYPES, Attendees.TYPE_NONE + "," + Attendees.TYPE_OPTIONAL + "," + Attendees.TYPE_REQUIRED + "," + Attendees.TYPE_RESOURCE);
		}
		
		if (info.getTimezone() != null)
			values.put(Calendars.CALENDAR_TIME_ZONE, info.getTimezone());
		
		Log.i(TAG, "Inserting calendar: " + values.toString() + " -> " + calendarsURI(account).toString());
		client.insert(calendarsURI(account), values);
	}
	
	public static LocalCalendar[] findAll(Account account, ContentProviderClient providerClient, AccountSettings accountSettings) throws RemoteException {
		@Cleanup Cursor cursor = providerClient.query(calendarsURI(account),
				new String[] { Calendars._ID, Calendars.NAME, COLLECTION_COLUMN_CTAG, Calendars.CALENDAR_TIME_ZONE },
				Calendars.DELETED + "=0 AND " + Calendars.SYNC_EVENTS + "=1", null, null);
		
		LinkedList<LocalCalendar> calendars = new LinkedList<LocalCalendar>();
		while (cursor != null && cursor.moveToNext())
			calendars.add(new LocalCalendar(account, providerClient, accountSettings, cursor.getInt(0), cursor.getString(3)));
		return calendars.toArray(new LocalCalendar[0]);
	}


	/* collection operations */
	
	public void setCTag(String cTag) {
		pendingOperations.add(ContentProviderOperation.newUpdate(ContentUris.withAppendedId(calendarsURI(), id))
			.withValue(COLLECTION_COLUMN_CTAG, cTag)
			.build());
	}


	/* create/update/delete */
	
	public Event newResource(long localID, String resourceName, String eTag) {
		return new Event(localID, resourceName, eTag);
	}
	
	public void deleteAllExceptRemoteIds(String[] preserveIds) {
		String where;
		
		if (preserveIds.length != 0) {
			where = entryColumnRemoteId() + " NOT IN (" + SQLUtils.quoteArray(preserveIds) + ")";
		} else
			where = entryColumnRemoteId() + " IS NOT NULL";
		
		Builder builder = ContentProviderOperation.newDelete(entriesURI())
				.withSelection(entryColumnParentID() + "=? AND (" + where + ")", new String[]{String.valueOf(id)});
		pendingOperations.add(builder
				.withYieldAllowed(true)
				.build());
	}
	
	public void deleteAllExceptUIDs(String[] preserveUids) {
		String where;
		
		if (preserveUids.length != 0) {
			where = entryColumnUID() + " NOT IN (" + SQLUtils.quoteArray(preserveUids) + ")";
		} else
			where = entryColumnUID() + " IS NOT NULL";
			
		Builder builder = ContentProviderOperation.newDelete(entriesURI())
				.withSelection(entryColumnParentID() + "=? AND (" + where + ")", new String[]{String.valueOf(id)});
		pendingOperations.add(builder
				.withYieldAllowed(true)
				.build());
	}
	
	/* methods for populating the data object from the content provider */

	protected String resourceToString(Resource resource) throws LocalStorageException{

		String output = "Event:";

		@Cleanup Cursor cursor = null;
		try {
			cursor = providerClient.query(ContentUris.withAppendedId(entriesURI(), resource.getLocalID()),
					new String[] {
					/*  0 */ Events.TITLE, Events.EVENT_LOCATION, Events.DESCRIPTION,
					/*  3 */ Events.DTSTART, Events.DTEND, Events.EVENT_TIMEZONE, Events.EVENT_END_TIMEZONE, Events.ALL_DAY,
					/*  8 */ Events.STATUS, Events.ACCESS_LEVEL,
					/* 10 */ Events.RRULE, Events.RDATE, Events.EXRULE, Events.EXDATE,
					/* 14 */ Events.HAS_ATTENDEE_DATA, Events.ORGANIZER, Events.SELF_ATTENDEE_STATUS,
					/* 17 */ entryColumnUID(), Events.DURATION, Events.AVAILABILITY,
					/* 20 */ entryColumnID(), entryColumnRemoteId()
					}, null, null, null);
		} catch (RemoteException e) {
			throw new LocalStorageException("Couldn't find event (" + resource.getLocalID() + ")" + e.getMessage());
		}

		if (cursor != null && cursor.moveToNext()) {

			output += "\nLocalId: " + cursor.getString(20);
			output += "\nRemoteId: " + cursor.getString(21);
			output += "\nUID: " + cursor.getString(17);
			output += "\nTITLE: " + cursor.getString(0);
			output += "\nEVENT_LOCATION: " + cursor.getString(1);
			output += "\nDESCRIPTION: " + cursor.getString(2);

			boolean allDay = cursor.getInt(7) != 0;
			long tsStart = cursor.getLong(3);
			long tsEnd = cursor.getLong(4);
			String duration = cursor.getString(18);
			String tzStart = cursor.getString(5);
			String tzEnd = cursor.getString(6);

			Date dtStart = new Date(tsStart);
			Date dtEnd = new Date(tsEnd);

			output += "\nALL_DAY: " + allDay;
			output += "\nDTSTART: " + dtStart.toString() + "(" + tsStart + ")";
			output += "\nEVENT_TIMEZONE: " + tzStart;
			output += "\nDTEND: " + dtEnd.toString() + "(" + tsEnd + ")";
			output += "\nEVENT_END_TIMEZONE: " + tzEnd;
			output += "\nDURATION: " + duration;
			output += "\nSTATUS: " + cursor.getString(8);
			output += "\nACCESS_LEVEL: " + cursor.getString(9);
			output += "\nRRULE: " + cursor.getString(10);
			output += "\nRDATE: " + cursor.getString(11);
			output += "\nEXRULE: " + cursor.getString(12);
			output += "\nEXDATE: " + cursor.getString(13);
			output += "\nHAS_ATTENDEE_DATA: " + cursor.getString(14);
			output += "\nORGANIZER: " + cursor.getString(15);
			output += "\nSELF_ATTENDEE_STATUS: " + cursor.getString(16);
			output += "\nAVAILABILITY: " + cursor.getString(19);
		} else {
			throw new LocalStorageException("Invalid cursor while fetching event (" + resource.getLocalID() + ")");
		}
		return output;
	}

	@Override
	public void populate(Resource resource) throws LocalStorageException {
		Log.d(TAG, "populate()");

		Log.d(TAG, resourceToString(resource));

		Event e = (Event)resource;

		ICalParameters icalParams = new ICalParameters();
		ParseContext parseContext = new ParseContext();

		try {
			@Cleanup Cursor cursor = providerClient.query(ContentUris.withAppendedId(entriesURI(), e.getLocalID()),
				new String[] {
					/*  0 */ Events.TITLE, Events.EVENT_LOCATION, Events.DESCRIPTION,
					/*  3 */ Events.DTSTART, Events.DTEND, Events.EVENT_TIMEZONE, Events.EVENT_END_TIMEZONE, Events.ALL_DAY,
					/*  8 */ Events.STATUS, Events.ACCESS_LEVEL,
					/* 10 */ Events.RRULE, Events.RDATE, Events.EXRULE, Events.EXDATE,
					/* 14 */ Events.HAS_ATTENDEE_DATA, Events.ORGANIZER, Events.SELF_ATTENDEE_STATUS,
					/* 17 */ entryColumnUID(), Events.DURATION, Events.AVAILABILITY
				}, null, null, null);
			if (cursor != null && cursor.moveToNext()) {
				e.setUid(cursor.getString(17));
				
				e.setSummary(cursor.getString(0));
				e.setLocation(cursor.getString(1));
				e.setDescription(cursor.getString(2));
				
				boolean allDay = cursor.getInt(7) != 0;
				long tsStart = cursor.getLong(3),
					 tsEnd = cursor.getLong(4);
				String duration = cursor.getString(18);
				
				String tzId = cursor.getString(5);
				TimeZone tz = TimeZone.getTimeZone(tzId);

				if (allDay) {
					e.setDtStart(new Date(tsStart), false);

					// provide only DTEND and not DURATION for all-day events
					if (tsEnd == 0) {
						Duration dur = Duration.parse(duration);
						Date dEnd = dur.add(new Date(tsStart));
						tsEnd = dEnd.getTime();
					}
					e.setDtEnd(new Date(tsEnd), false);

				} else {
					//FIXME - Dates are stored as longs, i.e. epoch, hence not clear why timezone property is needed
					// use the start time zone for the end time, too
					// because apps like Samsung Planner allow the user to change "the" time zone but change the start time zone only
					//tzId = cursor.getString(5);

					//e.setDtStart(new Date(tsStart), true);
					Date tmpStart = new Date(tsStart);
					e.setDtStart(tmpStart, true);
					if (tsEnd != 0) {
						//e.setDtEnd(new Date(tsEnd), true);
						Date tmpEnd = new Date(tsEnd);
						e.setDtEnd(tmpEnd, true);
					} else if (!StringUtils.isEmpty(duration)) {
						e.setDuration(Duration.parse(duration));
					}
				}
					
				// recurrence
				try {
					String strRRule = cursor.getString(10);
					if (!StringUtils.isEmpty(strRRule)) {
						//e.setRrule(parseRecurrenceRule(strRRule, tz));
						RecurrenceRuleScribe rRuleReader = new RecurrenceRuleScribe();
						e.setRrule(rRuleReader.parseText(strRRule, ICalDataType.RECUR, icalParams, parseContext));
					}

					String strRDate = cursor.getString(11);
					if (!StringUtils.isEmpty(strRDate)) {
						//e.setRdate(parseRecurrenceDates(strRDate, tz));
						RecurrenceDatesScribe rDatesReader = new RecurrenceDatesScribe();
						List<RecurrenceDates> listRDates = new ArrayList<RecurrenceDates>();
						listRDates.add(rDatesReader.parseText(strRDate, ICalDataType.DATE, icalParams, parseContext));
						e.setRdate(listRDates);
					}

					String strExRule = cursor.getString(12);
					if (!StringUtils.isEmpty(strExRule)) {
						//e.setExrule(parseExceptionRule(strExRule, tz));
						ExceptionRuleScribe exRuleReader = new ExceptionRuleScribe();
						List<ExceptionRule> listExRule = new ArrayList<ExceptionRule>();
						listExRule.add(exRuleReader.parseText(strExRule, ICalDataType.RECUR, icalParams, parseContext));
						e.setExrule(listExRule);
					}
					
					String strExDate = cursor.getString(13);
					if (!StringUtils.isEmpty(strExDate)) {
						// ignored, see https://code.google.com/p/android/issues/detail?id=21426
						//e.setExdate(parseExceptionDates(strExDate, tz));
						ExceptionDatesScribe exDatesReader = new ExceptionDatesScribe();
						List<ExceptionDates> listExDates = new ArrayList<ExceptionDates>();
						listExDates.add(exDatesReader.parseText(strRDate, ICalDataType.DATE, icalParams, parseContext));
						e.setExdate(listExDates);
					}
				} catch (IllegalArgumentException ex) {
					Log.w(TAG, "Invalid recurrence rules, ignoring", ex);
				}
	
				// status
				switch (cursor.getInt(8)) {
				case Events.STATUS_CONFIRMED:
					e.setStatus(Status.confirmed());
					break;
				case Events.STATUS_TENTATIVE:
					e.setStatus(Status.tentative());
					break;
				case Events.STATUS_CANCELED:
					e.setStatus(Status.cancelled());
				}
				
				// availability
				e.setOpaque(cursor.getInt(19) != Events.AVAILABILITY_FREE);
					
				// attendees
				if (cursor.getInt(14) != 0) {	// has attendees
					//TODO - parse name from email assuming it is in rfc2822 format
					e.setOrganizer(new Organizer(cursor.getString(15), cursor.getString(15)));
					populateAttendees(e);
				}
				
				// classification
				switch (cursor.getInt(9)) {
				case Events.ACCESS_CONFIDENTIAL:
				case Events.ACCESS_PRIVATE:
					e.setForPublic(false);
					break;
				case Events.ACCESS_PUBLIC:
					e.setForPublic(true);
				}
				
				populateReminders(e);
			} else
				throw new RecordNotFoundException();
		} catch(RemoteException ex) {
			throw new LocalStorageException(ex);
		}
	}

	
	void populateAttendees(Event e) throws RemoteException {
		Uri attendeesUri = Attendees.CONTENT_URI.buildUpon()
				.appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
				.build();
		@Cleanup Cursor c = providerClient.query(attendeesUri, new String[]{
				/* 0 */ Attendees.ATTENDEE_EMAIL, Attendees.ATTENDEE_NAME, Attendees.ATTENDEE_TYPE,
				/* 3 */ Attendees.ATTENDEE_RELATIONSHIP, Attendees.STATUS
		}, Attendees.EVENT_ID + "=?", new String[]{String.valueOf(e.getLocalID())}, null);

		while (c != null && c.moveToNext()) {

			Attendee attendee = new Attendee(c.getString(1), c.getString(0));

			// type
			int type = c.getInt(2);
			attendee.setParameter("TYPE", (type == Attendees.TYPE_RESOURCE) ? "RESOURCE" : "NONE");

			// role
			int relationship = c.getInt(3);
			switch (relationship) {
				case Attendees.RELATIONSHIP_ORGANIZER:
					attendee.setRole(Role.CHAIR);
					break;
				case Attendees.RELATIONSHIP_ATTENDEE:
				case Attendees.RELATIONSHIP_PERFORMER:
				case Attendees.RELATIONSHIP_SPEAKER:
					attendee.setRole(Role.ATTENDEE);
					break;
				case Attendees.RELATIONSHIP_NONE:
					//No role
					break;
				default:
					//Ignore
			}

			// status
			switch (c.getInt(4)) {
				case Attendees.ATTENDEE_STATUS_INVITED:
					attendee.setParticipationStatus(ParticipationStatus.NEEDS_ACTION);
					break;
				case Attendees.ATTENDEE_STATUS_ACCEPTED:
					attendee.setParticipationStatus(ParticipationStatus.ACCEPTED);
					break;
				case Attendees.ATTENDEE_STATUS_DECLINED:
					attendee.setParticipationStatus(ParticipationStatus.DECLINED);
					break;
				case Attendees.ATTENDEE_STATUS_TENTATIVE:
					attendee.setParticipationStatus(ParticipationStatus.TENTATIVE);
					break;
				default:
					//Ignore
			}

			e.addAttendee(attendee);
		}
	}
	
	void populateReminders(Event e) throws RemoteException {
		// reminders
		Uri remindersUri = Reminders.CONTENT_URI.buildUpon()
				.appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
				.build();
		@Cleanup Cursor c = providerClient.query(remindersUri, new String[]{
				/* 0 */ Reminders.MINUTES, Reminders.METHOD
		}, Reminders.EVENT_ID + "=?", new String[]{String.valueOf(e.getLocalID())}, null);
		while (c != null && c.moveToNext()) {
			//Duration duration = new Duration.Builder().prior(true).minutes(c.getInt(0)).build();
			Duration duration = new Duration.Builder().minutes(c.getInt(0)).build();
			Trigger trigger = new Trigger(duration, Related.START);
			VAlarm alarm = VAlarm.display(trigger, e.getSummary());
			e.addAlarm(alarm);
		}
	}
	
	
	/* content builder methods */

	@Override
	protected Builder buildEntry(Builder builder, Resource resource) {
		Log.d(TAG, "buildEntry()");
		Event event = (Event)resource;

		Log.d(TAG, "dtstart:  " + event.getDtStart().getValue().toString());
		Log.d(TAG, "dtend:    " + (event.getDtEnd() == null ? null : event.getDtEnd().getValue().toString()));
		Log.d(TAG, "duration: " + (event.getDuration() == null ? null : event.getDuration().getValue().toString()));

		TimezoneInfo tzInfo = new TimezoneInfo();
		tzInfo.setDefaultTimeZone(TimeZone.getTimeZone(this.timeZone));
		WriteContext writeContext = new WriteContext(ICalVersion.V2_0, tzInfo);

		builder = builder
				.withValue(Events.CALENDAR_ID, id)
				.withValue(entryColumnRemoteId(), event.getId())
				.withValue(entryColumnETag(), event.getETag())
				.withValue(entryColumnUID(), event.getUid())
				.withValue(Events.ALL_DAY, event.isAllDay() ? 1 : 0)
				.withValue(Events.DTSTART, event.getDtStart().getValue().getTime())
				.withValue(Events.EVENT_TIMEZONE, tzInfo.getTimeZoneToWriteIn(event.getDtStart()).getID())
				.withValue(Events.HAS_ATTENDEE_DATA, event.getAttendees().isEmpty() ? 0 : 1)
				.withValue(Events.GUESTS_CAN_INVITE_OTHERS, 1)
				.withValue(Events.GUESTS_CAN_MODIFY, 1)
				.withValue(Events.GUESTS_CAN_SEE_GUESTS, 1);
		
		boolean recurring = false;

		if (event.getRrule() != null) {
			recurring = true;

			RecurrenceRuleScribe rRuleWriter = new RecurrenceRuleScribe();

			builder = builder.withValue(Events.RRULE, rRuleWriter.writeText(event.getRrule(), writeContext));
		}
		if (event.getRdate() != null && event.getRdate().size() > 0) {
			recurring = true;

			RecurrenceDatesScribe rDateWriter = new RecurrenceDatesScribe();

			for (RecurrenceDates rDate: event.getRdate()) {
				builder = builder.withValue(Events.RDATE, rDateWriter.writeText(rDate, writeContext));
			}
		}
		if (event.getExrule() != null) {

			ExceptionRuleScribe exRuleWriter = new ExceptionRuleScribe();

			for (ExceptionRule exRule: event.getExrule()) {
				builder = builder.withValue(Events.EXRULE, exRuleWriter.writeText(exRule, writeContext));
			}
		}
		if (event.getExdate() != null && event.getExdate().size() > 0) {

			ExceptionDatesScribe exDateWriter = new ExceptionDatesScribe();

			for (ExceptionDates exDate: event.getExdate()) {
				builder = builder.withValue(Events.EXDATE, exDateWriter.writeText(exDate, writeContext));
			}
		}

		// set either DTEND for single-time events or DURATION for recurring events
		// because that's the way Android likes it (see docs)
		if (recurring) {
			// calculate DURATION from start and end date
			DurationPropertyScribe durWriter = new DurationPropertyScribe();

			DurationProperty duration;
			if (event.getDuration() != null) {
				duration = event.getDuration();
			} else {
				Duration dur = new Duration.Builder().seconds((int) ((event.getDtEnd().getValue().getTime() - event.getDtStart().getValue().getTime()) / 1000)).build();
				duration = new DurationProperty(dur);
			}

			builder = builder.withValue(Events.DURATION, durWriter.writeText(duration, writeContext));
		} else {
			builder = builder
					.withValue(Events.DTEND, event.getDtEnd().getValue().getTime())
					.withValue(Events.EVENT_END_TIMEZONE, tzInfo.getTimeZoneToWriteIn(event.getDtEnd()).getID());
		}
		
		if (event.getSummary() != null)
			builder = builder.withValue(Events.TITLE, event.getSummary());
		if (event.getLocation() != null)
			builder = builder.withValue(Events.EVENT_LOCATION, event.getLocation());
		if (event.getDescription() != null)
			builder = builder.withValue(Events.DESCRIPTION, event.getDescription());

		if (event.getOrganizer() != null && event.getOrganizer().getEmail() != null) {
			builder = builder.withValue(Events.ORGANIZER, event.getOrganizer().getEmail());
		}

		Status status = event.getStatus();
		if (status != null) {
			int statusCode = Events.STATUS_TENTATIVE;
			if (status == Status.confirmed())
				statusCode = Events.STATUS_CONFIRMED;
			else if (status == Status.cancelled())
				statusCode = Events.STATUS_CANCELED;
			builder = builder.withValue(Events.STATUS, statusCode);
		}
		
		builder = builder.withValue(Events.AVAILABILITY, event.isOpaque() ? Events.AVAILABILITY_BUSY : Events.AVAILABILITY_FREE);
		
		if (event.getForPublic() != null)
			builder = builder.withValue(Events.ACCESS_LEVEL, event.getForPublic() ? Events.ACCESS_PUBLIC : Events.ACCESS_PRIVATE);

		return builder;
	}

	
	@Override
	protected void addDataRows(Resource resource, long localID, int backrefIdx) {
		Event event = (Event)resource;
		for (Attendee attendee : event.getAttendees())
			pendingOperations.add(buildAttendee(newDataInsertBuilder(Attendees.CONTENT_URI, Attendees.EVENT_ID, localID, backrefIdx), attendee).build());
		for (VAlarm alarm : event.getAlarms())
			pendingOperations.add(buildReminder(newDataInsertBuilder(Reminders.CONTENT_URI, Reminders.EVENT_ID, localID, backrefIdx), alarm).build());
	}
	
	@Override
	protected void removeDataRows(Resource resource) {
		Event event = (Event)resource;
		pendingOperations.add(ContentProviderOperation.newDelete(syncAdapterURI(Attendees.CONTENT_URI))
				.withSelection(Attendees.EVENT_ID + "=?",
				new String[] { String.valueOf(event.getLocalID()) }).build());
		pendingOperations.add(ContentProviderOperation.newDelete(syncAdapterURI(Reminders.CONTENT_URI))
				.withSelection(Reminders.EVENT_ID + "=?",
						new String[]{String.valueOf(event.getLocalID())}).build());
	}

	
	@SuppressLint("InlinedApi")
	protected Builder buildAttendee(Builder builder, Attendee attendee) {
		String email = attendee.getEmail();
		
		String cn = attendee.getCommonName();
		if (cn != null)
			builder = builder.withValue(Attendees.ATTENDEE_NAME, cn);
		
		int type = Attendees.TYPE_NONE;
		if (attendee.getCalendarUserType() == CalendarUserType.RESOURCE)
			type = Attendees.TYPE_RESOURCE;
		else {
			int relationship;
			if (attendee.getRole() == Role.CHAIR)
				relationship = Attendees.RELATIONSHIP_ORGANIZER;
			else {
				relationship = Attendees.RELATIONSHIP_ATTENDEE;
			}
			builder = builder.withValue(Attendees.ATTENDEE_RELATIONSHIP, relationship);
		}
		
		int status = Attendees.ATTENDEE_STATUS_NONE;
		ParticipationStatus partStat = attendee.getParticipationStatus();
		if (partStat == null || partStat == ParticipationStatus.NEEDS_ACTION)
			status = Attendees.ATTENDEE_STATUS_INVITED;
		else if (partStat == ParticipationStatus.ACCEPTED)
			status = Attendees.ATTENDEE_STATUS_ACCEPTED;
		else if (partStat == ParticipationStatus.DECLINED)
			status = Attendees.ATTENDEE_STATUS_DECLINED;
		else if (partStat == ParticipationStatus.TENTATIVE)
			status = Attendees.ATTENDEE_STATUS_TENTATIVE;
		
		return builder
			.withValue(Attendees.ATTENDEE_EMAIL, email)
			.withValue(Attendees.ATTENDEE_TYPE, type)
			.withValue(Attendees.ATTENDEE_STATUS, status);
	}
	
	protected Builder buildReminder(Builder builder, VAlarm alarm) {
		int minutes = 0;

		if (alarm.getTrigger() != null && alarm.getTrigger().getDuration() != null)
			//minutes = duration.getDays() * 24*60 + duration.getHours()*60 + duration.getMinutes();
			minutes = (int)(alarm.getTrigger().getDuration().toMillis()/60000);

		Log.d(TAG, "Adding alarm " + minutes + " min before");
		
		return builder
				.withValue(Reminders.METHOD, Reminders.METHOD_ALERT)
				.withValue(Reminders.MINUTES, minutes);
	}

	protected RecurrenceRule parseRecurrenceRule(String property, TimeZone tz) throws ParseException {
		return new RecurrenceRule(parseRecurrence(property, tz));
	}

	protected List<ExceptionRule> parseExceptionRule(String property, TimeZone tz) throws ParseException {
		ExceptionRule exRule = new ExceptionRule(parseRecurrence(property, tz));

		List<ExceptionRule> listExRule = new ArrayList<ExceptionRule>();
		listExRule.add(exRule);
		return listExRule;
	}

	protected Recurrence parseRecurrence(String property, TimeZone tz) throws ParseException {
		RRule tmpRecRule = new RRule(property);

		Recurrence.Frequency freq = null;
		switch(tmpRecRule.getFreq()) {
			case DAILY:
				freq = Recurrence.Frequency.DAILY;
				break;
			case HOURLY:
				freq = Recurrence.Frequency.HOURLY;
				break;
			case MINUTELY:
				freq = Recurrence.Frequency.MINUTELY;
				break;
			case MONTHLY:
				freq = Recurrence.Frequency.MONTHLY;
				break;
			case SECONDLY:
				freq = Recurrence.Frequency.SECONDLY;
				break;
			case WEEKLY:
				freq = Recurrence.Frequency.WEEKLY;
				break;
			case YEARLY:
				freq = Recurrence.Frequency.YEARLY;
				break;
			default:
				//Fail quietly
		}

		List<Recurrence.DayOfWeek> dows = new ArrayList<Recurrence.DayOfWeek>();
		for (WeekdayNum wday: tmpRecRule.getByDay()) {
			switch(wday.wday) {
				case MO:
					dows.add(Recurrence.DayOfWeek.MONDAY);
					break;
				case TU:
					dows.add(Recurrence.DayOfWeek.TUESDAY);
					break;
				case WE:
					dows.add(Recurrence.DayOfWeek.WEDNESDAY);
					break;
				case TH:
					dows.add(Recurrence.DayOfWeek.THURSDAY);
					break;
				case FR:
					dows.add(Recurrence.DayOfWeek.FRIDAY);
					break;
				case SA:
					dows.add(Recurrence.DayOfWeek.SATURDAY);
					break;
				case SU:
					dows.add(Recurrence.DayOfWeek.SUNDAY);
					break;
				default:
					//Fail quietly
			}
		}

		Recurrence tmpRec = new Recurrence.Builder(freq)
				.interval(tmpRecRule.getInterval())
				.count(tmpRecRule.getCount())
				.until(dateValueToDate(tmpRecRule.getUntil()))
				.byDay(dows)
				.byHour(new ArrayList<Integer>(Arrays.asList(ArrayUtils.toObject(tmpRecRule.getByHour()))))
				.byMinute(new ArrayList<Integer>(Arrays.asList(ArrayUtils.toObject(tmpRecRule.getByMinute()))))
				.byMonth(new ArrayList<Integer>(Arrays.asList(ArrayUtils.toObject(tmpRecRule.getByMonth()))))
				.byMonthDay(new ArrayList<Integer>(Arrays.asList(ArrayUtils.toObject(tmpRecRule.getByMonthDay()))))
				.bySecond(new ArrayList<Integer>(Arrays.asList(ArrayUtils.toObject(tmpRecRule.getBySecond()))))
				.byWeekNo(new ArrayList<Integer>(Arrays.asList(ArrayUtils.toObject(tmpRecRule.getByWeekNo()))))
				.byYearDay(new ArrayList<Integer>(Arrays.asList(ArrayUtils.toObject(tmpRecRule.getByYearDay()))))
				.build();

		return tmpRec;
	}

	protected List<RecurrenceDates> parseRecurrenceDates(String property, TimeZone tz) throws ParseException {

		RDateList rDateList = new RDateList(property, tz);

		Calendar calUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

		RecurrenceDates recDates = new RecurrenceDates();

		for (DateValue rdate: rDateList.getDatesUtc()) {
			calUtc.clear();
			calUtc.set(rdate.year(), rdate.month(), rdate.day());
			recDates.addDate(calUtc.getTime());
		}

		List<RecurrenceDates> listRecDates = new ArrayList<RecurrenceDates>();
		listRecDates.add(recDates);




		return listRecDates;

	}


	protected List<ExceptionDates> parseExceptionDates(String property, TimeZone tz) throws ParseException {

		RDateList exDateList = new RDateList(property, tz);

		Calendar calUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

		ExceptionDates exDates = new ExceptionDates();

		for (DateValue exDate: exDateList.getDatesUtc()) {
			calUtc.clear();
			calUtc.set(exDate.year(), exDate.month(), exDate.day());
			exDates.addValue(calUtc.getTime());
		}

		List<ExceptionDates> listExDates = new ArrayList<ExceptionDates>();
		listExDates.add(exDates);

		return listExDates;
	}

	/* private helper methods */

	protected Date dateValueToDate(DateValue dateValue) {
		Calendar calUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		calUtc.clear();
		calUtc.set(dateValue.year(), dateValue.month(), dateValue.day());
		return calUtc.getTime();
	}

	protected static Uri calendarsURI(Account account) {
		return Calendars.CONTENT_URI.buildUpon().appendQueryParameter(Calendars.ACCOUNT_NAME, account.name)
				.appendQueryParameter(Calendars.ACCOUNT_TYPE, account.type)
				.appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true").build();
	}

	protected Uri calendarsURI() {
		return calendarsURI(account);
	}

	@Override
	public Double getModifiedTime() {
		return accountSettings.getModifiedTime(contentAuthority);
	}

	@Override
	public void setModifiedTime(Double modified) {
		accountSettings.setModifiedTime(contentAuthority, modified);
	}
}
