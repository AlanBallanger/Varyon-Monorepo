package fr.varyon.holograms.animation;

public enum EasingType {
    LINEAR,
    EASE_IN,
    EASE_OUT,
    EASE_IN_OUT,
    BOUNCE;

    public double apply(double t) {
        return switch (this) {
            case LINEAR -> t;
            case EASE_IN -> t * t;
            case EASE_OUT -> t * (2 - t);
            case EASE_IN_OUT -> t < 0.5 ? 2 * t * t : -1 + (4 - 2 * t) * t;
            case BOUNCE -> {
                if (t < 1 / 2.75) yield 7.5625 * t * t;
                else if (t < 2 / 2.75) { t -= 1.5 / 2.75; yield 7.5625 * t * t + 0.75; }
                else if (t < 2.5 / 2.75) { t -= 2.25 / 2.75; yield 7.5625 * t * t + 0.9375; }
                else { t -= 2.625 / 2.75; yield 7.5625 * t * t + 0.984375; }
            }
        };
    }
}
