package net.teogemini.nova.client.screen;

import net.teogemini.nova.entity.NovaEntity;

/** Keeps the existing control-screen entry point while the structure editor evolves separately. */
public final class NovaConstructionScreen extends NovaStructureScreen {
    public NovaConstructionScreen(NovaEntity nova, boolean building) {
        super(nova, building);
    }
}
