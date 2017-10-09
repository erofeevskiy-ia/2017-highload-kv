package ru.mail.polis.erofeevskiy_ia;

import com.sun.net.httpserver.HttpServer;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.KVService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.NoSuchElementException;

public class MyService implements KVService {
    private static final String PREFIX = "id=";
    @NotNull
    private final HttpServer server;
    @NotNull
    private final MyDAO dao;

    @NotNull
    private static String extractID(@NotNull final String query) {
        if (!query.startsWith(PREFIX)) {
            throw new IllegalArgumentException("dirty string");
        }
        String id = query.substring(PREFIX.length());
        if (id.isEmpty()) throw new IllegalArgumentException("empty id-string");
        return id;
    }

    public MyService(int port, @NotNull final MyDAO dao) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.dao = dao;

        this.server.createContext("/v0/status", httpExchange -> {
            final String response = "ONLINE";
            httpExchange.sendResponseHeaders(200, response.length());
            httpExchange.getResponseBody().write(response.getBytes());
            //httpExchange.close();
        });

        this.server.createContext("/v0/entity", httpExchange -> {

            String id = null;
            try {
                id = extractID(httpExchange.getRequestURI().getQuery());
            } catch (Exception e) {
                httpExchange.sendResponseHeaders(400, 0);
                httpExchange.close();
                return;
            }
            switch (httpExchange.getRequestMethod()) {
                case "GET":
                    try {
                        final byte[] getValue = dao.get(id);
                        httpExchange.sendResponseHeaders(200, getValue.length);
                        httpExchange.getResponseBody().write(getValue);
                    } catch (IOException e) {
                        httpExchange.sendResponseHeaders(404, 0);
                        httpExchange.close();
                    }
                    break;

                case "DELETE":
                    dao.delete(id);
                    httpExchange.sendResponseHeaders(202, 0);
                    break;

                case "PUT":
                    try {
                        final int contentLength
                                = Integer.valueOf(httpExchange.getRequestHeaders().getFirst("Content-length"));
                        final byte[] putValue = new byte[contentLength];
                        if (contentLength!=0 && httpExchange.getRequestBody().read(putValue) != putValue.length) {
                            throw new IOException("cant read in one go");
                        }
                        dao.upsert(id, putValue);
                        httpExchange.sendResponseHeaders(201, contentLength);
                        httpExchange.getResponseBody().write(putValue);
                        break;
                    } catch (IllegalArgumentException e) {
                        httpExchange.sendResponseHeaders(400, 0);
                    } catch (NoSuchElementException e) {
                        httpExchange.sendResponseHeaders(404, 0);
                    }
                default:
                    httpExchange.sendResponseHeaders(503, 0);
            }

            httpExchange.close();
        });
    }


    @Override
    public void start() {
        this.server.start();
    }

    @Override
    public void stop() {
        try {
            this.server.stop(1);
        } catch(Exception e){
            e.printStackTrace();
        }
    }
}
