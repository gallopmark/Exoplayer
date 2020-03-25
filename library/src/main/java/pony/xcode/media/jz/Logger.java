package pony.xcode.media.jz;

import android.text.TextUtils;
import android.util.Log;

class Logger {
    static boolean LOG_DEBUG = true;

    static void i(String tag, String message) {
        if (LOG_DEBUG && !TextUtils.isEmpty(message)) {
            Log.i(tag, message);
        }
    }

    static void d(String message) {
        if (LOG_DEBUG && !TextUtils.isEmpty(message)) {
            Log.d(JZvd.TAG, message);
        }
    }

    static void e(String message) {
        if (LOG_DEBUG && !TextUtils.isEmpty(message)) {
            Log.e(JZvd.TAG, message);
        }
    }
}
