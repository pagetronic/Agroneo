/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package com.agroneo.tropicos;

import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Sorts;
import live.page.web.db.Db;
import live.page.web.utils.Fx;
import live.page.web.utils.json.Json;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IADataTropicos {


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
		String type = findType(specimen);
		String description = specimen.getText("text", "");
		List<String> commons = new ArrayList<>();

		for (String common : commonsName(description)) {
			String url = Fx.cleanURL(common).toLowerCase();
			Json cmm = Db.findOneAndUpdate("Commons", Filters.eq("_id", url),
					new Json()
							.put("$inc", new Json("specimens", 1))
							.put("$setOnInsert", new Json("_id", url).put("name", common)),
					new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER));
			commons.add(cmm.getId());
		}

		List<Double> tall = findTall(description);
		List<Double> large = findLarge(description);

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

	public static String findType(Json specimen) {

		String family = "";
		if (specimen.getJson("family") != null) {
			family = specimen.getJson("family").getString("name");
		}
		if (family.equals("Hedwigiaceae") || family.equals("Pterobryaceae") || family.equals("Amblystegiaceae") || family.equals("Andreaeaceae") || family.equals("Brachytheciaceae")) {
			return "moss";
		}

		if (family.equals("Cactaceae")) {
			return "cactus";
		}

		if (family.equals("Agaricaceae") || family.equals("Boletaceae")) {

			return "mushroom";
		}

		if (family.equals("Orchidaceae")) {
			return "orchids";
		}

		if (family.equals("Tectariaceae") || family.equals("Aspleniaceae") || family.equals("Blechnaceae") || family.equals("Anemiaceae")) {

			return "fern";
		}
		String description = specimen.getText("text", "");


		if (testType(description, Arrays.asList(
				Pattern.compile("^Tall tree"),
				Pattern.compile("^Large tree"),
				Pattern.compile("^Tall tree"),
				Pattern.compile("^Slender tree"),
				Pattern.compile("^Grand arbre"),
				Pattern.compile("^Canopy tree"),
				Pattern.compile("^(Well)? branched tree"),
				Pattern.compile("canopy .* tree", Pattern.CASE_INSENSITIVE)
		))) {
			return "large tree";
		}


		if (testType(description, Arrays.asList(Pattern.compile("^Petit arbre"),
				Pattern.compile("^Small tree")
		))) {
			return "small tree";
		}


		if (testType(description, Arrays.asList(
				Pattern.compile("^Tree "),
				Pattern.compile("^Árbol"),
				Pattern.compile("^Arbol"),
				Pattern.compile("^Arbre"),
				Pattern.compile("^Arbres? ramifié"),
				Pattern.compile("^Sparsely branched tree"),
				Pattern.compile("^Branched tree")
		))) {
			return "tree";
		}


		if (testType(description, Arrays.asList(
				Pattern.compile("^Treelet"),
				Pattern.compile("^Petit arbuste"),
				Pattern.compile("^Arbrisseau"),
				Pattern.compile("^Suffrutex"),
				Pattern.compile("^Sub-?shrub"),
				Pattern.compile("^Sub-?arbusto"),

				Pattern.compile("^Sufr[úu]tice"),
				Pattern.compile("^Subfrutice"),
				Pattern.compile("^Subfrútice"),
				Pattern.compile("^Suffruticose"),
				Pattern.compile("Small shrub", Pattern.CASE_INSENSITIVE),
				Pattern.compile("subshrub", Pattern.CASE_INSENSITIVE)
		))) {
			return "small shrub";
		}


		if (testType(description, Arrays.asList(

				Pattern.compile("^Arbuste"),
				Pattern.compile("^Abuste"),
				Pattern.compile("^Arbusto"),
				Pattern.compile("^Shrub"),
				Pattern.compile("^Spreading shrub"),
				Pattern.compile("^Sparsely branched shrub"),
				Pattern.compile("^[0-9.]+ c?m shrub"),
				Pattern.compile("^([a-zA-Z\\-]+) shrub"),
				Pattern.compile("shrub[., ]", Pattern.CASE_INSENSITIVE)
		))) {
			return "shrub";
		}

		if (testType(description, Arrays.asList(
				Pattern.compile("^([a-zA-Z\\-]+ )?Woody vine", Pattern.CASE_INSENSITIVE),
				Pattern.compile("^Vine"),
				Pattern.compile("^Vigne"),
				Pattern.compile("^Bejuco"),
				Pattern.compile("Vine", Pattern.CASE_INSENSITIVE)
		))) {
			return "vine";
		}


		if (testType(description, Arrays.asList(
				Pattern.compile("Herbacée rampante", Pattern.CASE_INSENSITIVE)
		))) {
			return "creeping";
		}

		if (testType(description, Arrays.asList(Pattern.compile("^Bush"),
				Pattern.compile("^Buisson"),
				Pattern.compile("^Bush")
		))) {
			return "bush";
		}


		if (testType(description, Arrays.asList(
				Pattern.compile("^Giant herb"),
				Pattern.compile("^Erect herb"),
				Pattern.compile("^Large herb"),
				Pattern.compile("^Grande herbacée"),
				Pattern.compile("^Grande herbe")
		))) {
			return "large herb";
		}


		if (testType(description, Arrays.asList(
				Pattern.compile("Herbe rampante", Pattern.CASE_INSENSITIVE),
				Pattern.compile("^Creeping", Pattern.CASE_INSENSITIVE)
		))) {
			return "creeping";
		}

		if (testType(description, Arrays.asList(
				Pattern.compile("^Small herb"),
				Pattern.compile("^Petite herbacée")
		))) {
			return "small herb";
		}


		if (testType(description, Arrays.asList(
				Pattern.compile("^Hierba"),
				Pattern.compile("^Herbe"),
				Pattern.compile("^Creeping herb"),
				Pattern.compile("^Prostrate herb"),
				Pattern.compile("^Herb"),
				Pattern.compile("^Herbacée"),
				Pattern.compile("^Plante herbacée"),
				Pattern.compile("^Crassulescent herb"),
				Pattern.compile("^Terrestrial herb"),
				Pattern.compile("^Annual weed"),
				Pattern.compile("^Garden weed"),
				Pattern.compile("^Weed in garden"),
				Pattern.compile("^Weed"),
				Pattern.compile("^[0-9.]+ c?m herb"),
				Pattern.compile("stiff-stemmed plant"),
				Pattern.compile("Stems procumbent", Pattern.CASE_INSENSITIVE)
		))) {
			return "herb";
		}


		if (testType(description, Arrays.asList(
				Pattern.compile("^Epiphytic herb"),
				Pattern.compile("^Epiphyte"),
				Pattern.compile("^épiphyte"),
				Pattern.compile("^Epífito"),
				Pattern.compile("^Ep[ií]fita")
		))) {
			return "epiphytic herb";
		}


		if (testType(description, Arrays.asList(
				Pattern.compile("^Aquatic herb")
		))) {
			return "aquatic herb";
		}


		if (testType(description, Arrays.asList(
				Pattern.compile("^Spreading herb")
		))) {
			return "spreading herb";
		}


		if (testType(description, Arrays.asList(
				Pattern.compile("^Gramínea"),
				Pattern.compile("^Savanna"),
				Pattern.compile("^Graminée"),
				Pattern.compile("^Arborescent herb")
		))) {
			return "grasses";
		}


		if (testType(description, Arrays.asList(
				Pattern.compile("^Prolific climber"),
				Pattern.compile("^Climber"),
				Pattern.compile("^Trepadora")
		))) {
			return "climber";
		}


		if (testType(description, Arrays.asList(
				Pattern.compile("^Grande liane"),
				Pattern.compile("^Large liana"),
				Pattern.compile("^Petite liane"),
				Pattern.compile("^([a-zA-Z\\-]+) liane"),
				Pattern.compile("^Liane"),
				Pattern.compile("^Liana"),
				Pattern.compile("^([a-zA-Z\\-]+) Liana"),
				Pattern.compile("^Woody liana"),
				Pattern.compile("^Espèce lianescente"),
				Pattern.compile("^Plante lianescente")
		))) {
			return "liana";
		}


		if (testType(description, Arrays.asList(
				Pattern.compile("^Musgo"),
				Pattern.compile("^Mousse"),
				Pattern.compile("^Moss"),
				Pattern.compile("^Moist")
		))) {
			return "moss";
		}


		if (testType(description, Arrays.asList(
				Pattern.compile("^Palm"),
				Pattern.compile("^Palmera"),
				Pattern.compile("^Palmier")
		))) {
			return "palm";
		}


		if (testType(description, Arrays.asList(
				Pattern.compile("^Succulent"),
				Pattern.compile("^([a-zA-Z\\-]+) succulent", Pattern.CASE_INSENSITIVE),
				Pattern.compile("^Succulente"),
				Pattern.compile("^Succulenta")
		))) {
			return "succulent";
		}


		if (testType(description, Arrays.asList(
				Pattern.compile("aquatic", Pattern.CASE_INSENSITIVE),
				Pattern.compile("aquatique", Pattern.CASE_INSENSITIVE)
		))) {
			return "aquatic";
		}


		if (testType(description, Arrays.asList(
				Pattern.compile("cluster", Pattern.CASE_INSENSITIVE),
				Pattern.compile("Inflorescences pendent", Pattern.CASE_INSENSITIVE)
		))) {
			return "cluster";
		}


		return lastTest(description);
	}

	private static String lastTest(String description) {

		if (testType(description, Arrays.asList(
				Pattern.compile("rampante", Pattern.CASE_INSENSITIVE),
				Pattern.compile("creeping", Pattern.CASE_INSENSITIVE),
				Pattern.compile("slender", Pattern.CASE_INSENSITIVE)
		))) {
			return "creeping";
		}

		if (testType(description, Arrays.asList(
				Pattern.compile("flowers? ", Pattern.CASE_INSENSITIVE),
				Pattern.compile("fleur? ", Pattern.CASE_INSENSITIVE)
		))) {
			return "flower";
		}


		if (testType(description, Arrays.asList(

				Pattern.compile("treelet", Pattern.CASE_INSENSITIVE)
		))) {
			return "small shrub";
		}

		if (testType(description, Arrays.asList(

				Pattern.compile("shrubs?", Pattern.CASE_INSENSITIVE)
		))) {
			return "shrub";
		}

		return null;
	}

	private static boolean testType(String description, List<Pattern> pats) {
		for (Pattern pattern : pats) {
			if (pattern.matcher(description).find()) {
				return true;
			}
		}
		return false;
	}


	public static List<Double> findTall(String description) {

		String tor = "[ ]?+([0-9.]+)[ ]?+(à|to|\\-|\\+/\\-)?[ ]?+([0-9]+)?[ ]?+";
		String unitr = "[ ]?+(cm|m|m\\.|cm\\.|ft|metros|mètre|metre|mètres|metres)";

		List<Double> sizes = new ArrayList<>();
		description = cleaner(description);
		//Fx.log(description);

		String patter = "" + tor + "" + unitr + "?[ ]?+";
		for (String pat : new String[]{
				"(!?on tree)" + patter + "(tall|high|height|long|de\\saltura|de\\salto|de\\sporte|de long|de haut|haut|\\-|–|herb|shrub|tree|lianescent)",
				"^[a-z]+ de " + patter
		}) {
			Pattern find = Pattern.compile(pat, Pattern.CASE_INSENSITIVE);
			Matcher matcher = find.matcher(description);

			if (matcher.find()) {
				if (matcher.group(2) == null && matcher.group(3) == null && matcher.group(4) == null) {
					continue;
				}
				double unit = 1;
				if (matcher.group(4) != null) {
					if (matcher.group(4).equals("cm")) {
						unit = 1 / 100D;
					} else if (matcher.group(4).equals("ft")) {
						unit = 0.3048D;
					}
				}
				String num = matcher.group(1);
				String to = matcher.group(3);
				if (num.equals(".")) {
					num = to;
					to = null;
				}
				if (num != null) {
					try {
						sizes.add(Math.round(Double.valueOf(num + (num.endsWith(".") && to != null ? to : "")) * unit * 1000D) / 1000D);
					} catch (Exception ignored) {
					}
				}
				if (to != null && (num == null || !num.endsWith("."))) {
					try {
						sizes.add(Math.round(Double.valueOf(to) * unit * 1000D) / 1000D);
					} catch (Exception ignored) {
					}
				}

			}
			if (sizes.size() > 0) {
				break;
			}
		}

		return sizes;

	}

	public static List<Double> findLarge(String description) {

		String tor = "([0-9.]+)[ ]?+(à|to|\\-|\\+/\\-)?[ ]?+([0-9.]+)?[ ]?+";

		String unitr = "[ ]?+(cm|m|m\\.|cm\\.|ft|metros|mètre|metre|mètres|metres)";

		List<Double> sizes = new ArrayList<>();

		description = cleaner(description);


		for (String pat : new String[]{
				"(dbh )[ ]?+:?[ ]?+" + tor + unitr + "?[ ]?+(.*)?",
				"(dbh )?[ ]?+" + tor + unitr + "?[ ]?+(xxx)?(dbh|DAP)",
				"(dbh )?[ ]?+:?[ ]?+" + tor + unitr + "?[ ]?+(de )?(diamètre|large)",
				"(diamètre d)[^0-9]+" + tor + unitr + "?[ ]?+(.*)?"}) {
			Pattern find = Pattern.compile(pat, Pattern.CASE_INSENSITIVE);
			Matcher matcher = find.matcher(description);

			if (matcher.find()) {
				if (matcher.group(1) == null && matcher.group(7) == null) {
					continue;
				}

				if (matcher.group(2) == null && matcher.group(3) == null && matcher.group(4) == null) {
					continue;
				}
				double unit = 1;
				if (matcher.group(5) != null) {
					if (matcher.group(5).equals("cm")) {
						unit = 1 / 100D;
					} else if (matcher.group(5).equals("ft")) {
						unit = 0.3048D;
					}
				}

				if (matcher.group(2) != null) {
					try {
						sizes.add(Math.round(Double.valueOf(matcher.group(2) + (matcher.group(2).endsWith(".") && matcher.group(4) != null ? matcher.group(4) : "")) * unit * 1000D) / 1000D);
					} catch (Exception ignored) {
					}
				}
				if (matcher.group(4) != null && (matcher.group(2) == null || !matcher.group(2).endsWith("."))) {
					try {
						sizes.add(Math.round(Double.valueOf(matcher.group(4)) * unit * 1000D) / 1000D);
					} catch (Exception ignored) {
					}
				}

			}

			if (sizes.size() > 0) {
				break;
			}
		}

		return sizes;

	}

	private static String cleaner(String description) {

		description = description.toLowerCase();
		description = description.replaceAll("([0-9]+)[\\s]?+,[ ]?+([0-9]+)", "$1.$2");
		description = description.replaceAll("([0-9]+)m([0-9]+)", "$1.$2 m");
		description = description.replaceAll("([0-9]+)[ ]?+(\\.)?[ ]?+([0-9]+)?[ ]?+(cm|m|ft)", "$1$2$3 $4");
		description = description.replaceAll("on ([0-9]+)", "");
		description = description.replaceAll("\\. ([0-9]+)", " $1");

		//Fx.log(description);
		return description;
	}


	public static List<String> commonsName(String description) {
		description = description.replaceAll("[ ]?+\\([ ]?+([^)]+)[ ]?+\\)[ ]?+", "");
		description = description.replaceAll("\n", ".");

		String guill = "\"”“«»";
		String reject = "([^\".;–\\-" + guill + "]+)";
		List<String> commonsName = new ArrayList<>();
		for (String pattern : new String[]{
				"Noms? v[ea]rnaculaires?:? ?[" + guill + "]?" + reject + "[" + guill + "]?",
				"Noms? communs?:? ?[" + guill + "]?" + reject + "[" + guill + "]?",
				"Nombre común:? ?[" + guill + "]?" + reject + "[" + guill + "]?",
				"Nombres comunes:? ?[" + guill + "]?" + reject + "[" + guill + "]?",
				"Nombres vernáculos:? ?[" + guill + "]?" + reject + "[" + guill + "]?",
				"Nombre vernáculo:? ?[" + guill + "]?" + reject + "[" + guill + "]?",
				"Nomes? vernaculares?:? ?[" + guill + "]?" + reject + "[" + guill + "]?",
				" names? ?o?f? ?:? ?[" + guill + "]?" + reject + "[" + guill + "]?"
		}) {
			Pattern find = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
			Matcher matcher = find.matcher(description);
			if (matcher.find()) {
				for (int i = 1; i <= matcher.groupCount(); i++) {

					String common = matcher.group(i).replace("\"", "").replace(".", "").replaceAll("[ ]?+:[ ]?+", "")
							.replaceAll("\\(([^)]+)\\)", "").replaceAll("^[ ]?+", "").replaceAll("[ ]?+$", "");

					for (String commo : common.split("( or | ou |/|[ ]?+,[ ]?+)")) {
						commo = Fx.ucfirst(commo);
						if (!commo.equals("") && !commonsName.contains(commo) && commo.split(" ").length <= 3) {
							commonsName.add(commo);
							Fx.log(commo);

						}
					}

				}
			}
		}
		return commonsName;

	}


}
