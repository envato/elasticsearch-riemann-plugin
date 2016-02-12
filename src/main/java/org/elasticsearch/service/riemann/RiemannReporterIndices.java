package org.elasticsearch.service.riemann;

import org.elasticsearch.action.admin.indices.stats.CommonStats;
import org.elasticsearch.action.admin.indices.stats.IndexShardStats;
import org.elasticsearch.action.admin.indices.stats.IndexStats;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsResponse;
import org.elasticsearch.common.logging.ESLogger;

public class RiemannReporterIndices extends RiemannReporterIndexStats {
    private final IndicesStatsResponse indicesStatsResponse;
    private final Boolean reportIndices;
    private final Boolean reportShards;
    private final ESLogger logger;

    public RiemannReporterIndices(IndicesStatsResponse indicesStatsResponse, Boolean reportIndices, Boolean reportShards, ESLogger logger) {
        this.indicesStatsResponse = indicesStatsResponse;
        this.reportIndices = reportIndices;
        this.reportShards = reportShards;
        this.logger = logger;
    }

    public void run() {
        try {
            // First report totals
            logger.debug("Sending common stats");
            this.sendCommonStats(
                this.buildMetricName("indices"),
                this.indicesStatsResponse.getTotal()
            );

            if (this.reportIndices) {
                for (IndexStats indexStats : this.indicesStatsResponse.getIndices().values()) {
                    String indexPrefix = "index." + indexStats.getIndex();
                    logger.debug("Sending index stats for " + indexPrefix);

                    this.sendCommonStats(
                        this.buildMetricName(indexPrefix + ".total"),
                        indexStats.getTotal()
                    );

                    if (this.reportShards) {
                        for (IndexShardStats indexShardStats : indexStats.getIndexShards().values()) {
                            this.sendCommonStats(
                                this.buildMetricName(indexPrefix + "." + indexShardStats.getShardId().id()),
                                indexShardStats.getTotal()
                            );
                        }
                    }
                }
            }
        } catch (Exception e) {
            this.logException(e);
        }
    }

    private void sendCommonStats(String prefix, CommonStats stats) {
        this.sendDocsStats(prefix + ".docs", stats.getDocs());
        this.sendStoreStats(prefix + ".store", stats.getStore());
        this.sendIndexingStats(prefix + ".indexing", stats.getIndexing());
        this.sendGetStats(prefix + ".get", stats.getGet());
        this.sendSearchStats(prefix + ".search", stats.getSearch());
        this.sendMergeStats(prefix + ".merges", stats.getMerge());
        this.sendRefreshStats(prefix + ".refresh", stats.getRefresh());
        this.sendFlushStats(prefix + ".flush", stats.getFlush());
        this.sendWarmerStats(prefix + ".warmer", stats.getWarmer());
        this.sendFilterCacheStats(prefix + ".filter_cache", stats.getFilterCache());
        this.sendIdCacheStats(prefix + ".id_cache", stats.getIdCache());
        this.sendFielddataCacheStats(prefix + ".fielddata", stats.getFieldData());
        this.sendPercolateStats(prefix + ".percolate", stats.getPercolate());
        this.sendCompletionStats(prefix + ".completion", stats.getCompletion());
        this.sendSegmentsStats(prefix + ".segments", stats.getSegments());
        //TODO: getTranslog
        //TODO: getSuggest
    }
}
