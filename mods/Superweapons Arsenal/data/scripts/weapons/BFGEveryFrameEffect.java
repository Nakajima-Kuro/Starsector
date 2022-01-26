package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.GameState;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lazywizard.lazylib.combat.CombatUtils;
import java.awt.Color;


	public class BFGEveryFrameEffect implements EveryFrameWeaponEffectPlugin {
		
		
		private static final Color COLOR = new Color(70, 200, 70, 150);
		private static final float SEARCH_RANGE = 1000f;
		DamagingProjectileAPI s;
		float Counter =0f;
    
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused()) {return;}
		
		if (Global.getSettings().getCurrentState() == GameState.COMBAT){
			
			for(DamagingProjectileAPI p : engine.getProjectiles())
				if(p.getWeapon()==weapon)
					s = p;
			
			if(s != null){
				Counter = Counter + amount;
				if (!s.didDamage() && !s.isFading()){
					Vector2f point = s.getLocation();
					float damage = weapon.getDamage().getDamage() / 6.6f;      //Base Shock damage is 15%
			
					if (Counter > 0.33f){
						Counter = 0f;
						for (ShipAPI test : CombatUtils.getShipsWithinRange(point, SEARCH_RANGE)){
							if (test != weapon.getShip() &&
								test.isAlive() &&
								!test.isAlly() &&
								!test.isStationModule() &&
								weapon.getShip().getOriginalOwner() != test.getOriginalOwner()){
									engine.spawnEmpArc(s.getSource(), point, test, test,
										DamageType.ENERGY,
										damage / 3f,						//Shock damage is reduced to 33% of its original value but will occur 3 times every second.
										10f, 																						     //This is done to reduce overall effectiveness against armor.
										100000f, // max range 
										"BFG_Shock",
										40f, // thickness
										COLOR,
										COLOR.brighter());
							}
						}
					}
				}
			}
		}
	}
}
