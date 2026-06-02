package com.hookahmod.item;

import com.hookahmod.smoking.StackingEffects;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;

public class BuffTobaccoItem extends AbstractTobaccoItem {

    public record Buff(Holder<MobEffect> effect, int amplifier, int baseSeconds) {}

    private final List<Buff> buffs;
    private final Vector3f smokeColor;

    public BuffTobaccoItem(Properties properties, int intoxication, @Nullable Vector3f smokeColor, Buff... buffs) {
        super(properties, TobaccoCategory.REGULAR, intoxication);
        this.smokeColor = smokeColor;
        this.buffs = List.of(buffs);
    }

    @Override
    public void onExhale(ServerLevel level, ServerPlayer smoker, float charge, float effectMult, float combatMult) {
        for (Buff b : buffs) {
            StackingEffects.addStacked(smoker, b.effect(), b.amplifier(), b.baseSeconds(), charge, effectMult);
        }
    }

    @Override
    @Nullable
    public Vector3f smokeColor() {
        return smokeColor;
    }
}
