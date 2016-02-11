package org.elasticsearch.service.riemann;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.stats.CommonStatsFlags;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.component.Lifecycle;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.util.concurrent.EsExecutors;
import org.elasticsearch.indices.IndicesService;
import org.elasticsearch.node.service.NodeService;

public class RiemannService extends AbstractLifecycleComponent<RiemannService> {
    private final Client client;
    private final ClusterService clusterService;
    private final IndicesService indicesService;
    private final NodeService nodeService;
    private final String riemannHost;
    private final Integer riemannPort;
    private final String riemannSocketType;
    private final TimeValue riemannRefreshInterval;
    private final String riemannPrefix;
    private final String riemannNodeName;
    private final Boolean riemannReportNodeIndices;
    private final Boolean riemannReportNodeStats;
    private final Boolean riemannReportIndices;
    private final Boolean riemannReportShards;
    private final Boolean riemannReportFsDetails;
    private final RiemannClientWrapper riemannClient;

    private volatile Thread riemannReporterThread;
    private volatile boolean closed;

    @Inject
    public RiemannService(Settings settings, Client client, ClusterService clusterService, IndicesService indicesService, NodeService nodeService) {
        super(settings);
        this.client = client;
        this.clusterService = clusterService;
        this.indicesService = indicesService;
        this.nodeService = nodeService;
        this.riemannRefreshInterval = settings.getAsTime(
            "metrics.riemann.every", TimeValue.timeValueMinutes(1)
        );
        this.riemannHost = settings.get(
            "metrics.riemann.host"
        );
        this.riemannPort = settings.getAsInt(
            "metrics.riemann.port", 5555
        );
        this.riemannSocketType = settings.get(
            "metrics.riemann.socket_type", "tcp"
        );
        this.riemannPrefix = settings.get(
            "metrics.riemann.prefix", "elasticsearch" + "." + settings.get("cluster.name")
        );
        this.riemannNodeName = settings.get(
            "metrics.riemann.node_name"
        );
        this.riemannReportNodeStats = settings.getAsBoolean(
            "metrics.riemann.report.node_stats", false
        );
        this.riemannReportNodeIndices = settings.getAsBoolean(
            "metrics.riemann.report.node_indices", false
        );
        this.riemannReportIndices = settings.getAsBoolean(
            "metrics.riemann.report.indices", false
        );
        this.riemannReportShards = settings.getAsBoolean(
            "metrics.riemann.report.shards", false
        );
        this.riemannReportFsDetails = settings.getAsBoolean(
            "metrics.riemann.report.fs_details", false
        );

        this.riemannClient = new RiemannClientWrapper(this.riemannHost, this.riemannPort, this.riemannSocketType, this.logger);
    }

    @Override
    protected void doStart() throws ElasticsearchException {
        if (this.riemannHost != null && this.riemannHost.length() > 0) {
            this.riemannReporterThread = EsExecutors
                .daemonThreadFactory(this.settings, "riemann_reporter")
                .newThread(new RiemannReporterThread());
            this.riemannReporterThread.start();
            this.logger.info(
                "Riemann reporting triggered every [{}] to host [{}:{}:{}]",
                this.riemannRefreshInterval, this.riemannSocketType, this.riemannHost, this.riemannPort
            );
        } else {
            this.logger.error(
                "Riemann reporting disabled, no Riemann host configured"
            );
        }
    }

    @Override
    protected void doStop() throws ElasticsearchException {
        if (this.closed) {
            return;
        }
        if (this.riemannReporterThread != null) {
            this.riemannReporterThread.interrupt();
        }
        this.closed = true;
        this.logger.info("Riemann reporter stopped");
    }

    @Override
    protected void doClose() throws ElasticsearchException {
    }

    public class RiemannReporterThread implements Runnable {

        @Override
        public void run() {
            while (!RiemannService.this.closed) {
                logger.info("** Loop starts");
                DiscoveryNode node = RiemannService.this.clusterService.localNode();
                ClusterState state = RiemannService.this.clusterService.state();
                boolean isClusterStarted = RiemannService.this.clusterService
                    .lifecycleState()
                    .equals(Lifecycle.State.STARTED);

                if (node != null && state != null && isClusterStarted) {
                    String riemanndNodeName = RiemannService.this.riemannNodeName;
                    if (null == riemanndNodeName) riemanndNodeName = node.getName();

                    if(RiemannService.this.riemannReportNodeStats) {
                        // Report node stats -- runs for all nodes
                        RiemannReporter nodeStatsReporter = new RiemannReporterNodeStats(
                            RiemannService.this.nodeService.stats(
                                new CommonStatsFlags().clear(), // indices
                                true, // os
                                true, // process
                                true, // jvm
                                true, // threadPool
                                true, // network
                                true, // fs
                                true, // transport
                                true, // http
                                false // circuitBreaker
                            ),
                            riemanndNodeName,
                            RiemannService.this.riemannReportFsDetails
                        );
                        logger.info("*** Sending node-local stats");
                        nodeStatsReporter
                            .setRiemannClient(RiemannService.this.riemannClient)
                            .run();
                    }

                    // Maybe report index stats per node
                    if (RiemannService.this.riemannReportNodeIndices && node.isDataNode()) {
                        RiemannReporter nodeIndicesStatsReporter = new RiemannReporterNodeIndicesStats(
                            RiemannService.this.indicesService.stats(
                                false // includePrevious
                            ),
                            riemanndNodeName
                        );
                        logger.info("*** Sending node-local indices stats");
                        nodeIndicesStatsReporter
                            .setRiemannClient(RiemannService.this.riemannClient)
                            .run();
                    }

                    // Master node is the only one allowed to send cluster wide sums / stats
                    if (state.nodes().localNodeMaster()) {
                        RiemannReporter indicesReporter = new RiemannReporterIndices(
                            RiemannService.this.client
                                .admin()        // AdminClient
                                .indices()      // IndicesAdminClient
                                .prepareStats() // IndicesStatsRequestBuilder
                                .all()          // IndicesStatsRequestBuilder
                                .get(),         // IndicesStatsResponse
                            RiemannService.this.riemannReportIndices,
                            RiemannService.this.riemannReportShards,
                            logger
                        );
                        logger.info("*** Sending cluster-wide stats from master");
                        indicesReporter
                            .setRiemannClient(RiemannService.this.riemannClient)
                            .run();
                    }
                }

                logger.info("*** main loop complete; sleeping");
                try {
                    Thread.sleep(RiemannService.this.riemannRefreshInterval.millis());
                } catch (InterruptedException e1) {
                    continue;
                }
            }
        }
    }
}
