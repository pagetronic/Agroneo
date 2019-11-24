/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package com.agroneo.web.moon.utils;

import java.util.Calendar;
import java.util.Date;

public class MoonToolPhaseAlgorithm implements Constants {

	public static double fixangle(double a) {
		return ((a) - (360.0 * (Math.floor((a) / 360.0))));
	}

	public static double node(Calendar calender) {

		double pdate = MoonUtil.toJulian(calender);

		double dayOfEpoch = pdate - EPOCH;
		double meanSunAnomaly = fixangle((360 / 365.2422) * dayOfEpoch);

		// SUN
		// Convert from perigee coordinates to epoch 1980.0
		double M = fixangle((meanSunAnomaly + SUN_ECLIPTIC_LONGITUDE_AT_EPOCH) - SUN_ECLIPTIC_LONGITUDE_AT_EPOCH_AT_PERIGREE);

		double Ec = MoonUtil.solveKeplerEquation(M, ECCENTRICITY);
		Ec = Math.sqrt((1 + ECCENTRICITY) / (1 - ECCENTRICITY)) * Math.tan(Ec / 2);
		Ec = 2 * Math.toDegrees(Math.atan(Ec)); /* True anomaly */

		double sunGeocentricEclipticLongitude = fixangle(Ec + SUN_ECLIPTIC_LONGITUDE_AT_EPOCH_AT_PERIGREE);

		// MOON
		double meanMoonLongitude = fixangle((13.1763966 * dayOfEpoch) + MOONS_MEAN_LONGITUDE);
		double meanMoonAnomaly = fixangle(meanMoonLongitude - (0.1114041 * dayOfEpoch) - MOONS_MEAN_LONGITUDE_AT_EPOCH_AT_PERIGEE);

		/* Evection */
		double Ev = 1.2739 * MoonUtil.sinFromDegree((2 * (meanMoonLongitude - sunGeocentricEclipticLongitude)) - meanMoonAnomaly);

		double annualEquation = 0.1858 * MoonUtil.sinFromDegree(M);
		/* Correction term */
		double A3 = 0.37 * MoonUtil.sinFromDegree(M);
		/* Corrected anomaly */
		double MmP = (meanMoonAnomaly + Ev) - annualEquation - A3;
		/* Correction for the equation of the center */
		double mEc = 6.2886 * MoonUtil.sinFromDegree(MmP);
		/* Another correction term */
		double A4 = 0.214 * MoonUtil.sinFromDegree(2 * MmP);

		/* Corrected longitude */
		double lP = ((meanMoonLongitude + Ev + mEc) - annualEquation) + A4;
		/* Variation */
		double V = 0.6583 * MoonUtil.sinFromDegree(2 * (lP - sunGeocentricEclipticLongitude));
		/* True longitude */
		double lPP = lP + V;

		double MN = fixangle(mlnode - (0.0529539 * dayOfEpoch));
		double NP = MN - (0.16 * MoonUtil.sinFromDegree(M));

		double node = Math.toDegrees(Math.asin(MoonUtil.sinFromDegree(lPP - NP) * MoonUtil.sinFromDegree(minc)));
		return node;
	}

