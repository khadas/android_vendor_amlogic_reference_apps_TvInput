/*
 * Copyright (c) 2014 Amlogic, Inc. All rights reserved.
 *
 * This source code is subject to the terms and conditions defined in the
 * file 'LICENSE' which is part of this source code package.
 *
 * Description:
 *     AMLOGIC AudioEffectsSettingManagerService
 */

package com.droidlogic.tvinput.services;

import android.app.Service;
import android.content.Context;
import android.content.ContentProviderClient;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.database.ContentObserver;
import android.media.tv.TvContract;
import android.net.Uri;
import android.os.IBinder;
import android.os.UserHandle;
import android.os.Binder;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.droidlogic.tvinput.settings.SoundEffectManager;

/**
 * This Service modifies Audio and Picture Quality TV Settings.
 * It contains platform specific implementation of the TvTweak IOemSettings interface.
 */
public class AudioEffectsService extends PersistentService {
    private static final String TAG = AudioEffectsService.class.getSimpleName();
    private static boolean DEBUG = true;
    private SoundEffectManager mSoundEffectManager;
    private AudioEffectsService mAudioEffectsService;
    private Context mContext = null;

    // Service actions
    public static final String ACTION_STARTUP = "com.droidlogic.tvinput.services.AudioEffectsService.STARTUP";

    public static final String SOUND_EFFECT_SOUND_MODE            = "sound_effect_sound_mode";
    public static final String SOUND_EFFECT_SOUND_MODE_TYPE       = "sound_effect_sound_mode_type";
    public static final String SOUND_EFFECT_SOUND_MODE_TYPE_DAP   = "type_dap";
    public static final String SOUND_EFFECT_SOUND_MODE_TYPE_EQ    = "type_eq";
    public static final String SOUND_EFFECT_SOUND_MODE_DAP_VALUE  = "sound_effect_sound_mode_dap";
    public static final String SOUND_EFFECT_SOUND_MODE_EQ_VALUE   = "sound_effect_sound_mode_eq";
    public static final int MODE_STANDARD = 0;

    public AudioEffectsService() {
        super("AudioEffectsService");
        mAudioEffectsService = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (DEBUG) Log.d(TAG, "AudioEffectsService onCreate");
        mContext = this;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (DEBUG) Log.d(TAG, "onDestroy");
        if (mSoundEffectManager != null) {
            mSoundEffectManager.getInstance(mContext).cleanupAudioEffects();
        }
        unregisterCommandReceiver(this);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (DEBUG) Log.w(TAG, "onLowMemory");
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (DEBUG) Log.d(TAG, "onBind");
        return mBinder;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        final String action = intent.getAction();
        if (ACTION_STARTUP.equals(action)) {
            if (DEBUG) Log.d(TAG, "processing " + ACTION_STARTUP);
            mHandler.sendEmptyMessage(MSG_CHECK_BOOTVIDEO_FINISHED);
        } else {
            Log.w(TAG, "Unknown intent: " + action);
        }
    }

    private boolean isBootvideoStopped() {
        ContentProviderClient tvProvider = getContentResolver().acquireContentProviderClient(TvContract.AUTHORITY);

        return (tvProvider != null) &&
                (((SystemProperties.getInt("persist.vendor.media.bootvideo", 50)  > 100)
                        && TextUtils.equals(SystemProperties.get("service.bootvideo.exit", "1"), "0"))
                || ((SystemProperties.getInt("persist.vendor.media.bootvideo", 50)  <= 100)));
    }

