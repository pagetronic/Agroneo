/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package com.agroneo.web.moon.utils;

public enum MoonEventType {

	NEW_MOON(0.0d) {
		@Override
		public MoonEventType opposite() {
			return FULL_MOON;
		}
	}, FIRST_QUARTER_MOON(0.25d) {
		@Override
		public MoonEventType opposite() {
			return LAST_QUARTER_MOON;
		}
	}, FULL_MOON(0.5d) {
		@Override
		public MoonEventType opposite() {
			return NEW_MOON;
		}
	}, LAST_QUARTER_MOON(0.75d) {
		@Override
		public MoonEventType opposite() {
			return FIRST_QUARTER_MOON;
		}
	};

	private final double _fraction;

	MoonEventType(double fraction) {
		_fraction = fraction;
	}

	public double getFraction() {
		return _fraction;
	}

	public String getDisplayName() {
		final char[] chars = name().toCharArray();
		final StringBuilder displayName = new StringBuilder(chars.length);
		boolean initial = true;
		for (final char c : chars) {
			if (initial) {
				initial = false;
				displayName.append(c);
			} else {
				if (c == '_') {
					initial = true;
					displayName.append(' ');
				} else {
					displayName.append(Character.toLowerCase(c));
				}
			}
		}

		return displayName.toString();
	}

	public abstract MoonEventType opposite();

}
