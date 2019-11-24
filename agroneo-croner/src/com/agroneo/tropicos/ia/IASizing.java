/*
 * Copyright (c) 2019. PAGE and Sons
 */

package com.agroneo.tropicos.ia;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IASizing {
	public static List<Double> findTall(String description) {

		String tor = "[ ]?+([0-9.]+)[ ]?+(à|to|\\-|\\+/\\-)?[ ]?+([0-9]+)?[ ]?+";
		String unitr = "[ ]?+(cm|m|m\\.|cm\\.|ft|metros|mètre|metre|mètres|metres)";

		List<Double> sizes = new ArrayList<>();
		description = cleaner(description);
		//Fx.log(description);

		String patter = "" + tor + "" + unitr + "?[ ]?+";
		for (String pat : new String[]{
				"(!?on tree)" + patter + "(tall|high|height|long|de\\saltura|de\\salto|de\\sporte|de long|de haut|haut|\\-|–|herb|shrub|tree|lianescent)",
				"^[a-z]+ de " + patter
		}) {
			Pattern find = Pattern.compile(pat, Pattern.CASE_INSENSITIVE);
			Matcher matcher = find.matcher(description);

			if (matcher.find()) {
				if (matcher.group(2) == null && matcher.group(3) == null && matcher.group(4) == null) {
					continue;
				}
				double unit = 1;
				if (matcher.group(4) != null) {
					if (matcher.group(4).equals("cm")) {
						unit = 1 / 100D;
					} else if (matcher.group(4).equals("ft")) {
						unit = 0.3048D;
					}
				}
				String num = matcher.group(1);
				String to = matcher.group(3);
				if (num.equals(".")) {
					num = to;
					to = null;
				}
				if (num != null) {
					try {
						sizes.add(Math.round(Double.valueOf(num + (num.endsWith(".") && to != null ? to : "")) * unit * 1000D) / 1000D);
					} catch (Exception ignored) {
					}
				}
				if (to != null && (num == null || !num.endsWith("."))) {
					try {
						sizes.add(Math.round(Double.valueOf(to) * unit * 1000D) / 1000D);
					} catch (Exception ignored) {
					}
				}

			}
			if (sizes.size() > 0) {
				break;
			}
		}

		return sizes;

	}

	public static List<Double> findLarge(String description) {

		String tor = "([0-9.]+)[ ]?+(à|to|\\-|\\+/\\-)?[ ]?+([0-9.]+)?[ ]?+";

		String unitr = "[ ]?+(cm|m|m\\.|cm\\.|ft|metros|mètre|metre|mètres|metres)";

		List<Double> sizes = new ArrayList<>();

		description = cleaner(description);


		for (String pat : new String[]{
				"(dbh )[ ]?+:?[ ]?+" + tor + unitr + "?[ ]?+(.*)?",
				"(dbh )?[ ]?+" + tor + unitr + "?[ ]?+(xxx)?(dbh|DAP)",
				"(dbh )?[ ]?+:?[ ]?+" + tor + unitr + "?[ ]?+(de )?(diamètre|large)",
				"(diamètre d)[^0-9]+" + tor + unitr + "?[ ]?+(.*)?"}) {
			Pattern find = Pattern.compile(pat, Pattern.CASE_INSENSITIVE);
			Matcher matcher = find.matcher(description);

			if (matcher.find()) {
				if (matcher.group(1) == null && matcher.group(7) == null) {
					continue;
				}

				if (matcher.group(2) == null && matcher.group(3) == null && matcher.group(4) == null) {
					continue;
				}
				double unit = 1;
				if (matcher.group(5) != null) {
					if (matcher.group(5).equals("cm")) {
						unit = 1 / 100D;
					} else if (matcher.group(5).equals("ft")) {
						unit = 0.3048D;
					}
				}

				if (matcher.group(2) != null) {
					try {
						sizes.add(Math.round(Double.valueOf(matcher.group(2) + (matcher.group(2).endsWith(".") && matcher.group(4) != null ? matcher.group(4) : "")) * unit * 1000D) / 1000D);
					} catch (Exception ignored) {
					}
				}
				if (matcher.group(4) != null && (matcher.group(2) == null || !matcher.group(2).endsWith("."))) {
					try {
						sizes.add(Math.round(Double.valueOf(matcher.group(4)) * unit * 1000D) / 1000D);
					} catch (Exception ignored) {
					}
				}

			}

			if (sizes.size() > 0) {
				break;
			}
		}

		return sizes;

	}

	private static String cleaner(String description) {

		description = description.toLowerCase();
		description = description.replaceAll("([0-9]+)[\\s]?+,[ ]?+([0-9]+)", "$1.$2");
		description = description.replaceAll("([0-9]+)m([0-9]+)", "$1.$2 m");
		description = description.replaceAll("([0-9]+)[ ]?+(\\.)?[ ]?+([0-9]+)?[ ]?+(cm|m|ft)", "$1$2$3 $4");
		description = description.replaceAll("on ([0-9]+)", "");
		description = description.replaceAll("\\. ([0-9]+)", " $1");

		//Fx.log(description);
		return description;
	}
}
