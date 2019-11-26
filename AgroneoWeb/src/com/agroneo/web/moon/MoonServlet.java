/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package com.agroneo.web.moon;

import com.agroneo.web.moon.utils.MoonTools;
import live.page.web.system.servlet.HttpServlet;
import live.page.web.system.servlet.wrapper.WebServletRequest;
import live.page.web.system.servlet.wrapper.WebServletResponse;
import live.page.web.utils.Fx;
import live.page.web.system.json.Json;
import live.page.web.system.Language;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

//@WebServlet( urlPatterns = {"/moon", "/moon/*"})
public class MoonServlet extends HttpServlet {

	@Override
	public void doGetPublic(WebServletRequest req, WebServletResponse resp) throws IOException {

		TimeZone tz = TimeZone.getTimeZone("UTC");
		if (req.getParameter("tz") != null) {
			try {
				tz = TimeZone.getTimeZone(TimeZone.getAvailableIDs(0 - (Integer.parseInt(req.getParameter("tz")) * 60 * 1000))[0]);
			} catch (Exception e) {
			}
		}

		Calendar cal = Calendar.getInstance();
		cal.setTimeZone(tz);
		cal.set(Calendar.HOUR_OF_DAY, 12);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);

		String url_canonic = null;
		if (req.getRequestURI().equals("/moon") || req.getRequestURI().equals("/moon/")) {
			url_canonic = do365(cal, req);

		} else {

			String[] url = req.getRequestURI().split("/");
			if (url.length < 3) {
				resp.sendError(404, "Not found");
				return;
			}
			int year = Integer.valueOf(url[2]);
			if ((year > (cal.get(Calendar.YEAR) + 2)) || (year < cal.get(Calendar.YEAR))) {
				req.setRobotsIndex(false);
			} else {
				req.setRobotsIndex(true);
			}

			if (url.length == 3) {
				url_canonic = doYear(cal, url, req);
			} else if (url.length == 4) {
				url_canonic = doMonth(cal, url, req);
			} else if (url.length == 5) {
				url_canonic = doDay(cal, url, req);
			}

			if (url_canonic == null) {
				resp.sendError(404, "Not found");
				return;
			}
			req.setBreadCrumb(MoonTools.getBreadCrumb(cal.getTimeZone(), req.getRequestURI(), req.getLng()));

		}
		req.setCanonical(url_canonic.replaceAll("\\?.*", ""));

