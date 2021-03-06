package com.agroneo.web.gaia.utils;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.UpdateOptions;
import live.page.web.system.db.Db;
import live.page.web.system.json.Json;
import live.page.web.system.sessions.Users;
import live.page.web.utils.Fx;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SpecimensUtils {

	//TODO Tall/large/type
	//TODO multiple authors
	//TODO specify date
	//TODO specify copyright

	public static Json create(Json data, Users user) {
		Date date = new Date();

		Json errors = new Json();

		Json species = Db.findById("Species", data.getString("species"));
		if (species == null) {
			errors.put("species", data.getString("species") == null ? "EMPTY" : "UNKNOWN");
		}

		List<String> images = data.getList("images");
		if (images == null || images.size() == 0) {
			errors.put("images", "EMPTY");
		} else {
			List<String> errors_images = new ArrayList<>();
			for (String image : images) {
				if (!Db.exists("BlobFiles", Filters.eq("_id", image))) {
					errors_images.add(image);
				}
			}
			if (errors_images.size() > 0) {
				errors.put("images", errors_images);
			}
		}

		if (!data.containsKey("location")) {
			errors.put("location", "EMPTY");
		} else {
			if (!testLocation(data.getJson("location"))) {
				errors.put("location", "INVALID");
			}
		}
		if (!data.containsKey("authors")) {
			for (String author : data.getList("authors")) {
				if (!Db.exists("Users", Filters.eq("_id", author))) {
					errors.put("authors", "INVALID");
				}
			}
		}


		if (!errors.isEmpty()) {
			return new Json("errors", errors);
		}

		Json specimen = new Json().add("users", user.getId()).put("date", date).put("update", date);

		specimen.put("text", Fx.normalize(data.getText("text")));

		if (data.containsKey("commons")) {
			specimen.put("commons", Fx.cleanURL(species.getString("commons")));
		}

		specimen.put("species", species.getId());
		specimen.put("family", species.getString("family"));

		specimen.put("title",
				data.getString("title", "").equals("") ?
						species.getString("name") :
						Fx.normalize(data.getString("title", ""))
		);
		specimen.put("images", images);
		specimen.put("location", data.getJson("location"));

		Db.save("Specimens", specimen);
		Db.updateOne("Species", Filters.eq("_id", species.getId()), new Json("$inc", new Json("specimens", 1)));
		if (specimen.containsKey("commons")) {
			Db.updateOne("Commons", Filters.eq("_id", specimen.getString("commons")),
					new Json()
							.put("$inc", new Json("specimens", 1))
							.put("$setOnInsert",
									new Json()
											.put("_id", specimen.getString("commons"))
											.put("name", data.getString("commons"))
							),
					new UpdateOptions().upsert(true)
			);
		}

		return new Json("ok", true).put("id", specimen.getId());
	}

	public static Json edit(Json data, Users user) {

		Json rez = new Json();
		Date date = new Date();

		List<Bson> filters = new ArrayList<>();
		if (!user.getEditor()) {
			filters.add(Filters.eq("user", user.getId()));
		}
		filters.add(Filters.eq("_id", data.getId()));


		Json set = new Json("update", date);

		Json errors = new Json();

		Json species = Db.findById("Species", data.getString("species"));
		if (species == null) {
			errors.put("species", data.getString("species") == null ? "EMPTY" : "UNKNOWN");
		} else {
			set.put("species", species.getId());
			set.put("family", species.getString("family"));
		}

		if (data.containsKey("title")) {
			set.put("title",
					data.getString("title", "").equals("") ?
							species.getString("name") :
							Fx.normalize(data.getString("title", ""))
			);
		}

		if (data.containsKey("text")) {
			set.put("text", Fx.normalize(data.getText("text")));
		}

		if (data.containsKey("commons")) {
			set.put("commons", Fx.cleanURL(species.getString("commons")));
		}


		if (data.containsKey("images")) {
			List<String> images = data.getList("images");
			if (images == null || images.size() == 0) {
				errors.put("images", "EMPTY");
			} else {
				List<String> errors_images = new ArrayList<>();
				for (String image : images) {
					if (!Db.exists("BlobFiles", Filters.eq("_id", image))) {
						errors_images.add(image);
					}
				}
				if (errors_images.size() > 0) {
					errors.put("images", errors_images);
				} else {
					set.put("images", images);
				}
			}
		}

		if (data.containsKey("location")) {
			Json location = data.getJson("location");
			if (!testLocation(location)) {
				errors.put("location", "INVALID");
			} else {
				set.put("location", location);
			}
		}
		if (!Db.exists("Specimens", Filters.eq("_id", data.getId()))) {
			errors.put("id", "UNKNOWN");
		}

		if (!errors.isEmpty()) {
			return new Json("errors", errors);
		}

		Json before = Db.findOneAndUpdate("Specimens", Filters.and(filters), new Json("$set", set),
				new FindOneAndUpdateOptions().returnDocument(ReturnDocument.BEFORE)
		);

		if (set.containsKey("species") && !before.getString("species", "").equals(set.getString("species", ""))) {
			Db.updateOne("Species", Filters.eq("_id", species.getId()), new Json("$inc", new Json("specimens", -1)));
		}

		if (set.containsKey("commons") && !before.getString("commons", "").equals(set.getString("commons"))) {

			if (before.getString("commons", "").equals("")) {
				Db.updateOne("Commons", Filters.eq("_id", before.getString("commons")),
						new Json()
								.put("$inc", new Json("specimens", -1))
				);
			}

			Db.updateOne("Commons", Filters.eq("_id", set.getString("commons")),
					new Json()
							.put("$inc", new Json("specimens", 1))
							.put("$setOnInsert",
									new Json()
											.put("_id", set.getString("commons"))
											.put("name", data.getString("commons"))
							),
					new UpdateOptions().upsert(true)
			);
		}

		return rez;
	}

	private static boolean testLocation(Json location) {
		if (location == null) {
			return false;
		}
		if (location.getDouble("lat", Double.MAX_VALUE) > 90 && location.getDouble("lat", Double.MIN_VALUE) < -90) {
			return false;
		}
		if (location.getDouble("lon", Double.MAX_VALUE) > 180 && location.getDouble("lon", Double.MIN_VALUE) < -180) {
			return false;
		}
		if (location.getChoice("type", "Point", "Polygon") == null) {
			return false;
		}
		if (location.getList("coordinates") == null) {
			return false;
		}
		if (location.getString("type").equals("Polygon") && location.getList("coordinates").size() < 3) {
			return false;
		}
		return true;
	}
}
