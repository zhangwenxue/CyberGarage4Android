package com.android.dlna.player.control;

import org.cybergarage.upnp.Device;

public interface IController {
    int STATE_STOPPED = 0x01;
    int STATE_PLAYING = 0x02;
    int STATE_TRANSITIONING = 0x03;
    int STATE_PAUSED_PLAYBACK = 0x04;
    int STATE_PAUSED_RECORDING = 0x05;
    int STATE_RECORDING = 0x06;
    int STATE_NO_MEDIA_PRESENT = 0x07;
    int STATE_ERR = 0x08;

    interface PlayerMonitor {
        void onPreparing();

        void onGetMediaDuration(int totalTimeSeconds);

        void onGetMaxVolume(int max);

        void onMuteStatusChanged(boolean mute);

        void onVolumeChanged(int current);

        void onPlay();

        void onPause();

        void onStop();

        void onError();

        void onProgressUpdated(int currentTimeSeconds);

        void onSeekComplete();

        void onComplete();

        void onPlayItemChanged();

    }

    void setPlayMonitor(PlayerMonitor monitor);

    /**
     * Play the video with the video path.
     *
     * @param device The device be controlled.
     * @param path   The path of the video.
     */
    void play(Device device, String path);

    boolean isPlaying();

    boolean isPaused();

    /**
     * Go on playing the video from the position.
     *
     * @param device            The device be controlled.
     * @param pausedTimeSeconds Seconds
     */
    void resume(Device device, int pausedTimeSeconds);


    /**
     * All the state is "STOPPED" // "PLAYING" // "TRANSITIONING"//
     * "PAUSED_PLAYBACK"// "PAUSED_RECORDING"// "RECORDING" //
     * "NO_MEDIA_PRESENT//
     */
    void getTransportState(Device device);

    /**
     * Get the min volume value,this must be 0.
     *
     * @param device The device be controlled.
     */
    void getMinVolumeValue(Device device);

    /**
     * Get the max volume value, usually it is 100.
     *
     * @param device The device be controlled.
     */
    void getMaxVolumeValue(Device device);

    /**
     * Seek the playing video to a target position.
     *
     * @param device               The device be controlled.
     * @param targetPosTimeSeconds Target position we want to set.
     */
    void seek(Device device, int targetPosTimeSeconds);

    /**
     * Get the current playing position of the video.
     *
     * @param device The device be controlled.
     */
    void getPositionInfo(Device device);

    /**
     * Get the duration of the video playing.
     *
     * @param device The device be controlled.
     *               null.
     */
    void getMediaDuration(Device device);

    /**
     * Mute the device or not.
     *
     * @param device The device be controlled.
     * @param mute   true or false
     */
    void setMute(Device device, boolean mute);

    /**
     * Get if the device is mute.
     *
     * @param device The device be controlled.
     */
    void getMute(Device device);

    /**
     * Set the device's voice.
     *
     * @param device The device be controlled.
     * @param volume Target voice want be set.
     */
    void setVolume(Device device, int volume);

    /**
     * Get the current voice of the device.
     *
     * @param device The device be controlled.
     */
    void getVolume(Device device);

    /**
     * Stop to play.
     *
     * @param device The device to controlled.
     */
    void stop(Device device);

    /**
     * Pause the playing video.
     *
     * @param device The device to controlled.
     */
    void pause(Device device);

    void onSeekBegin();
}
