package com.hookahmod.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SoapBubbleParticle extends TextureSheetParticle {

    private SoapBubbleParticle(ClientLevel level, double x, double y, double z,
                               double xSpeed, double ySpeed, double zSpeed,
                               SpriteSet sprites) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.friction = 0.985F;
        this.gravity = -0.015F;
        this.hasPhysics = false;
        this.lifetime = 80 + this.random.nextInt(61);
        this.quadSize = 0.07F + this.random.nextFloat() * 0.08F;
        this.alpha = 0.78F;
        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.removed) return;
        this.xd += Math.sin(this.age * 0.22D + this.lifetime) * 0.0009D;
        this.zd += Math.cos(this.age * 0.18D + this.lifetime) * 0.0009D;
        if (this.age > this.lifetime - 20) {
            this.alpha = Math.max(0.0F, (this.lifetime - this.age) / 20.0F * 0.78F);
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new SoapBubbleParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
        }
    }
}
