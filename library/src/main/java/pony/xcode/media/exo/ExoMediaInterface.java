package pony.xcode.media.exo;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.os.Handler;
import android.view.Surface;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DefaultAllocator;
import com.google.android.exoplayer2.video.VideoListener;

import pony.xcode.media.jz.JZMediaInterface;
import pony.xcode.media.jz.JZvd;

public class ExoMediaInterface extends JZMediaInterface implements VideoListener, Player.EventListener {
    private SimpleExoPlayer mExoPlayer = null;
    private long previousSeek = 0;

    public ExoMediaInterface(JZvd jzvd) {
        super(jzvd);
    }

    @Override
    public void start() {
        if (mExoPlayer != null) {
            mExoPlayer.setPlayWhenReady(true);
        }
    }

    @Override
    public void prepare() {
        if (jzvd.jzDataSource == null || jzvd.jzDataSource.urlsMap.isEmpty() || jzvd.jzDataSource.getCurrentUrl() == null) {
            return;
        }
        Context context = jzvd.getContext();
        release();
        handler = new Handler();
        TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory();
        TrackSelector trackSelector = new DefaultTrackSelector(context, videoTrackSelectionFactory);
        // 2. Create the player
        LoadControl loadControl = new DefaultLoadControl.Builder()
                .setAllocator(new DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE))
                .setBufferDurationsMs(360000, 600000, 1000, 5000)
                .setTargetBufferBytes(C.LENGTH_UNSET)
                .setPrioritizeTimeOverSizeThresholds(false).createDefaultLoadControl();
        mExoPlayer = new SimpleExoPlayer.Builder(context, new DefaultRenderersFactory(context))
                .setTrackSelector(trackSelector).setLoadControl(loadControl).build();
        String currentUrl = jzvd.jzDataSource.getCurrentUrl().toString();
        MediaSource videoSource = WholeMediaSource.initMediaSource(context, Uri.parse(currentUrl));
        mExoPlayer.addVideoListener(this);
        mExoPlayer.addListener(this);
        mExoPlayer.setRepeatMode(jzvd.jzDataSource.looping ? Player.REPEAT_MODE_ONE : Player.REPEAT_MODE_OFF);
        mExoPlayer.prepare(videoSource);
        mExoPlayer.setPlayWhenReady(true);
        mExoPlayer.setVideoSurface(new Surface(mSavedSurface));
    }

    @Override
    public void pause() {
        if (mExoPlayer != null) {
            mExoPlayer.setPlayWhenReady(false);
        }
    }

    @Override
    public void seekTo(long time) {
        if (time != previousSeek) {
            if (mExoPlayer != null) {
                mExoPlayer.seekTo(time);
            }
            previousSeek = time;
            jzvd.seekToInAdvance = time;
        }
    }

    @Override
    public void release() {
        if (mExoPlayer != null) {
            mExoPlayer.release();
            mSavedSurface = null;
            mExoPlayer = null;
        }
    }

    @Override
    public long getCurrentPosition() {
        return mExoPlayer == null ? 0 : mExoPlayer.getCurrentPosition();
    }

    @Override
    public long getDuration() {
        return mExoPlayer == null ? 0 : mExoPlayer.getDuration();
    }

    @Override
    public void setSurface(Surface surface) {
        if (mExoPlayer != null) {
            mExoPlayer.setVideoSurface(surface);
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (mSavedSurface == null) {
            mSavedSurface = surface;
            prepare();
        } else {
            jzvd.textureView.setSurfaceTexture(mSavedSurface);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    @Override
    public void onVideoSizeChanged(final int width, final int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
        if (handler != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    jzvd.onVideoSizeChanged(width, height);
                }
            });
        }
    }

    @Override
    public void onRenderedFirstFrame() {

    }

    @Override
    public void onSeekProcessed() {
        if (handler != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    jzvd.onSeekComplete();
                }
            });
        }
    }

    @Override
    public void onPlayerStateChanged(final boolean playWhenReady, int playbackState) {
        if (playbackState == Player.STATE_BUFFERING) {
            if (handler != null && mExoPlayer != null) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        jzvd.setBufferProgress(mExoPlayer.getBufferedPercentage());
                    }
                });
            }
        } else if (playbackState == Player.STATE_READY) {
            if (handler != null) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (playWhenReady) {
                            jzvd.onStatePlaying();
                        }
                    }
                });
            }
        } else if (playbackState == Player.STATE_ENDED) {
            if (handler != null) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        jzvd.onAutoCompletion();
                    }
                });
            }
        }
    }

    @Override
    public void onPlayerError(final ExoPlaybackException error) {
        if (handler != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    jzvd.onError(1000, 1000);
                }
            });
        }
    }
}
