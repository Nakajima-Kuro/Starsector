package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import data.scripts.everyframe.TargetingLeadIndicator;
import java.io.IOException;
import org.apache.log4j.Level;
import org.json.JSONException;

public class TLIModPlugin extends BaseModPlugin {

    @Override
    public void onApplicationLoad() {
        try {
            TargetingLeadIndicator.reloadSettings();
        } catch (IOException | JSONException e) {
            Global.getLogger(TLIModPlugin.class).log(Level.ERROR, "Lead Indicator load failed: " + e.getMessage());
        }
    }
}
