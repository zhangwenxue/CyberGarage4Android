package com.android.dlna.manager;


import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import org.cybergarage.upnp.ControlPoint;
import org.cybergarage.upnp.Device;
import org.cybergarage.upnp.device.DeviceChangeListener;
import org.cybergarage.upnp.device.SearchResponseListener;
import org.cybergarage.upnp.ssdp.SSDPPacket;

import java.util.ArrayList;
import java.util.List;

public class DLNADeviceManager {
    public interface MediaRenderDeviceChangeListener {
        void onStarted();

        void onDeviceListChanged(final List<Device> list);

        void onFinished();
    }

    private static final int MSG_SEARCH_STARTED = 0x01;
    private static final int MSG_SEARCH_FINISHED = 0x02;
    private static final int MSG_DEVICE_LIST_CHANGED = 0x03;
    private volatile static DLNADeviceManager instance;
    private final ControlPoint mControlPoint;
    private volatile boolean started = false;
    private final List<Device> mDeviceList = new ArrayList<>();
    private SearchThread mThread;
    private MediaRenderDeviceChangeListener mListener;
    private Device mCurrentDevice;
    private final Handler HANDLER = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (mListener == null) {
                return false;
            }
            switch (msg.what) {
                case MSG_SEARCH_STARTED:
                    mListener.onStarted();
                    return true;
                case MSG_DEVICE_LIST_CHANGED:
                    synchronized (mDeviceList) {
                        mListener.onDeviceListChanged(mDeviceList);
                    }
                    return true;
                case MSG_SEARCH_FINISHED:
                    mListener.onFinished();
                    return true;
                default:
                    return false;
            }
        }
    });

    public static DLNADeviceManager getInstance() {
        if (instance == null) {
            synchronized (DLNADeviceManager.class) {
                if (instance == null) {
                    instance = new DLNADeviceManager();
                }
            }
        }
        return instance;
    }

    public synchronized void setCurrentDevice(Device device) {
        this.mCurrentDevice = device;
    }

    public synchronized Device getCurrentDevice() {
        return mCurrentDevice;
    }

    private DLNADeviceManager() {
        mControlPoint = new ControlPoint();
        mControlPoint.addSearchResponseListener(new SearchResponseListener() {
            @Override
            public void deviceSearchResponseReceived(SSDPPacket ssdpPacket) {
                Log.i("===ResponseReceived :", ssdpPacket.toString());
            }
        });
        DeviceChangeListener changeListener = new DeviceChangeListener() {
            @Override
            public void deviceAdded(final Device device) {
                Log.i("===deviceAdded :", device.getFriendlyName());
                if (isMediaRenderDevice(device)) {
                    synchronized (mDeviceList) {
                        for (Device d : mDeviceList) {
                            if (device.getUDN().equalsIgnoreCase(d.getUDN())) {
                                break;
                            }
                        }
                        mDeviceList.add(device);
                        HANDLER.obtainMessage(MSG_DEVICE_LIST_CHANGED).sendToTarget();
                    }
                }
            }

            @Override
            public void deviceRemoved(final Device device) {
                Log.i("===deviceRemoved :", device.getFriendlyName());
                if (isMediaRenderDevice(device)) {
                    synchronized (mDeviceList) {
                        for (Device d : mDeviceList) {
                            if (device.getUDN().equalsIgnoreCase(d.getUDN())) {
                                mDeviceList.remove(d);
                                HANDLER.obtainMessage(MSG_DEVICE_LIST_CHANGED).sendToTarget();
                                if (mCurrentDevice != null && mCurrentDevice.getUDN().equalsIgnoreCase(d.getUDN())) {
                                    setCurrentDevice(null);
                                }
                                break;
                            }
                        }
                    }
                }
            }
        };
        mControlPoint.addDeviceChangeListener(changeListener);
    }

    public void startDiscovery(MediaRenderDeviceChangeListener listener) {
        synchronized (this) {
            this.mListener = listener;
            if (mThread == null) {
                mThread = new SearchThread();
            }
            mThread.start();
            HANDLER.obtainMessage(MSG_SEARCH_STARTED).sendToTarget();
        }
    }

    public void stopDiscovery() {
        synchronized (this) {
            if (mThread != null) {
                mThread.interrupt();
            }
            if (mControlPoint != null) {
                mControlPoint.stop();
            }
        }

    }


    private class SearchThread extends Thread {
        SearchThread() {
            super("search_thr");
        }

        @Override
        public void run() {
            try {
                long start = SystemClock.elapsedRealtime();
                if (!started) {
                    started = mControlPoint.start();
                } else {
                    mControlPoint.search();
                }
                while (SystemClock.elapsedRealtime() - start <= 5000) {
                    if (!started) {
                        started = mControlPoint.start();
                    } else {
                        mControlPoint.search();
                        Thread.sleep(300);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                synchronized (DLNADeviceManager.this) {
                    if (mThread == this) {
                        mThread = null;
                        HANDLER.obtainMessage(MSG_SEARCH_FINISHED).sendToTarget();
                    }
                }
            }
        }
    }


    private static boolean isMediaRenderDevice(Device device) {
        return device != null
                && "urn:schemas-upnp-org:device:MediaRenderer:1".equalsIgnoreCase(device.getDeviceType());

    }

}
