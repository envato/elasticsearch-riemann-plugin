package org.elasticsearch.service.riemann;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;

import com.aphyr.riemann.client.RiemannClient;

public abstract class RiemannReporter {

	private static final String DEFAULT_JOINER = ".";
	private static final ESLogger logger = ESLoggerFactory.getLogger(RiemannReporter.class.getName());
	private RiemannClientWrapper riemannClient;

	public RiemannReporter setRiemannClient(RiemannClientWrapper riemannClient) {
		this.riemannClient = riemannClient;
		return this;
	}

	public abstract void run();

	protected void sendGauge(String name, String valueName, long value) {
		this.riemannClient.gauge(this.join(name, valueName), value);
	}

	protected void sendGauge(String name, String valueName, double value) {
		this.riemannClient.gauge(this.join(name, valueName), value);
	}

	protected void sendCount(String name, String valueName, long value) {
		this.riemannClient.count(this.join(name, valueName), value);
	}

	protected void sendTime(String name, String valueName, long value) {
		this.riemannClient.time(this.join(name, valueName), value);
	}

	protected String sanitizeString(String s) {
		return s.replace(' ', '-');
	}

	protected String buildMetricName(String name) {
		return this.sanitizeString(name);
	}

	private String join(String... parts) {
		if (parts == null) {
			return null;
		}
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < parts.length; i++) {
			builder.append(parts[i]);
			if (i < parts.length - 1) {
				builder.append(this.DEFAULT_JOINER);
			}
		}
		return builder.toString();
	}

	protected void logException(Exception e) {
		this.logger.warn("Error writing to Riemann", e);
	}
}
