package caprou.app.impl.render.animation;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;

/**
 * This code is part of Liticane's Animation Library.
 *
 * @author Liticane
 * @since 22/03/2024
 */
@Getter
@RequiredArgsConstructor
@SuppressWarnings("unused")
public enum Easing {
    LINEAR(x -> x),

    EASE_IN_SINE(x -> 1 - Math.cos((x * Math.PI) / 2)),
    EASE_OUT_SINE(x -> Math.sin((x * Math.PI) / 2)),
    EASE_IN_OUT_SINE(x -> -(Math.cos(Math.PI * x) - 1) / 2),

    EASE_IN_CIRC(x -> 1 - Math.sqrt(1 - Math.pow(x, 2))),
    EASE_OUT_CIRC(x -> Math.sqrt(1 - Math.pow(x - 1, 2))),
    EASE_IN_OUT_CIRC(x -> x < 0.5 ? (1 - Math.sqrt(1 - Math.pow(2 * x, 2))) / 2 : (Math.sqrt(1 - Math.pow(-2 * x + 2, 2)) + 1) / 2),

    EASE_IN_EXPO(x -> x == 0 ? 0 : Math.pow(2, 10 * x - 10)),
    EASE_OUT_EXPO(x -> x == 1 ? 1 : 1 - Math.pow(2, -10 * x)),
    EASE_IN_OUT_EXPO(x -> x == 0 ? 0 : x == 1 ? 1 : x < 0.5 ? Math.pow(2, 20 * x - 10) / 2 : (2 - Math.pow(2, -20 * x + 10)) / 2),

    EASE_IN_CUBIC(x -> Math.pow(x, 3)),
    EASE_OUT_CUBIC(x -> 1 - Math.pow(1 - x, 3)),
    EASE_IN_OUT_CUBIC(x -> x < 0.5 ? 4 * Math.pow(x, 3) : 1 - Math.pow(-2 * x + 2, 3) / 2),

    EASE_IN_OUT_QUAD(x -> x < 0.5 ? 2 * x * x : 1 - Math.pow(-2 * x + 2, 2) / 2),
    EASE_OUT_QUAD(x -> 1 - (1 - x) * (1 - x)),

    EASE_IN_OUT_BACK(x -> x < 0.5
            ? (Math.pow(2 * x, 2) * ((1.70158 * 1.525 + 1) * 2 * x - 1.70158 * 1.525)) / 2
            : (Math.pow(2 * x - 2, 2) * ((1.70158 * 1.525 + 1) * (x * 2 - 2) + 1.70158 * 1.525) + 2) / 2),
    EASE_OUT_BACK(x -> 1 + 2.70158 * Math.pow(x - 1, 3) + 1.70158 * Math.pow(x - 1, 2));

    private final Function<Double, Double> function;
}