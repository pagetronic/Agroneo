/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package com.agroneo.web;

import live.page.web.system.Settings;
import live.page.web.system.db.Db;
import live.page.web.system.servlet.BaseServlet;
import live.page.web.system.servlet.wrapper.BaseServletRequest;
import live.page.web.system.servlet.wrapper.BaseServletResponse;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import java.io.IOException;

@WebServlet(name = "Catch All", urlPatterns = {"/", "/*"})
public class CatchAll extends BaseServlet {
	@Override
	public void doService(BaseServletRequest req, BaseServletResponse resp) throws IOException, ServletException {

		if (req.getServerName().equals(Settings.HOST_CDN)) {
			resp.sendError(404, "Not found");
			return;
		}

		if (req.getRequestURI().equals("/")) {
			req.getRequestDispatcher("/index").forward(req, resp);
		} else if (req.getRequestURI().matches("/(questions|forum)/.*?([A-Z0-9]{" + Db.DB_KEY_LENGTH + "})$")) {
			req.getRequestDispatcher("/threads").forward(req, resp);
		} else if (req.getRequestURI().startsWith("/questions") || req.getRequestURI().startsWith("/forum")) {
			req.getRequestDispatcher("/forums").forward(req, resp);
		} else if (req.getParameter("edit") != null) {
			req.getRequestDispatcher("/edit").forward(req, resp);
		} else {
			req.getRequestDispatcher("/pages").forward(req, resp);
		}
	}

}