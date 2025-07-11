package com.Frozzy.AuthKey;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.UUID;

public class AuthKeyServer {
    private static final String FOLDER_NAME = "AuthKey";
    public static final Logger LOGGER = LogManager.getLogger("authkey");

    public static void handleAuthKey(EntityPlayerMP player, String key) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        LOGGER.info("[AuthKey] handleAuthKey called for player: {} (IP: {})", player.getName(), player.getPlayerIP());
        String playerName = player.getName();
        File authKeyDir = new File(server.getFile("."), FOLDER_NAME);
        if (!authKeyDir.exists()) authKeyDir.mkdirs();
        File keyFile = new File(authKeyDir, playerName + ".json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String offlineUUID = getOfflineUUID(playerName).toString();
        String onlineUUID = getOnlineUUID(playerName);
        if (server.isServerInOnlineMode()) {
            // online-mode:true — just record UUIDs and mark key
            JsonObject obj = new JsonObject();
            obj.addProperty("name", playerName);
            obj.addProperty("key", "online-mode:true");
            obj.addProperty("offline-uuid", offlineUUID);
            obj.addProperty("online-uuid", onlineUUID);
            try (FileWriter writer = new FileWriter(keyFile)) {
                gson.toJson(obj, writer);
            } catch (IOException e) {
                LOGGER.error("[AuthKey] Error saving key for {}: {}", playerName, e.getMessage());
            }
            LOGGER.info("[AuthKey] Server is in online-mode, only UUIDs recorded for {}", playerName);
            return;
        }
        if (!keyFile.exists()) {
            LOGGER.info("[AuthKey] First login, saving key for {} (file: {})", playerName, keyFile.getAbsolutePath());
            JsonObject obj = new JsonObject();
            obj.addProperty("name", playerName);
            obj.addProperty("key", key);
            obj.addProperty("offline-uuid", offlineUUID);
            obj.addProperty("online-uuid", onlineUUID);
            try (FileWriter writer = new FileWriter(keyFile)) {
                gson.toJson(obj, writer);
            } catch (IOException e) {
                LOGGER.error("[AuthKey] Error saving key for {}: {}", playerName, e.getMessage());
            }
        } else {
            LOGGER.info("[AuthKey] Checking key for {} (file: {})", playerName, keyFile.getAbsolutePath());
            try (FileReader reader = new FileReader(keyFile)) {
                JsonObject obj = gson.fromJson(reader, JsonObject.class);
                String savedKey = obj.get("key").getAsString();
                // Обновляем только uuid, не трогаем key
                obj.addProperty("offline-uuid", offlineUUID);
                obj.addProperty("online-uuid", onlineUUID);
                try (FileWriter writer = new FileWriter(keyFile)) {
                    gson.toJson(obj, writer);
                } catch (IOException e) {
                    LOGGER.error("[AuthKey] Error updating uuids for {}: {}", playerName, e.getMessage());
                }
                if (!savedKey.equals(key)) {
                    LOGGER.warn("[AuthKey] Invalid key for {}! Expected: {}, got: {}", playerName, savedKey, key);
                    // Add a delay before kicking
                    if (server != null) {
                        server.addScheduledTask(() -> {
                            player.connection.disconnect(new net.minecraft.util.text.TextComponentString("Invalid authorization key!"));
                        });
                    } else {
                        player.connection.disconnect(new net.minecraft.util.text.TextComponentString("Invalid authorization key!"));
                    }
                } else {
                    LOGGER.info("[AuthKey] Player {} successfully authorized (key matches)", playerName);
                }
            } catch (IOException e) {
                LOGGER.error("[AuthKey] Error reading key for {}: {}", playerName, e.getMessage());
            }
        }
    }

    // Получить offline-uuid по стандартной формуле
    private static UUID getOfflineUUID(String playerName) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes(StandardCharsets.UTF_8));
    }

    // Получить online-uuid через Mojang API (надёжный парсинг)
    private static String getOnlineUUID(String playerName) {
        try {
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + playerName);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setRequestMethod("GET");
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                Scanner scanner = new Scanner(conn.getInputStream(), "UTF-8");
                String json = scanner.useDelimiter("\\A").next();
                scanner.close();
                Gson gson = new Gson();
                JsonObject obj = gson.fromJson(json, JsonObject.class);
                if (obj.has("id")) {
                    String id = obj.get("id").getAsString();
                    // Добавляем тире в UUID
                    return id.replaceFirst(
                        "([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{12})",
                        "$1-$2-$3-$4-$5");
                } else {
                    LOGGER.warn("[AuthKey] Mojang API response for {} does not contain 'id' field! Response: {}", playerName, json);
                }
            } else {
                LOGGER.warn("[AuthKey] Mojang API returned response code {} for {}", responseCode, playerName);
            }
        } catch (Exception e) {
            LOGGER.warn("[AuthKey] Could not fetch online UUID for {}: {}", playerName, e.getMessage());
        }
        return "Pirated Minecraft account";
    }
}