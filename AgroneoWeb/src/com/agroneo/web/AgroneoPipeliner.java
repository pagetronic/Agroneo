/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package com.agroneo.web;

import com.agroneo.web.gaia.utils.SpecimensAggregator;
import live.page.web.system.db.tags.DbTagsUtils;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class AgroneoPipeliner implements ServletContextListener {

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		DbTagsUtils.addPipeliner("specimens", SpecimensAggregator.SearchSpecimensPipeline.class);
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {

	}
}
