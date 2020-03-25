package pony.xcode.media.jz;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Constructor;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import pony.xcode.media.exo.R;

public abstract class JZvd extends FrameLayout implements View.OnClickListener, SeekBar.OnSeekBarChangeListener, View.OnTouchListener {

    public static final String TAG = "JZVD";
    public static JZvd CURRENT_JZVD;
    public static LinkedList<ViewGroup> CONTAINER_LIST = new LinkedList<>();

    public static final int SCREEN_NORMAL = 0;
    public static final int SCREEN_FULLSCREEN = 1;
    public static final int SCREEN_TINY = 2;

    public static final int STATE_IDLE = -1;
    public static final int STATE_NORMAL = 0;
    public static final int STATE_PREPARING = 1;
    public static final int STATE_PREPARING_CHANGING_URL = 2;
    public static final int STATE_PREPARED = 3;
    public static final int STATE_PLAYING = 4;
    public static final int STATE_PAUSE = 5;
    public static final int STATE_AUTO_COMPLETE = 6;
    public static final int STATE_ERROR = 7;

    @Deprecated
    public static final int VIDEO_IMAGE_DISPLAY_TYPE_ADAPTER = 0;
    public static final int VIDEO_IMAGE_DISPLAY_TYPE_FILL_PARENT = 1; //DEFAULT
    public static final int VIDEO_IMAGE_DISPLAY_TYPE_FILL_SCROP = 2;
    public static final int VIDEO_IMAGE_DISPLAY_TYPE_ORIGINAL = 3;
    public static boolean TOOL_BAR_EXIST = true;
    public static int FULLSCREEN_ORIENTATION = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
    public static int NORMAL_ORIENTATION = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
    public static boolean SAVE_PROGRESS = false;
    public static boolean WIFI_TIP_DIALOG_SHOWED = false;
    public static int VIDEO_IMAGE_DISPLAY_TYPE = VIDEO_IMAGE_DISPLAY_TYPE_FILL_PARENT;
    public static long lastAutoFullscreenTime = 0;
    public static final int THRESHOLD = 80;
    public static int ON_PLAY_PAUSE_TMP_STATE = 0;//这个考虑不放到库里，去自定义

    public int state = -1;
    public int screen = -1;
    public JZDataSource jzDataSource;
    public int widthRatio = 0;
    public int heightRatio = 0;
    public Class mediaInterfaceClass;
    public JZMediaInterface mediaInterface;
    public int videoRotation = 0;
    protected long gobakFullscreenTime = 0;//这个应该重写一下，刷新列表，新增列表的刷新，不打断播放，应该是个flag

    public int seekToManulPosition = -1;
    public long seekToInAdvance = 0;

    public ImageView startButton;
    public ImageView bottomPlayButton;
    public SeekBar progressBar;
    public LinearLayout bottomVolumeBox; //底部调节音量布局
    public SeekBar volumeProgressBar;  //音量调节seekBar
    public ImageView decreaseVolumeImageView;  //减小音量按钮
    public ImageView increaseVolumeImageView; //增大音量按钮
    public ImageView fullscreenButton;
    public TextView currentTimeTextView, totalTimeTextView;
    public ViewGroup textureViewContainer;
    public ViewGroup topContainer, bottomContainer;
    public JZTextureView textureView;


    protected Timer UPDATE_PROGRESS_TIMER;
    protected int mScreenWidth;
    protected int mScreenHeight;
    protected AudioManager mAudioManager;
    protected ProgressTimerTask mProgressTimerTask;
    protected boolean mTouchingProgressBar;
    protected float mDownX;
    protected float mDownY;
    protected boolean mChangeVolume;
    protected boolean mChangePosition;
    protected boolean mChangeBrightness;
    protected long mGestureDownPosition;
    protected int mGestureDownVolume;
    protected float mGestureDownBrightness;
    protected long mSeekTimePosition;

    public int maxVolume; //系统最大音量值

    private int mNormalWidth, mNormalHeight; //小屏播放时的窗口宽高，设置之后退出全屏缩放到此宽高
    private ViewGroup.LayoutParams mNormalLayoutParams;
    private OnFullscreenClickListener mOnFullscreenClickListener;

    public JZvd(Context context) {
        super(context);
        init(context);
    }

