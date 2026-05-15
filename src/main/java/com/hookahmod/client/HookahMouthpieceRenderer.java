package com.hookahmod.client;

import com.hookahmod.HookahMod;
import com.hookahmod.item.HookahMouthpieceItem;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class HookahMouthpieceRenderer extends GeoItemRenderer<HookahMouthpieceItem> {
    public HookahMouthpieceRenderer() {
        super(new DefaultedItemGeoModel<>(HookahMod.id("hookah_mouthpiece")));
    }
}
