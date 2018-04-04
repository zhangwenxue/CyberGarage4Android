package com.android.dlna;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;


public class ControlPointView extends RelativeLayout implements View.OnClickListener {

    public interface OnMediaControlActionListener {
        void onPlayBtnClicked();

        void onNextBtnClicked();

        void onPreviousBtnClicked();

        void onFFBtnClicked();

        void onRewBtnClicked();
    }

    private Button mPlayBtn;
    private Button mPreviousBtn;
    private Button mNextBtn;
    private Button mFFBtn;
    private Button mRewBtn;
    private SeekBar mSeekBar;
    private SeekBar mVolumeSeekBar;
    private TextView mDurationTxt;
    private TextView mCurDurationTxt;
    private boolean isSeekable = false;

    private int totalDuration;
    private int currentDuration;

    private OnMediaControlActionListener mControlActionListener;

    public ControlPointView(Context context) {
        this(context, null);
    }

    public ControlPointView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ControlPointView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.layout_control_point, this, true);
        init();
    }

    @SuppressLint({"WrongViewCast", "ClickableViewAccessibility"})
    private void init() {
        mSeekBar = findViewById(R.id.seekBar);
        mDurationTxt = findViewById(R.id.durationTxt);
        mCurDurationTxt = findViewById(R.id.currentDurationTxt);

        mPlayBtn = findViewById(R.id.playBtn);
        mPreviousBtn = findViewById(R.id.previousBtn);
        mNextBtn = findViewById(R.id.nextBtn);
        mFFBtn = findViewById(R.id.ffBtn);
        mRewBtn = findViewById(R.id.rewBtn);
        mVolumeSeekBar = findViewById(R.id.volumeSeekBar);

        mPlayBtn.setOnClickListener(this);
        mPreviousBtn.setOnClickListener(this);
        mNextBtn.setOnClickListener(this);
        mFFBtn.setOnClickListener(this);
        mRewBtn.setOnClickListener(this);
        /*mSeekBar.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return isSeekable;
            }
        });*/
        reset();
    }

    public SeekBar getVolumeSeekBar() {
        return mVolumeSeekBar;
    }

    public void setDuration(int totalTimeSeconds) {
        mSeekBar.setMax(totalTimeSeconds);
        totalDuration = totalTimeSeconds;
        currentDuration = 0;
        mDurationTxt.setText(formatTimeSeconds(totalDuration));
        mCurDurationTxt.setText(formatTimeSeconds(currentDuration));
    }

    public void setSeekable(boolean seekAble) {
        this.isSeekable = seekAble;
    }

    public void setMaxVolume(int max) {
        mVolumeSeekBar.setMax(max);
    }

    public void setVolume(int volume) {
        mVolumeSeekBar.setProgress(volume);
    }


    public void reset() {
        mDurationTxt.setText("__");
        mCurDurationTxt.setText("__");
        mSeekBar.setMax(100);
        mSeekBar.setProgress(0);
        updatePlayBtnStatus(false);
        totalDuration = 0;
        currentDuration = 0;
    }

    public void updateProcess(int timeSeconds) {
        mCurDurationTxt.setText(formatTimeSeconds(timeSeconds));
        mSeekBar.setProgress(timeSeconds);
        currentDuration = timeSeconds;
    }

    public void setOnMediaControlActionListener(OnMediaControlActionListener l) {
        this.mControlActionListener = l;
    }

    public void setOnSeekChangeListener(SeekBar.OnSeekBarChangeListener listener) {
        mSeekBar.setOnSeekBarChangeListener(listener);
    }

    @Override
    public void onClick(View v) {
        if (mControlActionListener == null) {
            return;
        }
        switch (v.getId()) {
            case R.id.playBtn:
                mControlActionListener.onPlayBtnClicked();
                break;
            case R.id.previousBtn:
                mControlActionListener.onPreviousBtnClicked();
                break;
            case R.id.nextBtn:
                mControlActionListener.onNextBtnClicked();
                break;
            case R.id.ffBtn:
                mControlActionListener.onFFBtnClicked();
                break;
            case R.id.rewBtn:
                mControlActionListener.onRewBtnClicked();
                break;
            default:
                break;
        }
    }

    public void updatePlayBtnStatus(boolean isPlaying) {
        if (isPlaying) {
            mPlayBtn.setBackgroundResource(R.mipmap.ic_pause_circle_outline_white_36dp);
        } else {
            mPlayBtn.setBackgroundResource(R.mipmap.ic_play_circle_outline_white_36dp);
        }
    }

    public int getCurrentDuration() {
        return currentDuration;
    }

    private String formatTimeSeconds(int seconds) {
        if (seconds < 10) {
            return String.format("00:0%s", seconds);
        }
        if (seconds < 60) {
            return String.format("00:%s", seconds);
        }
        int minutes = seconds / 60;
        int secs = seconds % 60;
        if (minutes < 10) {
            if (secs < 10) {
                return String.format("0%s:0%s", minutes, secs);
            }
            return String.format("0%s:%s", minutes, secs);
        } else {
            return String.format("%s:%s", minutes, secs);
        }
    }
}
