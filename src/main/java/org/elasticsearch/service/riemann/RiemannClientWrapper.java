package org.elasticsearch.service.riemann;

import java.io.IOException;

import org.elasticsearch.common.logging.ESLogger;

import com.aphyr.riemann.Proto.Msg;
import com.aphyr.riemann.client.EventDSL;
import com.aphyr.riemann.client.RiemannClient;

public class RiemannClientWrapper {
    private final Integer  RIEMANN_SEND_TIMEOUT = 2500;
    private final String   TAG                  = "elasticsearch";
    private final String   UNKNOWN_CLUSTER      = "unknown_cluster";

    private final String   riemannHost;
    private final Integer  riemannPort;
    private final String   socketType;
    private final ESLogger logger;
    private final String   clusterName;

    private RiemannClient  client;

    public RiemannClientWrapper(String riemannHost, Integer riemannPort, String socketType, ESLogger logger, String clusterName) {
        this.riemannHost = riemannHost;
        this.riemannPort = riemannPort;
        this.socketType  = socketType;
        this.logger      = logger;
        this.clusterName = clusterName == null ? UNKNOWN_CLUSTER : clusterName;

        try {
            if(this.socketType.equals("udp")) {
                client = RiemannClient.udp(this.riemannHost, this.riemannPort);
            } else {
                client = RiemannClient.tcp(this.riemannHost, this.riemannPort);
            }
            client.connect();
        } catch(IOException ex) {
            logger.error("Error creating client to host=" + riemannHost + " port=" + riemannPort + " type=" + socketType, ex);
        }
    }

    public void gauge(String name, long value) {
        if(client != null) {
            logger.debug("Sending event: " + name + "=" + value);
            send(client.event().service(name).metric(value).time(now()));
        }
    }

    public void gauge(String name, double value) {
        if(client != null) {
            logger.debug("Sending event: " + name + "=" + value);
            send(client.event().service(name).metric(value).time(now()));
        }
    }

    public void count(String name, long value) {
        if(client != null) {
            logger.debug("Sending event: " + name + "=" + value);
            send(client.event().service(name).metric(value).time(now()));
        }
    }

    public void count(String name, double value) {
        if(client != null) {
            logger.debug("Sending event: " + name + "=" + value);
            send(client.event().service(name).metric(value).time(now()));
        }
    }

    public void time(String name, long value) {
        if(client != null) {
            logger.debug("Sending event: " + name + "=" + value);
            send(client.event().service(name).time(value));
        }
    }


    private long now() {
        return System.currentTimeMillis() / 1000l;
    }

    private Msg send(EventDSL event) {
        try {
            Msg msg = event
                      .tag(TAG)
                      .tag(this.clusterName)
                      .send()
                      .deref(RIEMANN_SEND_TIMEOUT, java.util.concurrent.TimeUnit.MILLISECONDS);
            logger.debug("Result: msg=" + msg);
            return msg;
        } catch(IOException ex) {
            logger.error("Error sending", ex);
            return null;
        }
    }
}
