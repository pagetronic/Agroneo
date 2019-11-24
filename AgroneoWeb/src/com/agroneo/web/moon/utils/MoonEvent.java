/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package com.agroneo.web.moon.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class MoonEvent {

	private MoonEventType _type;
	private Date _date;
	private String _url;
	private String date_8601;

	public MoonEvent(MoonEventType type, Date date, String url) {
		_type = type;
		_date = date;
		_url = url;

		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		cal.setTime(date);
		cal.set(Calendar.HOUR, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);

		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		df.setTimeZone(TimeZone.getTimeZone("UTC"));
		date_8601 = df.format(cal.getTime());
	}

	public MoonEventType getType() {
		return _type;
	}

	public Date getDate() {
		return _date;
	}

	public String getUrl() {
		return _url;
	}

	public String getDate_8601() {
		return date_8601;
	}

	@Override
	public String toString() {
		return getType() + ": " + getDate();
	}

	public enum EventAllocation {
		IN_FUTURE, IN_PRESENT, IN_PAST;

		public static EventAllocation getEventAllocation(long now, long eventTime, long eventPhaseMinus, long eventPhasePlus) {
			long presentMin = eventTime - eventPhaseMinus;
			long presentMax = eventTime + eventPhasePlus;
			if (now < presentMin) {
				return IN_FUTURE;
			}
			if (now >= presentMin) {
				if (now <= presentMax) {
					return IN_PRESENT;
				}
			}
			return IN_PAST;
		}
	}

}
