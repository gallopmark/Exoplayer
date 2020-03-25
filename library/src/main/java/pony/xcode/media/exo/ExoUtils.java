package pony.xcode.media.exo;

import android.net.Uri;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.util.Util;

public class ExoUtils {
    /*
     * Makes a best guess to infer the type from a file name.
     *
     * @param fileName Name of the file. It can include the path of the file.
     * @return The content type.
     */
    /**
     * Makes a best guess to infer the type from a {@link Uri}.
     *
     * @param uri The {@link Uri}.
     * @return The content type.
     */
    @C.ContentType
    public static int inferContentType(Uri uri) {
        String path = uri.getPath();
        return path == null ? C.TYPE_OTHER : inferContentType(path);
    }


    /**
     * Infer content type int.
     *
     * @param fileName the file name
     * @return the int
     */
    @C.ContentType
    private static int inferContentType(String fileName) {
        fileName = Util.toLowerInvariant(fileName);
        if (fileName.matches(".*m3u8.*")) {
            return C.TYPE_HLS;
        } else if (fileName.matches(".*mpd.*")) {
            return C.TYPE_DASH;
        } else if (fileName.matches(".*\\.ism(l)?(/manifest(\\(.+\\))?)?")) {
            return C.TYPE_SS;
        } else {
            return C.TYPE_OTHER;
        }
    }
}
