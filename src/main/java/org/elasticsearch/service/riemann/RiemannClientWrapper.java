package org.elasticsearch.service.riemann;

import java.io.IOException;

import org.elasticsearch.common.logging.ESLogger;

import com.aphyr.riemann.Proto.Msg;
import com.aphyr.riemann.client.EventDSL;
import com.aphyr.riemann.client.RiemannClient;
public class RiemannClientWrapper {
    private final ESLogger logger;
    private final String   riemannHost;
    private final Integer  riemannPort;
    private final String   socketType;

    private RiemannClient  client;

    public RiemannClientWrapper(String riemannHost, Integer riemannPort, String socketType, ESLogger logger) {
        this.riemannHost = riemannHost;
        this.riemannPort = riemannPort;
        this.socketType = socketType;
        this.logger = logger;

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
            logger.info("Sending event: " + name + "=" + value);
            send(client.event().service(name).metric(value));
        }
    }

    public void gauge(String name, double value) {
        if(client != null) {
            logger.info("Sending event: " + name + "=" + value);
            send(client.event().service(name).metric(value));
        }
    }

    public void count(String name, long value) {
        if(client != null) {
            logger.info("Sending event: " + name + "=" + value);
            send(client.event().service(name).metric(value));
        }
    }

    public void count(String name, double value) {
        if(client != null) {
            logger.info("Sending event: " + name + "=" + value);
            send(client.event().service(name).metric(value));
        }
    }

    public void time(String name, long value) {
        if(client != null) {
            logger.info("Sending event: " + name + "=" + value);
            send(client.event().service(name).time(value));
        }
    }


    private Msg send(EventDSL event) {
        try {
            Msg msg = event.send().deref(5000, java.util.concurrent.TimeUnit.MILLISECONDS);
            logger.info("Result: msg=" + msg);
            return msg;
        } catch(IOException ex) {
            logger.error("Error sending", ex);
            return null;
        }
    }
}
