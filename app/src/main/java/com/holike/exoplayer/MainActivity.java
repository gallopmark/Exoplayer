package com.holike.exoplayer;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import pony.xcode.media.exo.ExoMediaInterface;
import pony.xcode.media.jz.JZvd;
import pony.xcode.media.jz.JZVideoView;

public class MainActivity extends AppCompatActivity {
    JZVideoView videoView;
    private boolean isFullscreen = false;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        videoView = findViewById(R.id.videoView);
        int width = getResources().getDimensionPixelSize(R.dimen.dp_340);
        int height = getResources().getDimensionPixelSize(R.dimen.dp_260);
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) videoView.getLayoutParams();
        lp.width = width;
        lp.height = height;
        videoView.setLayoutParams(lp);
        videoView.setVideoSize(width, height, lp);
        videoView.setUp("https://file.holike.com/miniprogram/test/video/5f839692-8eeb-40e0-aa69-aee594e73ada.mp4"
                , "", JZvd.SCREEN_NORMAL, ExoMediaInterface.class);
        videoView.autoStart();
        final FrameLayout container = findViewById(R.id.container);
        videoView.setOnScreenChangedListener(isFullscreen -> {
//            container.removeView(findViewById(R.id.tv));
//            LayoutInflater.from(this).inflate(R.layout.include_helow,container,true);
        });
//        videoView.setOnFullscreenClickListener(new Jzvd.OnFullscreenClickListener() {
//            @Override
//            public boolean onClick() {
//                if (!isFullscreen) {
//                    setFullscreen();
//                } else {
//                    setNormal();
//                }
//                videoView.toggleFullscreenButton(isFullscreen);
//                return true;
//            }
//        });
    }

    private void setNormal() {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) videoView.getLayoutParams();
        videoView.setLayoutParams(lp);
        isFullscreen = false;
    }

    private void setFullscreen() {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) videoView.getLayoutParams();
        lp.topMargin = 0;
        lp.bottomMargin = 0;
        lp.leftMargin = 0;
        lp.rightMargin = 0;
        videoView.setLayoutParams(lp);
        isFullscreen = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        JZvd.goOnPlayOnResume();
    }

    @Override
    public void onBackPressed() {
        if (JZvd.backPress()) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        JZvd.goOnPlayOnPause();
    }

    @Override
    protected void onDestroy() {
        JZvd.releaseAllVideos();
        super.onDestroy();
    }
}
