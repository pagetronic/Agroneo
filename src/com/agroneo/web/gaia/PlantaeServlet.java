/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package com.agroneo.web.gaia;

import com.agroneo.web.gaia.utils.ClassificationUtils;
import com.agroneo.web.gaia.utils.SpecimensAggregator;
import live.page.web.servlet.wrapper.WebServletRequest;
import live.page.web.servlet.wrapper.WebServletResponse;
import live.page.web.utils.langs.Language;

import java.io.IOException;

public class PlantaeServlet {


	public static void doGetHome(WebServletRequest req, WebServletResponse resp) throws IOException {


		req.setRobotsIndex(true, true);
		String canonical = "/gaia";
		req.setCanonical(canonical, "paging");
		req.setHrefLangs(req.getLng());


		req.setAttribute("url_specimens", "/gaia/specimens");

		if (!req.getRequestURI().equals("/gaia")) {
			resp.sendRedirect("/gaia", 301);
			return;
		}

		req.setMetaTitle(Language.get("GAIA_TITLE", req.getLng()));
		req.setTitle(Language.get("GAIA_SMALL_TITLE", req.getLng()));
		req.setDescription(Language.get("GAIA_DESCRIPTION", req.getLng()));

		req.setAttribute("items", ClassificationUtils.getFamilies(null, req.getString("paging", null), 50));
		if (req.getString("paging", "").equals("")) {
			req.setAttribute("specimens", SpecimensAggregator.getSpecimens(null, "-date", null, 40));
		} else {
			req.addBreadCrumb(Language.get("GAIA_SMALL_TITLE", req.getLng()), "/gaia");
		}

		resp.sendTemplate(req, "/gaia/gaia.html");
	}
}
