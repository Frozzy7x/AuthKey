package com.Frozzy.AuthKey.network;

import com.Frozzy.AuthKey.AuthKeyMod;
import com.Frozzy.AuthKey.PacketAuthKey;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.relauncher.Side;

public class NetworkHandler {
    public static final SimpleNetworkWrapper INSTANCE = new SimpleNetworkWrapper(AuthKeyMod.MODID);

    public static void registerPackets() {
        INSTANCE.registerMessage(PacketAuthKey.Handler.class, PacketAuthKey.class, 0, Side.SERVER);
    }
} 