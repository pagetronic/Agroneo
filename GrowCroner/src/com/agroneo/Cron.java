/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package com.agroneo;

import com.agroneo.tropicos.api.TropicosApi;
import com.agroneo.tropicos.ia.TropicosIA;
import live.page.notice.Notices;
import live.page.web.utils.Fx;

public class Cron {

	public static void main(String[] args) {
		if (Fx.IS_DEBUG || (args.length > 0 && args[0].equals("update"))) {
			TropicosIA.update();
		} else {
			TropicosApi.run();
			Notices.cron();
		}
	}
}