	public static Moon calculate(Calendar calender) {

		double pdate = MoonUtil.toJulian(calender);

		double dayOfEpoch = pdate - EPOCH;
		double meanSunAnomaly = fixangle((360 / 365.2422) * dayOfEpoch);

		// SUN
		// Convert from perigee coordinates to epoch 1980.0
		double M = fixangle((meanSunAnomaly + SUN_ECLIPTIC_LONGITUDE_AT_EPOCH) - SUN_ECLIPTIC_LONGITUDE_AT_EPOCH_AT_PERIGREE);

		double Ec = MoonUtil.solveKeplerEquation(M, ECCENTRICITY);
		Ec = Math.sqrt((1 + ECCENTRICITY) / (1 - ECCENTRICITY)) * Math.tan(Ec / 2);
		Ec = 2 * Math.toDegrees(Math.atan(Ec)); /* True anomaly */

		double sunGeocentricEclipticLongitude = fixangle(Ec + SUN_ECLIPTIC_LONGITUDE_AT_EPOCH_AT_PERIGREE);

		// MOON
		double meanMoonLongitude = fixangle((13.1763966 * dayOfEpoch) + MOONS_MEAN_LONGITUDE);
		double meanMoonAnomaly = fixangle(meanMoonLongitude - (0.1114041 * dayOfEpoch) - MOONS_MEAN_LONGITUDE_AT_EPOCH_AT_PERIGEE);

		/* Evection */
		double Ev = 1.2739 * MoonUtil.sinFromDegree((2 * (meanMoonLongitude - sunGeocentricEclipticLongitude)) - meanMoonAnomaly);

		double annualEquation = 0.1858 * MoonUtil.sinFromDegree(M);
		/* Correction term */
		double A3 = 0.37 * MoonUtil.sinFromDegree(M);
		/* Corrected anomaly */
		double MmP = (meanMoonAnomaly + Ev) - annualEquation - A3;
		/* Correction for the equation of the center */
		double mEc = 6.2886 * MoonUtil.sinFromDegree(MmP);
		/* Another correction term */
		double A4 = 0.214 * MoonUtil.sinFromDegree(2 * MmP);

		/* Corrected longitude */
		double lP = ((meanMoonLongitude + Ev + mEc) - annualEquation) + A4;
		/* Variation */
		double V = 0.6583 * MoonUtil.sinFromDegree(2 * (lP - sunGeocentricEclipticLongitude));
		/* True longitude */
		double lPP = lP + V;

		Date lunarNode = null;
		double MN = fixangle(mlnode - (0.0529539 * dayOfEpoch));
		double NP = MN - (0.16 * MoonUtil.sinFromDegree(M));
		double node = Math.toDegrees(Math.asin(MoonUtil.sinFromDegree(lPP - NP) * MoonUtil.sinFromDegree(minc)));

		if ((node > -0.8) && (node < 0.8)) {
			Calendar pdatecal = Calendar.getInstance(calender.getTimeZone());
			pdatecal.setTimeInMillis(MoonUtil.toUnixTimestampFromJulian(pdate) * 1000);

			pdatecal.add(Calendar.HOUR_OF_DAY, -12);
			double prev_n = 10000D;
			for (int i = 0; i < (SECONDS_PER_DAY / 60); i++) {
				pdatecal.add(Calendar.MINUTE, 1);

				double n = node(pdatecal);
				if ((Math.abs(n) < 0.01) && (Math.abs(n) > Math.abs(prev_n))) {
					lunarNode = pdatecal.getTime();

					break;
				} else {
					prev_n = n;
				}
			}
		}
		double MoonAgeInDegrees = lPP - sunGeocentricEclipticLongitude;

		double moonFraction = fixangle(MoonAgeInDegrees) / 360.0;
		double moonAge = SYNODIC_MONTH * moonFraction;

		double moonDist = (((MDMAX + MDMIN) / 2) * (1 - (MECC * MECC))) / (1 + (MECC * Math.cos(Math.toRadians(MmP + mEc))));

		String moonZodiac = "PISCES";
		if (lPP < 33.18) {
			moonZodiac = "PISCES";
		} else if (lPP < 51.16) {
			moonZodiac = "ARIES";
		} else if (lPP < 93.44) {
			moonZodiac = "TAURUS";
		} else if (lPP < 119.48) {
			moonZodiac = "GEMINI";
		} else if (lPP < 135.30) {
			moonZodiac = "CANCER";
		} else if (lPP < 173.34) {
			moonZodiac = "LEO";
		} else if (lPP < 224.17) {
			moonZodiac = "VIRGO";
		} else if (lPP < 242.57) {
			moonZodiac = "LIBRA";
		} else if (lPP < 271.26) {
			moonZodiac = "SCORPIO";
		} else if (lPP < 302.49) {
			moonZodiac = "SAGITTARIUS";
		} else if (lPP < 311.72) {
			moonZodiac = "CAPRICORN";
		} else if (lPP < 348.58) {
			moonZodiac = "AQUARIUS";
		}

		return new Moon(moonAge, moonFraction, moonDist, moonZodiac, lunarNode);
	}

	public static double distance(Calendar cal) {

		double pdate = MoonUtil.toJulian(cal);
		double dayOfEpoch = pdate - EPOCH;
		double meanSunAnomaly = fixangle((360 / 365.2422) * dayOfEpoch);
		double M = fixangle((meanSunAnomaly + SUN_ECLIPTIC_LONGITUDE_AT_EPOCH) - SUN_ECLIPTIC_LONGITUDE_AT_EPOCH_AT_PERIGREE);
		double Ec = MoonUtil.solveKeplerEquation(M, ECCENTRICITY);
		Ec = Math.sqrt((1 + ECCENTRICITY) / (1 - ECCENTRICITY)) * Math.tan(Ec / 2);
		Ec = 2 * Math.toDegrees(Math.atan(Ec));
		double sunGeocentricEclipticLongitude = fixangle(Ec + SUN_ECLIPTIC_LONGITUDE_AT_EPOCH_AT_PERIGREE);
		double meanMoonLongitude = fixangle((13.1763966 * dayOfEpoch) + MOONS_MEAN_LONGITUDE);
		double meanMoonAnomaly = fixangle(meanMoonLongitude - (0.1114041 * dayOfEpoch) - MOONS_MEAN_LONGITUDE_AT_EPOCH_AT_PERIGEE);
		double Ev = 1.2739 * MoonUtil.sinFromDegree((2 * (meanMoonLongitude - sunGeocentricEclipticLongitude)) - meanMoonAnomaly);
		double annualEquation = 0.1858 * MoonUtil.sinFromDegree(M);
		double A3 = 0.37 * MoonUtil.sinFromDegree(M);
		double MmP = (meanMoonAnomaly + Ev) - annualEquation - A3;
		double mEc = 6.2886 * MoonUtil.sinFromDegree(MmP);
		double moonDist = (((MDMAX + MDMIN) / 2) * (1 - (MECC * MECC))) / (1 + (MECC * Math.cos(Math.toRadians(MmP + mEc))));
		return moonDist;
	}
}
