package pony.xcode.media.jz;

import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Surface;


import java.lang.reflect.Method;
import java.util.Map;

/**
 * 实现系统的播放引擎
 */
public class JZMediaSystem extends JZMediaInterface implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnErrorListener, MediaPlayer.OnInfoListener, MediaPlayer.OnVideoSizeChangedListener {

    public MediaPlayer mediaPlayer;

    public JZMediaSystem(JZvd jzvd) {
        super(jzvd);
    }

    @Override
    public void prepare() {
        release();
        mMediaHandlerThread = new HandlerThread("JZVD");
        mMediaHandlerThread.start();
        mMediaHandler = new Handler(mMediaHandlerThread.getLooper());//主线程还是非主线程，就在这里
        handler = new Handler();
        mMediaHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mediaPlayer = new MediaPlayer();
                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    mediaPlayer.setLooping(jzvd.jzDataSource.looping);
                    mediaPlayer.setOnPreparedListener(JZMediaSystem.this);
                    mediaPlayer.setOnCompletionListener(JZMediaSystem.this);
                    mediaPlayer.setOnBufferingUpdateListener(JZMediaSystem.this);
                    mediaPlayer.setScreenOnWhilePlaying(true);
                    mediaPlayer.setOnSeekCompleteListener(JZMediaSystem.this);
                    mediaPlayer.setOnErrorListener(JZMediaSystem.this);
                    mediaPlayer.setOnInfoListener(JZMediaSystem.this);
                    mediaPlayer.setOnVideoSizeChangedListener(JZMediaSystem.this);
                    Class<MediaPlayer> clazz = MediaPlayer.class;
                    Method method = clazz.getDeclaredMethod("setDataSource", String.class, Map.class);
                    method.invoke(mediaPlayer, jzvd.jzDataSource.getCurrentUrl().toString(), jzvd.jzDataSource.headerMap);
                    mediaPlayer.prepareAsync();
                    mediaPlayer.setSurface(new Surface(mSavedSurface));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void start() {
        mMediaHandler.post(new Runnable() {
            @Override
            public void run() {
                mediaPlayer.start();
            }
        });
    }

    @Override
    public void pause() {
        mMediaHandler.post(new Runnable() {
            @Override
            public void run() {
                mediaPlayer.pause();
            }
        });
    }

    @Override
    public void seekTo(final long time) {
        mMediaHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mediaPlayer.seekTo((int) time);
                } catch (Exception ignored) {
                }
            }
        });
    }

    @Override
    public void release() {//not perfect change you later
        if (mMediaHandler != null && mMediaHandlerThread != null && mediaPlayer != null) {//不知道有没有妖孽
            final HandlerThread tmpHandlerThread = mMediaHandlerThread;
            final MediaPlayer tmpMediaPlayer = mediaPlayer;
            mSavedSurface = null;
            mMediaHandler.post(new Runnable() {
                @Override
                public void run() {
                    tmpMediaPlayer.setSurface(null);
                    tmpMediaPlayer.release();
                    tmpHandlerThread.quit();
                }
            });
            mediaPlayer = null;
        }
    }

    @Override
    public long getCurrentPosition() {
        if (mediaPlayer != null) {
            return mediaPlayer.getCurrentPosition();
        } else {
            return 0;
        }
    }

    @Override
    public long getDuration() {
        if (mediaPlayer != null) {
            return mediaPlayer.getDuration();
        } else {
            return 0;
        }
    }


    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        //如果是mp3音频，走这里
        handler.post(new Runnable() {
            @Override
            public void run() {
                jzvd.onPrepared();
            }
        });
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                jzvd.onAutoCompletion();
            }
        });
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, final int percent) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                jzvd.setBufferProgress(percent);
            }
        });
    }

    @Override
    public void onSeekComplete(MediaPlayer mediaPlayer) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                jzvd.onSeekComplete();
            }
        });
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, final int what, final int extra) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                jzvd.onError(what, extra);
            }
        });
        return true;
    }

    @Override
    public boolean onInfo(MediaPlayer mediaPlayer, final int what, final int extra) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                jzvd.onInfo(what, extra);
            }
        });
        return false;
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mediaPlayer, final int width, final int height) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                jzvd.onVideoSizeChanged(width, height);
            }
        });
    }

    @Override
    public void setSurface(Surface surface) {
        mediaPlayer.setSurface(surface);
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
}
