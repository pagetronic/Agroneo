/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package com.agroneo.web.gaia;

import com.agroneo.web.gaia.utils.SpecimensAggregator;
import com.agroneo.web.gaia.utils.SubClassUtils;
import com.agroneo.web.template.GaiaTitle;
import com.mongodb.client.model.Filters;
import live.page.web.content.posts.utils.ThreadsAggregator;
import live.page.web.system.servlet.wrapper.WebServletRequest;
import live.page.web.system.servlet.wrapper.WebServletResponse;
import live.page.web.system.sessions.Users;
import live.page.web.utils.Fx;
import live.page.web.system.json.Json;
import live.page.web.system.Language;

import java.io.IOException;

public class SpecimensServlet {


	public static void doGetLastSpecimens(WebServletRequest req, WebServletResponse resp) throws IOException {

		req.setRobotsIndex(req.getQueryString() == null, true);
		String canonical = "/gaia/specimens";
		req.setCanonical(canonical, "sort", "paging");
		req.setHrefLangs(req.getLng());

		req.setTitle(Language.get("GAIA_LAST_SPECIMENS", req.getLng()));

		req.addBreadCrumb(Language.get("GAIA_SMALL_TITLE", req.getLng()), "/gaia");
		if (!req.getString("paging", "").equals("")) {
			req.addBreadCrumb(Language.get("GAIA_LAST_SPECIMENS", req.getLng()), "/gaia/specimens");
		}
		req.setAttribute("follow", "Specimens(ALL)");
		req.setAttribute("specimens", SpecimensAggregator.getSpecimens(null, req.getString("sort", "-date"), req.getString("paging", null)));
		resp.sendTemplate(req, "/gaia/specimens.html");

	}


	public static void doGetSpecimen(String id, WebServletRequest req, WebServletResponse resp, Users user, boolean remove) throws IOException {

		Json specimen = SpecimensAggregator.getSpecimen(id);
		if (specimen == null) {
			resp.sendError(404);
			return;
		}
		String canonical = specimen.getString("url");
		req.setCanonical(canonical);
		req.setHrefLangs(req.getLng());
		if (!req.getRequestURI().equals(canonical)) {
			resp.sendRedirect(canonical, 301);
			return;
		}

		req.setRobotsIndex(specimen.get("tropicos") == null, true);


		req.setTitle(GaiaTitle.convert(specimen.getString("title"), req.getLng()));

		req.addBreadCrumb(Language.get("GAIA_SMALL_TITLE", req.getLng()), "/gaia");
		Json species = specimen.getJson("species");
		Json genus = specimen.getJson("genus");
		Json family = specimen.getJson("family");
		if (family != null) {
			req.addBreadCrumb(family.getString("name"), family.getString("url"));
		}
		if (genus != null) {
			req.addBreadCrumb(genus.getString("name"), genus.getString("url"));
		}
		if (species != null) {
			req.addBreadCrumb(species.getString("name"), species.getString("url"));
		}

		req.setBreadCrumbTitle(Fx.ucfirst(Language.get("SPECIMEN", req.getLng())) + ": " + GaiaTitle.convert(specimen.getString("title"), req.getLng()));


		req.setAttribute("posts", ThreadsAggregator.getPosts(Filters.and(Filters.eq("lng", req.getLng()), Filters.eq("parents", "Specimens(" + id + ")")), req.getString("paging", null), user, remove));
		req.setAttribute("specimen", specimen);
		req.setAttribute("specimens", SpecimensAggregator.getSpecimens(
				Filters.and(
						Filters.ne("_id", specimen.getId()),
						SubClassUtils.urlSpeciesFilter("species", specimen.getJson("species").getId())
				),
				req.getString("sort", "-date"), req.getString("paging", null)));


		resp.sendTemplate(req, "/gaia/specimen.html");
	}

}
