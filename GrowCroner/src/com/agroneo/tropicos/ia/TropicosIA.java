/*
 * Copyright (c) 2019. PAGE and Sons
 */

package com.agroneo.tropicos.ia;

import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Sorts;
import live.page.web.db.Db;
import live.page.web.utils.Fx;
import live.page.web.utils.json.Json;

import java.util.ArrayList;
import java.util.List;

public class TropicosIA {
	public static void update() {

		try {

			Fx.log("Species control prepare");

			Db.updateMany("Specimens", new Json(), new Json("$unset", new Json("type", "").put("commons", "").put("commonsCount", "").put("size", "").put("tall", "").put("large", "")));
			Db.updateMany("Species", new Json(), new Json("$unset", new Json("type", "").put("commons", "").put("commonsCount", "").put("size", "").put("tall", "").put("large", "")));
			Db.deleteMany("Commons", Filters.ne("_id", null));

			Fx.log("Species control start");
			MongoCursor<Json> specimens = Db.find("Specimens").sort(Sorts.descending("date")).noCursorTimeout(true).iterator();
			while (specimens.hasNext()) {
				Json specimen = specimens.next();
				updateSpecimen(specimen);


			}
			specimens.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

		Fx.log("Species control done");

	}

	public static void updateSpecimen(Json specimen) {
		String type = Classification.findType(specimen);
		String description = specimen.getText("text", "");
		List<String> commons = new ArrayList<>();

		for (String common : Commons.commonsName(description)) {
			String url = Fx.cleanURL(common).toLowerCase();
			Json cmm = Db.findOneAndUpdate("Commons", Filters.eq("_id", url),
					new Json()
							.put("$inc", new Json("specimens", 1))
							.put("$setOnInsert", new Json("_id", url).put("name", common)),
					new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER));
			commons.add(cmm.getId());
		}

		List<Double> tall = Sizing.findTall(description);
		List<Double> large = Sizing.findLarge(description);

		Json update = new Json();
		Json size = new Json();

		if (type != null) {
			update.put("type", type);
		}
		if (commons.size() != 0) {
			update.put("commons", commons);
		}
		if (tall.size() != 0) {
			Json sizeTall = new Json("min", tall.get(0));
			if (tall.size() == 2) {
				sizeTall.put("max", tall.get(1));
			}
			size.put("tall", sizeTall);
		}
		if (large.size() != 0) {
			Json sizeLarge = new Json("min", large.get(0));
			if (large.size() == 2) {
				sizeLarge.put("max", large.get(1));
			}
			size.put("large", sizeLarge);
		}
		if (!size.isEmpty()) {
			update.put("size", size);
		}

		if (update.isEmpty()) {
			return;
		}

		Db.updateOne("Specimens", Filters.eq("_id", specimen.getId()), new Json("$set", update));


		Json species = Db.findById("Species", specimen.getString("species", ""));
		if (species == null) {
			return;
		}

		Json speciesTall = null;

		if (species.getJson("size") != null && species.getJson("size").getJson("tall") != null) {
			speciesTall = species.getJson("size").getJson("tall");
		}
		if (speciesTall != null && tall.size() > 0) {
			Json sizeTall = new Json();
			if (tall.size() == 1) {

				if (tall.get(0) > speciesTall.getDouble("max", -1)) {
					sizeTall.put("max", tall.get(0));
				} else if (tall.get(0) < speciesTall.getDouble("min", -1)) {
					sizeTall.put("min", tall.get(0));
				}

			} else if (tall.size() == 2) {

				if (tall.get(1) > speciesTall.getDouble("max", -1)) {
					sizeTall.put("max", tall.get(0));
				}
				if (tall.get(0) < speciesTall.getDouble("min", -1)) {
					sizeTall.put("min", tall.get(0));
				}

			}
			size.put("tall", sizeTall);
		}

		Json speciesLarge = null;
		if (species.getJson("size") != null && species.getJson("size").getJson("large") != null) {
			speciesLarge = species.getJson("size").getJson("large");
		}

		if (speciesLarge != null && large.size() > 0) {

			Json sizeLarge = new Json();
			if (large.size() == 1) {

				if (large.get(0) > speciesLarge.getDouble("max", -1)) {
					sizeLarge.put("max", large.get(0));
				} else if (large.get(0) < speciesLarge.getDouble("min", -1)) {
					sizeLarge.put("min", large.get(0));
				}

			} else if (large.size() == 2) {

				if (large.get(1) > speciesLarge.getDouble("max", -1)) {
					sizeLarge.put("max", large.get(0));
				}
				if (large.get(0) < speciesLarge.getDouble("min", -1)) {
					sizeLarge.put("min", large.get(0));
				}

			}
			size.put("large", sizeLarge);
		}
		Json set = new Json();
		Json addToSet = new Json();
		Json updater = new Json();

		if (!size.isEmpty()) {
			set.put("size", size);
		}

		if (type != null) {
			Json speciesType = species.getJson("type");
			if (speciesType == null) {
				speciesType = new Json();
			}
			speciesType.put(type, speciesType.getInteger(type, 0) + 1);
			addToSet.put("type", speciesType);
		}

		if (commons.size() > 0) {

			Json speciesCommons = species.getJson("commons");
			if (speciesCommons == null) {
				speciesCommons = new Json();
			}
			for (String common : commons) {
				speciesCommons.put(common, speciesCommons.getInteger(common, 0) + 1);
			}
			set.put("commonsCount", speciesCommons);
			addToSet.put("commons", new Json("$each", commons));
		}
		if (!set.isEmpty()) {
			updater.put("$set", set);
		}
		if (!addToSet.isEmpty()) {
			updater.put("$addToSet", addToSet);
		}

		if (!updater.isEmpty()) {
			Db.updateOne("Species", Filters.eq("_id", specimen.getString("species", "")),
					updater
			);
		}
	}
}
