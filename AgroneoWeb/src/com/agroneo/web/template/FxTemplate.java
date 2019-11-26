/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package com.agroneo.web.template;

import live.page.web.system.json.Json;

import java.util.List;

public class FxTemplate {

	public static String translate(String text, String lng) {
		StringBuilder translate = new StringBuilder();

		return translate.toString();

	}

	public static String convertDMS(Json loc) {
		List<Double> coordinates = loc.getList("coordinates", Double.class);
		double longitude_loc = coordinates.get(0);
		double latitude_loc = coordinates.get(1);

		double absolute = Math.abs(longitude_loc);
		double degrees = Math.floor(absolute);
		double minutesNotTruncated = (absolute - degrees) * 60;
		double minutes = Math.floor(minutesNotTruncated);
		double seconds = Math.floor((minutesNotTruncated - minutes) * 60);
		String longitude = degrees + "° " + minutes + "’ " + seconds + "” " + (Math.signum(longitude_loc) >= 0 ? "N" : "S");

		absolute = Math.abs(latitude_loc);
		degrees = Math.floor(absolute);
		minutesNotTruncated = (absolute - degrees) * 60;
		minutes = Math.floor(minutesNotTruncated);
		seconds = Math.floor((minutesNotTruncated - minutes) * 60);
		String latitude = degrees + "° " + minutes + "’ " + seconds + "” " + (Math.signum(latitude_loc) >= 0 ? "E" : "W");

		return "<span itemprop=\"geo\" itemscope=\"\" itemtype=\"https://schema.org/GeoCoordinates\">" +
				"<span itemprop=\"longitude\" content=\"" + longitude_loc + "\">" + longitude + "</span>" +
				" &#8211; " +
				"<span itemprop=\"latitude\" content=\"" + latitude_loc + "\">" + latitude + "</span>" +
				"</span>";
	}

}
