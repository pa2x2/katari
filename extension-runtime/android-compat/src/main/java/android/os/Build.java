package android.os;

/**
 * Android compat class
 */
public class Build {
    // Katari's desktop host emulates a user build, as on users' phones.
    public static boolean IS_DEBUGGABLE = false;

    //TODO Make all of this stuff configurable!

    // As Katari's SystemProperties leaves every property unset, the values are what Android's Build reads then: it
    // never leaves them null.
    public static final String UNKNOWN = "unknown";
    public static final String BOARD = UNKNOWN;
    public static final String BOOTLOADER = UNKNOWN;
    public static final String BRAND = UNKNOWN;
    /** @deprecated */
    @Deprecated
    public static final String CPU_ABI = null;
    /** @deprecated */
    @Deprecated
    public static final String CPU_ABI2 = null;
    public static final String DEVICE = UNKNOWN;
    public static final String DISPLAY = UNKNOWN;
    public static final String FINGERPRINT = "unknown/unknown/unknown:unknown/unknown/unknown:unknown/unknown";
    public static final String HARDWARE = UNKNOWN;
    public static final String HOST = UNKNOWN;
    public static final String ID = UNKNOWN;
    public static final String MANUFACTURER = UNKNOWN;
    public static final String MODEL = UNKNOWN;
    public static final String PRODUCT = UNKNOWN;
    /** @deprecated */
    @Deprecated
    public static final String RADIO = UNKNOWN;
    public static final String SERIAL = UNKNOWN;
    public static final String[] SUPPORTED_32_BIT_ABIS = new String[0];
    public static final String[] SUPPORTED_64_BIT_ABIS = new String[0];
    public static final String[] SUPPORTED_ABIS = new String[0];
    public static final String TAGS = UNKNOWN;
    public static final long TIME = 0L;
    public static final String TYPE = UNKNOWN;
    public static final String USER = UNKNOWN;

    public Build() {
        throw new RuntimeException("This class cannot be instantiated!");
    }

    public static String getRadioVersion() {
        throw new RuntimeException("Stub!");
    }

    public static class VERSION_CODES {
        public static final int BASE = 1;
        public static final int BASE_1_1 = 2;
        public static final int CUPCAKE = 3;
        public static final int CUR_DEVELOPMENT = 10000;
        public static final int DONUT = 4;
        public static final int ECLAIR = 5;
        public static final int ECLAIR_0_1 = 6;
        public static final int ECLAIR_MR1 = 7;
        public static final int FROYO = 8;
        public static final int GINGERBREAD = 9;
        public static final int GINGERBREAD_MR1 = 10;
        public static final int HONEYCOMB = 11;
        public static final int HONEYCOMB_MR1 = 12;
        public static final int HONEYCOMB_MR2 = 13;
        public static final int ICE_CREAM_SANDWICH = 14;
        public static final int ICE_CREAM_SANDWICH_MR1 = 15;
        public static final int JELLY_BEAN = 16;
        public static final int JELLY_BEAN_MR1 = 17;
        public static final int JELLY_BEAN_MR2 = 18;
        public static final int KITKAT = 19;
        public static final int KITKAT_WATCH = 20;
        public static final int LOLLIPOP = 21;
        public static final int LOLLIPOP_MR1 = 22;
        public static final int M = 23;
        public static final int N = 24;
        public static final int N_MR1 = 25;
        public static final int O = 26;
        public static final int O_MR1 = 27;
        public static final int P = 28;
        public static final int Q = 29;
        public static final int R = 30;
        public static final int S = 31;
        public static final int S_V2 = 32;
        public static final int TIRAMISU = 33;
        public static final int UPSIDE_DOWN_CAKE = 34;
        public static final int VANILLA_ICE_CREAM = 35;
        public static final int BAKLAVA = 36;
    }

    // The release whose API the stand-ins model: their Context is Android 7.0's, and the members extensions call on
    // newer releases' code paths (Context.getMainExecutor, ImageDecoder, Settings.Global's device name) aren't
    // implemented, while the paths extensions keep for 7.0 use what is (Build.MODEL, BitmapFactory).
    public static class VERSION {
        public static final String BASE_OS = "";
        public static final String CODENAME = "REL";
        public static final String INCREMENTAL = UNKNOWN;
        public static final int PREVIEW_SDK_INT = 0;
        public static final String RELEASE = "7.0";
        /** @deprecated */
        @Deprecated
        public static final String SDK = "24";
        public static final int SDK_INT = VERSION_CODES.N;
        public static final String SECURITY_PATCH = "";

        public VERSION() {
            throw new RuntimeException("Stub!");
        }
    }
}
