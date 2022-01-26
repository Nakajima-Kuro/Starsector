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
import org.lwjgl.util.vector.Vector2f;

public class SWP_IonBlasterOnHitEffect implements OnHitEffectPlugin {

    private static final Color COLOR1 = new Color(100, 255, 200);
    private static final Color COLOR2 = new Color(200, 255, 255);
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

            float hitLevel = 0f;
            float emp = projectile.getEmpAmount() * 0.25f;
            float dam = projectile.getDamageAmount() * 0.25f;
            for (int x = 0; x < 4; x++) {
                float pierceChance = ((ShipAPI) target).getHardFluxLevel() - 0.1f;
                pierceChance *= ship.getMutableStats().getDynamic().getValue(Stats.SHIELD_PIERCED_MULT);

                boolean piercedShield = hitShields && (float) Math.random() < pierceChance;
                if (!hitShields || piercedShield) {
                    hitLevel += 0.25f;
                    ShipAPI empTarget = ship;
                    engine.spawnEmpArcPierceShields(projectile.getSource(), point, empTarget, empTarget,
                            DamageType.ENERGY, dam, emp, 100000f, null, 20f, COLOR1, COLOR2);
                }
            }

            if (hitLevel > 0f) {
                engine.addSmoothParticle(point, ZERO, 300f * hitLevel, hitLevel, 0.75f, COLOR1);
                Global.getSoundPlayer().playSound("swp_ionblaster_hit", 1f, 1f * hitLevel, point, ZERO);
            }
        }
    }
}
