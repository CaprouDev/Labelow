package caprou.app.impl.render.animation;

import caprou.app.impl.render.util.time.TimeUtil;
import lombok.Getter;
import lombok.Setter;

/**
 * This code is part of Liticane's Animation Library.
 *
 * @author Liticane
 * @since 22/03/2024
 */
@Getter
@Setter
public class Animation {

    private final TimeUtil stopwatch = new TimeUtil();
    private Easing easing;

    private long duration;
    private double startPoint, endPoint, value;

    private boolean finished;

    public Animation(final Easing easing, final long duration) {
        this.easing = easing;
        this.duration = duration;
    }

    public void run(final double endPoint) {
        if (this.endPoint != endPoint) {
            this.endPoint = endPoint;
            this.reset();
        } else {
            this.finished = stopwatch.finished(duration);

            if (this.finished) {
                this.value = endPoint;
                return;
            }
        }

        final double newValue = this.easing.getFunction().apply(this.getProgress());

        if (this.value > endPoint) {
            this.value = this.startPoint - (this.startPoint - endPoint) * newValue;
        } else {
            this.value = this.startPoint + (endPoint - this.startPoint) * newValue;
        }
    }

    public void loop(double start, double end) {
        long elapsedTime = System.currentTimeMillis() - this.stopwatch.getLastMS();
        long fullCycleDuration = duration * 2;

        long currentCycleTime = elapsedTime % fullCycleDuration;
        boolean goingToEnd = currentCycleTime < duration;

        double phaseProgress;
        if (goingToEnd) {
            phaseProgress = (double)currentCycleTime / duration;
        } else {
            phaseProgress = (double)(fullCycleDuration - currentCycleTime) / duration;
        }

        double easedProgress = this.easing.getFunction().apply(phaseProgress);
        this.value = start + (end - start) * easedProgress;

        this.finished = false;
    }

    public double getProgress() {
        return (double) (System.currentTimeMillis() - this.stopwatch.getLastMS()) / (double) this.duration;
    }

    public void reset() {
        this.stopwatch.reset();
        this.startPoint = value;
        this.finished = false;
    }

    public void restart() {
        this.stopwatch.reset();
        this.startPoint = 0.0F;
        this.finished = false;
    }
}