package com.hookahmod.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.CampfireSmokeParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ColorParticleOption;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ColoredHookahSmokeParticle extends CampfireSmokeParticle {

    private ColoredHookahSmokeParticle(ClientLevel level, double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed,
                                       ColorParticleOption color) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed, false);
        setAlpha(0.9F * color.getAlpha());
        setColor(tint(color.getRed()), tint(color.getGreen()), tint(color.getBlue()));
    }

    private static float tint(float value) {
        return 0.18F + value * 0.82F;
    }

    public static class Provider implements ParticleProvider<ColorParticleOption> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(ColorParticleOption type, ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            ColoredHookahSmokeParticle particle = new ColoredHookahSmokeParticle(
                    level, x, y, z, xSpeed, ySpeed, zSpeed, type
            );
            particle.pickSprite(sprites);
            return particle;
        }
    }
}