    private static final int MSG_CHECK_BOOTVIDEO_FINISHED = 0;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_CHECK_BOOTVIDEO_FINISHED:
                    if (isBootvideoStopped()) {
                        handleActionStartUp();
                    } else {
                        mHandler.sendEmptyMessageDelayed(MSG_CHECK_BOOTVIDEO_FINISHED, 10);
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private final IAudioEffectsService.Stub mBinder = new IAudioEffectsService.Stub(){
        public int getSoundModeStatus () {
            return mSoundEffectManager.getInstance(mContext).getSoundModeStatus();
        }

        //return current is eq or dap
        public int getSoundModule() {
            return mSoundEffectManager.getInstance(mContext).getSoundModule();
        }

        public int getTrebleStatus () {
            return mSoundEffectManager.getInstance(mContext).getTrebleStatus();
        }

        public int getBassStatus () {
            return mSoundEffectManager.getInstance(mContext).getBassStatus();
        }

        public int getBalanceStatus () {
            return mSoundEffectManager.getInstance(mContext).getBalanceStatus();
        }

        public int getSurroundStatus () {
            return mSoundEffectManager.getInstance(mContext).getSurroundStatus();
        }

        public int getDialogClarityStatus () {
            return mSoundEffectManager.getInstance(mContext).getDialogClarityStatus();
        }

        public int getBassBoostStatus () {
            return mSoundEffectManager.getInstance(mContext).getBassBoostStatus();
        }

        public boolean getAgcEnableStatus () {
            return mSoundEffectManager.getInstance(mContext).getAgcEnableStatus();
        }

        public int getAgcMaxLevelStatus () {
            return mSoundEffectManager.getInstance(mContext).getAgcMaxLevelStatus();
        }

        public int getAgcAttrackTimeStatus () {
            return mSoundEffectManager.getInstance(mContext).getAgcAttrackTimeStatus();
        }

        public int getAgcReleaseTimeStatus () {
            return mSoundEffectManager.getInstance(mContext).getAgcReleaseTimeStatus();
        }

        public int getAgcSourceIdStatus () {
            return mSoundEffectManager.getInstance(mContext).getAgcSourceIdStatus();
        }

        public int getVirtualSurroundStatus() {
            return mSoundEffectManager.getInstance(mContext).getVirtualSurroundStatus();
        }

        public void setSoundMode (int mode) {
            mSoundEffectManager.getInstance(mContext).setSoundMode(mode);
        }

        public void setSoundModeByObserver (int mode) {
            mSoundEffectManager.getInstance(mContext).setSoundModeByObserver(mode);
        }

        public void setDifferentBandEffects(int bandnum, int value, boolean needsave) {
            mSoundEffectManager.getInstance(mContext).setDifferentBandEffects(bandnum, value, needsave);
        }

        public void setTreble (int step) {
            mSoundEffectManager.getInstance(mContext).setTreble (step);
        }

        public void setBass (int step) {
            mSoundEffectManager.getInstance(mContext).setBass (step);
        }

        public void setBalance (int step) {
            mSoundEffectManager.getInstance(mContext).setBalance (step);
        }

        public void setSurround (int mode) {
            mSoundEffectManager.getInstance(mContext).setSurround (mode);
        }

        public void setDialogClarity (int mode) {
            mSoundEffectManager.getInstance(mContext).setDialogClarity (mode);
        }

        public void setBassBoost (int mode) {
            mSoundEffectManager.getInstance(mContext).setBassBoost (mode);
        }

        public void setAgsEnable (int mode) {
            mSoundEffectManager.getInstance(mContext).setAgsEnable (mode);
        }

        public void setAgsMaxLevel (int step) {
            mSoundEffectManager.getInstance(mContext).setAgsMaxLevel (step);
        }

        public void setAgsAttrackTime (int step) {
            mSoundEffectManager.getInstance(mContext).setAgsAttrackTime (step);
        }

        public void setAgsReleaseTime (int step) {
            mSoundEffectManager.getInstance(mContext).setAgsReleaseTime (step);
        }

        public void setSourceIdForAvl (int step) {
            mSoundEffectManager.getInstance(mContext).setSourceIdForAvl (step);
        }

        public void setVirtualSurround (int mode) {
            mSoundEffectManager.getInstance(mContext).setVirtualSurround (mode);
        }

        public void setParameters(int order, int value) {
            mSoundEffectManager.getInstance(mContext).setParameters(order, value);
        }

        public int getParameters(int order) {
            return mSoundEffectManager.getInstance(mContext).getParameters(order);
        }

        public void cleanupAudioEffects() {
            mSoundEffectManager.getInstance(mContext).cleanupAudioEffects();
        }

        public void initSoundEffectSettings() {
            mSoundEffectManager.getInstance(mContext).initSoundEffectSettings();
        }

        public void resetSoundEffectSettings() {
            Log.d(TAG, "resetSoundEffectSettings");
            mSoundEffectManager.getInstance(mContext).resetSoundEffectSettings();
        }
    };

    private void handleActionStartUp() {
        // This will apply the saved audio settings on boot
        mSoundEffectManager.getInstance(mContext).initSoundEffectSettings();
        registerCommandReceiver(this);
    }

    private static final String RESET_ACTION = "droid.action.resetsoundeffect";
    private static final String AVL_SOURCE_ACTION = "droid.action.avlmodule";

    private void registerCommandReceiver(Context context) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(RESET_ACTION);
        intentFilter.addAction(AVL_SOURCE_ACTION);
        context.registerReceiver(mSoundEffectSettingsReceiver, intentFilter);
        context.getContentResolver().registerContentObserver(Settings.Global.getUriFor(SOUND_EFFECT_SOUND_MODE), false,
                mSoundEffectParametersObserver);
        context.getContentResolver().registerContentObserver(Settings.Global.getUriFor(SOUND_EFFECT_SOUND_MODE_EQ_VALUE), false,
                mSoundEffectParametersObserver);
        context.getContentResolver().registerContentObserver(Settings.Global.getUriFor(SOUND_EFFECT_SOUND_MODE_DAP_VALUE), false,
                mSoundEffectParametersObserver);
    }

    private void unregisterCommandReceiver(Context context) {
        context.unregisterReceiver(mSoundEffectSettingsReceiver);
        context.getContentResolver().unregisterContentObserver(mSoundEffectParametersObserver);
    }

    private ContentObserver mSoundEffectParametersObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (uri != null) {
                if (uri.equals(Settings.Global.getUriFor(SOUND_EFFECT_SOUND_MODE)) || uri.equals(Settings.Global.getUriFor(SOUND_EFFECT_SOUND_MODE_EQ_VALUE))
                        || uri.equals(Settings.Global.getUriFor(SOUND_EFFECT_SOUND_MODE_DAP_VALUE))) {
                    int mode = Settings.Global.getInt(mContext.getContentResolver(), uri.getLastPathSegment(), MODE_STANDARD);
                    Log.d(TAG, "onChange setSoundMode " + uri.getLastPathSegment() + ":" + mode);
                    mSoundEffectManager.getInstance(mContext).setSoundModeByObserver(mode);
                }
            }
        }
    };

    private final BroadcastReceiver mSoundEffectSettingsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DEBUG) Log.d(TAG, "intent = " + intent);
            if (intent != null) {
                if (RESET_ACTION.equals(intent.getAction())) {
                    mSoundEffectManager.getInstance(mContext).resetSoundEffectSettings();
                } else if (AVL_SOURCE_ACTION.equals(intent.getAction())) {
                    mSoundEffectManager.getInstance(mContext).setSourceIdForAvl(intent.getIntExtra("source_id", SoundEffectManager.DEFAULT_AGC_SOURCE_ID));
                }
            }
        }
    };
}