    public JZvd(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public abstract int getLayoutId();

    public void init(Context context) {
        View.inflate(context, getLayoutId(), this);
        mAudioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        //noinspection ConstantConditions
        maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        startButton = findViewById(R.id.jzvd_start_iv);
        bottomPlayButton = findViewById(R.id.jzvd_bottom_play_iv);
        fullscreenButton = findViewById(R.id.jzvd_fullscreen_iv);
        progressBar = findViewById(R.id.jzvd_bottom_seek_progress_sb);
        currentTimeTextView = findViewById(R.id.jzvd_current_time_tv);
        totalTimeTextView = findViewById(R.id.jzvd_total_time_tv);
        bottomVolumeBox = findViewById(R.id.jzvd_bottom_volume_box_ll);
        volumeProgressBar = findViewById(R.id.jzvd_bottom_volume_seek_progress_sb);
        decreaseVolumeImageView = findViewById(R.id.jzvd_bottom_volume_min_iv);
        increaseVolumeImageView = findViewById(R.id.jzvd_bottom_volume_max_iv);
        bottomContainer = findViewById(R.id.jzvd_layout_bottom_ll);
        textureViewContainer = findViewById(R.id.surface_container);
        topContainer = findViewById(R.id.jzvd_layout_top_rl);

        startButton.setOnClickListener(this);
        bottomPlayButton.setOnClickListener(this);
        decreaseVolumeImageView.setOnClickListener(this);
        increaseVolumeImageView.setOnClickListener(this);
        fullscreenButton.setOnClickListener(this);
        progressBar.setOnSeekBarChangeListener(this);
        volumeProgressBar.setOnSeekBarChangeListener(this);
        bottomContainer.setOnClickListener(this);
        textureViewContainer.setOnClickListener(this);
        textureViewContainer.setOnTouchListener(this);

        mScreenWidth = getContext().getResources().getDisplayMetrics().widthPixels;
        mScreenHeight = getContext().getResources().getDisplayMetrics().heightPixels;
        //设置最大音量
        volumeProgressBar.setMax(maxVolume);
        state = STATE_IDLE;
    }

    public void setUp(String url, String title) {
        setUp(new JZDataSource(url, title), SCREEN_NORMAL);
    }

    public void setUp(String url, String title, int screen) {
        setUp(new JZDataSource(url, title), screen);
    }

    public void setUp(JZDataSource jzDataSource, int screen) {
        setUp(jzDataSource, screen, JZMediaSystem.class);
    }

    public void setUp(String url, String title, int screen, Class mediaInterfaceClass) {
        setUp(new JZDataSource(url, title), screen, mediaInterfaceClass);
    }

    public void setUp(JZDataSource jzDataSource, int screen, Class mediaInterfaceClass) {
        if ((System.currentTimeMillis() - gobakFullscreenTime) < 200) return;

        this.jzDataSource = jzDataSource;
        this.screen = screen;
        onStateNormal();
        this.mediaInterfaceClass = mediaInterfaceClass;
    }

    public void setMediaInterface(Class mediaInterfaceClass) {
        reset();
        this.mediaInterfaceClass = mediaInterfaceClass;
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.jzvd_start_iv || i == R.id.jzvd_bottom_play_iv) {
            Logger.i(TAG, "onClick start [" + this.hashCode() + "] ");
            autoStart();
        } else if (i == R.id.jzvd_bottom_volume_min_iv) {
            decreaseVolume();
            //showToastSound(mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)+"");
        } else if (i == R.id.jzvd_bottom_volume_max_iv) {
            increaseVolume();
            //showToastSound(mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)+"");
        } else if (i == R.id.jzvd_fullscreen_iv) {
            if (mOnFullscreenClickListener == null) {
                Logger.i(TAG, "onClick fullscreen [" + this.hashCode() + "] ");
                if (state == STATE_AUTO_COMPLETE) return;
                if (screen == SCREEN_FULLSCREEN) {
                    //quit fullscreen
                    backPress();
                } else {
                    Logger.d("toFullscreenActivity [" + this.hashCode() + "] ");
                    gotoScreenFullscreen();
                }
            } else {
                mOnFullscreenClickListener.onClick(fullscreenButton);
            }
        }
    }

