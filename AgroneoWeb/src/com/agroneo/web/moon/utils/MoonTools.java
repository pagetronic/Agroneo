/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package com.agroneo.web.moon.utils;

import live.page.web.system.Language;
import live.page.web.system.json.Json;
import live.page.web.utils.Fx;

import java.text.SimpleDateFormat;
import java.util.*;

public class MoonTools {

	public static Json generateMoonTemplate() {
		Calendar cal = Calendar.getInstance();
		cal.setTimeZone(TimeZone.getTimeZone("UTC"));
		return generateMoonTemplate(cal);
	}

	public static Json generateMoonTemplate(Calendar date) {
		Json datas = new Json();
		int offset = date.getTimeZone().getRawOffset() / 3600000;
		String offset_string = "GMT";
		if (offset > 0) {
			offset_string += "+" + offset;
		} else if (offset < 0) {
			offset_string += offset;
		}
		datas.put("timezone", offset_string);

		Moon moon = MoonToolPhaseAlgorithm.calculate(date);

		double moon_phase_percent = (moon.getMoonAge() * 100D) / Constants.SYNODIC_MONTH;

		String phase = phase_name(moon_phase_percent);
		String moon_phase = "";

		datas.put("moon_distance", Math.round(moon.getMoonDist()));
		long moon_distance_percent = Math.min(100, Math.max(0, Math.round(((moon.getMoonDist() - Constants.MDMIN) * 100) / (Constants.MDMAX - Constants.MDMIN))));
		datas.put("moon_distance_percent", moon_distance_percent);
		datas.put("moon_size_percent", 100 - moon_distance_percent);
		Calendar date_prev = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		date_prev.setTime(date.getTime());
		date_prev.add(Calendar.SECOND, -1);
		int moon_direction = 1;
		if (MoonToolPhaseAlgorithm.distance(date_prev) > moon.getMoonDist()) {
			moon_direction = -1;
		}
		datas.put("moon_distance_direction", moon_direction);

		Json moon_date = new Json();
		moon_date.put("url", MoonTools.createURL(date.getTimeZone(), date.get(Calendar.YEAR), (date.get(Calendar.MONTH) + 1), (date.get(Calendar.DAY_OF_MONTH))));
		moon_date.put("date", date.getTime());
		datas.put("moon_date", moon_date);

		moon_phase += phase;

		datas.put("zodiac", moon.getMoonZodiac());

		datas.put("node", moon.dateLunarNode());
		datas.put("moon_phase", moon_phase);
		datas.put("moon_phase_percent", Math.round(moon_phase_percent));
		datas.put("moon_phase_num", ((moon_phase_percent * 22) / 100D) % 22);
		datas.put("moon_phase_css", ((int) Math.floor((moon_phase_percent / 100D) * 23D)) * -75);
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		df.setTimeZone(TimeZone.getTimeZone("UTC"));
		datas.put("date_8601", df.format(date.getTime()));

		return datas;
	}

	public static String phase_name(double phase) {
		String[] names = {"NEW_MOON", "FIRST_CRESCENMOON", "FIRST_QUARTER_MOON", "WAXING_GIBBOUS_MOON", "FULL_MOON", "WANING_GIBBOUS_MOON", "LAST_QUARTER_MOON", "WANING_CRESCENMOON", "NEW_MOON"};
		try {
			return names[(int) Math.floor((phase / 100D) * 9D)];
		} catch (Exception e) {
			return null;
		}
	}

	public static List<Json> generateMonth(Calendar month_cal) {

		List<Json> month = new ArrayList<Json>();
		Calendar month_cal_copy = (Calendar) month_cal.clone();
		while (month_cal_copy.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
			month_cal_copy.add(Calendar.DAY_OF_YEAR, -1);
		}
		for (int i = 0; (i < 35) || ((month_cal_copy.get(Calendar.MONTH) == month_cal.get(Calendar.MONTH)) || (month_cal_copy.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY)); i++) {
			Json datas = new Json();
			datas.put("num", month_cal_copy.get(Calendar.DAY_OF_MONTH));
			datas.put("moon", generateMoonTemplate(month_cal_copy));

			datas.put("url", generateURL(month_cal_copy));

			if (month_cal_copy.get(Calendar.MONTH) == month_cal.get(Calendar.MONTH)) {
				datas.put("grised", false);
			} else {
				datas.put("grised", true);
			}

			month.add(datas);
			month_cal_copy.add(Calendar.DAY_OF_YEAR, 1);
		}
		return month;
	}

	public static List<Json> getBreadCrumb(TimeZone tz, String uri, String lng) {

		String[] url = uri.split("/");

		List<Json> breads = new ArrayList<Json>();
		Json bread = new Json("title", Language.get("MOON_CALENDAR", lng));
		bread.put("url", MoonTools.createURL(tz));
		breads.add(bread);
		if (url.length > 3) {
			bread = new Json("title", url[2]);
			bread.put("url", MoonTools.createURL(tz, url[2]));
			breads.add(bread);
		}
		if (url.length > 4) {
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.MONTH, Integer.valueOf(url[3]) - 1);
			bread = new Json("title", Fx.ucfirst(new SimpleDateFormat("MMMM", new Locale(lng)).format(cal.getTime())));
			bread.put("url", MoonTools.createURL(tz, url[2], url[3]));
			breads.add(bread);
		}

		return breads;
	}

	public static String createURL(TimeZone tz, Object... items) {
		String url = "/moon";
		if (items.length > 0) {
			String two = String.valueOf(items[0]);
			while (two.length() < 4) {
				two = "0" + two;
			}
			url += "/" + two;
		}
		if (items.length > 1) {
			String two = String.valueOf(items[1]);
			if (two.length() != 2) {
				two = "0" + two;
			}
			url += "/" + two;
		}
		if (items.length > 2) {
			String two = String.valueOf(items[2]);
			if (two.length() != 2) {
				two = "0" + two;
			}
			url += "/" + two;
		}
		if (items.length > 3) {
			String two = String.valueOf(items[3]);
			if (two.length() != 2) {
				two = "0" + two;
			}
			url += "/" + two;
		}
		if (tz.getRawOffset() != 0) {
			return url + "?tz=" + (tz.getRawOffset() / -60000);
		}
		return url;
	}

	public static String generateURL(Calendar cal) {
		int year = cal.get(Calendar.YEAR);
		int month = (cal.get(Calendar.MONTH) + 1);
		int day = cal.get(Calendar.DAY_OF_MONTH);
		String month_s = String.valueOf(month);
		if (month_s.length() == 1) {
			month_s = "0" + month_s;
		}
		String day_s = String.valueOf(day);
		if (day_s.length() == 1) {
			day_s = "0" + day_s;
		}

		String url = MoonTools.createURL(cal.getTimeZone(), year, month_s, day_s);
		return url;
	}

}
