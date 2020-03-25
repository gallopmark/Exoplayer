package pony.xcode.media.exo;

import android.content.Context;
import android.net.Uri;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;

public class WholeMediaSource {

    public static MediaSource initMediaSource(Context context,  Uri uri) {
        return initMediaSource(context, uri, null);
    }

    public static MediaSource initMediaSource(Context context, Uri uri, DataSource.Factory factory) {
        int streamType = ExoUtils.inferContentType(uri);
        if (factory == null) {
            factory = new JDefaultDataSourceFactory(context);
        }
        switch (streamType) {
            case C.TYPE_SS:
                return new SsMediaSource.Factory(factory).createMediaSource(uri);
            case C.TYPE_DASH:
                return new DashMediaSource.Factory(factory).createMediaSource(uri);
            case C.TYPE_HLS:
                return new HlsMediaSource.Factory(factory).createMediaSource(uri);
            case C.TYPE_OTHER:
            default:
                return new ProgressiveMediaSource.Factory(factory).createMediaSource(uri);
        }
    }
}
