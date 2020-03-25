package pony.xcode.media.jz;

import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Surface;
import android.view.TextureView;

public abstract class JZMediaInterface implements TextureView.SurfaceTextureListener {

    public SurfaceTexture mSavedSurface;
    public HandlerThread mMediaHandlerThread;
    public Handler mMediaHandler;
    public Handler handler;
    public JZvd jzvd;


    public JZMediaInterface(JZvd jzvd) {
        this.jzvd = jzvd;
    }

    public abstract void start();

    public abstract void prepare();

    public abstract void pause();

    public abstract void seekTo(long time);

    public abstract void release();

    public abstract long getCurrentPosition();

    public abstract long getDuration();

    public abstract void setSurface(Surface surface);
}
