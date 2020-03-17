/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package com.agroneo.web.template;

import live.page.web.system.cosmetic.tmpl.BaseTemplate;

import javax.servlet.annotation.WebListener;

@WebListener
public class AgroTemplate extends BaseTemplate {
	public AgroTemplate() {
		setTemplate(this);
	}

	@Override
	public Class[] getUserDirective() {
		return new Class[]{GaiaTitle.class};
	}

	@Override
	public Class getUserFx() {
		return FxTemplate.class;
	}

}
