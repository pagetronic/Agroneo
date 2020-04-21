/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package com.agroneo.web.gaia;

import com.agroneo.web.gaia.utils.GaiaGeoUtils;
import com.agroneo.web.gaia.utils.SpecimensAggregator;
import com.agroneo.web.gaia.utils.SpecimensUtils;
import live.page.web.system.Language;
import live.page.web.system.Settings;
import live.page.web.system.json.Json;
import live.page.web.system.servlet.HttpServlet;
import live.page.web.system.servlet.utils.Api;
import live.page.web.system.servlet.wrapper.ApiServletRequest;
import live.page.web.system.servlet.wrapper.ApiServletResponse;
import live.page.web.system.servlet.wrapper.WebServletRequest;
import live.page.web.system.servlet.wrapper.WebServletResponse;
import live.page.web.system.sessions.Users;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Api(scope = "gaia")
@WebServlet(name = "Gaia Servlet", urlPatterns = {"/gaia", "/gaia/*"})
public class GaiaServlet extends HttpServlet {

	@Override
	public void doGetPublic(WebServletRequest req, WebServletResponse resp) throws IOException {
		resp.sendError(401, Language.get("PLEASE_LOGIN", req.getLng()));
	}

	@Override
	public void doGetAuth(WebServletRequest req, WebServletResponse resp, Users user) throws IOException, ServletException {

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
	public void doGetApiAuth(ApiServletRequest req, ApiServletResponse resp, Users user) throws IOException, ServletException {
		if (req.getRequestURI().equals("/gaia/specimens")) {
			resp.sendResponse(SpecimensAggregator.getSpecimens(
					null,
					req.getString("sort", "-date"),
					req.getString("paging", null))
			);
		} else if (req.getId() != null) {
			resp.sendResponse(SpecimensAggregator.getSpecimen(
					req.getId())
			);
		} else {
			resp.sendResponse(new Json("error", "INVALID"));
		}
	}


	@Override
	public void doPostApiAuth(ApiServletRequest req, ApiServletResponse resp, Json data, Users user) throws IOException, ServletException {

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

			case "create":
				rez = SpecimensUtils.create(data, user);
				break;
			case "edit":
				rez = SpecimensUtils.edit(data, user);
				break;
		}
		resp.sendResponse(rez);
	}

}
