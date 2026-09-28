package net.teogemini.nova;

import net.teogemini.nova.registry.ModEntities;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import net.teogemini.nova.server.NovaOwnerSessions;

import net.teogemini.nova.network.NovaControlPayload;

import net.teogemini.nova.registry.ModItems;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.teogemini.nova.registry.ModBlocks;
import net.teogemini.nova.registry.ModBlockEntities;

public class ROBOTNOVA_MOD implements ModInitializer {
	public static final String MOD_ID = "robot-nova_mod";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		net.teogemini.nova.screen.NovaMenus.register();
		ModItems.register();
		ModBlocks.register();
        ModBlockEntities.register();
		ModEntities.register();
        LOGGER.info("NOVA entities registered.");    
		NovaOwnerSessions.register();
		NovaControlPayload.register();
        net.teogemini.nova.entity.NovaChestHandler.register();
        net.teogemini.nova.entity.NovaRallyManager.register();

	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
