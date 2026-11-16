package net.teogemini.nova.client.model;

import net.teogemini.nova.ROBOTNOVA_MOD;
import net.teogemini.nova.item.NovaDormantItem;
import software.bernie.geckolib.model.DefaultedGeoModel;

public class NovaDormantItemModel extends DefaultedGeoModel<NovaDormantItem> {
    public NovaDormantItemModel() {
        super(ROBOTNOVA_MOD.id("nova"));
    }

    @Override
    protected String subtype() {
        return "entity";
    }
}
