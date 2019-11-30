/*
 * Copyright (c) 2019. PAGE and Sons
 */
package com.agroneo.tropicos.api;

import com.agroneo.tropicos.ia.TropicosIA;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.client.model.*;
import com.mongodb.client.result.UpdateResult;
import live.page.web.blobs.BlobsUtils;
import live.page.web.content.notices.Notifier;
import live.page.web.system.Settings;
import live.page.web.system.db.Db;
import live.page.web.system.json.Json;
import live.page.web.utils.Fx;
import live.page.web.utils.http.HttpClient;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/*

 */
public class TropicosApi {

	private final static int pause_min = 600;
	private static int pause = pause_min;
	private final static ExecutorService control = Executors.newSingleThreadExecutor();

	static {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> Fx.shutdownService(control)));
	}


	public static void run() {
		Fx.log("Tropicos Api started " + new Date().toString());
		control.submit(() -> {
			Thread.currentThread().setName("tropicos-shuddle");
			try {
				while (true) {
					lastNames();
					Thread.sleep(pause);
					updateName();
					Thread.sleep(pause);
				}
			} catch (InterruptedException e) {
				Fx.log("Tropicos Api stopped " + new Date().toString());
			}
		});
	}


	private static void lastNames() throws InterruptedException {

		Fx.log("Tropicos Api lastNames");
		Json lastspecies = Db.find("Species").sort(Sorts.descending("tId")).first();
		int startid = 0;
		if (lastspecies != null) {
			startid = lastspecies.getInteger("tId") + 1;
		}
		JsonArray listNameid = getTropicos("Name/list", "startid=" + startid, "pagesize=100").getAsJsonArray();
		if (listNameid != null && !listNameid.get(0).getAsJsonObject().keySet().contains("Error")) {
			for (JsonElement nameIdObj : listNameid) {
				int nameid = nameIdObj.getAsJsonObject().get("NameId").getAsInt();
				if (!getNameid(nameid, null)) {
					break;
				}
			}
			lastNames();
		}

	}

	private static void updateName() throws InterruptedException {

		if (control.isTerminated() || control.isShutdown()) {
			throw new InterruptedException("");
		}

		Fx.log("Tropicos Api updateName");
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MONTH, -6);
		List<Json> species = Db.find("Species",
				Filters.and(
						Filters.ne("sym", "*"), Filters.ne("sym", "**"), Filters.ne("sym", "***"),
						Filters.eq("rank", "species"),
						Filters.or(Filters.eq("update", null), Filters.lt("update", cal.getTime()))
				)
		).hint(new Json("sym", 1).put("rank", 1).put("update", 1).put("specimens", -1)).sort(Sorts.orderBy(Sorts.ascending("update"), Sorts.descending("specimens"))).limit(300).into(new ArrayList<>());


		for (Json specie : species) {
			if (!getNameid(specie.getInteger("tId"), specie.getId())) {
				break;
			}
			if (control.isTerminated() || control.isShutdown()) {
				throw new InterruptedException("");
			}
		}


	}

	private static boolean getNameid(int nameid, String species_id) throws InterruptedException {

		if (control.isTerminated() || control.isShutdown()) {
			throw new InterruptedException("");
		}

		Json species = getSpecies(nameid);

		if (control.isTerminated() || control.isShutdown()) {
			throw new InterruptedException("");
		}

		if (species == null) {
			return false;
		}
		//!! = nom. cons., ! = Legitimate, ** = Invalid, *** = nom. rej., * = Illegitimate
		if (species_id == null) {
			species_id = getSpeciesUrl(species.getString("name", "specy"), species.getString("sym", ""));
		}

		List<Json> specimens = new ArrayList<>();

		if (!species.getString("rank", "").equals("") &&
				!species.getString("rank", "").equals("genus") &&
				!species.getString("rank", "").equals("family") &&
				!species.getString("rank", "").equals("division")) {

			List<Json> tmp_images = getImages(nameid);
			if (tmp_images == null) {
				return false;
			}

			List<Json> images = new ArrayList<>();
			for (Json image : tmp_images) {
				if (!Db.exists("Specimens", Filters.eq("tId", image.getInteger("stId")))) {
					images.add(image);
				}
			}

			if (images.size() > 0) {
				List<Json> tmp_specimens = getSpecimens(nameid, species, species_id, images);
				if (tmp_specimens == null) {
					return false;
				}
				for (Json specimen : tmp_specimens) {
					if (!Db.exists("Specimens", Filters.eq("tId", specimen.getInteger("tId")))) {
						specimens.add(specimen);
					}
				}
			}
		}

		species.put("update", new Date());

		UpdateResult rez = Db.updateOne("Species",
				Filters.eq("_id", species_id),
				new Json()
						.put("$set", species)
						.put("$inc", new Json("specimens", specimens.size()))
						.put("$setOnInsert", new Json("_id", species_id)),
				new UpdateOptions().upsert(true).bypassDocumentValidation(true)
		);

		if (!species.getString("family", "").equals("")) {
			Json inc = new Json("specimens", specimens.size());
			if (rez.getMatchedCount() == 0L) {
				inc.put("species", 1);
			}
			Db.updateOne("Families", Filters.eq("_id", species.getString("family", "").toLowerCase()),
					new Json()
							.put("$inc", inc)
							.put("$setOnInsert",
									new Json("_id", species.getString("family", "").toLowerCase())
											.put("name", Fx.ucfirst(species.getString("family", "")))
							)
					, new UpdateOptions().upsert(true)
			);
		}

		if (specimens.size() > 0) {

			Db.save("Specimens", specimens);
			specimens.forEach(TropicosIA::updateSpecimen);
			System.out.println();
			Fx.log("Tropicos : " + nameid + " / " + specimens.size() + " specimens");

			//TODO do that !
			specimens.forEach((specimen) -> notify(
					specimen.getString("species"),
					specimen.getString("family"),
					specimen.getString("title"),
					specimen.getText("text"),
					"/gaia/" + specimen.getId())
			);

		}
		System.out.print(".");

		return true;

	}

	private static String getSpeciesUrl(String name, String sym) {
		String url = Fx.cleanURL(name).toLowerCase();
		long count = 0;
		if (!sym.equals("") && !sym.equals("!") && !sym.equals("!!")) {
			count += 1;
		}
		while (Db.exists("Species", Filters.eq("_id", (count > 0) ? url + "-" + count : url))) {
			count++;
		}
		if (count > 0) {
			url += "-" + count;
		}
		return url;
	}

	private static Json getSpecies(int nameId) throws InterruptedException {

		Json species = new Json();
		JsonObject speciestp = getTropicos("Name/" + nameId).getAsJsonObject();
		if (speciestp == null || speciestp.getAsJsonObject().keySet().contains("Error")) {
			error("Species error " + nameId);
			return null;
		}
		species.put("tId", speciestp.get("NameId").getAsInt());
		species.put("name", speciestp.get("ScientificName").getAsString());

		if (speciestp.get("Family") != null) {
			species.put("family", speciestp.get("Family").getAsString().toLowerCase());
		}

		if (speciestp.get("SynonymCount") != null) {
			int synonym = Integer.valueOf(speciestp.get("SynonymCount").getAsString());
			if (synonym > 0) {
				List<String> synonyms = getSynonym(nameId);
				if (synonyms == null) {
					error("Synonym error " + nameId);
					return null;
				}
				species.put("synonym", synonyms);
			}
		}

		if (speciestp.get("AcceptedNameCount") != null) {
			int accepted = Integer.valueOf(speciestp.get("AcceptedNameCount").getAsString());
			if (accepted > 0) {
				List<String> accepteds = getAccepted(nameId);
				if (accepteds == null) {
					error("AcceptedName error " + nameId);
					return null;
				}
				species.put("accepted", accepteds);
			}
		}

		if (speciestp.get("Rank") != null) {
			species.put("rank", speciestp.get("Rank").getAsString());
		}
		if (speciestp.get("Author") != null) {
			species.put("author", speciestp.get("Author").getAsString());
		}
		if (speciestp.get("Symbol") != null) {
			species.put("sym", speciestp.get("Symbol").getAsString());
		}

		if (speciestp.get("Genus") != null) {
			species.put("genus", speciestp.get("Genus").getAsString());
		}
		return species;

	}

	private static List<Json> getImages(int nameId) throws InterruptedException {

		List<Json> images = new ArrayList<>();
		JsonArray listImages = getTropicos("Name/" + nameId + "/Images").getAsJsonArray();
		if (listImages == null) {
			error("Tropicos error Images " + nameId);
			return null;
		}

		if (!listImages.get(0).getAsJsonObject().keySet().contains("Error")) {
			for (JsonElement imageObj : listImages) {
				JsonObject imagetp = imageObj.getAsJsonObject();
				if (imagetp.get("SpecimenId") != null &&
						imagetp.get("ImageKindText") != null &&
						StringUtils.containsIgnoreCase(imagetp.get("ImageKindText").getAsString(), "photo") &&
						imagetp.get("DetailJpgUrl") != null &&
						!StringUtils.containsIgnoreCase(imagetp.get("DetailJpgUrl").getAsString(), "imageprotected")) {

					Json image = new Json();
					image.put("tId", imagetp.get("ImageId").getAsInt());
					image.put("stId", imagetp.get("SpecimenId").getAsInt());
					image.put("copyright", imagetp.get("Copyright").getAsString() + " / TropicosIA");
					image.put("url", imagetp.get("DetailJpgUrl").getAsString());
					image.put("caption", imagetp.get("Caption").getAsString());
					image.put("kind", imagetp.get("ImageKindText").getAsString());
					images.add(image);

				}
			}
		} else if (!listImages.get(0).getAsJsonObject().get("Error").getAsString().equals("No records were found")) {
			error("Tropicos error Images " + nameId);
			return null;
		}
		return images;

	}

	private static List<Json> getSpecimens(int nameId, Json species, String species_id, List<Json> images) throws InterruptedException {

		List<Json> specimens = new ArrayList<>();
		Map<Integer, List<Json>> specimens_images = new HashMap<>();
		for (Json image : images) {
			int specimenId = image.getInteger("stId");
			if (!Db.exists("Specimens", Filters.eq("tId", specimenId))) {
				List<Json> specimen_images = new ArrayList<>();
				if (specimens_images.containsKey(specimenId)) {
					specimen_images = specimens_images.get(specimenId);
				}
				specimen_images.add(image);
				specimens_images.put(specimenId, specimen_images);
			}
		}
		for (Entry<Integer, List<Json>> images_group : specimens_images.entrySet()) {
			int specimenId = images_group.getKey();

			List<Json> images_specimen = images_group.getValue();

			JsonObject data_specimen = getTropicos("Specimen/" + specimenId).getAsJsonObject();

			if (data_specimen == null) {
				error("Tropicos error Specimen " + specimenId);
				return null;
			}

			if (data_specimen.get("LatitudeDecDeg") != null && data_specimen.get("LongitudeDecDeg") != null && data_specimen.get("CollectionYear") != null) {

				Json specimen = new Json("update", new Date());
				for (Json image : images_specimen) {
					Thread.sleep(pause_min);
					String imageid = BlobsUtils.downloadToDb(
							image.getString("url"),
							new Json()
									.put("text", image.getString("caption") + " © " + image.getString("copyright"))
									.put("mobot", image.getInteger("tId"))
							, 2048);
					if (imageid == null) {
						error("Error download TropicosIA Image " + image.getString("url") + " for " + nameId);
						return null;
					}
					specimen.add("images", imageid);
				}
				specimen.put("title", species.getString("name"));
				specimen.put("species", species_id);
				specimen.put("family", species.getString("family"));
				specimen.put("tId", specimenId);
				String collector = data_specimen.get("CollectorString").getAsString();

				String[] users = collector.split("(, | & et | & )");
				Arrays.asList(users).forEach(author -> {
					Json user_db = Db.findOneAndUpdate("Users",
							Filters.and(Filters.eq("email", null), Filters.eq("name", author)),
							new Json("$setOnInsert", new Json("name", author).put("_id", Db.getKey())),
							new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER));

					specimen.add("users", user_db.getId());

				});

				specimen.put("location", new Json("type", "Point").put("coordinates", Arrays.asList(data_specimen.get("LongitudeDecDeg").getAsDouble(), data_specimen.get("LatitudeDecDeg").getAsDouble())));

				Calendar cal = Calendar.getInstance();
				cal.set(Calendar.YEAR, data_specimen.get("CollectionYear").getAsInt());
				if (data_specimen.get("CollectionMonth") != null) {
					cal.set(Calendar.MONTH, data_specimen.get("CollectionMonth").getAsInt() - 1);
				} else {
					cal.set(Calendar.MONTH, 0);
				}
				if (data_specimen.get("CollectionDay") != null) {
					cal.set(Calendar.DAY_OF_MONTH, data_specimen.get("CollectionDay").getAsInt());
				} else {
					cal.set(Calendar.DAY_OF_MONTH, 1);
				}

				specimen.put("date", cal.getTime());

				if (data_specimen.get("NoteDescription") != null) {
					specimen.put("text", Fx.normalizePost(data_specimen.get("NoteDescription").getAsString()));
				}

				specimens.add(specimen);

			}
		}
		return specimens;

	}

	private static List<String> getSynonym(int nameId) throws InterruptedException {

		List<String> synonyms = new ArrayList<>();
		JsonArray synonymstp = getTropicos("Name/" + nameId + "/Synonyms").getAsJsonArray();
		if (synonymstp == null) {
			error("Tropicos error Synonyms " + nameId);
			return null;
		}
		for (JsonElement nameIdObj : synonymstp) {
			String name = nameIdObj.getAsJsonObject().get("SynonymName").getAsJsonObject().get("ScientificName").getAsString();
			if (!synonyms.contains(name)) {
				synonyms.add(name);
			}
		}
		return synonyms;

	}

	private static List<String> getAccepted(int nameId) throws InterruptedException {

		List<String> accepted = new ArrayList<>();
		JsonArray acceptedstp = getTropicos("Name/" + nameId + "/AcceptedNames").getAsJsonArray();
		if (acceptedstp == null) {
			error("Tropicos error AcceptedNames " + nameId);
			return null;
		}
		for (JsonElement nameIdObj : acceptedstp) {
			String name = nameIdObj.getAsJsonObject().get("AcceptedName").getAsJsonObject().get("ScientificName").getAsString();
			if (!accepted.contains(name)) {
				accepted.add(name);
			}
		}
		return accepted;

	}

	private static JsonElement getTropicos(String type, String... parameters) throws InterruptedException {
		if (control.isTerminated() || control.isShutdown()) {
			throw new InterruptedException("");
		}
		StringBuilder url = new StringBuilder("http://services.tropicos.org/" + type + "?");
		for (String parameter : parameters) {
			url.append(parameter).append("&");
		}

		url.append("apikey=");
		url.append(Settings.getString("TROPICOS_API_KEY"));
		url.append("&format=json");

		while (true) {

			Thread.sleep(pause_min);

			String rez = HttpClient.get(url.toString());
			if (rez != null) {
				pause = Math.max(pause_min, pause - pause_min);
				return new JsonParser().parse(rez);
			}
			error("Error get Api");
		}
	}

	private static void error(String err) throws InterruptedException {
		Fx.log(err);
		if (pause < 60 * 60 * 1000) {
			pause += pause;
		}
		Thread.sleep(pause);
	}

	private static void notify(String species, String family, String title, String text, String url) {

		Notifier.notify(Arrays.asList("Specimens(ALL)", "Species(" + species + ")", "Families(" + family + ")"), null, title, text, url, null, 1, TimeUnit.HOURS);

	}
}

