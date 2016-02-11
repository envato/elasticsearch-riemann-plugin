# Elasticsearch Riemann Plugin

This plugin creates a little push service, which regularly updates a Riemann host with indices stats and nodes stats.
Index stats that apply across the entire cluster is only pushed from the elected master which node level stats are pushed from every node.

The data sent to the Riemann server tries to be roughly equivalent to the [Indices Stats API](http://www.elasticsearch.org/guide/reference/api/admin-indices-stats.html) and [Nodes Stats Api](http://www.elasticsearch.org/guide/reference/api/admin-cluster-nodes-stats.html).


## Installation

To install a prepackaged plugin use the following command:

```
bin/plugin -install riemann -url https://github.com/envato/elasticsearch-riemann-plugin/releases/download/v0.1.0/elasticsearch-riemann-0.1.0.zip
```

You can also build your own by doing the following:

```
git clone http://github.com/envato/elasticsearch-riemann-plugin.git
cd elasticsearch-riemann-plugin
mvn package
bin/plugin -install riemann -url file:///absolute/path/to/current/dir/target/releases/elasticsearch-riemann-0.1.0.zip
```


## Configuration

Configuration is possible via these parameters:

* `metrics.riemann.host`: The riemann host to connect to (default: none)
* `metrics.riemann.port`: The port to connect to (default: 8125)
* `metrics.riemann.every`: The interval to push data (default: 1m)
* `metrics.riemann.prefix`: The metric prefix that's sent with metric names (default: elasticsearch.your_cluster_name)
* `metrics.riemann.node_name`: Override the name for node used in the stat keys (default: the ES node name)
* `metrics.riemann.report.node_indices`: If per node index sums should be reported (default: false)
* `metrics.riemann.report.indices`: If index level sums should be reported (default: true)
* `metrics.riemann.report.shards`: If shard level stats should be reported (default: false)
* `metrics.riemann.report.fs_details`: If nodes should break down the FS by device instead of total disk (default: false)

Check your elasticsearch log file for a line like this after adding the configuration parameters below to the configuration file

```
[2013-02-08 16:01:49,153][INFO ][service.riemann        ] [Sea Urchin] Riemann reporting triggered every [1m] to host [tcp:localhost:5555]
```


## Stats Key Formats

This plugin reports both node level and cluster level stats, the Riemann names will be in the formats:

* `{PREFIX}.node.{NODE_NAME}.{STAT_KEY}`: Node level stats (CPU / JVM / etc.)
* `{PREFIX}.node.{NODE_NAME}.indices.{STAT_KEY}`: Index stats summed across the node (off by default)
* `{PREFIX}.indices.{STAT_KEY}`: Index stats summed across the entire cluster
* `{PREFIX}.index.{INDEX_NAME}.total.{STAT_KEY}`: Index stats summed per index across all shards
* `{PREFIX}.index.{INDEX_NAME}.{SHARD_ID}.{STAT_KEY}` -- Index stats per shard (off by default)


## Bugs/TODO

* Not extensively tested
* In case of a master node failover, counts are starting from 0 again (in case you are wondering about spikes)


## Credits

This is a fork of the [Automattic plugin](https://github.com/Automattic/elasticsearch-statsd-plugin) for multi-node clusters on ES 1.7.x+.

... which was a fork of the [Swoop plugin](https://github.com/swoop-inc/elasticsearch-statsd-plugin) for multi-node clusters on ES 1.5.x+.

Heavily inspired by the excellent [metrics library](http://metrics.codahale.com) by Coda Hale and its [GraphiteReporter add-on](http://metrics.codahale.com/manual/graphite/).


## License

See LICENSE
