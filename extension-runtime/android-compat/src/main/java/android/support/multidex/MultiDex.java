package android.support.multidex;

import android.content.Context;
import android.util.Log;

/**
 * MultiDex that does nothing.
 */
public class MultiDex {
    private static final String TAG = "MultiDex";

    public static void install(Context context) {
        Log.d(TAG, "Ignoring MultiDex installation attempt for app: " + context.getPackageName());
    }
}
