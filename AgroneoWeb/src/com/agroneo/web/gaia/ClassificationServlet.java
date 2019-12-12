/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package com.agroneo.web.gaia;

import com.agroneo.web.gaia.utils.ClassificationUtils;
import com.agroneo.web.gaia.utils.SpecimensAggregator;
import com.agroneo.web.gaia.utils.SubClassUtils;
import com.mongodb.client.model.Filters;
import live.page.web.system.Language;
import live.page.web.system.Settings;
import live.page.web.system.db.Db;
import live.page.web.system.json.Json;
import live.page.web.system.servlet.wrapper.WebServletRequest;
import live.page.web.system.servlet.wrapper.WebServletResponse;
import live.page.web.utils.Fx;

import java.io.IOException;
import java.util.regex.Pattern;

public class ClassificationServlet {

	private static final int limit_specimens = 40;

	public static void doGetSpecy(String species_str, WebServletRequest req, WebServletResponse resp) throws IOException {

		req.setTitle(Language.get("PLANTS_MAP", req.getLng()));

		req.setRobotsIndex(req.getString("paging", null) == null, true);
		req.setImageOg(Settings.getCDNHttp() + "/css/map/map.png");


		Json species = ClassificationUtils.getSpecy(species_str);
		if (species == null) {
			resp.sendError(404, "Not found");
			return;
		}
		req.setAttribute("synonyms", species.getList("synonym"));
		req.setAttribute("commons", species.getListJson("commons"));
		req.setBreadCrumbTitle(Fx.ucfirst(Language.get("SPECIES", req.getLng())) + ": " + species.getString("name"));

		Json family = species.getJson("family");
		Json genus = species.getJson("genus");

		String canonical = species.getString("url", null);
		if (!req.getRequestURI().equals(canonical)) {
			resp.sendRedirect(canonical, 301);
			return;
		}

		req.setAttribute("species", species.getId());
		req.setAttribute("genus", genus.getId());
		req.setAttribute("family", family.getId());


		req.setRobotsIndex(req.getQueryString() == null, true);

		req.setCanonical(canonical, "sort", "paging");
		req.setHrefLangs(req.getLng());

		String title = species.getString("name", "")
				.replace(" var. ", Language.get("GAIA_VARIETY", req.getLng()))
				.replace(" subsp. ", Language.get("GAIA_SUBSPECIES", req.getLng()));
		req.setTitle(Language.get("GAIA_SPECIES_TITLE", req.getLng(), title, family.getString("name")));
		req.setDescription(Language.get("GAIA_SPECIES_DESCRIPTION", req.getLng(), title, family.getString("name")));

		req.addBreadCrumb(Language.get("GAIA_SMALL_TITLE", req.getLng()), "/gaia");
		req.addBreadCrumb(family.getString("name"), family.getString("url"));

		if (genus != null) {
			req.addBreadCrumb(genus.getString("name"), genus.getString("url"));
		} else if (Fx.IS_DEBUG) {
			Fx.log("no genus ! ");
		}
		if (req.getParameter("paging") != null) {
			req.addBreadCrumb(title, species.getString("url"));
		}

		req.setAttribute("specimens", SpecimensAggregator.getSpecimens(SubClassUtils.urlSpeciesFilter("species", species.getId()), "-date", req.getString("paging", null), limit_specimens));

		req.setAttribute("follow", "Species(" + species.getId() + ")");
		resp.sendTemplate(req, "/gaia/specimens.html");


	}

	public static void doGetGenus(String family_str, String genus_str, boolean isSpecimens, WebServletRequest req, WebServletResponse resp) throws IOException {

		Json genus = ClassificationUtils.getGenus(family_str, genus_str);

		if (genus == null) {
			resp.sendError(404);
			return;
		}

		String canonical = genus.getString("url");


		if (isSpecimens) {
			canonical += "/specimens";
		}

		if (!req.getRequestURI().equals(canonical)) {
			resp.sendRedirect(canonical, 301);
			return;
		}

		req.setAttribute("genus", genus.getId());
		req.setAttribute("family", genus.getJson("family").getId());

		req.setAttribute("subtitle", genus.getString("name"));

		req.setRobotsIndex(!isSpecimens || req.getQueryString() == null, true);

		req.setCanonical(canonical, "sort", "paging");
		req.setHrefLangs(req.getLng());

		req.setAttribute("url_specimens", genus.getString("url") + "/specimens");
		Json family = genus.getJson("family");
		req.setAttribute("items", ClassificationUtils.getSpecies(Filters.eq("genus", Fx.ucfirst(genus.getId())), req.getString("paging", null)));

		req.addBreadCrumb(Language.get("GAIA_SMALL_TITLE", req.getLng()), "/gaia");
		req.addBreadCrumb(family.getString("name"), family.getString("url"));

		if (isSpecimens || !req.getString("paging", "").equals("")) {
			req.addBreadCrumb(genus.getString("name"), genus.getString("url"));
			if (isSpecimens && !req.getString("paging", "").equals("")) {
				req.addBreadCrumb(Language.get("GAIA_LAST_SPECIMENS", req.getLng()), genus.getString("url") + "/specimens");

			}
		}
		req.setAttribute("specimens", SpecimensAggregator.getSpecimens(Filters.and(
				Filters.regex("species", Pattern.compile("^" + genus.getId() + "-", Pattern.CASE_INSENSITIVE)),
				Filters.eq("family", family.getId())), req.getString("sort", "-date"), req.getString("paging", null), limit_specimens)
		);

		if (isSpecimens) {
			req.setTitle(Language.get("GAIA_LAST_SPECIMENS", req.getLng()) + ": " + genus.getString("name"));
			req.setAttribute("follow", "Genus(" + genus.getId() + ")");
			resp.sendTemplate(req, "/gaia/specimens.html");
		} else {
			req.setTitle(Language.get("GAIA_GENUS_TITLE", req.getLng(), genus.getString("name"), family.getString("name")));
			req.setBreadCrumbTitle(Fx.ucfirst(Language.get("GENUS", req.getLng())) + ": " + genus.getString("name"));
			req.setDescription(Language.get("GAIA_GENUS_DESCRIPTION", req.getLng(), genus.getString("name"), family.getString("name")));
			resp.sendTemplate(req, "/gaia/gaia.html");
		}
	}

