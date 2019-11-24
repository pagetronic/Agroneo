/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package com.agroneo.web.gaia.utils;

import com.mongodb.client.model.Filters;
import live.page.web.utils.json.Json;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class SubClassUtils {

	private static List<String> urls = Arrays.asList("-var-", "-subsp-");
	private static List<String> titles = Arrays.asList(" var. ", " subsp. ");

	public static Json urlSpeciesSpliter(String key) {
		return speciesSpliter(key, urls);
	}

	public static Json nameSpeciesSpliter(String key) {
		return speciesSpliter(key, titles);
	}

	private static Json speciesSpliter(String key, List<String> splits) {
		Json spliter = null;
		for (String split : splits) {
			spliter = new Json("$arrayElemAt", Arrays.asList(new Json("$split", Arrays.asList(spliter == null ? key : spliter.clone(), split)), 0));
		}
		return spliter;
	}


	public static String cleanUrlSpecies(String value) {
		for (String sub : urls) {
			value = value.replaceFirst(sub + ".*$", "");
		}
		return value;
	}


	public static Bson urlSpeciesFilter(String key, String value) {
		List<Bson> filters = new ArrayList<>();
		filters.add(Filters.eq(key, value));
		for (String sub : urls) {
			filters.add(Filters.regex(key, Pattern.compile("^" + value + sub)));
		}
		return Filters.or(filters);
	}
}