		req.setAttribute("active", "moon");
		if (req.getParameter("tz") != null) {
			req.setRobotsIndex(false);
			req.removeAttribute("unavailable_after");
		}
		resp.sendTemplate(req, "/moon.html");

	}

	private String doDay(Calendar cal, String[] url, WebServletRequest req) {

		cal.set(Calendar.YEAR, Integer.valueOf(url[2]));
		cal.set(Calendar.MONTH, Integer.valueOf(url[3]) - 1);
		cal.set(Calendar.DAY_OF_MONTH, Integer.valueOf(url[4]));

		if (new Date().after(cal.getTime())) {
			req.setRobotsIndex(false);
		}

		String url_canonic = MoonTools.createURL(cal.getTimeZone(), cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));

		req.setAttribute("moon", MoonTools.generateMoonTemplate(cal));

		SimpleDateFormat my = new SimpleDateFormat("EEEE d MMMM yyyy", new Locale(req.getLng()));

		req.setTitle(Language.get("MOON", req.getLng()) + Language.get("OF", req.getLng()) + my.format(cal.getTime()) + " 🌙");

		cal.add(Calendar.DAY_OF_YEAR, -1);
		Json prev = new Json("title", my.format(cal.getTime()));
		prev.put("href", MoonTools.createURL(cal.getTimeZone(), cal.get(Calendar.YEAR), (cal.get(Calendar.MONTH) + 1), (cal.get(Calendar.DAY_OF_MONTH))));
		prev.put("nofollow", cal.before(Calendar.getInstance()));
		req.setAttribute("prev", prev);

		cal.add(Calendar.DAY_OF_YEAR, 2);
		Json next = new Json("title", my.format(cal.getTime()));
		next.put("href", MoonTools.createURL(cal.getTimeZone(), cal.get(Calendar.YEAR), (cal.get(Calendar.MONTH) + 1), (cal.get(Calendar.DAY_OF_MONTH))));
		next.put("nofollow", cal.before(Calendar.getInstance()));
		req.setAttribute("next", next);

		SimpleDateFormat rfc850 = new SimpleDateFormat("dd-MMM-yy HH:mm:ss zzz", Locale.ENGLISH);
		rfc850.setTimeZone(cal.getTimeZone());
		req.setAttribute("unavailable_after", rfc850.format(cal.getTime()));

		Calendar cal_lat = (Calendar) cal.clone();
		List<Json> lats = new ArrayList<Json>();
		cal_lat.add(Calendar.DAY_OF_MONTH, -15);
		for (int i = 0; i < 70; i++) {

			cal_lat.add(Calendar.DAY_OF_MONTH, 1);
			Json lat = MoonTools.generateMoonTemplate(cal_lat);
			lat.put("url", MoonTools.createURL(cal_lat.getTimeZone(), cal_lat.get(Calendar.YEAR), cal_lat.get(Calendar.MONTH) + 1, cal_lat.get(Calendar.DAY_OF_MONTH)));
			lat.put("date", cal_lat.getTime());
			lat.put("nofollow", cal_lat.before(Calendar.getInstance()));
			lats.add(lat);
		}

		req.setAttribute("lats", lats);

		return url_canonic;
	}

	private String doMonth(Calendar cal, String[] url, WebServletRequest req) {

		cal.set(Integer.valueOf(url[2]), Integer.valueOf(url[3]) - 1, 1);
		SimpleDateFormat my = new SimpleDateFormat("MMMM yyyy", new Locale(req.getLng()));
		String title = my.format(cal.getTime());

		String url_canonic = MoonTools.createURL(cal.getTimeZone(), cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1);

		req.setTitle(Language.get("MOON_CALENDAR", req.getLng()) + Language.get("OF", req.getLng()) + title + " 🌙");

		Json month = new Json();
		month.put("data", MoonTools.generateMonth(cal));
		month.put("caption", Fx.ucfirst(title));
		List<Json> months = new ArrayList<Json>();
		months.add(month);
		req.setAttribute("months", months);

		cal.add(Calendar.MONTH, -1);
		Json prev = new Json("title", my.format(cal.getTime()));
		prev.put("href", MoonTools.createURL(cal.getTimeZone(), cal.get(Calendar.YEAR), (cal.get(Calendar.MONTH) + 1)));
		req.setAttribute("prev", prev);

		cal.add(Calendar.MONTH, 2);
		int month_s = (cal.get(Calendar.MONTH) + 1);
		Json next = new Json("title", my.format(cal.getTime()));
		next.put("href", MoonTools.createURL(cal.getTimeZone(), cal.get(Calendar.YEAR), month_s));
		req.setAttribute("next", next);

		return url_canonic;

	}

	private String doYear(Calendar cal, String[] url, WebServletRequest req) {

		int year = Integer.valueOf(url[2]);
		cal.set(year, 0, 1);

		String url_canonic = MoonTools.createURL(cal.getTimeZone(), year);

		SimpleDateFormat my = new SimpleDateFormat("MMMM yyyy", new Locale(req.getLng()));

		Calendar year_cal_copy = (Calendar) cal.clone();
		year_cal_copy.add(Calendar.YEAR, -1);
		Json prev = new Json("title", year_cal_copy.get(Calendar.YEAR));
		prev.put("href", MoonTools.createURL(cal.getTimeZone(), year_cal_copy.get(Calendar.YEAR)));
		req.setAttribute("prev", prev);

		year_cal_copy.add(Calendar.YEAR, 2);
		Json next = new Json("title", year_cal_copy.get(Calendar.YEAR));
		next.put("href", MoonTools.createURL(cal.getTimeZone(), year_cal_copy.get(Calendar.YEAR)));
		req.setAttribute("next", next);

		SimpleDateFormat fp = new SimpleDateFormat("yyyy", new Locale(req.getLng()));
		req.setTitle(Language.get("MOON_CALENDAR", req.getLng()) + Language.get("OF", req.getLng()) + fp.format(cal.getTime()) + " 🌙");

		List<Json> months = new ArrayList<Json>();

		while (cal.get(Calendar.YEAR) <= year) {
			Json month = new Json();
			month.put("data", MoonTools.generateMonth(cal));
			month.put("caption", Fx.ucfirst(my.format(cal.getTime())));
			months.add(month);
			cal.add(Calendar.MONTH, 1);
		}
		req.setAttribute("months", months);

		return url_canonic;
	}

	private String do365(Calendar cal, WebServletRequest req) {

		String url_canonic = MoonTools.createURL(cal.getTimeZone());

		SimpleDateFormat my = new SimpleDateFormat("MMMM yyyy", new Locale(req.getLng()));

		Calendar year_cal_copy = (Calendar) cal.clone();
		year_cal_copy.set(Calendar.DAY_OF_MONTH, 1);

		req.setTitle(Language.get("MOON_CALENDAR", req.getLng()));

		List<Json> months = new ArrayList<Json>();

		for (int cmp = 0; cmp < 12; cmp++) {
			Json month = new Json();
			month.put("data", MoonTools.generateMonth(year_cal_copy));
			month.put("caption", Fx.ucfirst(my.format(year_cal_copy.getTime())));
			months.add(month);
			year_cal_copy.add(Calendar.MONTH, 1);
		}
		req.setAttribute("months", months);

		return url_canonic;
	}

}
