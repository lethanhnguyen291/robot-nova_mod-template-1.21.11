package net.teogemini.nova.client.model;

import net.teogemini.nova.ROBOTNOVA_MOD;
import net.teogemini.nova.entity.NovaEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class NovaModel extends DefaultedEntityGeoModel<NovaEntity> {
    public NovaModel() {
        super(ROBOTNOVA_MOD.id("nova"));
    }
}