	public static void doGetFamily(String family_str, boolean isSpecimens, WebServletRequest req, WebServletResponse resp) throws IOException {

		Json family = ClassificationUtils.getFamily(family_str);


		if (family == null) {
			resp.sendError(404);
			return;
		}


		String canonical = family.getString("url");


		if (isSpecimens) {
			canonical += "/specimens";
		}

		if (!req.getRequestURI().equals(canonical)) {
			resp.sendRedirect(canonical, 301);
			return;
		}

		req.setAttribute("family", family.getId());

		req.setCanonical(canonical, "sort", "paging");
		req.setHrefLangs(req.getLng());

		req.setAttribute("subtitle", family.getString("name"));
		req.setRobotsIndex(!isSpecimens || req.getQueryString() == null, true);

		req.setAttribute("url_specimens", family.getString("url") + "/specimens");

		req.setAttribute("items", ClassificationUtils.getSpecies(Filters.eq("family", family.getId()), req.getString("paging", null)));

		req.addBreadCrumb(Language.get("GAIA_SMALL_TITLE", req.getLng()), "/gaia");


		if (isSpecimens || !req.getString("paging", "").equals("")) {
			req.addBreadCrumb(family.getString("name"), family.getString("url"));
			if (isSpecimens && !req.getString("paging", "").equals("")) {
				req.addBreadCrumb(Language.get("GAIA_LAST_SPECIMENS", req.getLng()), family.getString("url") + "/specimens");
			}
		}
		req.setAttribute("specimens",
				SpecimensAggregator.getSpecimens(Filters.eq("family", family.getId()), req.getString("sort", "-date"), req.getString("paging", null), limit_specimens)
		);

		if (isSpecimens) {
			req.setTitle(Language.get("GAIA_LAST_SPECIMENS", req.getLng()) + ": " + family.getString("name"));
			req.setAttribute("follow", "Families(" + family.getId() + ")");
			resp.sendTemplate(req, "/gaia/specimens.html");
		} else {
			req.setTitle(Language.get("GAIA_FAMILY_TITLE", req.getLng(), family.getString("name")));
			req.setDescription(Language.get("GAIA_FAMILY_DESCRIPTION", req.getLng(), family.getString("name")));
			req.setBreadCrumbTitle(Fx.ucfirst(Language.get("FAMILY", req.getLng())) + ": " + family.getString("name"));
			resp.sendTemplate(req, "/gaia/gaia.html");
		}

	}

	public static void doGetCommons(WebServletRequest req, WebServletResponse resp) throws IOException {

		String canonical = "/gaia/commons";
		String id = req.getRequestURI().replaceAll("/gaia/commons/?([a-z0-9\\-]+)?(/specimens)?", "$1");


		req.setRobotsIndex(req.getQueryString() == null, true);


		req.addBreadCrumb(Language.get("GAIA_SMALL_TITLE", req.getLng()), "/gaia");

		if (!id.equals("")) {
			Json common = Db.findById("Commons", id);
			if (common == null) {
				resp.sendError(404);
				return;
			}

			canonical = canonical + "/" + id;
			req.setTitle(Language.get("COMMON_NAME", req.getLng()) + " " + common.getString("name"));
			req.addBreadCrumb(Language.get("COMMONS_NAMES", req.getLng()), "/gaia/commons");
			if (req.getRequestURI().endsWith("/specimens")) {

				req.setTitle(Language.get("GAIA_LAST_SPECIMENS", req.getLng()) + " " + common.getString("name"));
				req.addBreadCrumb(common.getString("name"), canonical);
				canonical = canonical + "/specimens";
				req.setAttribute("specimens",
						SpecimensAggregator.getSpecimens(Filters.eq("commons", common.getId()), req.getString("sort", "-date"), req.getString("paging", null), limit_specimens)
				);

			} else {
				req.setAttribute("items", ClassificationUtils.getCommon(common.getId(), req.getString("paging", null)));
			}

			req.setAttribute("url_specimens", "/gaia/commons/" + common.getId() + "/specimens");

		} else {
			req.setTitle(Language.get("COMMONS_NAMES", req.getLng()));
			req.setAttribute("items", ClassificationUtils.getCommons(req.getString("paging", null)));
		}


		if (!req.getRequestURI().equals(canonical)) {
			resp.sendRedirect(canonical, 301);
			return;
		}

		req.setRobotsIndex(req.getQueryString() == null, true);

		req.setCanonical(canonical, "sort", "paging");
		req.setHrefLangs(req.getLng());

		if (req.getRequestURI().endsWith("/specimens")) {
			resp.sendTemplate(req, "/gaia/specimens.html");
		} else {
			resp.sendTemplate(req, "/gaia/gaia.html");
		}
	}
}
