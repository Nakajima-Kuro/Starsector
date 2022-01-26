package data.scripts.hullmods;

public class SWP_NoPackage extends SWP_BasePackage {

    @Override
    protected String getHullModId() {
        return SWP_NO_PACKAGE;
    }

    @Override
    protected String getFlavorText() {
        return "This hull is already too specialized to be upgraded with an Imperial modification suite.";
    }
}
