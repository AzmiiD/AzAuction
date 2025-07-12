package io.github.AzmiiD.azAuction.web;

import io.github.AzmiiD.azAuction.AzAuction;
import io.github.AzmiiD.azAuction.models.AuctionItem;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class WebServer {

    private final AzAuction plugin;
    private HttpServer server;
    private final Gson gson;
    private boolean running = false;

    public WebServer(AzAuction plugin) {
        this.plugin = plugin;
        this.gson = new Gson();
    }

    public void start() {
        try {
            int port = plugin.getConfigManager().getWebPort();
            String host = plugin.getConfigManager().getWebHost();

            server = HttpServer.create(new InetSocketAddress(host, port), 0);
            server.createContext("/", new StaticHandler());
            server.createContext("/api/auctions", new AuctionsHandler());
            server.createContext("/api/auction/", new AuctionHandler());
            server.setExecutor(Executors.newFixedThreadPool(4));
            server.start();

            running = true;
            plugin.getLogger().info("Web server started on " + host + ":" + port);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to start web server: " + e.getMessage());
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            running = false;
            plugin.getLogger().info("Web server stopped");
        }
    }

    public boolean isRunning() {
        return running;
    }

    private class StaticHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();

            if (path.equals("/")) {
                path = "/index.html";
            }

            InputStream is = getClass().getResourceAsStream("/web" + path);
            if (is == null) {
                // Return 404
                String response = "404 Not Found";
                exchange.sendResponseHeaders(404, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                return;
            }

            // Determine content type
            String contentType = "text/html";
            if (path.endsWith(".css")) {
                contentType = "text/css";
            } else if (path.endsWith(".js")) {
                contentType = "application/javascript";
            } else if (path.endsWith(".png")) {
                contentType = "image/png";
            } else if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
                contentType = "image/jpeg";
            }

            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, 0);

            try (OutputStream os = exchange.getResponseBody()) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }

            is.close();
        }
    }

    private class AuctionsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            List<AuctionItem> auctions = plugin.getAuctionManager().getActiveAuctions();
            JsonArray jsonArray = new JsonArray();

            for (AuctionItem auction : auctions) {
                JsonObject jsonAuction = new JsonObject();
                jsonAuction.addProperty("id", auction.getId());
                jsonAuction.addProperty("sellerId", auction.getSellerId().toString());
                jsonAuction.addProperty("sellerName", auction.getSellerName());
                jsonAuction.addProperty("price", auction.getPrice());
                jsonAuction.addProperty("listTime", auction.getListTime());
                jsonAuction.addProperty("expireTime", auction.getExpireTime());

                // Item information
                JsonObject itemJson = itemToJson(auction.getItem());
                jsonAuction.add("item", itemJson);

                jsonArray.add(jsonAuction);
            }

            String response = gson.toJson(jsonArray);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    private class AuctionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            String idStr = path.substring("/api/auction/".length());

            try {
                int id = Integer.parseInt(idStr);
                AuctionItem auction = plugin.getAuctionManager().getAuction(id);

                if (auction == null) {
                    exchange.sendResponseHeaders(404, -1);
                    return;
                }

                JsonObject jsonAuction = new JsonObject();
                jsonAuction.addProperty("id", auction.getId());
                jsonAuction.addProperty("sellerId", auction.getSellerId().toString());
                jsonAuction.addProperty("sellerName", auction.getSellerName());
                jsonAuction.addProperty("price", auction.getPrice());
                jsonAuction.addProperty("listTime", auction.getListTime());
                jsonAuction.addProperty("expireTime", auction.getExpireTime());

                // Item information
                JsonObject itemJson = itemToJson(auction.getItem());
                jsonAuction.add("item", itemJson);

                String response = gson.toJson(jsonAuction);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }
            } catch (NumberFormatException e) {
                exchange.sendResponseHeaders(400, -1);
            }
        }
    }

    private JsonObject itemToJson(ItemStack item) {
        JsonObject itemJson = new JsonObject();

        itemJson.addProperty("type", item.getType().name());
        itemJson.addProperty("amount", item.getAmount());
        itemJson.addProperty("durability", item.getDurability());

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName()) {
                itemJson.addProperty("displayName", meta.getDisplayName());
            }

            if (meta.hasLore()) {
                JsonArray loreArray = new JsonArray();
                for (String loreLine : meta.getLore()) {
                    loreArray.add(loreLine);
                }
                itemJson.add("lore", loreArray);
            }

            if (meta.hasEnchants()) {
                JsonArray enchantArray = new JsonArray();
                for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
                    JsonObject enchantJson = new JsonObject();
                    enchantJson.addProperty("type", entry.getKey().getKey().getKey());
                    enchantJson.addProperty("level", entry.getValue());
                    enchantArray.add(enchantJson);
                }
                itemJson.add("enchantments", enchantArray);
            }
        }

        return itemJson;
    }
}
