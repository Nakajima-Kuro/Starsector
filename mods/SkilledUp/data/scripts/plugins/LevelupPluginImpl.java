package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.plugins.LevelupPlugin;

public class LevelupPluginImpl implements LevelupPlugin {

	public int getPointsAtLevel(int level) {
		return 1;
	}

	
	public long getXPForLevel(int level) {

		if (level <= 1) return 0;

            float p1 = 10;
            float p2 = 35;
            float p3 = 40;

            float f1 = 1f;
            float f2 = Math.min(1, Math.max(0, level - p1) / 5f);
            float f3 = Math.max(0, level - p2);
            float f4 = Math.max(0, level - p3);

            float p1level = Math.max(0, level - p1 + 1);
            float p2level = Math.max(0, level - p2 + 1);
            float p3level = Math.max(0, level - p3 + 1);

            float mult1 = (1f + (float)level) * 0.5f * (float)level * 1f;
            float mult2 = (1f + (float)p1level) * 0.5f * (float)p1level * 0.25f;
            float mult3 = (1f + (float)p2level) * 0.4f * (float)p2level * 0.15f;
            float mult4 = (1f + (float)p3level) * 0.3f * (float)p3level * 0.1f;

            float base1 = 1500;

            float r = f1 * mult1 * base1 +
                      f2 * mult2 * base1 +
                      f3 * mult3 * base1 +
                      f4 * mult4 * base1;

            return (long)r;
	}


	public int getMaxLevel() {
		return (int) Global.getSettings().getFloat("playerMaxLevel");
	}
	
	
	public static void main(String[] args) {
		LevelupPluginImpl plugin = new LevelupPluginImpl();
		for (int i = 2; i < 20; i++) {
			System.out.println(i + ": " + plugin.getXPForLevel(i));
		}
	}


}
