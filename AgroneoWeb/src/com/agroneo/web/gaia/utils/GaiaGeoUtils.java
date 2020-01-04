/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package com.agroneo.web.gaia.utils;

import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import live.page.web.system.Settings;
import live.page.web.system.db.Aggregator;
import live.page.web.system.db.Db;
import live.page.web.system.db.paginer.Paginer;
import live.page.web.system.json.Json;
import live.page.web.utils.Fx;
import org.bson.BsonUndefined;
import org.bson.conversions.Bson;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@WebListener
public class GaiaGeoUtils implements ServletContextListener {

	private static final int limit = 20;
	private static final int limit_search = 50;
	private static final int max_global = 500;
	private static final int max_explore = 50;

	public static Json search(String search, String family, boolean families, String paging_str) {

		List<Bson> pipeline = new ArrayList<>();

		Paginer paginer = new Paginer(paging_str, "name", limit_search);

		Bson paging_filter = paginer.getFilters();

		List<Bson> filters = new ArrayList<>();
		filters.add(Filters.gt("specimens", 0));
		if (search != null) {
			filters.add(Filters.or(Filters.eq("_id", search), Filters.regex("name", Pattern.compile(search, Pattern.CASE_INSENSITIVE))));
		}
		if (paging_filter != null) {
			filters.add(paging_filter);
		}
		if (family != null && !family.equals("")) {
			filters.add(Filters.eq("family", family));
		}

		pipeline.add(Aggregates.match(Filters.and(filters)));

		pipeline.add(paginer.getFirstSort());

		pipeline.add(Aggregates.limit(limit_search + 2));

		pipeline.add(Aggregates.project(new Json().put("name", "$name").put("family", "$family").put("infos", "$specimens")));

		pipeline.add(paginer.getLastSort());

		return paginer.getResult(families ? "Families" : "Species", pipeline);
	}


	private static Json global = null;

	public static Json getSpecimens(Json bounds, int zoom, String species, String family) {
		if (zoom < 5 && zoom >= 0 && species.equals("") && family.equals("")) {
			return global;
		}
		Aggregator grouper = new Aggregator(
				"id", "title", "text", "images", "date", "species", "location", "distance", "sames", "author", "tropicos", "tId"
		);

		List<Json> zones = (zoom > 5) ? getZones(bounds) : getZones();

		List<Bson> pipeline = new ArrayList<>();
		pipeline.add(Aggregates.limit(1));

		pipeline.add(Aggregates.project(new Json("_id", false).put("result", new ArrayList<>())));

		for (Json zone : zones) {
			Json filter_zone = new Json();

			if (!species.equals("")) {
				filter_zone.put("species", species);
			}
			if (!family.equals("")) {
				filter_zone.put("family", family);
			}

			filter_zone.put("location", new Json("$geoWithin", new Json("$geometry", zone)));

			pipeline.add(new Json("$lookup",
					new Json("from", "Specimens")
							.put("pipeline", Arrays.asList(

									Aggregates.match(filter_zone),
									Aggregates.limit(max_explore),

									Aggregates.lookup("Species", "species", "_id", "species"),
									Aggregates.project(grouper.getProjection()
											.put("species", new Json("$arrayElemAt", Arrays.asList("$species", 0)))
									),
									Aggregates.lookup("BlobFiles", "images", "_id", "images"),
									Aggregates.unwind("$images"),
									Aggregates.project(grouper.getProjection()
											.put("images", new Json("_id", true).put("type", true).put("size", true).put("text", true).put("url",
													new Json("$concat", Arrays.asList(Settings.getCDNHttp() + "/files/", "$images._id"))
											))
									),
									Aggregates.group("$_id", grouper.getGrouper(
											Accumulators.push("images", "$images")
									)),
									Aggregates.project(grouper.getProjection()
											.put("title", new Json("$cond", Arrays.asList(
													new Json("$or", Arrays.asList(new Json("$eq", Arrays.asList("$title", new BsonUndefined())), new Json("$eq", Arrays.asList("$title", null)))),
													"$species.name", "$title"
											)))
											.put("species", new Json("_id", true).put("name", true).put("family", true))
											.put("tropicos", new Json("$concat", Arrays.asList("http://www.tropicos.org/Specimen/", new Json("$substr", Arrays.asList("$tId", 0, -1)))))
											.remove("tId")

									),
									Aggregates.project(grouper.getProjectionOrder())

							))
							.put("as", "specimens")
			));

			pipeline.add(Aggregates.project(new Json("_id", false).put("result", true)
							.put("specimens",
									new Json("$cond",
											Arrays.asList(
													new Json("$gte", Arrays.asList(new Json("$size", "$specimens"), max_explore)),
													new Json()
															.put("count", new Json("$size", "$specimens"))
															.put("points", "$specimens.location.coordinates")
															.put("location", zone),
													new Json("$cond",
															Arrays.asList(
																	new Json("$gt", Arrays.asList(new Json("$size", "$specimens"), 0)),
																	new Json()
																			.put("specimens", "$specimens"),
																	null
															)
													)
											)
									)
							)
					)
			);

			pipeline.add(Aggregates.project(new Json("_id", false).put("result", new Json("$cond",
							Arrays.asList(
									new Json("$ne", Arrays.asList("$specimens", null)),
									new Json("$concatArrays", Arrays.asList("$result", Arrays.asList("$specimens"))), "$result")
					)))
			);

		}
		Json specimens = Db.aggregate("Specimens", pipeline).first();
		if (specimens == null) {
			return null;
		}
		specimens.put("max", max_explore);
		return specimens;
	}

