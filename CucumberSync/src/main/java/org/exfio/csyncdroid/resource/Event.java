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

import android.text.format.Time;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import biweekly.Biweekly;
import biweekly.ICalVersion;
import biweekly.ICalendar;
import biweekly.component.VAlarm;
import biweekly.component.VEvent;
import biweekly.property.Attendee;
import biweekly.property.DateEnd;
import biweekly.property.DateStart;
import biweekly.property.DurationProperty;
import biweekly.property.ExceptionDates;
import biweekly.property.ExceptionRule;
import biweekly.property.Organizer;
import biweekly.property.RecurrenceDates;
import biweekly.property.RecurrenceRule;
import biweekly.property.Status;
import biweekly.property.Transparency;
import biweekly.util.Duration;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.exfio.weave.WeaveException;
import org.exfio.weave.storage.WeaveBasicObject;
import org.exfio.csyncdroid.Constants;

@ToString(callSuper = true)
public class Event extends Resource {
	private final static String TAG = "csyncdroid.Event";
	
	@Getter @Setter private String summary, location, description;
	
	@Getter @Setter private DateStart  dtStart;
	@Getter @Setter private DateEnd dtEnd;
	@Getter         private DurationProperty duration;
	@Getter @Setter private RecurrenceRule rrule;
    @Getter @Setter private List<RecurrenceDates> rdate;
    @Getter @Setter private List<ExceptionRule> exrule;
	@Getter @Setter private List<ExceptionDates> exdate;

	@Getter @Setter private Boolean forPublic;
	@Getter @Setter private Status status;
	
	@Getter @Setter private boolean opaque;	
	
	@Getter @Setter private Organizer organizer;
	@Getter private List<Attendee> attendees = new LinkedList<Attendee>();
	public void addAttendee(Attendee attendee) {
		attendees.add(attendee);
	}
	
	@Getter private List<VAlarm> alarms = new LinkedList<VAlarm>();
	public void addAlarm(VAlarm alarm) {
		alarms.add(alarm);
	}

    /* instance methods */

    public Event(Resource resource) {
        super(resource);
    }

    public Event(String id, String ETag) {
        super(id, ETag);
    }

    public Event(long localID, String id, String ETag) {
        super(localID, id, ETag);
    }

    public void setDtStart(Date date, boolean hasTime) {
        if (dtStart == null) {
            dtStart = new DateStart(date, hasTime);
        } else {
            dtStart.setValue(date, hasTime);
        }
    }

    public void setDtEnd(Date date, boolean hasTime) {
        if (dtEnd == null) {
            dtEnd = new DateEnd(date, hasTime);
        } else {
            dtEnd.setValue(date, hasTime);
        }
    }

    public void setDuration(Duration duration) {
        setDuration(new DurationProperty(duration));
    }
    public void setDuration(DurationProperty duration) {
        setDuration(duration);
    }

    public static Event fromWeaveBasicObject(WeaveBasicObject wbo) throws WeaveException {
        Log.d(TAG, "fromWeaveBasicObject()");

        Event con = new Event(wbo.getId(), wbo.getModified().toString());

        try {
            InputStream is = new ByteArrayInputStream(wbo.getPayload().getBytes("UTF-8"));
            con.parseJCal(is);
        } catch (IOException | InvalidResourceException e) {
            throw new WeaveException(e);
        }
        return con;
    }

    public void parseJCal(InputStream is) throws IOException, InvalidResourceException {
        Log.d(TAG, "parseJCal()");

        List<List<String>> warnings = new LinkedList<List<String>>();

        ICalendar ical = Biweekly.parseJson(is).warnings(warnings).first();

        Log.d(TAG, "Num Biweekly parse warnings: " + warnings.get(0).size());

        if (warnings.get(0).size() > 0) {
            Log.w(TAG, "Biweekly parse warnings");
            Iterator<String> iter = warnings.get(0).listIterator();
            while (iter.hasNext()) {
                Log.w(TAG, iter.next());
            }
        }

        if (ical == null) {
            Log.e(TAG, "No valid JCal data found");
            return;
        }

        if (ical.getEvents().size() != 1) {
            Log.e(TAG, "Only one VEvent expected");
            return;
        }

        VEvent vevent = ical.getEvents().get(0);

        Log.d(TAG, "parsed JCal:\n" + Biweekly.write(ical).version(ICalVersion.V2_0).go());

        fromVEventObject(vevent);
    }

    public void parseICal(InputStream is) throws IOException, InvalidResourceException {
        Log.d(TAG, "parseICal()");

        //Log.d(TAG, "JSON: " + IOUtils.toString(is));

        List<List<String>> warnings = new LinkedList<List<String>>();

        ICalendar ical = Biweekly.parse(is).warnings(warnings).first();

        Log.d(TAG, "Num Biweekly parse warnings: " + warnings.get(0).size());

        if (warnings.get(0).size() > 0) {
            Log.w(TAG, "Biweekly parse warnings");
            Iterator<String> iter = warnings.get(0).listIterator();
            while (iter.hasNext()) {
                Log.w(TAG, iter.next());
            }
        }

        if (ical == null) {
            Log.e(TAG, "No valid JCal data found");
            return;
        }

        if (ical.getEvents().size() != 1) {
            Log.e(TAG, "Only one VEvent expected");
            return;
        }

        VEvent vevent = ical.getEvents().get(0);

        Log.d(TAG, "parsed JCal:\n" + Biweekly.write(ical).version(ICalVersion.V2_0).go());

        fromVEventObject(vevent);
    }

