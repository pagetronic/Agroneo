package com.agroneo.web.api;

import javax.servlet.annotation.WebListener;
import java.util.Arrays;

@WebListener
public class Scopes {
	public Scopes() {
		live.page.web.api.Scopes.scopes.addAll(Arrays.asList(
				"gaia",
				"email",
				"pm",
				"threads",
				"accounts"
		));
	}
}
