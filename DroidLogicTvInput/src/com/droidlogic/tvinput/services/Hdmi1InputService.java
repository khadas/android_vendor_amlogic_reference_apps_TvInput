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

import com.droidlogic.app.tv.DroidLogicTvInputService;
import com.droidlogic.app.tv.DroidLogicTvUtils;
import com.droidlogic.app.tv.TvInputBaseSession;
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

import java.util.HashMap;
import java.util.Map;
import android.net.Uri;

public class Hdmi1InputService extends DroidLogicTvInputService {
    private static final String TAG = Hdmi1InputService.class.getSimpleName();
    private Hdmi1InputSession mCurrentSession;
    private int id = 0;
    private final int TV_SOURCE_EXTERNAL = 0;
    private final int TV_SOURCE_INTERNAL = 1;
    private Map<Integer, Hdmi1InputSession> sessionMap = new HashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        initInputService(DroidLogicTvUtils.DEVICE_ID_HDMI1, Hdmi1InputService.class.getName());
    }

    @Override
    public Session onCreateSession(String inputId) {
        Utils.logd(TAG, "onCreateSession() inputId = " + inputId + "sessionId = " + id);
        super.onCreateSession(inputId);

        mCurrentSession = new Hdmi1InputSession(getApplicationContext(), inputId, getHardwareDeviceId(inputId));
        mCurrentSession.setSessionId(id);
        registerInputSession(mCurrentSession);
        sessionMap.put(id, mCurrentSession);
        id++;

        return mCurrentSession;
    }

    @Override
    public void setCurrentSessionById(int sessionId) {
        Utils.logd(TAG, "setCurrentSessionById:"+sessionId);
        Hdmi1InputSession session = sessionMap.get(sessionId);
        if (session != null) {
            mCurrentSession = session;
        }
    }

    public class Hdmi1InputSession extends TvInputBaseSession {
        public Hdmi1InputSession(Context context, String inputId, int deviceId) {
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
        public void doRelease() {
            super.doRelease();
            if (sessionMap.containsKey(getSessionId())) {
                sessionMap.remove(getSessionId());
                if (mCurrentSession == this) {
                    mCurrentSession = null;
                    registerInputSession(null);
                }
            }
        }

        @Override
        public void doAppPrivateCmd(String action, Bundle bundle) {
            //super.doAppPrivateCmd(action, bundle);
            if (TextUtils.equals(DroidLogicTvUtils.ACTION_STOP_TV, action)) {
                if (mHardware != null) {
                    mHardware.setSurface(null, null);
                }
            }
        }
    }

    public String getDeviceClassName() {
        return Hdmi1InputService.class.getName();
    }

    public int getDeviceSourceType() {
        return DroidLogicTvUtils.DEVICE_ID_HDMI1;
    }

}
