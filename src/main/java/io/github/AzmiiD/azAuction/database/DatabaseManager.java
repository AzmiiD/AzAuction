package io.github.AzmiiD.azAuction.database;

import io.github.AzmiiD.azAuction.AzAuction;
import io.github.AzmiiD.azAuction.models.AuctionItem;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DatabaseManager {

    private final AzAuction plugin;
    private Connection connection;
    private final String databaseType;

    public DatabaseManager(AzAuction plugin) {
        this.plugin = plugin;
        FileConfiguration config = plugin.getConfig();
        this.databaseType = config.getString("database.type", "sqlite");
    }

    public boolean initialize() {
        try {
            if (databaseType.equalsIgnoreCase("sqlite")) {
                setupSQLite();
            } else if (databaseType.equalsIgnoreCase("mysql")) {
                setupMySQL();
            } else {
                plugin.getLogger().severe("Invalid database type: " + databaseType);
                return false;
            }

            createTables();
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
            return false;
        }
    }

    private void setupSQLite() throws SQLException {
        FileConfiguration config = plugin.getConfig();
        String fileName = config.getString("database.sqlite.file", "auctions.db");
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        String url = "jdbc:sqlite:" + new File(dataFolder, fileName).getAbsolutePath();
        connection = DriverManager.getConnection(url);
    }

    private void setupMySQL() throws SQLException {
        FileConfiguration config = plugin.getConfig();
        String host = config.getString("database.mysql.host", "localhost");
        int port = config.getInt("database.mysql.port", 3306);
        String database = config.getString("database.mysql.database", "auctionhouse");
        String username = config.getString("database.mysql.username", "root");
        String password = config.getString("database.mysql.password", "");

        String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true";
        connection = DriverManager.getConnection(url, username, password);
    }

    private void createTables() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS auction_items (" +
                "id " + (databaseType.equalsIgnoreCase("sqlite") ? "INTEGER PRIMARY KEY AUTOINCREMENT" : "INT AUTO_INCREMENT PRIMARY KEY") + "," +
                "seller_uuid VARCHAR(36) NOT NULL," +
                "seller_name VARCHAR(16) NOT NULL," +
                "item_data " + (databaseType.equalsIgnoreCase("sqlite") ? "BLOB" : "LONGBLOB") + " NOT NULL," +
                "price DOUBLE NOT NULL," +
                "list_time BIGINT NOT NULL," +
                "expire_time BIGINT NOT NULL" +
                ")";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    public boolean insertAuction(AuctionItem auction) {
        String sql = "INSERT INTO auction_items (seller_uuid, seller_name, item_data, price, list_time, expire_time) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, auction.getSellerId().toString());
            pstmt.setString(2, auction.getSellerName());
            pstmt.setBytes(3, serializeItemStack(auction.getItem()));
            pstmt.setDouble(4, auction.getPrice());
            pstmt.setLong(5, auction.getListTime());
            pstmt.setLong(6, auction.getExpireTime());

            int result = pstmt.executeUpdate();

            if (result > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        auction.setId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to insert auction: " + e.getMessage());
        }

        return false;
    }

    public List<AuctionItem> getAllAuctions() {
        List<AuctionItem> auctions = new ArrayList<>();
        String sql = "SELECT * FROM auction_items ORDER BY list_time DESC";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                AuctionItem auction = new AuctionItem(
                        rs.getInt("id"),
                        UUID.fromString(rs.getString("seller_uuid")),
                        rs.getString("seller_name"),
                        deserializeItemStack(rs.getBytes("item_data")),
                        rs.getDouble("price"),
                        rs.getLong("list_time"),
                        rs.getLong("expire_time")
                );
                auctions.add(auction);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get auctions: " + e.getMessage());
        }

        return auctions;
    }

    public List<AuctionItem> getPlayerAuctions(UUID playerId) {
        List<AuctionItem> auctions = new ArrayList<>();
        String sql = "SELECT * FROM auction_items WHERE seller_uuid = ? ORDER BY list_time DESC";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerId.toString());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    AuctionItem auction = new AuctionItem(
                            rs.getInt("id"),
                            UUID.fromString(rs.getString("seller_uuid")),
                            rs.getString("seller_name"),
                            deserializeItemStack(rs.getBytes("item_data")),
                            rs.getDouble("price"),
                            rs.getLong("list_time"),
                            rs.getLong("expire_time")
                    );
                    auctions.add(auction);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get player auctions: " + e.getMessage());
        }

        return auctions;
    }

    public boolean removeAuction(int id) {
        String sql = "DELETE FROM auction_items WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to remove auction: " + e.getMessage());
            return false;
        }
    }

    public AuctionItem getAuctionById(int id) {
        String sql = "SELECT * FROM auction_items WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new AuctionItem(
                            rs.getInt("id"),
                            UUID.fromString(rs.getString("seller_uuid")),
                            rs.getString("seller_name"),
                            deserializeItemStack(rs.getBytes("item_data")),
                            rs.getDouble("price"),
                            rs.getLong("list_time"),
                            rs.getLong("expire_time")
                    );
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get auction by ID: " + e.getMessage());
        }

        return null;
    }

    private byte[] serializeItemStack(ItemStack item) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             BukkitObjectOutputStream boos = new BukkitObjectOutputStream(baos)) {
            boos.writeObject(item);
            return baos.toByteArray();
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to serialize ItemStack: " + e.getMessage());
            return new byte[0];
        }
    }

    private ItemStack deserializeItemStack(byte[] data) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             BukkitObjectInputStream bois = new BukkitObjectInputStream(bais)) {
            return (ItemStack) bois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            plugin.getLogger().severe("Failed to deserialize ItemStack: " + e.getMessage());
            return null;
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to close database connection: " + e.getMessage());
        }
    }
}