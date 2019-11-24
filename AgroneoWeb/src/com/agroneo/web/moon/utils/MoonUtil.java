/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package com.agroneo.web.moon.utils;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class MoonUtil implements Constants {

	public static Calendar newCalendar(Date date) {
		return newCalendar(date.getTime());
	}

	public static Calendar newCalendar(long timeInMillis) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(timeInMillis);
		return calendar;
	}

	public static Calendar newCalendar(int year, int month, int day, int hour, int minutes, int seconds, TimeZone timeZone) {
		Calendar calendar = Calendar.getInstance(timeZone);
		calendar.set(year, month, day, hour, minutes, seconds);
		return calendar;
	}

	public static Calendar newCalendar(int year, int month, int day) {
		Calendar calendar = Calendar.getInstance();
		calendar.set(year, month, day, 0, 0, 0);
		return calendar;
	}

	public static double toJulian(Calendar calendar) {
		return toJulianFromUnixTimestamp(calendar.getTimeInMillis() / 1000);
	}

	public static double toJulian(long date) {
		return toJulianFromUnixTimestamp(date / 1000);
	}

	public static double toModifiedJulian(Calendar calendar) {
		return toJulian(calendar) - 2400000.5d;

	}

	public static double toJulianFromUnixTimestamp(long unixSecs) {
		return (unixSecs / SECONDS_PER_DAY) + JULIAN_1_1_1970;
	}

	public static long toUnixTimestampFromJulian(double julianDate) {
		return (long) ((julianDate - JULIAN_1_1_1970) * SECONDS_PER_DAY);
	}

	public static Calendar toGregorian(double julianDate) {
		return newCalendar(toUnixTimestampFromJulian(julianDate) * 1000);
	}

	public static Date toGregorianDate(double julianDate) {
		return new Date(toUnixTimestampFromJulian(julianDate) * 1000);
	}

	public static double sinFromDegree(double x) {
		return Math.sin(Math.toRadians((x)));
	}

	public static double cosFromDegree(double x) {
		return Math.cos(Math.toRadians((x)));
	}

	public static double solveKeplerEquation(double m, double ecc) {
		double e, delta;
		double EPSILON = 1E-6;
		e = m = Math.toRadians(m);
		do {
			delta = e - (ecc * Math.sin(e)) - m;
			e -= delta / (1 - (ecc * Math.cos(e)));
		} while (Math.abs(delta) > EPSILON);
		return e;
	}

	public static double fixAngleDegrees(double deg) {
		double fixed = deg % 360d;
		if (fixed < 0) {
			fixed += 360d;
		}
		return fixed;
	}
}
