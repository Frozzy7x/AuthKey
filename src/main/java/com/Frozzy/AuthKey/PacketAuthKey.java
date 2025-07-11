package com.Frozzy.AuthKey;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class PacketAuthKey implements IMessage {
    private String playerName;
    private String key;

    public PacketAuthKey() {}
    public PacketAuthKey(String playerName, String key) {
        this.playerName = playerName;
        this.key = key;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int nameLen = buf.readInt();
        byte[] nameBytes = new byte[nameLen];
        buf.readBytes(nameBytes);
        this.playerName = new String(nameBytes);
        int keyLen = buf.readInt();
        byte[] keyBytes = new byte[keyLen];
        buf.readBytes(keyBytes);
        this.key = new String(keyBytes);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        byte[] nameBytes = playerName.getBytes();
        buf.writeInt(nameBytes.length);
        buf.writeBytes(nameBytes);
        byte[] keyBytes = key.getBytes();
        buf.writeInt(keyBytes.length);
        buf.writeBytes(keyBytes);
    }

    public static class Handler implements IMessageHandler<PacketAuthKey, IMessage> {
        @Override
        public IMessage onMessage(PacketAuthKey message, MessageContext ctx) {
            MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
            server.addScheduledTask(() -> {
                EntityPlayerMP player = ctx.getServerHandler().player;
                AuthKeyServer.handleAuthKey(player, message.key);
            });
            return null;
        }
    }
} 