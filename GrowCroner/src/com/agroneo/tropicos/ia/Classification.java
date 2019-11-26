/*
 * Copyright (c) 2019. PAGE and Sons
 */

package com.agroneo.tropicos.ia;

import live.page.web.system.json.Json;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class Classification {

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

}
