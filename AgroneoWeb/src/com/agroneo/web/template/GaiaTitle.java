/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package com.agroneo.web.template;

import live.page.web.system.Language;
import live.page.web.system.json.Json;
import live.page.web.utils.Fx;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;

import java.io.Writer;

public class GaiaTitle extends Directive {

	@Override
	public String getName() {
		return "gaiatitle";
	}

	@Override
	public int getType() {
		return LINE;
	}


	@Override
	public boolean render(InternalContextAdapter context, Writer writer, Node node) throws MethodInvocationException, ResourceNotFoundException, ParseErrorException {

		try {

			String title = (String) node.jjtGetChild(0).value(context);
			if (title == null) {
				return false;
			}

			writer.write(convert(title, context.get("lng").toString()));

		} catch (Exception e) {
			if (Fx.IS_DEBUG) {
				e.printStackTrace();
			}
			return false;
		}
		return true;
	}

	public static String convert(String title, String lng) {
		Json subspecies = new Json(" var. ", "GAIA_VARIETY").put(" subsp. ", "GAIA_SUBSPECIES");
		for (String key : subspecies.keySet()) {
			if (title.contains(key)) {
				title = title.replace(key, " " + Language.get(subspecies.getString(key), lng) + " ");
			}
		}
		return title;
	}
}