	@SuppressWarnings("unchecked")
	public void fromVEventObject(VEvent event) throws InvalidResourceException {
        Log.d(TAG, "fromVEventObject()");

		if (event.getUid() != null)
			uid = event.getUid().getValue();
		else {
			Log.w(TAG, "Received VEVENT without UID, generating new one");
			generateUID();
		}

        dtStart  = event.getDateStart();
        dtEnd    = event.getDateEnd();
        duration = event.getDuration();

        if (dtStart == null || (dtEnd == null && duration == null))
			throw new InvalidResourceException("Invalid start time/end time/duration");

		// all-day events and "events on that day":
		// * related UNIX times must be in UTC
		// * must have a duration (set to one day if missing)
		if (!dtStart.getValue().hasTime() && !dtEnd.getValue().after(dtStart.getValue())) {
			Log.i(TAG, "Repairing iCal: DTEND := DTSTART+1");
			Calendar c = Calendar.getInstance(TimeZone.getTimeZone(Time.TIMEZONE_UTC));
			c.setTime(dtStart.getValue());
			c.add(Calendar.DATE, 1);
			dtEnd.setValue(new Date(c.getTimeInMillis()), false);
		}

        Log.d(TAG, "dtstart:  " + dtStart.getValue().toString());
        Log.d(TAG, "dtend:    " + (dtEnd == null ? null : dtEnd.getValue().toString()));
        Log.d(TAG, "duration: " + (duration == null ? null : duration.getValue().toString()));

		rrule = event.getRecurrenceRule();
		rdate = event.getRecurrenceDates();
		exrule = event.getExceptionRules();
		exdate = event.getExceptionDates();
		
		if (event.getSummary() != null)
			summary = event.getSummary().getValue();
		if (event.getLocation() != null)
			location = event.getLocation().getValue();
		if (event.getDescription() != null)
			description = event.getDescription().getValue();
		
		status = event.getStatus();
		
		opaque = true;
		if (event.getTransparency() == Transparency.transparent())
			opaque = false;
		
		organizer = event.getOrganizer();
        attendees = event.getAttendees();

        forPublic = false;
		if (event.getClassification() != null && event.getClassification().isPublic()) {
            forPublic = true;
		}
		
		this.alarms = event.getAlarms();
	}

    public static WeaveBasicObject toWeaveBasicObject(Resource res) throws WeaveException {
        return ((Event)res).toWeaveBasicObject();
    }

    public WeaveBasicObject toWeaveBasicObject() throws WeaveException {
        WeaveBasicObject wbo = new WeaveBasicObject(this.getId(), null, null, null, this.writeJCal());
        return wbo;
    }

    public String writeJCal() throws WeaveException {
        ICalendar ical = new ICalendar();
        ical.setProductId("CucumberSync/" + Constants.APP_VERSION + " (biweekly/" + Biweekly.VERSION + ")");

        ical.addEvent(toVEventObject());

        return Biweekly
                .writeJson(ical)
                .go();
    }

    public String writeICal() throws WeaveException {
        ICalendar ical = new ICalendar();
        ical.setProductId("CucumberSync/" + Constants.APP_VERSION + " (biweekly/" + Biweekly.VERSION + ")");

        ical.addEvent(toVEventObject());

        return Biweekly
                .write(ical)
                .go();
    }

    @SuppressWarnings("unchecked")
	public VEvent toVEventObject() {

		VEvent event = new VEvent();

		if (uid != null)
            event.setUid(uid);

		event.setDateStart(dtStart);
		if (dtEnd != null)
			event.setDateEnd(dtEnd);
		if (duration != null)
			event.setDuration(duration);
		
		if (rrule != null)
			event.setRecurrenceRule(rrule);
		if (rdate != null && !rdate.isEmpty()) {
            for (RecurrenceDates rd : rdate) {
                event.addRecurrenceDates(rd);
            }
        }
		if (exrule != null && !exrule.isEmpty()) {
            for (ExceptionRule exr : exrule) {
                event.addExceptionRule(exr);
            }
        }
		if (exdate != null && !exdate.isEmpty()) {
            for (ExceptionDates exd: exdate) {
                event.addExceptionDates(exd);
            }
        }
		
		if (summary != null && !summary.isEmpty())
			event.setSummary(summary);
		if (location != null && !location.isEmpty())
            event.setLocation(location);
		if (description != null && !description.isEmpty())
			event.setDescription(description);
		
		if (status != null)
			event.setStatus(status);
		if (!opaque)
			event.setTransparency(true);
		
		if (organizer != null)
			event.setOrganizer(organizer);

        for (Attendee attendee: attendees) {
            event.addAttendee(attendee);
        }

		if (forPublic != null)
			event.setClassification(forPublic ? "public" : "private");
		
		for (VAlarm alarm: alarms) {
            event.addAlarm(alarm);
        }

		event.setLastModified(new Date());

        return event;
	}

    public boolean isAllDay() {
        return !dtStart.getValue().hasTime();
    }

}
