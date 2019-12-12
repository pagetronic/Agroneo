/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package com.agroneo.web.gaia;

import com.agroneo.web.gaia.utils.GaiaGeoUtils;
import com.mongodb.client.model.Filters;
import live.page.web.system.Settings;
import live.page.web.system.db.Db;
import live.page.web.system.json.Json;
import live.page.web.system.servlet.HttpServlet;
import live.page.web.system.servlet.utils.Api;
import live.page.web.system.servlet.wrapper.ApiServletRequest;
import live.page.web.system.servlet.wrapper.ApiServletResponse;
import live.page.web.system.servlet.wrapper.WebServletRequest;
import live.page.web.system.servlet.wrapper.WebServletResponse;

import javax.servlet.annotation.WebServlet;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Api(scope = "gaia")
@WebServlet(name = "Gaia Servlet", urlPatterns = {"/gaia", "/gaia/*"})
public class GaiaServlet extends HttpServlet {


	@Override
	public void doGetPublic(WebServletRequest req, WebServletResponse resp) throws IOException {


		req.setAttribute("active", "gaia");
		req.setImageOg(Settings.getCDNHttp() + "/css/map/map.png");

		if (req.getRequestURI().startsWith("/gaia/specimens")) {
			SpecimensServlet.doGetLastSpecimens(req, resp);
			return;
		}
		if (req.getRequestURI().startsWith("/gaia/commons")) {
			ClassificationServlet.doGetCommons(req, resp);
			return;
		}

		Matcher family_matcher = Pattern.compile("^/(plantae|gaia|species|especes)(/[a-z]+)(/[a-z]+)?$", Pattern.CASE_INSENSITIVE).matcher(
				req.getRequestURI().replace("/specimens", "")
		);
		if (family_matcher.find()) {
			String family_str = family_matcher.group(2) != null ? family_matcher.group(2).substring(1) : null;
			String genus_str = family_matcher.group(3) != null ? family_matcher.group(3).substring(1) : null;
			if (genus_str == null) {
				ClassificationServlet.doGetFamily(family_str, req.getRequestURI().endsWith("/specimens"), req, resp);
			} else {
				ClassificationServlet.doGetGenus(family_str, genus_str, req.getRequestURI().endsWith("/specimens"), req, resp);
			}
			return;
		}

		Matcher species_matcher = Pattern.compile("^/(plantae|gaia|species|especes)(/[a-z0-9\\-]+)?(/[a-z0-9\\-]+)$", Pattern.CASE_INSENSITIVE).matcher(req.getRequestURI());
		if (species_matcher.find()) {
			ClassificationServlet.doGetSpecy(species_matcher.group(3).substring(1), req, resp);
			return;
		}
		if (req.getId() != null) {
			SpecimensServlet.doGetSpecimen(req.getId(), req, resp, req.getUser(), req.contains("remove"));
			return;
		}

		PlantaeServlet.doGetHome(req, resp);
	}

	@Override
	public void doPostApiPublic(ApiServletRequest req, ApiServletResponse resp, Json data) throws IOException {

		Json rez = new Json("error", "NOT_FOUND");
		switch (data.getString("action")) {
			case "specimens":
				rez = GaiaGeoUtils.getSpecimens(
						data.getJson("bounds"), data.getInteger("zoom", -1),
						data.getString("species", ""), data.getString("family", "")
				);

				break;
			case "search":
				rez = GaiaGeoUtils.search(
						data.getString("search"),
						data.getJson("filter") != null ? data.getJson("filter").getString("family") : null,
						req.getQueryString() != null && req.getQueryString().equals("families"), data.getString("paging", null)
				);

				break;
		}

		resp.sendResponse(rez);
	}

}
