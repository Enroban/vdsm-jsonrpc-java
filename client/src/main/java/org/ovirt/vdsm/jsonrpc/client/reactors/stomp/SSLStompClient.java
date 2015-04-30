package org.ovirt.vdsm.jsonrpc.client.reactors.stomp;

import static org.ovirt.vdsm.jsonrpc.client.reactors.stomp.impl.Message.*;
import static org.ovirt.vdsm.jsonrpc.client.utils.JsonUtils.reduceGracePeriod;

import java.nio.channels.Selector;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import javax.net.ssl.SSLContext;

import org.ovirt.vdsm.jsonrpc.client.ClientConnectionException;
import org.ovirt.vdsm.jsonrpc.client.reactors.Reactor;
import org.ovirt.vdsm.jsonrpc.client.reactors.SSLClient;
import org.ovirt.vdsm.jsonrpc.client.reactors.stomp.impl.Message;
import org.ovirt.vdsm.jsonrpc.client.utils.OneTimeCallback;

public class SSLStompClient extends SSLClient {

    private OneTimeCallback callback = new OneTimeCallback() {

        @Override
        public void execute() {
            if (connected.getCount() == 0) {
                connected = new CountDownLatch(1);
            }
            if (subscribed.getCount() == 0) {
                subscribed = new CountDownLatch(1);
            }

            Message message = new Message().connect().withHeader(HEADER_ACCEPT, "1.2").withHeader(HEADER_HOST, policy.getIdentifier());
            if (policy.isHeartbeat()) {
                message.withHeader(HEADER_HEART_BEAT, 0 + "," + reduceGracePeriod(policy.getHeartbeat()));
            }
            send(message.build());

            subscribtionId = UUID.randomUUID().toString();
            send(new Message().subscribe().withHeader(HEADER_DESTINATION, RESPONSE_QUEUE)
                    .withHeader(HEADER_ID, subscribtionId).withHeader(HEADER_ACK, "client").build());
        }

    };

    public SSLStompClient(Reactor reactor, Selector selector, String hostname, int port, SSLContext sslContext)
            throws ClientConnectionException {
        super(reactor, selector, hostname, port, sslContext);
        setWaitForConnect();
    }

    @Override
    public void sendMessage(byte[] message) throws ClientConnectionException {
        waitForConnect();

        send(new Message().send()
                .withHeader(HEADER_DESTINATION, REQUEST_QUEUE)
                .withContent(message)
                .build());
    }

    @Override
    protected void postConnect(OneTimeCallback callback) throws ClientConnectionException {
        super.postConnect(getPostConnectCallback());
    }

    @Override
    protected OneTimeCallback getPostConnectCallback() {
        this.callback.resetExecution();
        setWaitForConnect();
        return this.callback;
    }

    private void setWaitForConnect() {
        this.connected = new CountDownLatch(1);
        this.subscribed = new CountDownLatch(1);
    }

    private void waitForConnect() throws ClientConnectionException {
        try {
            this.connected.await(policy.getRetryTimeOut(), policy.getTimeUnit());
            // TODO wait for the mini broker to be finished
            // this.subscribed.await();
        } catch (InterruptedException e) {
            disconnect("Waiting for connect interrupted");
            throw new IllegalStateException("Communication interrupted");
        }
    }

    @Override
    public boolean isInInit() {
        return this.nioEngine == null || this.nioEngine.handshakeInProgress();
    }
}
