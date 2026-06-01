package com.hookahmod.smoking;

public enum IntoxicationBand {
    SOBER("intoxication.hookahmod.sober"),
    RELAXED("intoxication.hookahmod.relaxed"),
    HIGH("intoxication.hookahmod.high"),
    TRIP("intoxication.hookahmod.trip"),
    OVERDOSE("intoxication.hookahmod.overdose");

    private final String localizationKey;

    IntoxicationBand(String localizationKey) {
        this.localizationKey = localizationKey;
    }

    public String localizationKey() {
        return localizationKey;
    }

    public boolean atLeast(IntoxicationBand band) {
        return ordinal() >= band.ordinal();
    }
}
