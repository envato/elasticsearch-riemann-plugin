package org.elasticsearch.module.riemann.test;

import static org.elasticsearch.common.base.Predicates.containsPattern;
import static org.elasticsearch.module.riemann.test.NodeTestHelper.createNode;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.common.collect.Iterables;
import org.elasticsearch.node.Node;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RiemannPluginIntegrationTest {
    public static final int		RIEMANN_SERVER_PORT	= 12345;

    private RiemannMockServer	riemannMockServer;

    private String				clusterName			= RandomStringGenerator.randomAlphabetic(10);
    private String				index				= RandomStringGenerator.randomAlphabetic(6).toLowerCase();
    private String				type				= RandomStringGenerator.randomAlphabetic(6).toLowerCase();
    private Node				node_1;
    private Node				node_2;
    private Node				node_3;

    @Before
    public void startRiemannMockServerAndNode() throws Exception
    {
        /*
        riemannMockServer = new RiemannMockServer(RIEMANN_SERVER_PORT);
        riemannMockServer.start();
        node_1 = createNode(clusterName, 4, RIEMANN_SERVER_PORT, "1s");
        node_2 = createNode(clusterName, 4, RIEMANN_SERVER_PORT, "1s");
        node_3 = createNode(clusterName, 4, RIEMANN_SERVER_PORT, "1s");
        */
    }

    @After
    public void stopRiemannServer() throws Exception
    {
        /*
        riemannMockServer.close();
        if (!node_1.isClosed()) {
            node_1.close();
        }
        if (!node_2.isClosed()) {
            node_2.close();
        }
        if (!node_3.isClosed()) {
            node_3.close();
        }
        */
    }

    @Test
    public void testNothing() {
    }


    /*
    @Test
    public void testThatIndexingResultsInMonitoring() throws Exception
    {
        IndexResponse indexResponse = indexElement(node_1, index, type, "value");
        assertThat(indexResponse.getId(), is(notNullValue()));

        //Index some more docs
        this.indexSomeDocs(101);

        Thread.sleep(4000);

        ensureValidKeyNames();
        assertRiemannMetricIsContained("elasticsearch." + clusterName + ".index." + index + ".shard.0.indexing.index_total:51|c");
        assertRiemannMetricIsContained("elasticsearch." + clusterName + ".index." + index + ".shard.1.indexing.index_total:51|c");
        assertRiemannMetricIsContained(".jvm.threads.peak_count:");
    }

    @Test
    public void masterFailOverShouldWork() throws Exception
    {
        String clusterName = RandomStringGenerator.randomAlphabetic(10);
        IndexResponse indexResponse = indexElement(node_1, index, type, "value");
        assertThat(indexResponse.getId(), is(notNullValue()));

        Node origNode = node_1;
        node_1 = createNode(clusterName, 1, RIEMANN_SERVER_PORT, "1s");
        riemannMockServer.content.clear();
        origNode.stop();
        indexResponse = indexElement(node_1, index, type, "value");
        assertThat(indexResponse.getId(), is(notNullValue()));

        // wait for master fail over and writing to graph reporter
        Thread.sleep(4000);
        assertRiemannMetricIsContained("elasticsearch." + clusterName + ".index." + index + ".shard.0.indexing.index_total:1|c");
    }
    */

    // the stupid hamcrest matchers have compile erros depending whether they run on java6 or java7, so I rolled my own version
    // yes, I know this sucks... I want power asserts, as usual
    private void assertRiemannMetricIsContained(final String id)
    {
        assertThat(Iterables.any(riemannMockServer.content, containsPattern(id)), is(true));
    }

    // Make sure no elements with a chars [] are included
    private void ensureValidKeyNames()
    {
        assertThat(Iterables.any(riemannMockServer.content, containsPattern("\\.\\.")), is(false));
        assertThat(Iterables.any(riemannMockServer.content, containsPattern("\\[")), is(false));
        assertThat(Iterables.any(riemannMockServer.content, containsPattern("\\]")), is(false));
        assertThat(Iterables.any(riemannMockServer.content, containsPattern("\\(")), is(false));
        assertThat(Iterables.any(riemannMockServer.content, containsPattern("\\)")), is(false));
    }

    private IndexResponse indexElement(Node node, String index, String type, String fieldValue)
    {
        return node.client().prepareIndex(index, type).setSource("field", fieldValue).execute().actionGet();
    }

    private void indexSomeDocs(int docs)
    {
        while( docs > 0 ) {
            indexElement(node_1, index, type, "value " + docs);
            docs--;
        }
    }
}
