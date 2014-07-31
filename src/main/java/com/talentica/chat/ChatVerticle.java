package com.talentica.chat;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.http.ServerWebSocket;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

public class ChatVerticle extends Verticle {

	public void start() {

		final Logger logger = container.logger();
		final Set<ServerWebSocket> wsList = new HashSet<ServerWebSocket>();

		RouteMatcher routeMatcher = new RouteMatcher().get("/",
				new Handler<HttpServerRequest>() {

					@Override
					public void handle(HttpServerRequest request) {
						request.response().sendFile("web/index.html");
					}
				}).get(".*\\.(css|js)$", new Handler<HttpServerRequest>() {

			@Override
			public void handle(HttpServerRequest request) {
				request.response().sendFile("web/" + new File(request.path()));
			}
		});
		vertx.createHttpServer().requestHandler(routeMatcher).listen(9090);

		// Second server for WebSocket
		vertx.createHttpServer()
				.websocketHandler(new Handler<ServerWebSocket>() {
					@Override
					public void handle(final ServerWebSocket serverWebSocket) {
						logger.info("got connection");
						wsList.add(serverWebSocket);
						serverWebSocket.dataHandler(new Handler<Buffer>() {

							@Override
							public void handle(Buffer buffer) {
								String brodcastMessage = "From:"
										+ serverWebSocket.remoteAddress()
												.toString() + "::"
										+ buffer.toString();
								for (ServerWebSocket socket : wsList) {
									socket.writeTextFrame(brodcastMessage);
								}
							}
						});

						serverWebSocket.closeHandler(new Handler<Void>() {

							@Override
							public void handle(Void event) {
								wsList.remove(serverWebSocket);
							}
						});

					}
				}).listen(9099);

	}
}
