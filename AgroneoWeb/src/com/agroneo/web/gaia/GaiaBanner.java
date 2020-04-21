package com.agroneo.web.gaia;

import live.page.web.utils.Fx;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@WebListener
public class GaiaBanner implements ServletContextListener {

	public static final int MAX = 3; // times
	public static final int DELAY = 3; //per seconds
	private static final Map<String, Integer> banned = new HashMap<>();
	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		scheduler.scheduleAtFixedRate(banned::clear, DELAY, DELAY, TimeUnit.SECONDS);
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		Fx.shutdownService(scheduler);
	}

	/**
	 * @param remoteAddr address to control
	 * @return true if authorized
	 */
	public static boolean authorized(String remoteAddr) {
		banned.put(remoteAddr, banned.getOrDefault(remoteAddr, 0) + 1);
		return banned.get(remoteAddr) < MAX;
	}

	/**
	 * @param remoteAddr address to control
	 * @param max        maximum per rounds
	 * @return true if authorized
	 */
	public static boolean authorized(String remoteAddr, int max) {
		banned.put(remoteAddr, banned.getOrDefault(remoteAddr, 0) + 1);
		return banned.get(remoteAddr) < max;
	}
}