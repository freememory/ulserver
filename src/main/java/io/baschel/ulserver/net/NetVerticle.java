package io.baschel.ulserver.net;

import io.baschel.ulserver.Main;
import io.baschel.ulserver.msgs.InternalServerMessage;
import io.baschel.ulserver.msgs.internal.DisconnectClient;
import io.baschel.ulserver.util.Json;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.NetServer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.vertx.core.net.NetSocket;

/**
 * Created by macobas on 13/06/16.
 */
public class NetVerticle extends AbstractVerticle {

    private NetServer tcpServer;
    private Set<ClientConnection> clientConnections = new HashSet<>();
    private Map<String, NetSocket> idSocketMap = new HashMap<>();
    public static final String EVENTBUS_ADDRESS = NetVerticle.class.getName();
    public static final Logger L = LoggerFactory.getLogger(NetVerticle.class);

    @Override
    public void start()
    {
        int port = Main.config.serverConfig.port;
        tcpServer = vertx.createNetServer();

        tcpServer.connectHandler(this::handleConnection);
        tcpServer.listen(port, result -> {
            if(!result.succeeded())
            {
                L.error("Failed to deploy", result.cause());
                vertx.close();
            }
            else
                L.info("Server started and bound on {0}", port);
        });

        vertx.eventBus().consumer(EVENTBUS_ADDRESS, this::handleInternalMessage);
    }

    private void handleInternalMessage(Message<JsonObject> tMessage) {
        InternalServerMessage imsg = Json.objectFromJsonObject(tMessage.body(), InternalServerMessage.class);

        if(imsg instanceof DisconnectClient)
        {
            handleDisconnectClient((DisconnectClient)imsg);
            return;
        }
    }

    private void handleDisconnectClient(DisconnectClient imsg) {
        NetSocket sock = idSocketMap.get(imsg.getClientId());
        if(sock != null)
            sock.close();
    }

    private void handleConnection(NetSocket socket)
    {
        ClientConnection cxn = new ClientConnection(socket.writeHandlerID());
        L.info("NEW CONNECTION from {0}", socket.remoteAddress().toString());
        idSocketMap.put(socket.writeHandlerID(), socket);
        clientConnections.add(cxn);
        socket.handler(cxn::handleMessage);
    }
}