/*
 * Copyright (c) 2014 Amlogic, Inc. All rights reserved.
 *
 * This source code is subject to the terms and conditions defined in the
 * file 'LICENSE' which is part of this source code package.
 *
 * Description: JAVA file
 */

package com.droidlogic.tvinput.services;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParserException;

import com.droidlogic.tvinput.Utils;

import com.droidlogic.app.tv.DroidLogicTvUtils;
import com.droidlogic.tvinput.R;

import android.content.Context;
import android.content.pm.ResolveInfo;
import android.media.tv.TvInputHardwareInfo;
import android.media.tv.TvInputInfo;
import android.media.tv.TvStreamConfig;
import android.media.tv.TvInputManager.Hardware;
import android.hardware.hdmi.HdmiDeviceInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Surface;
import android.widget.FrameLayout;

import java.util.HashMap;
import java.util.Map;
import android.net.Uri;

public class Hdmi2InputService extends DroidLogicTvInputService {
    private static final String TAG = Hdmi2InputService.class.getSimpleName();
    private Hdmi2InputSession mCurrentSession;
    private int id = 0;
    private final int TV_SOURCE_EXTERNAL = 0;
    private final int TV_SOURCE_INTERNAL = 1;

   private Map<Integer, Hdmi2InputSession> sessionMap = new HashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        initInputService(DroidLogicTvUtils.DEVICE_ID_HDMI2, Hdmi2InputService.class.getName());
    }

    @Override
    public Session onCreateSession(String inputId) {
        super.onCreateSession(inputId);

        mCurrentSession = new Hdmi2InputSession(this, inputId, getHardwareDeviceId(inputId));
        mCurrentSession.setSessionId(id);
        registerInputSession(mCurrentSession);
        sessionMap.put(id, mCurrentSession);
        id++;

        return mCurrentSession;
    }

    @Override
    public void setCurrentSessionById(int sessionId) {
        Utils.logd(TAG, "setCurrentSessionById:"+sessionId);
        Hdmi2InputSession session = sessionMap.get(sessionId);
        if (session != null) {
            mCurrentSession = session;
        }
    }

    public class Hdmi2InputSession extends TvInputBaseSession {
        public Hdmi2InputSession(Context context, String inputId, int deviceId) {
            super(context, inputId, deviceId);
            Utils.logd(TAG, "=====new HdmiInputSession=====");
            initOverlayView(R.layout.layout_overlay_no_subtitle);
            if (mOverlayView != null) {
                mOverlayView.setImage(R.drawable.bg_no_signal);
            }
        }

        @Override
        public boolean onSetSurface(Surface surface) {
            super.onSetSurface(surface);
            return setSurfaceInService(surface,this);
        }

        @Override
        public boolean onTune(Uri channelUri) {
            return doTuneInService(channelUri, getSessionId());
        }

        @Override
        public void onOverlayViewSizeChanged(int width, int height) {
            Utils.logd(TAG, "onOverlayViewSizeChanged: "+width+","+height);
            super.onOverlayViewSizeChanged(width, height);
            if (mIsPip) {
                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(width, height);
                mOverlayView.setLayoutParams(layoutParams);
            }
        }

        @Override
        public boolean onTune(Uri channelUri, Bundle params) {
            if (params != null) {
                mIsPip = params.getBoolean("is_pip", false);
            }
            return super.onTune(channelUri, params);
        }

        @Override
        public void doRelease() {
            if (sessionMap.containsKey(getSessionId())) {
                sessionMap.remove(getSessionId());
                if (mCurrentSession == this) {
                    mCurrentSession = null;
                    registerInputSession(null);
                }
            }
            super.doRelease();
        }

        @Override
        public void doAppPrivateCmd(String action, Bundle bundle) {
            super.doAppPrivateCmd(action, bundle);
            if (TextUtils.equals(DroidLogicTvUtils.ACTION_STOP_TV, action)) {
                if (mHardware != null) {
                    mHardware.setSurface(null, null);
                }
            } else if (TextUtils.equals("action_enabled_hdmi_pip", action)) {
                Utils.logd(TAG,"doAppPrivateCmd action = " + action);
                mIsPip = true;
            }
        }
    }

    public String getDeviceClassName() {
        return Hdmi2InputService.class.getName();
    }

    public int getDeviceSourceType() {
        return DroidLogicTvUtils.DEVICE_ID_HDMI2;
    }
}
