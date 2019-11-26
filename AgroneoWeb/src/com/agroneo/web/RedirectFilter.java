/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package com.agroneo.web;

import live.page.web.system.servlet.utils.ServletUtils;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@WebFilter(asyncSupported = true, displayName = "redirect", urlPatterns = {"/", "/*"})
public class RedirectFilter implements Filter {

	@Override
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {


		String host = req.getServerName();
		String requestURI = ((HttpServletRequest) req).getRequestURI();
		if (host == null) {
			host = "127.0.0.1";
		}


		if (requestURI.contains("/phytotherapie")) {
			ServletUtils.redirect301("https://renseigner.com/sante" + requestURI, resp);
			return;

		}

		if (requestURI.startsWith("/plantes/plantes-medicinales")) {
			ServletUtils.redirect301("https://renseigner.com/sante" + requestURI.replace("/plantes/plantes-medicinales", "/sante/phytotherapie"), resp);
			return;

		}


		if (host.equals("agroneo.com") && !requestURI.equals("/robots.txt") && !requestURI.equals("/") && !requestURI.equals("/oauth")) {

			String queryString = ((HttpServletRequest) req).getQueryString();

			ServletUtils.redirect301("https://fr.agroneo.com" + requestURI + (queryString != null ? "?" + queryString : ""), resp);
			return;
		}

		chain.doFilter(req, resp);

	}

	@Override
	public void destroy() {

	}

	@Override
	public void init(FilterConfig filterConfig) {

	}
}