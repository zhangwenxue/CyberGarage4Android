package com.android.dlna.player.control;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import org.cybergarage.upnp.Action;
import org.cybergarage.upnp.Device;
import org.cybergarage.upnp.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MultiPointController implements IController {
    private static final int MSG_UPDATE_PROGRESS = 0x01;
    private final Handler HANDLER = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_PROGRESS:
                    HANDLER.removeMessages(MSG_UPDATE_PROGRESS);
                    getPositionInfo(mDevice);
                    HANDLER.sendEmptyMessageDelayed(MSG_UPDATE_PROGRESS, 1000);
                    break;
                default:
                    break;
            }
        }
    };
    private static final String AVTransport1 = "urn:schemas-upnp-org:service:AVTransport:1";
    private static final String SetAVTransportURI = "SetAVTransportURI";
    private static final String RenderingControl = "urn:schemas-upnp-org:service:RenderingControl:1";
    private static final String Play = "Play";
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private PlayerMonitor playerMonitor;
    private Device mDevice;
    private boolean isPlaying;
    private boolean isPaused;

    @Override
    public void setPlayMonitor(PlayerMonitor monitor) {
        this.playerMonitor = monitor;
    }

    @Override
    public void play(final Device device, final String path) {
        this.mDevice = device;
        if (playerMonitor != null) {
            HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    playerMonitor.onPreparing();
                }
            });
        }
        executor.execute(new Runnable() {
            @Override
            public void run() {
                final boolean ret = playSync(device, path);
                if (playerMonitor != null) {
                    HANDLER.post(new Runnable() {
                        @Override
                        public void run() {
                            isPlaying = ret;
                            if (ret) {
                                playerMonitor.onPlay();
                                getMediaDuration(device);
                                getMaxVolumeValue(device);
                                getVolume(device);
                            } else {
                                playerMonitor.onError();
                            }
                        }
                    });
                }
            }
        });
    }

    @Override
    public boolean isPlaying() {
        return isPlaying;
    }

    @Override
    public boolean isPaused() {
        return isPaused;
    }

    @Override
    public void resume(final Device device, final int pausedTimeSeconds) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                final boolean ret = resumeSync(device, pausedTimeSeconds);
                if (playerMonitor != null) {
                    HANDLER.post(new Runnable() {
                        @Override
                        public void run() {
                            isPlaying = ret;
                            isPaused = !ret;
                            if (ret) {
                                HANDLER.sendEmptyMessage(MSG_UPDATE_PROGRESS);
                                playerMonitor.onPlay();
                            } else {
                                HANDLER.removeMessages(MSG_UPDATE_PROGRESS);
                                playerMonitor.onError();
                            }
                        }
                    });
                }
            }
        });
    }


    @Override
    public void getTransportState(final Device device) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                final int ret = getTransportStateSync(device);
                System.out.print("current state intValue:" + ret);
            }
        });
    }


    @Override
    public void getMinVolumeValue(final Device device) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                final String value = getVolumeRangeSync(device, "MinValue");
                if (playerMonitor != null) {
                    HANDLER.post(new Runnable() {
                        @Override
                        public void run() {
                            if (TextUtils.isEmpty(value)) {
                                playerMonitor.onError();
                            }
                        }
                    });
                }
            }
        });
    }

    @Override
    public void getMaxVolumeValue(final Device device) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                final String value = getVolumeRangeSync(device, "MaxValue");
                if (playerMonitor != null) {
                    HANDLER.post(new Runnable() {
                        @Override
                        public void run() {
                            if (TextUtils.isEmpty(value)) {
                                playerMonitor.onError();
                                return;
                            }
                            playerMonitor.onGetMaxVolume(Integer.valueOf(value));
                        }
                    });
                }
            }
        });
    }


    @Override
    public void seek(final Device device, final int targetPosTimeSeconds) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                final boolean ret = seekToSync(device, format(targetPosTimeSeconds));
                if (playerMonitor != null) {
                    HANDLER.post(new Runnable() {
                        @Override
                        public void run() {
                            if (ret) {
                                playerMonitor.onSeekComplete();
                                HANDLER.sendEmptyMessage(MSG_UPDATE_PROGRESS);
                            } else {
                                playerMonitor.onError();
                                HANDLER.removeMessages(MSG_UPDATE_PROGRESS);
                            }
                        }
                    });
                }
            }
        });
    }


    @Override
    public void getPositionInfo(final Device device) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                final String ret = getPositionInfoSync(device);
                if (playerMonitor != null) {
                    HANDLER.post(new Runnable() {
                        @Override
                        public void run() {
                            if (TextUtils.isEmpty(ret)) {
                                playerMonitor.onError();
                                return;
                            }
                            int process = "NOT_IMPLEMENTED".equals(ret) ? -1 : format(ret);
                            playerMonitor.onProgressUpdated(process);
                            if (process < 0) {
                                HANDLER.removeMessages(MSG_UPDATE_PROGRESS);
                            }
                        }
                    });
                }
            }
        });
    }

    public void getMediaDuration(final Device device) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                final String ret = getMediaDurationSync(device);
                if (playerMonitor != null) {
                    HANDLER.post(new Runnable() {
                        @Override
                        public void run() {
                            if (TextUtils.isEmpty(ret)) {
                                playerMonitor.onError();
                                return;
                            }
                            int duration = "NOT_IMPLEMENTED".equals(ret) ? -1 : format(ret);
                            playerMonitor.onGetMediaDuration(duration);
                            if (duration > 0) {
                                HANDLER.sendEmptyMessage(MSG_UPDATE_PROGRESS);
                            } else {
                                HANDLER.removeMessages(MSG_UPDATE_PROGRESS);
                            }
                        }
                    });
                }
            }
        });
    }


    @Override
    public void setMute(final Device device, final boolean mute) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                boolean ret = setMuteSync(device, mute);
                if (playerMonitor != null && ret) {
                    HANDLER.post(new Runnable() {
                        @Override
                        public void run() {
                            playerMonitor.onMuteStatusChanged(mute);
                        }
                    });
                }
            }
        });
    }


    @Override
    public void getMute(final Device device) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                final String ret = getMuteSync(device);
                if (playerMonitor != null) {
                    HANDLER.post(new Runnable() {
                        @Override
                        public void run() {
                            if (TextUtils.isEmpty(ret)) {
                                playerMonitor.onError();
                                return;
                            }
                            playerMonitor.onMuteStatusChanged("1".equals(ret));
                        }
                    });
                }
            }
        });
    }


    @Override
    public void setVolume(final Device device, final int volume) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                final boolean ret = setVolumeSync(device, volume);
                if (playerMonitor != null) {
                    HANDLER.post(new Runnable() {
                        @Override
                        public void run() {
                            if (!ret) {
                                playerMonitor.onError();
                                return;
                            }
                            playerMonitor.onVolumeChanged(volume);
                        }
                    });
                }
            }
        });
    }


    @Override
    public void getVolume(final Device device) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                final int ret = getVolumeSync(device);
                if (playerMonitor != null) {
                    HANDLER.post(new Runnable() {
                        @Override
                        public void run() {
                            playerMonitor.onVolumeChanged(ret);
                        }
                    });
                }
            }
        });
    }

    @Override
    public void pause(final Device device) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                final boolean ret = pauseSync(device);
                if (playerMonitor != null) {
                    HANDLER.post(new Runnable() {
                        @Override
                        public void run() {
                            isPaused = ret;
                            isPlaying = !ret;
                            if (!ret) {
                                playerMonitor.onError();
                            } else {
                                playerMonitor.onPause();
                                HANDLER.removeMessages(MSG_UPDATE_PROGRESS);
                            }
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onSeekBegin() {
        HANDLER.removeMessages(MSG_UPDATE_PROGRESS);
    }

    @Override
    public void stop(final Device device) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                final boolean ret = stopSync(device);
                if (playerMonitor != null) {
                    HANDLER.post(new Runnable() {
                        @Override
                        public void run() {
                            if (isPlaying) {
                                isPlaying = !ret;
                            }
                            if (isPaused) {
                                isPaused = !ret;
                            }
                            if (!ret) {
                                playerMonitor.onError();
                            } else {
                                playerMonitor.onStop();
                                HANDLER.removeMessages(MSG_UPDATE_PROGRESS);
                            }
                        }
                    });
                }
            }
        });
    }

    //===========================private methods====================================//

    private synchronized boolean playSync(Device device, String path) {
        Service service = device.getService(AVTransport1);
        if (service == null) {
            return false;
        }

        final Action action = service.getAction(SetAVTransportURI);
        if (action == null) {
            return false;
        }

        final Action playAction = service.getAction(Play);
        if (playAction == null) {
            return false;
        }

        if (TextUtils.isEmpty(path)) {
            return false;
        }

        action.setArgumentValue("InstanceID", 0);
        action.setArgumentValue("CurrentURI", path);
        action.setArgumentValue("CurrentURIMetaData", 0);
        if (!action.postControlAction()) {
            return false;
        }

        playAction.setArgumentValue("InstanceID", 0);
        playAction.setArgumentValue("Speed", "1");
        return playAction.postControlAction();
    }

    private synchronized boolean resumeSync(Device device, int pausePosition) {

        Service localService = device.getService(AVTransport1);
        if (localService == null)
            return false;

        final Action localAction = localService.getAction("Seek");
        if (localAction == null)
            return false;
        localAction.setArgumentValue("InstanceID", "0");
        // if (mUseRelTime) {
        // } else {
        // localAction.setArgumentValue("Unit", "ABS_TIME");
        // }
        // LogUtil.e(tag, "继续相对时间："+mUseRelTime);
        // 测试解决播放暂停后时间不准确
        localAction.setArgumentValue("Unit", "ABS_TIME");
        localAction.setArgumentValue("Target", format(pausePosition));
        localAction.postControlAction();

        Action playAction = localService.getAction("Play");
        if (playAction == null) {
            return false;
        }

        playAction.setArgumentValue("InstanceID", 0);
        playAction.setArgumentValue("Speed", "1");
        return playAction.postControlAction();
    }

    private synchronized int getTransportStateSync(Device device) {
        Service localService = device.getService(AVTransport1);
        if (localService == null) {
            return formatTransportState(null);
        }

        final Action localAction = localService.getAction("GetTransportInfo");
        if (localAction == null) {
            return formatTransportState(null);
        }

        localAction.setArgumentValue("InstanceID", "0");

        if (localAction.postControlAction()) {
            String value = localAction.getArgumentValue("CurrentTransportState");
            System.out.println("current state:" + value);
            return formatTransportState(localAction.getArgumentValue("CurrentTransportState"));
        } else {
            return formatTransportState(null);
        }
    }

    private synchronized String getVolumeRangeSync(Device device, String argument) {
        Service localService = device.getService(RenderingControl);
        if (localService == null) {
            return null;
        }
        Action localAction = localService.getAction("GetVolumeDBRange");
        if (localAction == null) {
            return null;
        }
        localAction.setArgumentValue("InstanceID", "0");
        localAction.setArgumentValue("Channel", "Master");
        if (!localAction.postControlAction()) {
            return null;
        } else {
            return localAction.getArgumentValue(argument);
        }
    }

    private synchronized boolean seekToSync(Device device, String targetPosition) {
        Service localService = device.getService(AVTransport1);
        if (localService == null)
            return false;

        Action localAction = localService.getAction("Seek");
        if (localAction == null) {
            return false;
        }
        localAction.setArgumentValue("InstanceID", "0");
        // if (mUseRelTime) {
        // localAction.setArgumentValue("Unit", "REL_TIME");
        // } else {
        localAction.setArgumentValue("Unit", "ABS_TIME");
        // }
        localAction.setArgumentValue("Target", targetPosition);
        boolean postControlAction = localAction.postControlAction();
        if (!postControlAction) {
            localAction.setArgumentValue("Unit", "REL_TIME");
            localAction.setArgumentValue("Target", targetPosition);
            return localAction.postControlAction();
        } else {
            return postControlAction;
        }

    }

    private synchronized String getPositionInfoSync(Device device) {
        Service localService = device.getService(AVTransport1);

        if (localService == null)
            return null;

        final Action localAction = localService.getAction("GetPositionInfo");
        if (localAction == null) {
            return null;
        }

        localAction.setArgumentValue("InstanceID", "0");
        boolean isSuccess = localAction.postControlAction();
        if (isSuccess) {
            return localAction.getArgumentValue("AbsTime");
        } else {
            return null;
        }
    }

    private synchronized String getMediaDurationSync(Device device) {
        Service localService = device.getService(AVTransport1);
        if (localService == null) {
            return null;
        }

        final Action localAction = localService.getAction("GetMediaInfo");
        if (localAction == null) {
            return null;
        }

        localAction.setArgumentValue("InstanceID", "0");
        if (localAction.postControlAction()) {
            return localAction.getArgumentValue("MediaDuration");
        } else {
            return null;
        }

    }

    private synchronized boolean setMuteSync(Device mediaRenderDevice, boolean mute) {
        Service service = mediaRenderDevice.getService(RenderingControl);
        if (service == null) {
            return false;
        }
        final Action action = service.getAction("SetMute");
        if (action == null) {
            return false;
        }

        action.setArgumentValue("InstanceID", "0");
        action.setArgumentValue("Channel", "Master");
        action.setArgumentValue("DesiredMute", mute ? "1" : "0");
        return action.postControlAction();
    }

    private synchronized String getMuteSync(Device device) {
        Service service = device.getService(RenderingControl);
        if (service == null) {
            return null;
        }

        final Action getAction = service.getAction("GetMute");
        if (getAction == null) {
            return null;
        }
        getAction.setArgumentValue("InstanceID", "0");
        getAction.setArgumentValue("Channel", "Master");
        getAction.postControlAction();
        return getAction.getArgumentValue("CurrentMute");
    }

    private synchronized boolean setVolumeSync(Device device, int value) {
        Service service = device.getService(RenderingControl);
        if (service == null) {
            return false;
        }

        final Action action = service.getAction("SetVolume");
        if (action == null) {
            return false;
        }

        action.setArgumentValue("InstanceID", "0");
        action.setArgumentValue("Channel", "Master");
        action.setArgumentValue("DesiredVolume", value);
        return action.postControlAction();
    }

    private synchronized int getVolumeSync(Device device) {
        Service service = device.getService(RenderingControl);
        if (service == null) {
            return -1;
        }

        final Action getAction = service.getAction("GetVolume");
        if (getAction == null) {
            return -1;
        }
        getAction.setArgumentValue("InstanceID", "0");
        getAction.setArgumentValue("Channel", "Master");
        if (getAction.postControlAction()) {
            return getAction.getArgumentIntegerValue("CurrentVolume");
        } else {
            return -1;
        }

    }

    private synchronized boolean pauseSync(Device mediaRenderDevice) {

        Service service = mediaRenderDevice.getService(AVTransport1);
        if (service == null) {
            return false;
        }
        final Action pauseAction = service.getAction("Pause");
        if (pauseAction == null) {
            return false;
        }
        pauseAction.setArgumentValue("InstanceID", 0);
        return pauseAction.postControlAction();
    }

    private synchronized boolean stopSync(Device device) {
        Service service = device.getService(AVTransport1);

        if (service == null) {
            return false;
        }
        final Action stopAction = service.getAction("Stop");
        if (stopAction == null) {
            return false;
        }

        stopAction.setArgumentValue("InstanceID", 0);
        return stopAction.postControlAction();

    }

    private int format(String timeStr) {
        if (TextUtils.isEmpty(timeStr)) {
            return -1;
        }
        if (timeStr.contains(".")) {
            timeStr = timeStr.substring(0, timeStr.indexOf("."));
        }
        String[] values = timeStr.split(":");
        if (values.length < 1) {
            return -1;
        }
        if (values.length == 1) {
            return Integer.valueOf(values[0]);
        }
        if (values.length == 2) {
            return 60 * Integer.valueOf(values[0]) + Integer.valueOf(values[1]);
        }
        return 3600 * Integer.valueOf(values[0]) + 60 * Integer.valueOf(values[1]) + Integer.valueOf(values[2]);
    }

    private String format(int timeSeconds) {
        if (timeSeconds < 60) {
            return "00:00:" + unitFormat(timeSeconds);
        }
        int minutes = timeSeconds / 60;
        if (minutes < 60) {
            int seconds = timeSeconds % 60;
            return "00:" + String.format("%s:%s", unitFormat(minutes), unitFormat(seconds));
        }
        int hours = Math.min(minutes / 60, 99);
        minutes = minutes % 60;
        int seconds = timeSeconds - hours * 3600 - minutes * 60;
        return String.format("%s:%s:%s", unitFormat(hours), unitFormat(minutes), unitFormat(seconds));
    }

    private String unitFormat(int time) {
        if (time < 10) {
            return String.format("0%s", time);
        }
        return String.format("%s", time);
    }

    private int formatTransportState(String value) {
        if ("STOPPED".equals(value)) {
            return STATE_STOPPED;
        }
        if ("PLAYING".equals(value)) {
            return STATE_PLAYING;
        }
        if ("TRANSITIONING".equals(value)) {
            return STATE_TRANSITIONING;
        }
        if ("PAUSED_PLAYBACK".equals(value)) {
            return STATE_PAUSED_PLAYBACK;
        }
        if ("PAUSED_RECORDING".equals(value)) {
            return STATE_PAUSED_RECORDING;
        }
        if ("RECORDING".equals(value)) {
            return STATE_RECORDING;
        }
        if ("NO_MEDIA_PRESENT".equals(value)) {
            return STATE_NO_MEDIA_PRESENT;
        }
        return STATE_ERR;
    }

}
