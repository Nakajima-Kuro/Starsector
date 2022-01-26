package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnHitEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import data.scripts.SWPModPlugin;
import data.scripts.hullmods.TEM_LatticeShield;
import java.awt.Color;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.util.vector.Vector2f;

public class SWP_IonTorpedoOnHitEffect implements OnHitEffectPlugin {

    private static final Color EXPLOSION_COLOR = new Color(150, 175, 255, 150);
    private static final float FLUX_DAMAGE = 1000f;
    private static final Color PARTICLE_COLOR = new Color(175, 200, 255, 150);
    private static final Vector2f ZERO = new Vector2f();

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult,
            CombatEngineAPI engine) {
        boolean hitShields = shieldHit;
        if (point == null) {
            return;
        }

        if (target instanceof ShipAPI) {
            ShipAPI ship = (ShipAPI) target;
            if (!hitShields) {
                if (SWPModPlugin.templarsExists && ship.getVariant().getHullMods().contains("tem_latticeshield")
                        && TEM_LatticeShield.shieldLevel(ship) > 0f) {
                    hitShields = true;
                }
            }
            if (projectile.getSource() == null) {
                if (hitShields) {
                    ship.getFluxTracker().increaseFlux(FLUX_DAMAGE, true);
                } else {
                    ship.getFluxTracker().increaseFlux(FLUX_DAMAGE, false);
                }
            } else {
                if (hitShields) {
                    ship.getFluxTracker().increaseFlux(FLUX_DAMAGE * projectile.getSource().getMutableStats().getEnergyWeaponDamageMult().getModifiedValue(), true);
                } else {
                    ship.getFluxTracker().increaseFlux(FLUX_DAMAGE * projectile.getSource().getMutableStats().getEnergyWeaponDamageMult().getModifiedValue(), false);
                }
            }
            float emp = projectile.getEmpAmount() * 0.25f;
            float dam = projectile.getDamageAmount() * 0.25f;
            for (int x = 0; x < 4; x++) {
                float pierceChance = ((ShipAPI) target).getHardFluxLevel() - 0.1f;
                pierceChance *= ship.getMutableStats().getDynamic().getValue(Stats.SHIELD_PIERCED_MULT);

                boolean piercedShield = hitShields && (float) Math.random() < pierceChance;

                if (!hitShields || piercedShield) {
                    ShipAPI empTarget = ship;
                    engine.spawnEmpArcPierceShields(projectile.getSource(), point, empTarget, empTarget,
                            DamageType.FRAGMENTATION, dam, emp, 100000f, null, 20f,
                            PARTICLE_COLOR, EXPLOSION_COLOR);
                }
            }
        }
        if (hitShields) {
            Global.getSoundPlayer().playSound("swp_ionblaster_hit", 1.1f, 0.6f, point, ZERO);
        } else {
            Global.getSoundPlayer().playSound("swp_ionblaster_hit", 0.9f, 0.7f, point, ZERO);
        }
        for (int x = 0; x < 3; x++) {
            float angle = (float) Math.random() * 360f;
            float distance = (float) Math.random() * 100f + 150f;
            Vector2f point1 = MathUtils.getPointOnCircumference(point, distance, angle);
            Vector2f point2 = new Vector2f(point);
            engine.spawnEmpArc(projectile.getSource(), point1, new SimpleEntity(point1), new SimpleEntity(point2),
                    DamageType.ENERGY, 0f, 0f, 1000f, null, 15f,
                    EXPLOSION_COLOR, PARTICLE_COLOR);
        }
    }
}
