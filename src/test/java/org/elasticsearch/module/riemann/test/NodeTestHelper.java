package org.elasticsearch.module.riemann.test;

import org.elasticsearch.common.logging.log4j.LogConfigurator;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import java.io.IOException;

public class NodeTestHelper
{

	public static Node createNode(String clusterName, final int numberOfShards, int riemannPort, String refreshInterval)
		throws IOException
	{
		ImmutableSettings.Builder settingsBuilder = ImmutableSettings.settingsBuilder();

		settingsBuilder.put("gateway.type", "none");
		settingsBuilder.put("cluster.name", clusterName);
		settingsBuilder.put("index.number_of_shards", numberOfShards);
		settingsBuilder.put("index.number_of_replicas", 1);

		settingsBuilder.put("metrics.riemann.host", "localhost");
		settingsBuilder.put("metrics.riemann.port", riemannPort);
		settingsBuilder.put("metrics.riemann.socket_type", "udp");
		settingsBuilder.put("metrics.riemann.every", refreshInterval);

		settingsBuilder.put("path.conf", "target/test-classes/config");

		LogConfigurator.configure(settingsBuilder.build());

		return NodeBuilder.nodeBuilder().settings(settingsBuilder.build()).node();
	}
}
