package com.Frozzy.AuthKey;

import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.Frozzy.AuthKey.network.NetworkHandler;
import com.google.gson.GsonBuilder;

@Mod.EventBusSubscriber
public class AuthKeyClient {
    private static final String FOLDER_NAME = "AuthKey";
    private static String cachedKey = null;
    private static boolean keySent = false;

    @SideOnly(Side.CLIENT)
    public static void generateAndSaveKey(FMLPreInitializationEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        Session session = mc.getSession();
        String playerName = session.getUsername();

        File mcDir = event.getModConfigurationDirectory().getParentFile(); // .minecraft
        File authKeyDir = new File(mcDir, FOLDER_NAME);
        if (!authKeyDir.exists()) authKeyDir.mkdirs();
        File keyFile = new File(authKeyDir, playerName + ".json");

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        if (!keyFile.exists()) {
            String uuid = UUID.randomUUID().toString();
            JsonObject obj = new JsonObject();
            obj.addProperty("name", playerName);
            obj.addProperty("key", uuid);
            try (FileWriter writer = new FileWriter(keyFile)) {
                gson.toJson(obj, writer);
            } catch (IOException e) {
                e.printStackTrace();
            }
            cachedKey = uuid;
        } else {
            try (FileReader reader = new FileReader(keyFile)) {
                JsonObject obj = gson.fromJson(reader, JsonObject.class);
                cachedKey = obj.get("key").getAsString();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Отправка ключа на сервер при подключении
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void onClientConnected(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.isSingleplayer()) return; // Не отправляем в одиночке
        Session session = mc.getSession();
        String playerName = session.getUsername();

        File mcDir = mc.mcDataDir;
        File authKeyDir = new File(mcDir, FOLDER_NAME);
        if (!authKeyDir.exists()) authKeyDir.mkdirs();
        File keyFile = new File(authKeyDir, playerName + ".json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        // Если файла нет — генерируем новый ключ
        if (!keyFile.exists()) {
            String uuid = UUID.randomUUID().toString();
            JsonObject obj = new JsonObject();
            obj.addProperty("name", playerName);
            obj.addProperty("key", uuid);
            try (FileWriter writer = new FileWriter(keyFile)) {
                gson.toJson(obj, writer);
            } catch (IOException e) {
                e.printStackTrace();
            }
            cachedKey = uuid;
        } else {
            // Если файл есть — читаем ключ
            try (FileReader reader = new FileReader(keyFile)) {
                JsonObject obj = gson.fromJson(reader, JsonObject.class);
                cachedKey = obj.get("key").getAsString();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // Сбросить флаг отправки ключа
        keySent = false;
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player != null && mc.world != null && !mc.isSingleplayer() && !keySent) {
            String playerName = mc.getSession().getUsername();
            if (cachedKey == null) {
                // Попробуем загрузить ключ из файла
                File mcDir = mc.mcDataDir;
                File authKeyDir = new File(mcDir, FOLDER_NAME);
                File keyFile = new File(authKeyDir, playerName + ".json");
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                if (keyFile.exists()) {
                    try (FileReader reader = new FileReader(keyFile)) {
                        JsonObject obj = gson.fromJson(reader, JsonObject.class);
                        cachedKey = obj.get("key").getAsString();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (cachedKey != null) {
                NetworkHandler.INSTANCE.sendToServer(new PacketAuthKey(playerName, cachedKey));
                System.out.println("[AuthKey] Ключ отправлен на сервер через TickEvent");
                keySent = true;
            }
        }
    }
} 