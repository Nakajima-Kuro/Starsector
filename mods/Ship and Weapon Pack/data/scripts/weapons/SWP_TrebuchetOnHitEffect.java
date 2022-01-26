package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnHitEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import data.scripts.util.SWP_Util;
import java.awt.Color;
import java.util.List;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.util.vector.Vector2f;

public class SWP_TrebuchetOnHitEffect implements OnHitEffectPlugin {

    private static final float AREA_EFFECT = 250f;
    private static final float AREA_EFFECT_INNER = 50f;

    private static final Color COLOR1 = new Color(25, 100, 155);
    private static final Color COLOR2 = new Color(255, 255, 255);

    private static final Vector2f ZERO = new Vector2f();

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult,
            CombatEngineAPI engine) {
        if (target == null || point == null) {
            return;
        }
        float emp = projectile.getEmpAmount();
        float dam = projectile.getDamageAmount();

        List<ShipAPI> targets = SWP_Util.getShipsWithinRange(point, AREA_EFFECT);

        SWP_Util.filterObscuredTargets(target, point, targets, true, true, false);

        if (target instanceof ShipAPI) {
            ShipAPI ship = (ShipAPI) target;
            targets.remove(ship);

            engine.spawnEmpArc(projectile.getSource(), point, null, target, DamageType.ENERGY, 0.0f, emp, 100000f,
                    "tachyon_lance_emp_impact", 40f, COLOR1, COLOR2);
        }

        for (ShipAPI ship : targets) {
            float distance = MathUtils.getDistance(ship.getLocation(), point);
            float reduction = 1;
            if (distance > AREA_EFFECT_INNER + ship.getCollisionRadius()) {
                reduction = (AREA_EFFECT - distance) / (AREA_EFFECT - AREA_EFFECT_INNER);
            }

            engine.spawnEmpArc(projectile.getSource(), point, null, ship, DamageType.ENERGY, dam * reduction, emp
                    * reduction, 500f, "tachyon_lance_emp_impact",
                    40f, COLOR1, COLOR2);
        }

        for (int i = 0; i < 6; i++) {
            Vector2f location = new Vector2f(projectile.getLocation().x + (float) Math.random() * 200.0f + 100.0f,
                    projectile.getLocation().y);
            location = VectorUtils.rotateAroundPivot(location, projectile.getLocation(), (float) Math.random() * 360f,
                    location);
            engine.spawnEmpArc(projectile.getSource(), point, null, new SimpleEntity(location), DamageType.ENERGY, 0.0f,
                    0.0f, 100000f,
                    "tachyon_lance_emp_impact", 20f, COLOR1, COLOR2);
        }

        Global.getSoundPlayer().playSound("disabled_large", 0.75f, 0.8f, point, ZERO);
    }
}
