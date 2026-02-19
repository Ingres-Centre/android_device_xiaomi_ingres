package vendor.lineage.leds;

@VintfStability
interface ILeds {
    // Power control
    // Enabled by default.
    void setEnabled(in boolean enabled);
    boolean isEnabled();

    // Brightness
    int getBrightness();
    void setBrightness(in int brightness);

    // Effects
    int getCurrentEffect();
    void setCurrentEffect(in int effectIndex);
    
    List<String> getAvailableEffects();

    // Colors
    // Available only when useCustomColors is true while flushing config.
    int getColor(in int ledIndex);
    void setColor(in int ledIndex, in int rgb);

    int[] getAllColors();

    // Frequency
    // Controls speed of effects.
    int getFrequency();
    void setFrequency(in int frequency);

    // Used to apply new config.
    void flushConfig(in boolean useCustomColors);
}
