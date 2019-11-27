/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package com.agroneo.web.db;

import live.page.web.system.Settings;
import live.page.web.system.db.IndexBuilder;
import live.page.web.system.db.IndexBuilder.IndexData;
import live.page.web.system.json.Json;

import javax.servlet.annotation.WebListener;


@WebListener
public class Indexes {

	public Indexes() {

		String def_lang = Settings.getLangs().get(0);
		IndexBuilder.addIndex("Species",

				IndexData.get(new Json("genus", 1).put("specimens", 1).put("family", 1), "grouper"),
				IndexData.get(new Json("tId", -1), "tropicosId"),
				IndexData.get(new Json("name", 1), "name"),
				IndexData.get(new Json("synonym", 1), "synonym"),
				IndexData.get(new Json("accepted", 1), "accepted"),
				IndexData.get(new Json("family", 1), "family"),
				IndexData.get(new Json("genus", 1), "genus"),
				IndexData.get(new Json("family", 1).put("genus", 1), "family_genus"),
				IndexData.get(new Json("sym", 1), "sym"),
				IndexData.get(new Json("specimens", 1), "specimens"),
				IndexData.get(new Json("rank", 1), "rank"),
				IndexData.get(new Json("genus", 1).put("rank", 1), "genus_rank"),

				IndexData.get(new Json("sym", 1).put("rank", 1).put("update", 1).put("specimens", -1), "update_api"),
				IndexData.get(new Json("update", 1).put("specimens", -1), "update_specimens"),

				IndexData.getText(new Json("name", 2).put("synonym", 1).put("commons", 2), "search", def_lang),

				IndexData.get(new Json("type", 1), "type"),
				IndexData.get(new Json("commons", 1), "commons"),
				IndexData.get(new Json().put("size.tall.min", 1).put("size.tall.max", 1).put("size.large.min", 1).put("size.large.max", 1), "size")

		);
		IndexBuilder.addIndex("Commons",
				IndexData.get(new Json("name", 1), "name"),
				IndexData.get(new Json("specimens", 1), "specimens"),
				IndexData.getText(new Json("name", 2), "search", def_lang)


		);

		IndexBuilder.addIndex("Specimens",

				IndexData.get(new Json("replies", 1).put("last.date", -1), "replies_date"),

				IndexData.get(new Json("tId", 1), "tropicosId"),

				IndexData.get(new Json("species", 1), "species"),
				IndexData.get(new Json("date", -1), "-date"),
				IndexData.get(new Json("date", 1), "date"),
				IndexData.get(new Json("users", 1), "users"),
				IndexData.get(new Json("location", "2dsphere"), "location"),
				IndexData.get(new Json("location", "hashed"), "doublons"),
				IndexData.get(new Json("update", -1), "update"),
				IndexData.get(new Json("family", 1), "family"),
				IndexData.get(new Json("family", 1).put("species", 1).put("date", 1), "family_species"),
				IndexData.get(new Json("family", 1).put("species", 1).put("date", -1), "-family_species"),
				IndexData.getText(new Json("text", 1).put("title", 2), "search", def_lang),

				IndexData.get(new Json("type", 1), "type"),
				IndexData.get(new Json("commons", 1), "commons"),
				IndexData.get(new Json("large.min", 1).put("large.max", 1), "large"),
				IndexData.get(new Json("tall.min", 1).put("tall.max", 1), "tall")

		);


		IndexBuilder.addIndex("Families",
				IndexData.get(new Json("name", 1), "name"),
				IndexData.get(new Json("_id", 1).put("species", -1), "id_species"),
				IndexData.get(new Json("specimens", -1), "-specimens"),
				IndexData.get(new Json("species", -1), "-species"),
				IndexData.get(new Json("specimens", 1).put("species", 1), "specimens_species"),
				IndexData.getText(new Json("name", 1), "search", def_lang)

		);

	}
}

