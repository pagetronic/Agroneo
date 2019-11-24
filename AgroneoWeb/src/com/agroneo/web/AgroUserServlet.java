/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package com.agroneo.web;

import com.agroneo.web.gaia.utils.SpecimensAggregator;
import com.mongodb.client.model.Filters;
import live.page.web.servlet.wrapper.WebServletRequest;
import live.page.web.servlet.wrapper.WebServletResponse;
import live.page.web.users.UserServlet;

import javax.servlet.annotation.WebServlet;
import java.io.IOException;

@WebServlet(urlPatterns = {"/users/*"})
public class AgroUserServlet extends UserServlet {
	@Override
	public void extra(WebServletRequest req, WebServletResponse resp) throws IOException {
		req.setAttribute("specimens", SpecimensAggregator.getSpecimens(Filters.eq("users", req.getId()), req.getString("sort", "-date"), req.getString("paging", null)));
	}

}
