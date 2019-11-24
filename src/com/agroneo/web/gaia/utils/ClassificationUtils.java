/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package com.agroneo.web.gaia.utils;

import com.mongodb.client.model.*;
import live.page.web.db.Aggregator;
import live.page.web.db.Db;
import live.page.web.utils.Fx;
import live.page.web.utils.json.Json;
import live.page.web.utils.paginer.Paginer;
import org.bson.BsonUndefined;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClassificationUtils {

	public static Json getSpecies(Bson filter, String paging_str) {
		int limit = 50;
		Paginer paginer = new Paginer(paging_str, "-specimens", limit);
		Aggregator grouper = new Aggregator("name", "url", "family", "commons", "rank", "genus", "specimens");

		List<Bson> pipeline = new ArrayList<>();

		pipeline.add(Aggregates.match(Filters.gt("specimens", 0)));
		Json id = SubClassUtils.urlSpeciesSpliter("$_id");

		pipeline.add(Aggregates.group(id, grouper.getGrouper(
				Accumulators.sum("specimens", "$specimens")
				)
		));

		Bson paging = paginer.getFilters();

		pipeline.add(paging != null ? Aggregates.match(Filters.and(paging, filter)) : Aggregates.match(filter));


		pipeline.add(paginer.getFirstSort());
		pipeline.add(paginer.getLimit());


		pipeline.addAll(getFamilyPipeline(grouper));
		pipeline.addAll(ClassificationUtils.getCommonPipeline(grouper));

		pipeline.add(Aggregates.project(grouper.getProjection().put("name", SubClassUtils.nameSpeciesSpliter("$name")).put("url", new Json("$concat", Arrays.asList("$family.url", "/", SubClassUtils.urlSpeciesSpliter("$_id"))))));
		pipeline.add(Aggregates.project(grouper.getProjectionOrder()));
		pipeline.add(paginer.getLastSort());

		return paginer.getResult("Species", pipeline);

	}

	public static Json getSpecy(String species) {

		Aggregator grouper = new Aggregator("name", "url", "family", "commons", "rank", "genus", "specimens", "synonym");
		List<Bson> pipeline = new ArrayList<>();

		pipeline.add(Aggregates.match(
				Filters.eq("_id", SubClassUtils.cleanUrlSpecies(species))
		));

		pipeline.add(Aggregates.limit(1));

		pipeline.addAll(getFamilyPipeline(grouper));
		pipeline.addAll(ClassificationUtils.getCommonPipeline(grouper));

		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("name", SubClassUtils.nameSpeciesSpliter("$name")).put("url", new Json("$concat", Arrays.asList("$family.url", "/", SubClassUtils.urlSpeciesSpliter("$_id"))))
				.put("genus",
						new Json().put("id", new Json("$toLower", "$genus")).put("name", "$genus")
								.put("url", new Json("$concat", Arrays.asList("$family.url", "/", new Json("$toLower", "$genus"))))
				)
		));
		pipeline.add(Aggregates.project(grouper.getProjectionOrder()));

		return Db.aggregate("Species", pipeline).first();
	}

	public static List<Bson> getClassificationPipeline(Aggregator grouper) {

		List<Bson> pipeline = new ArrayList<>();
		pipeline.add(Aggregates.lookup("Species", "species", "_id", "species"));
		pipeline.add(Aggregates.unwind("$species", new UnwindOptions().preserveNullAndEmptyArrays(true)));
		pipeline.add(Aggregates.lookup("Families", "species.family", "_id", "species.family"));
		pipeline.add(Aggregates.unwind("$species.family", new UnwindOptions().preserveNullAndEmptyArrays(true)));

		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("species",
						new Json().put("id", "$species._id")

								.put("name", SubClassUtils.nameSpeciesSpliter("$species.name"))
								.put("original", "$species.name")
								.put("url", new Json("$concat", Arrays.asList("/gaia/", "$species.family._id", "/",
										SubClassUtils.urlSpeciesSpliter("$species._id")
										))
								))
				.put("genus",
						new Json().put("id", new Json("$toLower", "$species.genus")).put("name", "$species.genus")
								.put("url", new Json("$concat", Arrays.asList("/gaia/", "$species.family._id", "/", new Json("$toLower", "$species.genus"))))
				).put("family", new Json()
						.put("id", "$species.family._id")
						.put("name", "$species.family.name").put("species", "$species.family.species").put("specimens", "$species.family.specimens")
						.put("url", new Json("$concat", Arrays.asList("/gaia/", "$species.family._id")))
				)
		));

		return pipeline;
	}

	public static List<Bson> getFamilyPipeline(Aggregator grouper) {
		List<Bson> pipeline = new ArrayList<>();


		pipeline.add(Aggregates.lookup("Families", "family", "_id", "family"));
		pipeline.add(Aggregates.unwind("$family", new UnwindOptions().preserveNullAndEmptyArrays(true)));

		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("family", new Json()
						.put("id", "$family._id")
						.put("name", "$family.name").put("species", "$family.species").put("specimens", "$family.specimens")
						.put("url", new Json("$concat", Arrays.asList("/gaia/", "$family._id")))
				)

		));
		pipeline.add(Aggregates.project(grouper.getProjectionOrder()));
		return pipeline;
	}

	public static List<Bson> getCommonPipeline(Aggregator grouper) {

		List<Bson> pipeline = new ArrayList<>();

		pipeline.add(Aggregates.unwind("$commons", new UnwindOptions().preserveNullAndEmptyArrays(true).includeArrayIndex("commonsorder")));
		pipeline.add(Aggregates.lookup("Commons", "commons", "_id", "commons"));
		pipeline.add(Aggregates.unwind("$commons", new UnwindOptions().preserveNullAndEmptyArrays(true)));


		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("commons", new Json()
						.put("id", "$commons._id")
						.put("name", "$commons.name")
						.put("url", new Json("$concat", Arrays.asList("/gaia/commons/", "$commons._id")))
				)

		));
		pipeline.add(Aggregates.sort(Sorts.ascending("commonsorder")));
		pipeline.add(Aggregates.group("$_id", grouper.getGrouper(
				Accumulators.push("commons", "$commons")
		)));

		pipeline.add(Aggregates.project(grouper.getProjection().put("commons",
				new Json("$filter", new Json("input", "$commons").put("as", "ele").put("cond", new Json("$ne", Arrays.asList("$$ele.id", new BsonUndefined()))))
		)));

		return pipeline;
	}

	public static Json getFamilies(Bson filter, String paging_str, int limit) {

		Paginer paginer = new Paginer(paging_str, "-specimens", limit);
		Aggregator grouper = new Aggregator("name", "url", "specimens", "species");

		List<Bson> pipeline = new ArrayList<>();
		List<Bson> filters = new ArrayList<>();

		filters.add(Filters.gt("species", 0));

		Bson paging = paginer.getFilters();
		if (paging != null) {
			filters.add(paging);
		}
		if (filter != null) {
			filters.add(filter);
		}
		pipeline.add(Aggregates.match(Filters.and(filters)));

		pipeline.add(paginer.getFirstSort());
		pipeline.add(paginer.getLimit());

		pipeline.add(Aggregates.project(grouper.getProjection().put("url", new Json("$concat", Arrays.asList("/gaia/", "$_id")))));
		pipeline.add(Aggregates.project(grouper.getProjectionOrder()));

		pipeline.add(paginer.getLastSort());

		return paginer.getResult("Families", pipeline);

	}

	public static Json getFamily(String id) {

		Aggregator grouper = new Aggregator("name", "url", "specimens", "species");

		List<Bson> pipeline = new ArrayList<>();

		pipeline.add(Aggregates.match(Filters.and(Filters.eq("_id", id), Filters.gt("species", 0))));

		pipeline.add(Aggregates.limit(1));

		pipeline.add(Aggregates.project(grouper.getProjection().put("url", new Json("$concat", Arrays.asList("/gaia/", "$_id")))));
		pipeline.add(Aggregates.project(grouper.getProjectionOrder()));

		return Db.aggregate("Families", pipeline).first();

	}

	public static Json getGenus(String family, String genus) {

		Aggregator grouper = new Aggregator("name", "url", "specimens", "species", "family");

		List<Bson> pipeline = new ArrayList<>();


		pipeline.add(Aggregates.match(Filters.and(Filters.eq("family", family), Filters.eq("genus", Fx.ucfirst(genus)))));

		pipeline.add(Aggregates.group(new Json("$toLower", "$genus"), grouper.getGrouper(
				Accumulators.first("name", "$genus"),
				Accumulators.sum("specimens", "$specimens"),
				Accumulators.sum("species", 1)
				)
		));


		pipeline.addAll(ClassificationUtils.getFamilyPipeline(grouper));

		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("url", new Json("$concat", Arrays.asList("$family.url", "/", "$_id")))

		));

		pipeline.add(Aggregates.project(grouper.getProjectionOrder()));

		return Db.aggregate("Species", pipeline).first();
	}

	public static Json getCommons(String paging_str) {

		Aggregator grouper = new Aggregator("name", "url", "species", "specimens", "species");

		Paginer paginer = new Paginer(paging_str, "-specimens", 50);


		List<Bson> pipeline = new ArrayList<>();

		if (paginer.getFilters() != null) {
			pipeline.add(Aggregates.match(paginer.getFilters()));
		}
		pipeline.add(paginer.getFirstSort());
		pipeline.add(paginer.getLimit());
		pipeline.add(Aggregates.lookup("Species", "_id", "commons", "species"));

		pipeline.add(Aggregates.unwind("$species", new UnwindOptions().preserveNullAndEmptyArrays(true)));
		pipeline.add(Aggregates.group("$_id", grouper.getGrouper(
				Accumulators.sum("species", 1)
		)));
		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("url", new Json("$concat", Arrays.asList("/gaia/commons/", "$_id")))

		));

		pipeline.add(paginer.getLastSort());
		pipeline.add(Aggregates.project(grouper.getProjectionOrder()));

		return paginer.getResult("Commons", pipeline);
	}

	public static Json getCommon(String common, String paging_str) {

		Aggregator grouper = new Aggregator("name", "url", "specimens", "species", "family");
		Paginer paginer = new Paginer(paging_str, "-specimens", 1000);

		List<Bson> pipeline = new ArrayList<>();

		List<Bson> filters = new ArrayList<>();
		filters.add(Filters.eq("commons", common));
		if (paginer.getFilters() != null) {
			filters.add(paginer.getFilters());
		}
		pipeline.add(Aggregates.match(Filters.and(filters)));
		pipeline.add(paginer.getFirstSort());
		pipeline.add(paginer.getLimit());


		pipeline.addAll(ClassificationUtils.getFamilyPipeline(grouper));

		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("url", new Json("$concat", Arrays.asList("$family.url", "/", "$_id")))

		));

		pipeline.add(paginer.getLastSort());
		pipeline.add(Aggregates.project(grouper.getProjectionOrder()));

		return paginer.getResult("Species", pipeline);
	}

}