	private static void getSpecimensGlobalCount() {
		long time = System.currentTimeMillis();

		List<Json> zones = getZones();
		Json rez = new Json("result", new ArrayList<>());
		for (Json zone : zones) {

			try {
				long count = Db.countLimit("Specimens", new Json("location", new Json("$geoWithin", new Json("$geometry", zone))), max_global);
				if (count > 0) {
					rez.add("result", new Json("location", zone).put("count", count));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		global = rez.clone().put("max", max_global);
		Fx.log("Global Zoom calculated in " + (System.currentTimeMillis() - time) + "ms");

	}

	private static final List<Json> worldzone = new ArrayList<>();

	static {
		Document countries = Jsoup.parse(Fx.getResource("/res/countries.xml"));
		for (Element placemark : countries.select("Placemark")) {
			List zone = new ArrayList();
			for (Element coordinate : placemark.select("Polygon coordinates")) {
				List<List<Double>> coordina = new ArrayList<>();
				String[] coos = coordinate.text().split("[ \n\r\t]+");
				for (String coo : coos) {
					if (!coo.equals("")) {
						String[] coo_ = coo.split(",");
						coordina.add(Arrays.asList(Double.valueOf(coo_[0]), Double.valueOf(coo_[1])));
					}
				}
				zone.add(Arrays.asList(coordina));
			}
			worldzone.add(new Json("type", "MultiPolygon").put("coordinates", zone));
		}
	}

	private static List<Json> getZones() {
		return worldzone;
	}

	private static List<Json> getZones(Json bounds) {

		int width = 14;
		List<Json> zones = new ArrayList<>();
		double south = -90;
		double west = -180;
		double north = 90;
		double east = 180;

		if (bounds != null) {
			south = bounds.getDouble("south");
			west = bounds.getDouble("west");
			north = bounds.getDouble("north");
			east = bounds.getDouble("east");
		}

		double tileWidth = (east - west) / width;


		for (int x = 0; x < width; x++) {
			for (int y = 0; y < width; y++) {
				double x1 = south + (tileWidth * x);
				double y1 = west + (tileWidth * y);
				double x2 = x1 + tileWidth;
				double y2 = y1 + tileWidth;

				x1 = Math.max(Math.min(x1, 90), -90);
				x2 = Math.max(Math.min(x2, 90), -90);
				y1 = Math.max(Math.min(y1, 180), -180);
				y2 = Math.max(Math.min(y2, 180), -180);

				if (x1 != x2 && y1 != y2) {

					zones.add(new Json("type", "Polygon")
							.put("coordinates", Arrays.asList(
									Arrays.asList(
											Arrays.asList(y1, x1),
											Arrays.asList(y2, x1),
											Arrays.asList(y2, x2),
											Arrays.asList(y1, x2),
											Arrays.asList(y1, x1)
									))

							)

					);
				}
			}
		}
		return zones;
	}

	private static final ScheduledExecutorService globalcreator = Executors.newSingleThreadScheduledExecutor();


	@Override
	public void contextInitialized(ServletContextEvent sce) {
		globalcreator.scheduleAtFixedRate(GaiaGeoUtils::getSpecimensGlobalCount, 10, 18 * 3600, TimeUnit.SECONDS);
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		Fx.shutdownService(globalcreator);

	}

}
