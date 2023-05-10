package org.apache.cassandra.sidecar.logging;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.LoggerHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SidecarLoggerHandler implements Handler<RoutingContext> {
    Logger LOG = LoggerFactory.getLogger("error-logger");
    final LoggerHandler loggerHandler;

    private SidecarLoggerHandler(LoggerHandler loggerHandler) {
        this.loggerHandler = loggerHandler;
    }

    public static Handler<RoutingContext> create(LoggerHandler handler) {
        return new SidecarLoggerHandler(handler);
    }

    @Override
    public void handle (RoutingContext context){
        context.addBodyEndHandler((Void) -> {
            if (context.statusCode() >= 500 && context.failure() != null) {
                LOG.error(String.format("code=%d path=%s params=%s", context.statusCode(),
                        context.request().path(), toJson(context.request().params())), context.failure());
            }
        });
        loggerHandler.handle(context);
    }

    private Object toJson(MultiMap params) {
        if (params == null) return "";
        return Json.encode(params.entries());
    }
}
