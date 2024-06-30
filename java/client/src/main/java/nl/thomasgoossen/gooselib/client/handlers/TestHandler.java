package nl.thomasgoossen.gooselib.client.handlers;

import java.io.IOException;
import java.io.OutputStream;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class TestHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String reqParamvalue = null;
        if (exchange.getRequestMethod().equals("GET")) {
            reqParamvalue = handleGetReq(exchange);
        }

        handleResponse(exchange, reqParamvalue);
    }
    
    private String handleGetReq(HttpExchange exchange) {
        return exchange.getRequestURI().toString()
                .split("\\?")[1]
                .split("=")[1];
    }

    private void handleResponse(HttpExchange exchange, String reqParamValue) throws IOException {
        try (OutputStream stream = exchange.getResponseBody()) {
            System.out.println(reqParamValue);
            StringBuilder htmlBuilder = new StringBuilder();
            
            htmlBuilder.append("<html>")
                    .append("<body>")
                    .append("<h1>")
                    .append("Welcome to the Gooselib client HTTP server!")
                    .append("</h1>")
                    .append("</body>")
                    .append("</html>");
            
            String htmlRes = htmlBuilder.toString();
            exchange.sendResponseHeaders(200, htmlRes.length());
            stream.write(htmlRes.getBytes());
            stream.flush();
        }
    }
}
