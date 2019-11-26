/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package com.agroneo.web.gaia.utils;

import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UnwindOptions;
import live.page.web.system.db.Aggregator;
import live.page.web.system.db.Db;
import live.page.web.system.db.Pipeliner;
import live.page.web.content.users.UsersAggregator;
import live.page.web.content.congrate.RatingsTools;
import live.page.web.system.Settings;
import live.page.web.system.json.Json;
import live.page.web.system.db.paginer.Paginer;
import live.page.web.system.cosmetic.svg.SVGTemplate;
import org.bson.BsonUndefined;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class SpecimensAggregator {

	public static Json getSpecimens(Bson filter, String sort_str, String paging_str) {
		return getSpecimens(filter, sort_str, paging_str, 20);
	}

	public static Json getSpecimens(Bson filter, String sort_str, String paging_str, int limit) {
		if (!Pattern.compile("^-(update|date)$").matcher(sort_str).find()) {
			return null;
		}
		Paginer paginer = new Paginer(paging_str, sort_str, limit);
		return getSpecimens(filter, paginer);
	}

	public static Json getSpecimens(Bson filter, Paginer paginer) {
		List<Bson> pipeline = new ArrayList<>();
		Aggregator grouper = new Aggregator(
				"title", "replies", "url", "date", "update", "type", "size", "commons", "species", "family", "genus", "users", "location", "distance", "text", "images", "sames", "tropicos", "tId"
		);


		Bson paging_filter = paginer.getFilters();

		List<Bson> filters = new ArrayList<>();
		if (filter != null) {
			filters.add(filter);
		}
		if (paging_filter != null) {
			filters.add(paging_filter);
		}
		if (filters.size() > 0) {
			pipeline.add(Aggregates.match(Filters.and(filters)));
		}

		pipeline.add(paginer.getFirstSort());

		pipeline.add(paginer.getLimit());

		pipeline.addAll(ClassificationUtils.getCommonPipeline(grouper));

		pipeline.addAll(getPipelineSpecimen(grouper, 3));

		pipeline.addAll(RatingsTools.getRatingPipeline(grouper));

		pipeline.add(Aggregates.project(grouper.getProjection().put("update", new Json("$cond", Arrays.asList(new Json("$eq", Arrays.asList("$update", new BsonUndefined())), "$date", "$update")))));

		pipeline.add(Aggregates.project(grouper.getProjectionOrder()));

		pipeline.add(paginer.getLastSort());


		return paginer.getResult("Specimens", pipeline);
	}

	public static Json getSpecimen(String id) {
		List<Bson> pipeline = new ArrayList<>();
		Aggregator grouper = new Aggregator(
				"url", "title", "date", "type", "size", "commons", "update", "species", "family", "genus", "users", "location", "distance", "text", "images", "sames", "tropicos", "tId"
		);

		pipeline.add(Aggregates.match(Filters.eq("_id", id)));

		pipeline.add(Aggregates.limit(1));

		pipeline.addAll(getPipelineSpecimen(grouper));
		pipeline.addAll(ClassificationUtils.getCommonPipeline(grouper));

		pipeline.addAll(RatingsTools.getRatingPipeline(grouper));


		pipeline.add(Aggregates.project(grouper.getProjection().put("update", new Json("$cond", Arrays.asList(new Json("$eq", Arrays.asList("$update", new BsonUndefined())), "$date", "$update")))));

		pipeline.add(Aggregates.project(grouper.getProjectionOrder()));

		return Db.aggregate("Specimens", pipeline).first();
	}

	private static List<Bson> getPipelineSpecimen(Aggregator grouper) {
		return getPipelineSpecimen(grouper, -1);
	}

	private static List<Bson> getPipelineSpecimen(Aggregator grouper, int numImgs) {

		List<Bson> pipeline = new ArrayList<>();


		pipeline.addAll(ClassificationUtils.getClassificationPipeline(grouper));

		if (numImgs >= 0) {
			pipeline.add(Aggregates.project(grouper.getProjection().put("images", new Json("$slice", Arrays.asList("$images", numImgs)))));
		}

		pipeline.add(Aggregates.lookup("BlobFiles", "images", "_id", "images"));

		pipeline.add(Aggregates.unwind("$images", new UnwindOptions().preserveNullAndEmptyArrays(true)));

		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("url", new Json("$concat", Arrays.asList("$species.url", "/", "$_id")))

				.put("images", new Json("_id", true).put("type", true).put("size", true).put("text", true).put("url",
						new Json("$concat", Arrays.asList(Settings.getCDNHttp() + "/files/", "$images._id"))
				))
		));
		pipeline.add(Aggregates.group("$_id", grouper.getGrouper(
				Accumulators.push("images", "$images")
		)));
		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("title", new Json("$cond", Arrays.asList(
						new Json("$or", Arrays.asList(new Json("$eq", Arrays.asList("$title", new BsonUndefined())), new Json("$eq", Arrays.asList("$title", null)))),
						"$species.original", "$title"
				)))
				.put("tropicos", new Json("$concat", Arrays.asList("http://www.tropicos.org/Specimen/", new Json("$substr", Arrays.asList("$tId", 0, -1)))))
				.remove("tId")
		));


		pipeline.addAll(UsersAggregator.getUserPipeline(grouper, true));

		pipeline.add(Aggregates.project(grouper.getProjectionOrder()));

		return pipeline;
	}

	public static class SearchSpecimensPipeline extends Pipeliner {

		public SearchSpecimensPipeline(String type, String lng, Paginer paginer) {
			super(type, paginer);
		}


		@Override
		protected List<Bson> getSearchPipeline() {

			List<Bson> pipeline = new ArrayList<>();
			Aggregator grouper = new Aggregator(
					"title", "url", "date", "update", "intro", "images", "svg", "logo", "images", "breadcrumb", "species", "family", "genus", "score", "type", "tag"
			);


			pipeline.add(Aggregates.project(grouper.getProjection()
					.put("intro", new Json("$reduce", new Json("input", new Json("$split", Arrays.asList("$text", "\n"))).put("initialValue", "").put("in", new Json("$concat", Arrays.asList("$$value", " ", "$$this")))))
			));

			pipeline.addAll(getPipelineSpecimen(grouper, 1));

			pipeline.add(Aggregates.project(grouper.getProjection()
					.put("logo", new Json("$cond", Arrays.asList(
							new Json("$gt", Arrays.asList(new Json("$size", "$images"), 0)),
							new Json("$arrayElemAt", Arrays.asList("$images.url", 0)),
							null
							))
					)
					.put("svg", SVGTemplate.get("fa_icon_tree"))
					.put("breadcrumb", Arrays.asList(
							new Json("id", "$family.id").put("title", "$family.name").put("url", "$family.url"),
							new Json("id", "$genus.id").put("title", "$genus.name").put("url", "$genus.url"),
							new Json("id", "$species.id").put("title", "$species.name").put("url", "$species.url")
					))
					.put("update", new Json("$cond", Arrays.asList(new Json("$eq", Arrays.asList("$update", new BsonUndefined())), "$date", "$update")))
					.remove("family").remove("genus").remove("species").remove("images")
					.put("tag", new Json("$concat", Arrays.asList("Specimens(", "$_id", ")")))
			));

			pipeline.add(Aggregates.project(grouper.getProjectionOrder()));

			return pipeline;
		}


		@Override
		public List<Bson> getUrlizifier(Aggregator grouper, String key) {

			List<Bson> pipeline = new ArrayList<>();

			pipeline.add(Aggregates.lookup("Species", key + ".species", "_id", key + ".species"));
			pipeline.add(Aggregates.unwind("$" + key + ".species", new UnwindOptions().preserveNullAndEmptyArrays(true)));

			pipeline.add(Aggregates.project(grouper.getProjection()
					.put(key, new Json()
							.put("_id", true)
							.put("title", new Json("$cond", Arrays.asList(
									new Json("$or", Arrays.asList(
											new Json("$eq", Arrays.asList("$" + key + ".title", null)),
											new Json("$eq", Arrays.asList("$" + key + ".title", new BsonUndefined()))
									))
									, "$" + key + ".species.name", "$" + key + ".title"

							)))
							.put("url", new Json("$concat", Arrays.asList("/gaia/",
									new Json("$toLower", "$" + key + ".species.family"), "/",
									new Json("$toLower", "$" + key + ".species.genus"), "/",
									new Json("$toLower", "$" + key + ".species._id"), "/",
									"$" + key + "._id"))
							)


					)
			));

			return pipeline;
		}
	}
}
