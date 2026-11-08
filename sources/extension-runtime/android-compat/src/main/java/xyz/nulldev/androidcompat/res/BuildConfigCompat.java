package xyz.nulldev.androidcompat.res;

import app.katari.extension.compat.AndroidCompatEnvironment;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * BuildConfig compat class.
 */
public class BuildConfigCompat {
    public static final boolean DEBUG = false;

    //We assume application ID = package name
    public static final String APPLICATION_ID = AndroidCompatEnvironment.current().getPackageName();

    //TODO Build time is hardcoded currently
    public static final String BUILD_TIME;
    static {
        Calendar cal = Calendar.getInstance();
        cal.set(2000, Calendar.JANUARY, 1);
        BUILD_TIME = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'").format(cal.getTime());
    }
}
