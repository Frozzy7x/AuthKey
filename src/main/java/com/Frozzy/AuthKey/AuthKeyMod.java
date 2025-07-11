package com.Frozzy.AuthKey;

import com.Frozzy.AuthKey.network.NetworkHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.SidedProxy;

/**
 * Основной класс мода AuthKey для Forge 1.12.2
 * Мод предназначен для авторизации игроков по уникальному ключу при offline-mode сервере.
 */
@Mod(modid = AuthKeyMod.MODID, name = AuthKeyMod.NAME, version = AuthKeyMod.VERSION)
public class AuthKeyMod {
    public static final String MODID = "authkey";
    public static final String NAME = "AuthKey";
    public static final String VERSION = "0.1.1";

    @SidedProxy(clientSide = "com.Frozzy.AuthKey.ClientProxy", serverSide = "com.Frozzy.AuthKey.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        System.out.println(
        "        AuthKey ON");
        proxy.preInit(event);
        NetworkHandler.registerPackets();
        // Здесь будет регистрация каналов, обработчиков и т.д.
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // Здесь будет основная инициализация
    }

    @Mod.EventHandler
    public void onServerStarting(FMLServerStartingEvent event) {
        // Здесь можно регистрировать команды, если потребуется
    }
} 