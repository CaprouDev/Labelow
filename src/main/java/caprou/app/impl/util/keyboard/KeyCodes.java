package caprou.app.impl.util.keyboard;

public final class KeyCodes {
    private KeyCodes() {
    }

    public static final int CTRL_LEFT = 341;
    public static final int CTRL_RIGHT = 345;
    public static final int SHIFT_LEFT = 340;
    public static final int SHIFT_RIGHT = 344;

    public static final int ENTER = 257;
    public static final int BACKSPACE = 259;
    public static final int DELETE = 261;

    public static final int LEFT = 263;
    public static final int RIGHT = 262;
    public static final int UP = 265;
    public static final int DOWN = 264;

    public static final int HOME = 268;
    public static final int END = 269;

    public static final int A = 81;
    public static final int C = 67;
    public static final int X = 88;
    public static final int V = 86;

    public static boolean isCtrl(int keyCode) {
        return keyCode == CTRL_LEFT || keyCode == CTRL_RIGHT;
    }

    public static boolean isShift(int keyCode) {
        return keyCode == SHIFT_LEFT || keyCode == SHIFT_RIGHT;
    }
}
