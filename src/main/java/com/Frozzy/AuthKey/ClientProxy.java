package com.Frozzy.AuthKey;

import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {
    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        AuthKeyClient.generateAndSaveKey(event);
    }
} 