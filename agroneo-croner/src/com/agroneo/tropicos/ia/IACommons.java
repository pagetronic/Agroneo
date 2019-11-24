/*
 * Copyright (c) 2019. PAGE and Sons
 */

package com.agroneo.tropicos.ia;

import live.page.web.utils.Fx;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IACommons {
	public static List<String> commonsName(String description) {
		description = description.replaceAll("[ ]?+\\([ ]?+([^)]+)[ ]?+\\)[ ]?+", "");
		description = description.replaceAll("\n", ".");

		String guill = "\"”“«»";
		String reject = "([^\".;–\\-" + guill + "]+)";
		List<String> commonsName = new ArrayList<>();
		for (String pattern : new String[]{
				"Noms? v[ea]rnaculaires?:? ?[" + guill + "]?" + reject + "[" + guill + "]?",
				"Noms? communs?:? ?[" + guill + "]?" + reject + "[" + guill + "]?",
				"Nombre común:? ?[" + guill + "]?" + reject + "[" + guill + "]?",
				"Nombres comunes:? ?[" + guill + "]?" + reject + "[" + guill + "]?",
				"Nombres vernáculos:? ?[" + guill + "]?" + reject + "[" + guill + "]?",
				"Nombre vernáculo:? ?[" + guill + "]?" + reject + "[" + guill + "]?",
				"Nomes? vernaculares?:? ?[" + guill + "]?" + reject + "[" + guill + "]?",
				" names? ?o?f? ?:? ?[" + guill + "]?" + reject + "[" + guill + "]?"
		}) {
			Pattern find = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
			Matcher matcher = find.matcher(description);
			if (matcher.find()) {
				for (int i = 1; i <= matcher.groupCount(); i++) {

					String common = matcher.group(i).replace("\"", "").replace(".", "").replaceAll("[ ]?+:[ ]?+", "")
							.replaceAll("\\(([^)]+)\\)", "").replaceAll("^[ ]?+", "").replaceAll("[ ]?+$", "");

					for (String commo : common.split("( or | ou |/|[ ]?+,[ ]?+)")) {
						commo = Fx.ucfirst(commo);
						if (!commo.equals("") && !commonsName.contains(commo) && commo.split(" ").length <= 3) {
							commonsName.add(commo);
							Fx.log(commo);

						}
					}

				}
			}
		}
		return commonsName;

	}
}