    /**
     * 减小音量
     */
    private void decreaseVolume() {
        int progress = volumeProgressBar.getProgress();
        volumeProgressBar.setProgress(progress > 0 ? progress - 1 : 0);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volumeProgressBar.getProgress(), 0);
    }

    /**
     * 增大音量
     */
    private void increaseVolume() {
        volumeProgressBar.setProgress(volumeProgressBar.getProgress() < maxVolume ?
                volumeProgressBar.getProgress() + 1 : maxVolume);
        if (volumeProgressBar.getProgress() == 100) {
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0);
        } else {
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volumeProgressBar.getProgress(), 0);
        }
    }

    //自动开始播放 --调用之前必须先调用setUp函数初始化视频地址
    public void autoStart() {
        if (jzDataSource == null || jzDataSource.urlsMap.isEmpty() || jzDataSource.getCurrentUrl() == null) {
            Toast.makeText(getContext(), getResources().getString(R.string.no_url), Toast.LENGTH_SHORT).show();
            return;
        }
        if (state == STATE_NORMAL) {
            if (!jzDataSource.getCurrentUrl().toString().startsWith("file") && !
                    jzDataSource.getCurrentUrl().toString().startsWith("/") &&
                    !JZUtils.isWifiConnected(getContext()) && !WIFI_TIP_DIALOG_SHOWED) {//这个可以放到std中
                showWifiDialog();
                return;
            }
            startVideo();
            startButton.setVisibility(View.GONE);
        } else if (state == STATE_PLAYING) {
            Logger.d("pauseVideo [" + this.hashCode() + "] ");
            mediaInterface.pause();
            onStatePause();
        } else if (state == STATE_PAUSE) {
            mediaInterface.start();
            onStatePlaying();
        } else if (state == STATE_AUTO_COMPLETE) {
            startVideo();
            startButton.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        int id = v.getId();
        if (id == R.id.surface_container) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Logger.i(TAG, "onTouch surfaceContainer actionDown [" + this.hashCode() + "] ");
                    mTouchingProgressBar = true;

                    mDownX = x;
                    mDownY = y;
                    mChangeVolume = false;
                    mChangePosition = false;
                    mChangeBrightness = false;
                    break;
                case MotionEvent.ACTION_MOVE:
                    Logger.i(TAG, "onTouch surfaceContainer actionMove [" + this.hashCode() + "] ");
                    float deltaX = x - mDownX;
                    float deltaY = y - mDownY;
                    float absDeltaX = Math.abs(deltaX);
                    float absDeltaY = Math.abs(deltaY);
                    if (screen == SCREEN_FULLSCREEN) {
                        if (!mChangePosition && !mChangeVolume && !mChangeBrightness) {
                            if (absDeltaX > THRESHOLD || absDeltaY > THRESHOLD) {
                                cancelProgressTimer();
                                if (absDeltaX >= THRESHOLD) {
                                    // 全屏模式下的CURRENT_STATE_ERROR状态下,不响应进度拖动事件.
                                    // 否则会因为mediaplayer的状态非法导致App Crash
                                    if (state != STATE_ERROR) {
                                        mChangePosition = true;
                                        mGestureDownPosition = getCurrentPositionWhenPlaying();
                                    }
                                } else {
                                    //如果y轴滑动距离超过设置的处理范围，那么进行滑动事件处理
                                    if (mDownX < mScreenWidth * 0.5f) {//左侧改变亮度
                                        mChangeBrightness = true;
                                        WindowManager.LayoutParams lp = JZUtils.getWindow(getContext()).getAttributes();
                                        if (lp.screenBrightness < 0) {
                                            try {
                                                mGestureDownBrightness = Settings.System.getInt(getContext().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
                                                Logger.i(TAG, "current system brightness: " + mGestureDownBrightness);
                                            } catch (Settings.SettingNotFoundException e) {
                                                e.printStackTrace();
                                            }
                                        } else {
                                            mGestureDownBrightness = lp.screenBrightness * 255;
                                            Logger.i(TAG, "current activity brightness: " + mGestureDownBrightness);
                                        }
                                    } else {//右侧改变声音
                                        mChangeVolume = true;
                                        mGestureDownVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                                    }
                                }
                            }
                        }
                    }
                    if (mChangePosition) {
                        long totalTimeDuration = getDuration();
                        mSeekTimePosition = (int) (mGestureDownPosition + deltaX * totalTimeDuration / mScreenWidth);
                        if (mSeekTimePosition > totalTimeDuration)
                            mSeekTimePosition = totalTimeDuration;
                        String seekTime = JZUtils.stringForTime(mSeekTimePosition);
                        String totalTime = JZUtils.stringForTime(totalTimeDuration);

                        showProgressDialog(deltaX, seekTime, mSeekTimePosition, totalTime, totalTimeDuration);
                    }
                    if (mChangeVolume) {
                        deltaY = -deltaY;
                        int max = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                        int deltaV = (int) (max * deltaY * 3 / mScreenHeight);
                        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mGestureDownVolume + deltaV, 0);
                        //dialog中显示百分比
                        int volumePercent = (int) (mGestureDownVolume * 100 / max + deltaY * 3 * 100 / mScreenHeight);
                        showVolumeDialog(-deltaY, volumePercent);
                    }

                    if (mChangeBrightness) {
                        deltaY = -deltaY;
                        int deltaV = (int) (255 * deltaY * 3 / mScreenHeight);
                        WindowManager.LayoutParams params = JZUtils.getWindow(getContext()).getAttributes();
                        if (((mGestureDownBrightness + deltaV) / 255) >= 1) {//这和声音有区别，必须自己过滤一下负值
                            params.screenBrightness = 1;
                        } else if (((mGestureDownBrightness + deltaV) / 255) <= 0) {
                            params.screenBrightness = 0.01f;
                        } else {
                            params.screenBrightness = (mGestureDownBrightness + deltaV) / 255;
                        }
                        JZUtils.getWindow(getContext()).setAttributes(params);
                        //dialog中显示百分比
                        int brightnessPercent = (int) (mGestureDownBrightness * 100 / 255 + deltaY * 3 * 100 / mScreenHeight);
                        showBrightnessDialog(brightnessPercent);
//                        mDownY = y;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    Logger.i(TAG, "onTouch surfaceContainer actionUp [" + this.hashCode() + "] ");
                    mTouchingProgressBar = false;
                    dismissProgressDialog();
                    dismissVolumeDialog();
                    dismissBrightnessDialog();
                    if (mChangePosition) {
                        mediaInterface.seekTo(mSeekTimePosition);
                        long duration = getDuration();
                        int progress = (int) (mSeekTimePosition * 100 / (duration == 0 ? 1 : duration));
                        progressBar.setProgress(progress);
                    }
                    startProgressTimer();
                    break;
            }
        }
        return false;
    }

    public void onStateNormal() {
        Logger.i(TAG, "onStateNormal " + " [" + this.hashCode() + "] ");
        state = STATE_NORMAL;
        cancelProgressTimer();
        if (mediaInterface != null) mediaInterface.release();
    }

    public void onStatePreparing() {
        Logger.i(TAG, "onStatePreparing " + " [" + this.hashCode() + "] ");
        state = STATE_PREPARING;
        resetProgressAndTime();
    }

    public void onPrepared() {
        Logger.i(TAG, "onPrepared " + " [" + this.hashCode() + "] ");
        state = STATE_PREPARED;
        if (!preLoading) {
            mediaInterface.start();//这里原来是非县城
            preLoading = false;
        }
        if (jzDataSource.getCurrentUrl().toString().toLowerCase().contains("mp3") ||
                jzDataSource.getCurrentUrl().toString().toLowerCase().contains("wma") ||
                jzDataSource.getCurrentUrl().toString().toLowerCase().contains("aac") ||
                jzDataSource.getCurrentUrl().toString().toLowerCase().contains("m4a") ||
                jzDataSource.getCurrentUrl().toString().toLowerCase().contains("wav")) {
            onStatePlaying();
        }
    }

    public boolean preLoading = false;

    public void startPreLoading() {
        preLoading = true;
        startVideo();
    }

    /**
     * 如果STATE_PREPARED就播放，如果没准备完成就走正常的播放函数startVideo();
     */
    public void startVideoAfterPreLoading() {
        if (state == STATE_PREPARED) {
            mediaInterface.start();
        } else {
            preLoading = false;
            startVideo();
        }
    }

    public void onStatePlaying() {
        Logger.i(TAG, "onStatePlaying " + " [" + this.hashCode() + "] ");
        if (state == STATE_PREPARED) {//如果是准备完成视频后第一次播放，先判断是否需要跳转进度。
            if (seekToInAdvance != 0) {
                mediaInterface.seekTo(seekToInAdvance);
                seekToInAdvance = 0;
            } else {
                long position = JZUtils.getSavedProgress(getContext(), jzDataSource.getCurrentUrl());
                if (position != 0) {
                    mediaInterface.seekTo(position);//这里为什么区分开呢，第一次的播放和resume播放是不一样的。 这里怎么区分是一个问题。然后
                }
            }
        }
        state = STATE_PLAYING;
        startProgressTimer();
    }

    public void onStatePause() {
        Logger.i(TAG, "onStatePause " + " [" + this.hashCode() + "] ");
        state = STATE_PAUSE;
        startProgressTimer();
    }

    public void onStateError() {
        Logger.i(TAG, "onStateError " + " [" + this.hashCode() + "] ");
        state = STATE_ERROR;
        cancelProgressTimer();
    }

    public void onStateAutoComplete() {
        Logger.i(TAG, "onStateAutoComplete " + " [" + this.hashCode() + "] ");
        state = STATE_AUTO_COMPLETE;
        cancelProgressTimer();
        progressBar.setProgress(100);
        currentTimeTextView.setText(totalTimeTextView.getText());
    }

    public void onInfo(int what, int extra) {
        Logger.d("onInfo what - " + what + " extra - " + extra);
        if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
            if (state == JZvd.STATE_PREPARED
                    || state == JZvd.STATE_PREPARING_CHANGING_URL) {
                onStatePlaying();//真正的prepared，本质上这是进入playing状态。
            }
        }
    }

    public void onError(int what, int extra) {
        Logger.e("onError " + what + " - " + extra + " [" + this.hashCode() + "] ");
        if (what != 38 && extra != -38 && what != -38 && extra != 38 && extra != -19) {
            onStateError();
            mediaInterface.release();
        }
    }

    public void onAutoCompletion() {
        Runtime.getRuntime().gc();
        Logger.i(TAG, "onAutoCompletion " + " [" + this.hashCode() + "] ");
        cancelProgressTimer();
        dismissBrightnessDialog();
        dismissProgressDialog();
        dismissVolumeDialog();
        onStateAutoComplete();
        mediaInterface.release();
        JZUtils.scanForActivity(getContext()).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        JZUtils.saveProgress(getContext(), jzDataSource.getCurrentUrl(), 0);
    }

    /**
     * 多数表现为中断当前播放
     */
    public void reset() {
        Logger.i(TAG, "reset " + " [" + this.hashCode() + "] ");
        if (state == STATE_PLAYING || state == STATE_PAUSE) {
            long position = getCurrentPositionWhenPlaying();
            JZUtils.saveProgress(getContext(), jzDataSource.getCurrentUrl(), position);
        }
        cancelProgressTimer();
        dismissBrightnessDialog();
        dismissProgressDialog();
        dismissVolumeDialog();
        onStateNormal();
        textureViewContainer.removeAllViews();

        mAudioManager.abandonAudioFocus(onAudioFocusChangeListener);
        JZUtils.scanForActivity(getContext()).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (mediaInterface != null) mediaInterface.release();
    }

    public void setState(int state) {
        setState(state, 0, 0);
    }

    public void setState(int state, int urlMapIndex, int seekToInAdvance) {//后面两个参数干嘛的
        switch (state) {
            case STATE_NORMAL:
                onStateNormal();
                break;
            case STATE_PREPARING:
                onStatePreparing();
                break;
            case STATE_PREPARING_CHANGING_URL:
                changeUrl(urlMapIndex, seekToInAdvance);
                break;
            case STATE_PLAYING:
                onStatePlaying();
                break;
            case STATE_PAUSE:
                onStatePause();
                break;
            case STATE_ERROR:
                onStateError();
                break;
            case STATE_AUTO_COMPLETE:
                onStateAutoComplete();
                break;
        }
    }

    public void setScreen(int screen) {//特殊的个别的进入全屏的按钮在这里设置  只有setup的时候能用上
        switch (screen) {
            case SCREEN_NORMAL:
                setScreenNormal();
                break;
            case SCREEN_FULLSCREEN:
                setScreenFullscreen();
                break;
            case SCREEN_TINY:
                setScreenTiny();
                break;
        }
    }

    public void startVideo() {
        Logger.d("startVideo [" + this.hashCode() + "] ");
        setCurrentJzvd(this);
        try {
            Constructor<JZMediaInterface> constructor = mediaInterfaceClass.getConstructor(JZvd.class);
            this.mediaInterface = constructor.newInstance(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        addTextureView();

        mAudioManager.requestAudioFocus(onAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        JZUtils.scanForActivity(getContext()).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        volumeProgressBar.setProgress(mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC));//设置当前音量

        onStatePreparing();
    }

    public void changeUrl(String url, String title, long seekToInAdvance) {
        changeUrl(new JZDataSource(url, title), seekToInAdvance);
    }

    public void changeUrl(int urlMapIndex, long seekToInAdvance) {
        state = STATE_PREPARING_CHANGING_URL;
        this.seekToInAdvance = seekToInAdvance;
        jzDataSource.currentUrlIndex = urlMapIndex;
        mediaInterface.setSurface(null);
        mediaInterface.release();
        mediaInterface.prepare();
    }

    public void changeUrl(JZDataSource jzDataSource, long seekToInAdvance) {
        state = STATE_PREPARING_CHANGING_URL;
        this.seekToInAdvance = seekToInAdvance;
        this.jzDataSource = jzDataSource;
        mediaInterface.setSurface(null);
        mediaInterface.release();
        mediaInterface.prepare();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (screen == SCREEN_FULLSCREEN || screen == SCREEN_TINY) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        if (widthRatio != 0 && heightRatio != 0) {
            int specWidth = MeasureSpec.getSize(widthMeasureSpec);
            int specHeight = (int) ((specWidth * (float) heightRatio) / widthRatio);
            setMeasuredDimension(specWidth, specHeight);

            int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(specWidth, MeasureSpec.EXACTLY);
            int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(specHeight, MeasureSpec.EXACTLY);
            getChildAt(0).measure(childWidthMeasureSpec, childHeightMeasureSpec);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }

    }

    public void addTextureView() {
        Logger.d("addTextureView [" + this.hashCode() + "] ");
        if (textureView != null) textureViewContainer.removeView(textureView);
        textureView = new JZTextureView(getContext().getApplicationContext());
        textureView.setSurfaceTextureListener(mediaInterface);

        LayoutParams layoutParams = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.CENTER);
        textureViewContainer.addView(textureView, layoutParams);
    }

    public void clearFloatScreen() {
        JZUtils.showStatusBar(getContext());
        JZUtils.setRequestedOrientation(getContext(), NORMAL_ORIENTATION);
        JZUtils.showSystemUI(getContext());

        ViewGroup vg = (ViewGroup) (JZUtils.scanForActivity(getContext())).getWindow().getDecorView();
        vg.removeView(this);
        if (mediaInterface != null) mediaInterface.release();
        CURRENT_JZVD = null;
    }

    public void onVideoSizeChanged(int width, int height) {
        Logger.i(TAG, "onVideoSizeChanged " + " [" + this.hashCode() + "] ");
        if (textureView != null) {
            if (videoRotation != 0) {
                textureView.setRotation(videoRotation);
            }
            textureView.setVideoSize(width, height);
        }
    }

    public void startProgressTimer() {
        Logger.i(TAG, "startProgressTimer: " + " [" + this.hashCode() + "] ");
        cancelProgressTimer();
        UPDATE_PROGRESS_TIMER = new Timer();
        mProgressTimerTask = new ProgressTimerTask();
        UPDATE_PROGRESS_TIMER.schedule(mProgressTimerTask, 0, 300);
    }

    public void cancelProgressTimer() {
        if (UPDATE_PROGRESS_TIMER != null) {
            UPDATE_PROGRESS_TIMER.cancel();
        }
        if (mProgressTimerTask != null) {
            mProgressTimerTask.cancel();
        }
    }

    public void onProgress(int progress, long position, long duration) {
//        Logger.d(TAG, "onProgress: progress=" + progress + " position=" + position + " duration=" + duration);
        if (!mTouchingProgressBar) {
            if (seekToManulPosition != -1) {
                if (seekToManulPosition > progress) {
                    return;
                } else {
                    seekToManulPosition = -1;//这个关键帧有没有必要做
                }
            } else {
                if (progress != 0) progressBar.setProgress(progress);
            }
        }
        if (position != 0) currentTimeTextView.setText(JZUtils.stringForTime(position));
        totalTimeTextView.setText(JZUtils.stringForTime(duration));
    }

    public void setBufferProgress(int bufferProgress) {
        if (bufferProgress != 0) progressBar.setSecondaryProgress(bufferProgress);
    }

    public void resetProgressAndTime() {
        progressBar.setProgress(0);
        progressBar.setSecondaryProgress(0);
        currentTimeTextView.setText(JZUtils.stringForTime(0));
        totalTimeTextView.setText(JZUtils.stringForTime(0));
    }

    public long getCurrentPositionWhenPlaying() {
        long position = 0;
        if (state == STATE_PLAYING ||
                state == STATE_PAUSE) {
            try {
                position = mediaInterface.getCurrentPosition();
            } catch (Exception e) {
                e.printStackTrace();
                return position;
            }
        }
        return position;
    }

    public long getDuration() {
        long duration = 0;
        try {
            duration = mediaInterface.getDuration();
        } catch (Exception e) {
            return duration;
        }
        return duration;
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        Logger.i(TAG, "bottomProgress onStartTrackingTouch [" + this.hashCode() + "] ");
        cancelProgressTimer();
        ViewParent vpdown = getParent();
        while (vpdown != null) {
            vpdown.requestDisallowInterceptTouchEvent(true);
            vpdown = vpdown.getParent();
        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        Logger.i(TAG, "bottomProgress onStopTrackingTouch [" + this.hashCode() + "] ");
        startProgressTimer();
        ViewParent vpup = getParent();
        while (vpup != null) {
            vpup.requestDisallowInterceptTouchEvent(false);
            vpup = vpup.getParent();
        }
        if (state != STATE_PLAYING &&
                state != STATE_PAUSE) return;
        long time = seekBar.getProgress() * getDuration() / 100;
        seekToManulPosition = seekBar.getProgress();
        mediaInterface.seekTo(time);
        Logger.i(TAG, "seekTo " + time + " [" + this.hashCode() + "] ");
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (seekBar.getId() == R.id.jzvd_bottom_volume_seek_progress_sb) {
            if (progress <= 0) {
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
            } else if (progress >= volumeProgressBar.getMax()) {
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0);
            } else {
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
            }
        } else {
            if (fromUser) {
                //设置这个progress对应的时间，给textview
                long duration = getDuration();
                currentTimeTextView.setText(JZUtils.stringForTime(progress * duration / 100));
            }
        }
    }

    public void cloneAJzvd(ViewGroup vg) {
        try {
            Constructor<JZvd> constructor = (Constructor<JZvd>) JZvd.this.getClass().getConstructor(Context.class);
            JZvd jzvd = constructor.newInstance(getContext());
            jzvd.setId(getId());
            vg.addView(jzvd);
            jzvd.setUp(jzDataSource.cloneMe(), SCREEN_NORMAL, mediaInterfaceClass);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void gotoScreenFullscreen() {
        ViewGroup vg = (ViewGroup) getParent();
        vg.removeView(this);
        cloneAJzvd(vg);
        CONTAINER_LIST.add(vg);
        vg = (ViewGroup) (JZUtils.scanForActivity(getContext())).getWindow().getDecorView();//和他也没有关系
        vg.addView(this, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        setScreenFullscreen();
        JZUtils.hideStatusBar(getContext());
        JZUtils.setRequestedOrientation(getContext(), FULLSCREEN_ORIENTATION);
        JZUtils.hideSystemUI(getContext());//华为手机和有虚拟键的手机全屏时可隐藏虚拟键 issue:1326
    }

    public void setVideoSize(int videoWidth, int videoHeight) {
        setVideoSize(videoWidth, videoHeight, null);
    }

    public void setVideoSize(int videoWidth, int videoHeight, ViewGroup.LayoutParams lp) {
        this.mNormalWidth = videoWidth;
        this.mNormalHeight = videoHeight;
        this.mNormalLayoutParams = lp;
    }

    public void gotoScreenNormal() {//goback本质上是goto
        gobakFullscreenTime = System.currentTimeMillis();//退出全屏
        ViewGroup vg = (ViewGroup) (JZUtils.scanForActivity(getContext())).getWindow().getDecorView();
        vg.removeView(this);
        CONTAINER_LIST.getLast().removeAllViews();
        ViewGroup.LayoutParams lp;
        if (mNormalLayoutParams != null) {
            lp = mNormalLayoutParams;
        } else {
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            if (mNormalWidth > 0 && mNormalHeight > 0) {
                layoutParams.width = mNormalWidth;
                layoutParams.height = mNormalHeight;
            }
            layoutParams.gravity = Gravity.CENTER;
            lp = layoutParams;
        }
        CONTAINER_LIST.getLast().addView(this, lp);
        CONTAINER_LIST.pop();

        setScreenNormal();//这块可以放到jzvd中
        JZUtils.showStatusBar(getContext());
        JZUtils.setRequestedOrientation(getContext(), NORMAL_ORIENTATION);
        JZUtils.showSystemUI(getContext());
    }

    public void setScreenNormal() {
        screen = SCREEN_NORMAL;
    }

    public void setScreenFullscreen() {
        screen = SCREEN_FULLSCREEN;
    }

    public void setScreenTiny() {
        screen = SCREEN_TINY;
    }

    //    //重力感应的时候调用的函数，、、这里有重力感应的参数，暂时不能删除
    public void autoFullscreen(float x) {//TODO写道demo中
        if (CURRENT_JZVD != null
                && (state == STATE_PLAYING || state == STATE_PAUSE)
                && screen != SCREEN_FULLSCREEN
                && screen != SCREEN_TINY) {
            if (x > 0) {
                JZUtils.setRequestedOrientation(getContext(), ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            } else {
                JZUtils.setRequestedOrientation(getContext(), ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
            }
            gotoScreenFullscreen();
        }
    }

    public void autoQuitFullscreen() {
        if ((System.currentTimeMillis() - lastAutoFullscreenTime) > 2000
//                && CURRENT_JZVD != null
                && state == STATE_PLAYING
                && screen == SCREEN_FULLSCREEN) {
            lastAutoFullscreenTime = System.currentTimeMillis();
            backPress();
        }
    }

    public void onSeekComplete() {

    }

    public void showWifiDialog() {
    }

    public void showProgressDialog(float deltaX,
                                   String seekTime, long seekTimePosition,
                                   String totalTime, long totalTimeDuration) {
    }

    public void dismissProgressDialog() {

    }

    public void showVolumeDialog(float deltaY, int volumePercent) {

    }

    public void dismissVolumeDialog() {

    }

    public void showBrightnessDialog(int brightnessPercent) {

    }

    public void dismissBrightnessDialog() {

    }

    public Context getApplicationContext() {//这个函数必要吗
        Context context = getContext();
        if (context != null) {
            Context applicationContext = context.getApplicationContext();
            if (applicationContext != null) {
                return applicationContext;
            }
        }
        return context;
    }

    public class ProgressTimerTask extends TimerTask {
        @Override
        public void run() {
            if (state == STATE_PLAYING || state == STATE_PAUSE) {
//                Log.v(TAG, "onProgressUpdate " + "[" + this.hashCode() + "] ");
                post(new Runnable() {
                    @Override
                    public void run() {
                        long position = getCurrentPositionWhenPlaying();
                        long duration = getDuration();
                        int progress = (int) (position * 100 / (duration == 0 ? 1 : duration));
                        onProgress(progress, position, duration);
                    }
                });
            }
        }
    }

    public static AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {//是否新建个class，代码更规矩，并且变量的位置也很尴尬
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    releaseAllVideos();
                    Logger.d("AUDIOFOCUS_LOSS [" + this.hashCode() + "]");
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    try {
                        JZvd player = CURRENT_JZVD;
                        if (player != null && player.state == JZvd.STATE_PLAYING) {
                            player.startButton.performClick();
                        }
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                    Logger.d("AUDIOFOCUS_LOSS_TRANSIENT [" + this.hashCode() + "]");
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    break;
            }
        }
    };

    public static void goOnPlayOnResume() {
        if (CURRENT_JZVD != null) {
            if (CURRENT_JZVD.state == JZvd.STATE_PAUSE) {
                if (ON_PLAY_PAUSE_TMP_STATE == STATE_PAUSE) {
                    CURRENT_JZVD.onStatePause();
                    CURRENT_JZVD.mediaInterface.pause();
                } else {
                    CURRENT_JZVD.onStatePlaying();
                    CURRENT_JZVD.mediaInterface.start();
                }
                ON_PLAY_PAUSE_TMP_STATE = 0;
            }
        }
    }

    public static void goOnPlayOnPause() {
        if (CURRENT_JZVD != null) {
            if (CURRENT_JZVD.state == JZvd.STATE_AUTO_COMPLETE ||
                    CURRENT_JZVD.state == JZvd.STATE_NORMAL ||
                    CURRENT_JZVD.state == JZvd.STATE_PREPARING ||
                    CURRENT_JZVD.state == JZvd.STATE_ERROR) {
                JZvd.releaseAllVideos();
            } else {
                ON_PLAY_PAUSE_TMP_STATE = CURRENT_JZVD.state;
                CURRENT_JZVD.onStatePause();
                CURRENT_JZVD.mediaInterface.pause();
            }
        }
    }

    public static void startFullscreenDirectly(Context context, Class _class, String url, String title) {
        startFullscreenDirectly(context, _class, new JZDataSource(url, title));
    }

    public static void startFullscreenDirectly(Context context, Class _class, JZDataSource jzDataSource) {
        JZUtils.hideStatusBar(context);
        JZUtils.setRequestedOrientation(context, FULLSCREEN_ORIENTATION);
        JZUtils.hideSystemUI(context);

        ViewGroup vp = (ViewGroup) JZUtils.scanForActivity(context).getWindow().getDecorView();
        try {
            Constructor<JZvd> constructor = _class.getConstructor(Context.class);
            final JZvd jzvd = constructor.newInstance(context);
            LayoutParams lp = new LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            vp.addView(jzvd, lp);
            jzvd.setUp(jzDataSource, JZVideoView.SCREEN_FULLSCREEN);
            jzvd.startVideo();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void releaseAllVideos() {
        Logger.d("releaseAllVideos");
        if (CURRENT_JZVD != null) {
            CURRENT_JZVD.reset();
            CURRENT_JZVD = null;
        }
    }

    public static boolean backPress() {
        Logger.i(TAG, "backPress");
        if (CONTAINER_LIST.size() != 0 && CURRENT_JZVD != null) {//判断条件，因为当前所有goBack都是回到普通窗口
            CURRENT_JZVD.gotoScreenNormal();
            return true;
        } else if (CONTAINER_LIST.size() == 0 && CURRENT_JZVD != null && CURRENT_JZVD.screen != SCREEN_NORMAL) {//退出直接进入的全屏
            CURRENT_JZVD.clearFloatScreen();
            return true;
        }
        return false;
    }

    public static void setCurrentJzvd(JZvd jzvd) {
        if (CURRENT_JZVD != null) CURRENT_JZVD.reset();
        CURRENT_JZVD = jzvd;
    }

    public static void clearSavedProgress(Context context, String url) {
        JZUtils.clearSavedProgress(context, url);
    }

    public static void setTextureViewRotation(int rotation) {
        if (CURRENT_JZVD != null && CURRENT_JZVD.textureView != null) {
            CURRENT_JZVD.textureView.setRotation(rotation);
        }
    }

    public static void setVideoImageDisplayType(int type) {
        JZvd.VIDEO_IMAGE_DISPLAY_TYPE = type;
        if (CURRENT_JZVD != null && CURRENT_JZVD.textureView != null) {
            CURRENT_JZVD.textureView.requestLayout();
        }
    }

    //设置是否打印信息
    public static void setDebugMode(boolean isDebug) {
        Logger.LOG_DEBUG = isDebug;
    }

    //设置底部音量调节布局是否可见
    public void setBottomVolumeBoxEnabled(boolean isEnabled) {
        bottomVolumeBox.setVisibility(isEnabled ? View.VISIBLE : View.GONE);
    }

    //设置横竖屏切换按钮是否可见
    public void setFullscreenEnabled(boolean isEnabled) {
        fullscreenButton.setVisibility(isEnabled ? View.VISIBLE : View.GONE);
    }

    //改变横竖屏按钮状态
    public void toggleFullscreenButton(boolean isFullscreen) {
        if (isFullscreen) {
            fullscreenButton.setImageResource(R.drawable.jz_shrink);
        } else {
            fullscreenButton.setImageResource(R.drawable.jz_enlarge);
        }
    }

    /**
     * 全屏按钮点击
     * 返回true 拦截 false 不拦截
     */
    public interface OnFullscreenClickListener {
        void onClick(ImageView fullscreenButton);
    }

    public void setOnFullscreenClickListener(OnFullscreenClickListener l) {
        this.mOnFullscreenClickListener = l;
    }
}
