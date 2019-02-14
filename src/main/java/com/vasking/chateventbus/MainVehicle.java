package com.vasking.chateventbus;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

public class MainVehicle extends AbstractVerticle {

    public static void main(String args[]){
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new MainVehicle());
    }


    @Override
    public void start(Future<Void> future) throws Exception {
        Router router = Router.router(vertx);

        router.route("/static/*").handler(
                StaticHandler.create().setCachingEnabled(false));
        router.post("/chat/push").handler(BodyHandler.create());
        router.post("/chat/push").handler(handlePushMessage());
        router.route("/chat/messages").handler(handlerSendMessages());
        router.get("/").handler(c -> {
            c.response()
                    .putHeader("Location","/static/index.html")
                    .setStatusCode(302)
                    .end();
        });

        vertx
                .createHttpServer()
                .requestHandler(router::accept)
                .listen(
                        config().getInteger("http.port", 8080),
                        result -> {
                            if( result.succeeded()){
                                future.complete();
                            }else{
                                future.fail(result.cause());
                            }
                        }
                );
    }

    private Handler<RoutingContext> handlerSendMessages(){

        return context -> {
            context.response()
                    .putHeader("cache-control","no-cache")
                    .putHeader("content-type", "text/event-stream")
                    .putHeader("connection", "keep-alive")
                    .setChunked(true)
                    .writeContinue();

            MessageConsumer<String> consumer = vertx.eventBus().consumer("chat");
            consumer.handler(message -> {
                if (message.body() == null) return;
                if(context.response().closed()){
                    consumer.unregister();
                    return;
                }

                String msg = "data: " + message.body() + "\n\n";
                context.response().write(msg);
            });
        };
    }

    private Handler<RoutingContext> handlePushMessage(){
        return context -> {
            String msg = context.getBodyAsString();

            if(msg != null){
                vertx.eventBus().publish("chat", msg);
            }

            context.response()
                  .setChunked(true)
                  .end();
        };
    }
}
