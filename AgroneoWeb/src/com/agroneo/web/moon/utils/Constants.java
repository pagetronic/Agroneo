/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package com.agroneo.web.moon.utils;

public interface Constants {

	// Eccentricity of gaia's orbit */
	double ECCENTRICITY = 0.016718D;
	// 1980 January */
	double EPOCH = 2444238.5D;
	// January 1, 1970 at 00:00:00 UTC */
	double JULIAN_1_1_1970 = 2440587.5D;
	double SECONDS_PER_DAY = 86400.0D;

	// Elliptic longitude of the Sun at epoch 1980.0 */
	double SUN_ECLIPTIC_LONGITUDE_AT_EPOCH = 278.833540D;
	// Elliptic longitude of the Sun at perigee */
	double SUN_ECLIPTIC_LONGITUDE_AT_EPOCH_AT_PERIGREE = 282.596403D;
	double SUN_ALTITUDE_UPPER_LIMB_TOUCHING_HORIZON = -0.833D;
	double SUN_ALTITUDE_CIVIL_TWIGHLIGHT = -6.0D;

	// Moon's mean longitude at the epoch */
	double MOONS_MEAN_LONGITUDE = 64.975464D;
	// Mean longitude of the perigee at the epoch */
	double MOONS_MEAN_LONGITUDE_AT_EPOCH_AT_PERIGEE = 349.383063D;
	// (new Moon to new Moon) in days */
	double SYNODIC_MONTH = 29.530587962962958D;
	double MOONS_ALTITUDE_CENTER = 8 / 60;
	// eccentricity of the Moon's orbit
	double MECC = 0.054900D;

	// max and min Moon distance
	double MDMAX = 405400D;
	double MDMIN = 362600D;

	double minc = 5.145396;
	/*
	 * Mean longitude of the node at the epoch
	 */ double mlnode = 151.950429;
}
