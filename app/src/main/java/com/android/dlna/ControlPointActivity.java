package com.android.dlna;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;

import com.android.dlna.manager.DLNADeviceManager;
import com.android.dlna.player.control.IController;
import com.android.dlna.player.control.MultiPointController;

import org.cybergarage.upnp.Action;
import org.cybergarage.upnp.Device;
import org.cybergarage.upnp.control.ActionListener;

public class ControlPointActivity extends Activity implements ControlPointView.OnMediaControlActionListener, SeekBar.OnSeekBarChangeListener {
    public static final int REQ_CHOOSE_DEVICE = 0x01;
    private ControlPointView controlPointView;
    private Device mDevice;
    private IController mController;
    private String currentUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control_point);
        controlPointView = findViewById(R.id.controlPointView);
        controlPointView.setOnMediaControlActionListener(this);
        controlPointView.setOnSeekChangeListener(this);
        mController = new MultiPointController();
        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mController.getTransportState(mDevice);
            }
        });

        currentUri = getIntent().getStringExtra("url");
        mController.setPlayMonitor(new IController.PlayerMonitor() {
            @Override
            public void onPreparing() {

            }

            @Override
            public void onGetMediaDuration(int totalTimeSeconds) {
                controlPointView.setDuration(totalTimeSeconds);
            }

            @Override
            public void onGetMaxVolume(int max) {
                controlPointView.setMaxVolume(max);
            }

            @Override
            public void onMuteStatusChanged(boolean mute) {

            }

            @Override
            public void onVolumeChanged(int current) {
                controlPointView.setVolume(current);
            }

            @Override
            public void onPlay() {
                controlPointView.updatePlayBtnStatus(true);
            }

            @Override
            public void onPause() {
                controlPointView.updatePlayBtnStatus(false);
            }

            @Override
            public void onStop() {

            }

            @Override
            public void onError() {

            }

            @Override
            public void onProgressUpdated(int currentTimeSeconds) {
                controlPointView.updateProcess(currentTimeSeconds);
            }

            @Override
            public void onSeekComplete() {

            }

            @Override
            public void onComplete() {

            }

            @Override
            public void onPlayItemChanged() {

            }
        });
        checkDevice();
        controlPointView.getVolumeSeekBar().setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mController.setVolume(mDevice, seekBar.getProgress());
            }
        });
    }

    private void onDeviceReady() {
        play(currentUri);
        mDevice.setActionListener(new ActionListener() {
            @Override
            public boolean actionControlReceived(Action action) {
                return false;
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQ_CHOOSE_DEVICE:
                if (resultCode == RESULT_OK) {
                    //refresh device
                    mDevice = DLNADeviceManager.getInstance().getCurrentDevice();
                    onDeviceReady();
                } else {
                    Toast.makeText(this, "无可用设备", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private void checkDevice() {
        if (DLNADeviceManager.getInstance().getCurrentDevice() == null) {
            Intent intent = new Intent(this, DiscoveryActivity.class);
            startActivityForResult(intent, REQ_CHOOSE_DEVICE);
        } else {
            mDevice = DLNADeviceManager.getInstance().getCurrentDevice();
            onDeviceReady();
        }
    }


    private void play(String uri) {
        mController.play(mDevice, uri);
    }

    private void pause() {
        mController.pause(mDevice);
    }

    private void resumePlay(final int pausedTimeSeconds) {
        mController.resume(mDevice, pausedTimeSeconds);
    }

    private void stop() {
        mController.stop(mDevice);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        controlPointView.updateProcess(progress);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mController.onSeekBegin();
    }

    @Override
    public void onStopTrackingTouch(final SeekBar seekBar) {
        final int progress = seekBar.getProgress();
        mController.seek(mDevice, progress);
    }

    @Override
    public void onPlayBtnClicked() {
        if (mController.isPlaying()) {
            pause();
            return;
        }
        if (mController.isPaused()) {
            resumePlay(controlPointView.getCurrentDuration());
        } else {
            play(currentUri);
        }
    }

    @Override
    public void onNextBtnClicked() {

    }

    @Override
    public void onPreviousBtnClicked() {

    }

    @Override
    public void onFFBtnClicked() {

    }

    @Override
    public void onRewBtnClicked() {

    }

    Toast toast;

    private void toast(String msg) {
        if (toast == null) {
            toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        } else {
            toast.cancel();
            toast.setText(msg);
        }
        toast.show();
    }
}
