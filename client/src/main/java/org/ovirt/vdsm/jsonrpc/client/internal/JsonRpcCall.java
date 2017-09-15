package org.ovirt.vdsm.jsonrpc.client.internal;

import org.ovirt.vdsm.jsonrpc.client.BrokerCommandCallback;
import org.ovirt.vdsm.jsonrpc.client.JsonRpcResponse;

/**
 * Abstraction for single and batch calls to be updated
 * when response arrives.
 *
 */
public interface JsonRpcCall {
    /**
     * @param response Added to current call object.
     */
    void addResponse(JsonRpcResponse response);

    /**
     * The callback to notify after the specific command was invoked.
     *
     * @return  The callback that receives the notification.
     */
    BrokerCommandCallback getCallback();
}
