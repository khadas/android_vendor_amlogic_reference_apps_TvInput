package com.droidlogic.tvinput.services;


import java.io.IOException;

import org.xmlpull.v1.XmlPullParserException;

import com.droidlogic.common.DroidLogicTvInputService;
import com.droidlogic.utils.Utils;

import android.amlogic.Tv.SourceInput;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.tv.TvInputHardwareInfo;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputService;
import android.net.Uri;
import android.util.Log;
import android.view.Surface;

@SuppressLint("NewApi")
public class AVInputService extends DroidLogicTvInputService {
    private static final String TAG = AVInputService.class.getSimpleName();

    private TvInputInfo mTvInputInfo;

    @Override
    public Session onCreateSession(String inputId) {
        Utils.logd(TAG, "=====onCreateSession====");
        return new AVInputSession(getApplicationContext());
    }

    public class AVInputSession extends TvInputService.Session {

        public AVInputSession(Context context) {
            super(context);
        }

        @Override
        public void onRelease() {
            // TODO Auto-generated method stub
        }

        @Override
        public boolean onSetSurface(Surface surface) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public void onSetStreamVolume(float volume) {
            // TODO Auto-generated method stub
        }

        @Override
        public boolean onTune(Uri channelUri) {
            Utils.logd(TAG, "====onTune====");
            switchToSourceInput(Utils.SOURCE_AV1);
            return false;
        }

        @Override
        public void onSetCaptionEnabled(boolean enabled) {
            // TODO Auto-generated method stub
        }
    }

    public TvInputInfo onHardwareAdded(TvInputHardwareInfo hardwareInfo) {
        if (hardwareInfo.getType() != TvInputHardwareInfo.TV_INPUT_TYPE_COMPONENT)
            return null;

        Utils.logd(TAG, "=====onHardwareAdded====="+hardwareInfo.getDeviceId());

        TvInputInfo info = null;
        ResolveInfo rInfo = getResolveInfo(AVInputService.class.getName());
        if (rInfo != null) {
            try {
                info = TvInputInfo.createTvInputInfo(getApplicationContext(), rInfo,
                        hardwareInfo, null, null);
            } catch (XmlPullParserException e) {
                // TODO: handle exception
            }catch (IOException e) {
                // TODO: handle exception
            }
        }

        return info;
    }

    public String onHardwareRemoved(TvInputHardwareInfo hardwareInfo) {
        if (hardwareInfo.getType() != TvInputHardwareInfo.TV_INPUT_TYPE_COMPONENT
                || mTvInputInfo == null)
            return null;

        Utils.logd(TAG, "===onHardwareRemoved===" + mTvInputInfo.getId());
        return mTvInputInfo.getId();
    }

}
