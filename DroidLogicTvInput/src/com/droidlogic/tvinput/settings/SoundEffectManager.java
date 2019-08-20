/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.droidlogic.tvinput.settings;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.Log;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.media.AudioManager;
import android.app.ActivityManager;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.widget.Toast;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.media.audiofx.AudioEffect;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioTrack;

import java.util.*;
import com.droidlogic.app.OutputModeManager;
import com.droidlogic.app.tv.AudioEffectManager;

public class SoundEffectManager {

    public static final String TAG = "SoundEffectManager";

    private static final UUID EFFECT_TYPE_TRUSURROUND           = UUID.fromString("1424f5a0-2457-11e6-9fe0-0002a5d5c51b");
    private static final UUID EFFECT_TYPE_BALANCE               = UUID.fromString("7cb34dc0-242e-11e6-bb63-0002a5d5c51b");
    private static final UUID EFFECT_TYPE_TREBLE_BASS           = UUID.fromString("7e282240-242e-11e6-bb63-0002a5d5c51b");
    private static final UUID EFFECT_TYPE_DAP                   = UUID.fromString("3337b21d-c8e6-4bbd-8f24-698ade8491b9");
    private static final UUID EFFECT_TYPE_EQ                    = UUID.fromString("ce2c14af-84df-4c36-acf5-87e428ed05fc");
    private static final UUID EFFECT_TYPE_AGC                   = UUID.fromString("4a959f5c-e33a-4df2-8c3f-3066f9275edf");
    private static final UUID EFFECT_TYPE_VIRTUAL_SURROUND      = UUID.fromString("c656ec6f-d6be-4e7f-854b-1218077f3915");
    private static final UUID EFFECT_TYPE_VIRTUAL_X             = UUID.fromString("5112a99e-b8b9-4c5e-91fd-a804d29c36b2");

    private static final UUID EFFECT_UUID_VIRTUAL_X             = UUID.fromString("61821587-ce3c-4aac-9122-86d874ea1fb1");

    //SoundMode mode.  Parameter ID
    public static final int PARAM_SRS_PARAM_DIALOGCLARTY_MODE           = 1;
    public static final int PARAM_SRS_PARAM_SURROUND_MODE               = 2;
    public static final int PARAM_SRS_PARAM_VOLUME_MODE                 = 3;
    public static final int PARAM_SRS_PARAM_TRUEBASS_ENABLE             = 5;

    //Balance level.  Parameter ID
    public static final int PARAM_BALANCE_LEVEL         = 0;
    //Tone level.  Parameter ID for
    public static final int PARAM_BASS_LEVEL            = 0;
    public static final int PARAM_TREBLE_LEVEL          = 1;
    public static final int PARAM_BAND_ENABLE           = 2;
    public static final int PARAM_BAND1                 = 3;
    public static final int PARAM_BAND2                 = 4;
    public static final int PARAM_BAND3                 = 5;
    public static final int PARAM_BAND4                 = 6;
    public static final int PARAM_BAND5                 = 7;
    public static final int PARAM_BAND_COUNT            = 8;
    //dap AudioEffect, [ HPEQparams ] enumeration alignment in Hpeq.cpp
    public static final int PARAM_EQ_ENABLE             = 0;
    public static final int PARAM_EQ_EFFECT             = 1;
    public static final int PARAM_EQ_CUSTOM             = 2;
    //agc effect define
    public static final int PARAM_AGC_ENABLE            = 0;
    public static final int PARAM_AGC_MAX_LEVEL         = 1;
    public static final int PARAM_AGC_ATTRACK_TIME      = 4;
    public static final int PARAM_AGC_RELEASE_TIME      = 5;
    public static final int PARAM_AGC_SOURCE_ID         = 6;

    public static final boolean DEFAULT_AGC_ENABLE      = true; //enable 1, disable 0
    public static final int DEFAULT_AGC_MAX_LEVEL       = -18;  //db
    public static final int DEFAULT_AGC_ATTRACK_TIME    = 10;   //ms
    public static final int DEFAULT_AGC_RELEASE_TIME    = 2;    //s
    public static final int DEFAULT_AGC_SOURCE_ID       = 3;
    //virtual surround
    public static final int PARAM_VIRTUALSURROUND       = 0;
    /* Modes of dialog clarity */
    public static final int DIALOG_CLARITY_OFF          = 0;
    public static final int DIALOG_CLARITY_LOW          = 1;
    public static final int DIALOG_CLARITY_HIGH         = 2;
    //definition off and on
    private static final int PARAMETERS_SWITCH_OFF      = 1;
    private static final int PARAMETERS_SWITCH_ON       = 0;

    private static final int UI_SWITCH_OFF              = 0;
    private static final int UI_SWITCH_ON               = 1;

    private static final int PARAMETERS_DAP_ENABLE      = 1;
    private static final int PARAMETERS_DAP_DISABLE     = 0;
    //band 1, band 2, band 3, band 4, band 5  need transfer 0~100 to -10~10
    private static final int[] EFFECT_SOUND_MODE_USER_BAND = {50, 50, 50, 50, 50};
    private static final int EFFECT_SOUND_TYPE_NUM = 6;

    // Virtual X effect param type
    private static final int PARAM_DTS_PARAM_MBHL_ENABLE_I32            = 3;
    private static final int PARAM_DTS_PARAM_TBHDX_ENABLE_I32           = 36;
    private static final int PARAM_DTS_PARAM_VX_ENABLE_I32              = 44;
    private static final int PARAM_DTS_PARAM_LOUDNESS_CONTROL_ENABLE_I32= 65;

    private int mSoundModule = AudioEffectManager.DAP_MODULE;
    // Prefix to append to audio preferences file
    private Context mContext;
    private AudioManager mAudioManager;

    //sound effects
    private AudioEffect mVirtualX;
    private AudioEffect mTruSurround;
    private AudioEffect mBalance;
    private AudioEffect mTrebleBass;
    private AudioEffect mSoundMode;
    private AudioEffect mAgc;
    private AudioEffect mVirtualSurround;

    private boolean mSupportVirtualX;

    private static SoundEffectManager mInstance;

    public static synchronized SoundEffectManager getInstance(Context context) {
        if (null == mInstance) {
            mInstance = new SoundEffectManager(context);
        }
        return mInstance;
    }
    private SoundEffectManager (Context context) {
        mContext = context;
        mAudioManager = (AudioManager) context.getSystemService(context.AUDIO_SERVICE);
    }

    static public boolean CanDebug() {
        return SystemProperties.getBoolean("sys.droidlogic.tvinput.debug", false);
    }

    public void createAudioEffects() {
        if (CanDebug()) Log.d(TAG, "Create Audio Effects");
        if (creatVirtualXAudioEffects()) {
            mSupportVirtualX = true;
        } else {
            mSupportVirtualX = false;
            Log.i(TAG, "current not support Virtual X, begin to create TruSurround effect");
            creatTruSurroundAudioEffects();
        }
        creatTrebleBassAudioEffects();
        creatSoundModeAudioEffects();
        creatVirtualSurroundAudioEffects();
        creatBalanceAudioEffects();
    }
    private boolean creatVirtualXAudioEffects() {
        try {
            if (mVirtualX == null) {
                if (CanDebug()) Log.d(TAG, "begin to create VirtualX effect");
                mVirtualX = new AudioEffect(EFFECT_TYPE_VIRTUAL_X, EFFECT_UUID_VIRTUAL_X, 0, 0);
            }
            int result = mVirtualX.setEnabled(true);
            if (result != AudioEffect.SUCCESS) {
                Log.e(TAG, "enable VirtualX effect fail, ret:" + result);
            }
            return true;
        } catch (RuntimeException e) {
            if (CanDebug()) Log.i(TAG, "create VirtualX effect fail", e);
            return false;
        }
    }

    private boolean creatTruSurroundAudioEffects() {
        try {
            if (mTruSurround == null) {
                if (CanDebug()) Log.d(TAG, "begin to create TruSurround effect");
                mTruSurround = new AudioEffect(EFFECT_TYPE_TRUSURROUND, AudioEffect.EFFECT_TYPE_NULL, 0, 0);
            }
            int result = mTruSurround.setEnabled(true);
            if (result != AudioEffect.SUCCESS) {
                Log.e(TAG, "enable TruSurround effect fail, ret:" + result);
            }
            return true;
        } catch (RuntimeException e) {
            Log.e(TAG, "Unable to create mTruSurround audio effect", e);
            return false;
        }
    }

    private boolean creatBalanceAudioEffects() {
        try {
            if (mBalance == null) {
                if (CanDebug()) Log.d(TAG, "creatBalanceAudioEffects");
                mBalance = new AudioEffect(EFFECT_TYPE_BALANCE, AudioEffect.EFFECT_TYPE_NULL, 0, 0);
            }
            return true;
        } catch (RuntimeException e) {
            Log.e(TAG, "Unable to create mBalance audio effect", e);
            return false;
        }
    }

    private boolean creatTrebleBassAudioEffects() {
        try {
            if (mTrebleBass == null) {
                if (CanDebug()) Log.d(TAG, "creatTrebleBassAudioEffects");
                mTrebleBass = new AudioEffect(EFFECT_TYPE_TREBLE_BASS, AudioEffect.EFFECT_TYPE_NULL, 0, 0);
            }
            return true;
        } catch (RuntimeException e) {
            Log.e(TAG, "Unable to create mTrebleBass audio effect", e);
            return false;
        }
    }

    private boolean creatSoundModeAudioEffects() {
        //dap both enable on mbox and tv, so move dap to public code
        //effect num has limit in hal, so if dap valid, ignore eq
        OutputModeManager opm = new OutputModeManager(mContext);
        if (!opm.isDapValid())
            return creatEqAudioEffects();
        return false;
    }

    private boolean creatEqAudioEffects() {
        try {
            if (mSoundMode == null) {
                if (CanDebug()) Log.d(TAG, "creatEqAudioEffects");
                mSoundMode = new AudioEffect(EFFECT_TYPE_EQ, AudioEffect.EFFECT_TYPE_NULL, 0, 0);
                int result = mSoundMode.setEnabled(true);
                if (result == AudioEffect.SUCCESS) {
                    if (CanDebug()) Log.d(TAG, "creatEqAudioEffects enable eq");
                    mSoundMode.setParameter(PARAM_EQ_ENABLE, PARAMETERS_DAP_ENABLE);
                    mSoundModule = AudioEffectManager.EQ_MODULE;
                    Settings.Global.putString(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_SOUND_MODE_TYPE,
                            AudioEffectManager.DB_ID_SOUND_EFFECT_SOUND_MODE_TYPE_EQ);
                }
            }
            return true;
        } catch (RuntimeException e) {
            Log.e(TAG, "Unable to create Eq audio effect", e);
            return false;
        }
    }

    private boolean creatDapAudioEffects() {
        try {
            if (mSoundMode == null) {
                if (CanDebug()) Log.d(TAG, "creatDapAudioEffects");
                mSoundMode = new AudioEffect(EFFECT_TYPE_DAP, AudioEffect.EFFECT_TYPE_NULL, 0, 0);
                int result = mSoundMode.setEnabled(true);
                if (result == AudioEffect.SUCCESS) {
                    if (CanDebug()) Log.d(TAG, "creatDapAudioEffects enable dap");
                    mSoundMode.setParameter(PARAM_EQ_ENABLE, PARAMETERS_DAP_ENABLE);
                    mSoundModule = AudioEffectManager.DAP_MODULE;
                    Settings.Global.putString(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_SOUND_MODE_TYPE,
                            AudioEffectManager.DB_ID_SOUND_EFFECT_SOUND_MODE_TYPE_DAP);
                }
            }
            return true;
        } catch (RuntimeException e) {
            Log.e(TAG, "Unable to create Dap audio effect", e);
            return false;
        }
    }

    private boolean creatAgcAudioEffects() {
        try {
            if (mAgc == null) {
                if (CanDebug()) Log.d(TAG, "creatAgcAudioEffects");
                mAgc = new AudioEffect(EFFECT_TYPE_AGC, AudioEffect.EFFECT_TYPE_NULL, 0, 0);
            }
            return true;
        } catch (RuntimeException e) {
            Log.e(TAG, "Unable to create Agc audio effect", e);
            return false;
        }
    }

    private boolean creatVirtualSurroundAudioEffects() {
        try {
            if (mVirtualSurround == null) {
                if (CanDebug()) Log.d(TAG, "creatVirtualSurroundAudioEffects");
                mVirtualSurround = new AudioEffect(EFFECT_TYPE_VIRTUAL_SURROUND, AudioEffect.EFFECT_TYPE_NULL, 0, 0);
            }
            return true;
        } catch (RuntimeException e) {
            Log.e(TAG, "Unable to create VirtualSurround audio effect", e);
            return false;
        }
    }

    public boolean isSupportVirtualX() {
        return mSupportVirtualX;
    }

    public void setDtsVirtualXMode(int virtalXMode) {
        if (null == mVirtualX) {
            Log.e(TAG, "The VirtualX effect is not created, the mode cannot be setDtsVirtualXMode.");
            return;
        }
        if (CanDebug()) Log.d(TAG, "setDtsVirtualXMode = " + virtalXMode);
        switch (virtalXMode) {
            case AudioEffectManager.SOUND_EFFECT_VIRTUALX_MODE_OFF:
                mVirtualX.setParameter(PARAM_DTS_PARAM_MBHL_ENABLE_I32, 0);
                mVirtualX.setParameter(PARAM_DTS_PARAM_TBHDX_ENABLE_I32, 0);
                mVirtualX.setParameter(PARAM_DTS_PARAM_VX_ENABLE_I32, 0);
                break;
            case AudioEffectManager.SOUND_EFFECT_VIRTUALX_MODE_BASS:
                mVirtualX.setParameter(PARAM_DTS_PARAM_MBHL_ENABLE_I32, 1);
                mVirtualX.setParameter(PARAM_DTS_PARAM_TBHDX_ENABLE_I32, 1);
                mVirtualX.setParameter(PARAM_DTS_PARAM_VX_ENABLE_I32, 0);
                break;
            case AudioEffectManager.SOUND_EFFECT_VIRTUALX_MODE_FULL:
                mVirtualX.setParameter(PARAM_DTS_PARAM_MBHL_ENABLE_I32, 1);
                mVirtualX.setParameter(PARAM_DTS_PARAM_TBHDX_ENABLE_I32, 1);
                mVirtualX.setParameter(PARAM_DTS_PARAM_VX_ENABLE_I32, 1);
                break;
            default:
                Log.w(TAG, "VirtualX effect mode invalid, mode:" + virtalXMode);
                return;
        }
        saveAudioParameters(AudioEffectManager.SET_VIRTUALX_MODE, virtalXMode);
    }

    public int getDtsVirtualXMode() {
        return getSavedAudioParameters(AudioEffectManager.SET_VIRTUALX_MODE);
    }

    public void setDtsTruVolumeHdEnable(boolean enable) {
        if (null == mVirtualX) {
            Log.e(TAG, "The VirtualX effect is not created, the mode cannot be setDtsTruVolumeHdEnable.");
            return;
        }
        if (CanDebug()) Log.d(TAG, "setDtsTruVolumeHdEnable = " + enable);
        int dbSwitch = enable ? 1 : 0;
        mVirtualX.setParameter(PARAM_DTS_PARAM_LOUDNESS_CONTROL_ENABLE_I32, dbSwitch);
        saveAudioParameters(AudioEffectManager.SET_TRUVOLUME_HD_ENABLE, dbSwitch);
    }

    public boolean getDtsTruVolumeHdEnable() {
        int dbSwitch = getSavedAudioParameters(AudioEffectManager.SET_TRUVOLUME_HD_ENABLE);
        boolean enable = (1 == dbSwitch);
        if (dbSwitch != 1 && dbSwitch != 0) {
            Log.w(TAG, "DTS Tru Volume HD db value invalid, db:" + dbSwitch + ", return default false");
        }
        return enable;
    }

    public int getSoundModeStatus () {
        int saveresult = -1;
        if (!creatSoundModeAudioEffects()) {
            Log.e(TAG, "getSoundModeStatus creat fail");
            return AudioEffectManager.MODE_STANDARD;
        }
        int[] value = new int[1];
        mSoundMode.getParameter(PARAM_EQ_EFFECT, value);
        saveresult = getSavedAudioParameters(AudioEffectManager.SET_SOUND_MODE);
        if (saveresult != value[0]) {
            Log.e(TAG, "getSoundModeStatus erro get: " + value[0] + ", saved: " + saveresult);
        } else if (CanDebug()) {
            Log.d(TAG, "getSoundModeStatus = " + saveresult);
        }
        return saveresult;
    }

    //return current is eq or dap
    public int getSoundModule() {
        return mSoundModule;
    }

    public int getTrebleStatus () {
        int saveresult = -1;
        if (!creatTrebleBassAudioEffects()) {
            Log.e(TAG, "getTrebleStatus mTrebleBass creat fail");
            return AudioEffectManager.EFFECT_TREBLE_DEFAULT;
        }
        int[] value = new int[1];
        mTrebleBass.getParameter(PARAM_TREBLE_LEVEL, value);
        saveresult = getSavedAudioParameters(AudioEffectManager.SET_TREBLE);
        if (saveresult != value[0]) {
            Log.e(TAG, "getTrebleStatus erro get: " + value[0] + ", saved: " + saveresult);
        } else if (CanDebug()) {
            Log.d(TAG, "getTrebleStatus = " + saveresult);
        }
        return saveresult;
    }

    public int getBassStatus () {
        int saveresult = -1;
        if (!creatTrebleBassAudioEffects()) {
            Log.e(TAG, "getBassStatus mTrebleBass creat fail");
            return AudioEffectManager.EFFECT_BASS_DEFAULT;
        }
        int[] value = new int[1];
        mTrebleBass.getParameter(PARAM_BASS_LEVEL, value);
        saveresult = getSavedAudioParameters(AudioEffectManager.SET_BASS);
        if (saveresult != value[0]) {
            Log.e(TAG, "getBassStatus erro get: " + value[0] + ", saved: " + saveresult);
        } else if (CanDebug()) {
            Log.d(TAG, "getBassStatus = " + saveresult);
        }
        return saveresult;
    }

    public int getBalanceStatus () {
        int saveresult = -1;
        if (!creatBalanceAudioEffects()) {
            Log.e(TAG, "getBalanceStatus mBalance creat fail");
            return AudioEffectManager.EFFECT_BALANCE_DEFAULT;
        }
        int[] value = new int[1];
        mBalance.getParameter(PARAM_BALANCE_LEVEL, value);
        saveresult = getSavedAudioParameters(AudioEffectManager.SET_BALANCE);
        if (saveresult != value[0]) {
            Log.e(TAG, "getBalanceStatus erro get: " + value[0] + ", saved: " + saveresult);
        } else if (CanDebug()) {
            Log.d(TAG, "getBalanceStatus = " + saveresult);
        }
        return saveresult;
    }

    public boolean getAgcEnableStatus () {
        int saveresult = -1;
        if (!creatAgcAudioEffects()) {
            Log.e(TAG, "getAgcEnableStatus mAgc creat fail");
            return DEFAULT_AGC_ENABLE;
        }
        int[] value = new int[1];
        mAgc.getParameter(PARAM_AGC_ENABLE, value);
        saveresult = getSavedAudioParameters(AudioEffectManager.SET_AGC_ENABLE);
        if (saveresult != value[0]) {
            Log.e(TAG, "getAgcEnableStatus erro get: " + value[0] + ", saved: " + saveresult);
        } else if (CanDebug()) {
            Log.d(TAG, "getAgcEnableStatus = " + saveresult);
        }
        return saveresult == 1;
    }

    public int getAgcMaxLevelStatus () {
        int saveresult = -1;
        if (!creatAgcAudioEffects()) {
            Log.e(TAG, "getAgcEnableStatus mAgc creat fail");
            return DEFAULT_AGC_MAX_LEVEL;
        }
        int[] value = new int[1];
        mAgc.getParameter(PARAM_AGC_MAX_LEVEL, value);
        saveresult = getSavedAudioParameters(AudioEffectManager.SET_AGC_MAX_LEVEL);
        if (saveresult != value[0]) {
            Log.e(TAG, "getAgcMaxLevelStatus erro get: " + value[0] + ", saved: " + saveresult);
        } else if (CanDebug()) {
            Log.d(TAG, "getAgcMaxLevelStatus = " + saveresult);
        }
        return value[0];
    }

    public int getAgcAttrackTimeStatus () {
        int saveresult = -1;
        if (!creatAgcAudioEffects()) {
            Log.e(TAG, "getAgcAttrackTimeStatus mAgc creat fail");
            return DEFAULT_AGC_ATTRACK_TIME;
        }
        int[] value = new int[1];
        mAgc.getParameter(PARAM_AGC_ATTRACK_TIME, value);
        saveresult = getSavedAudioParameters(AudioEffectManager.SET_AGC_ATTRACK_TIME);
        if (saveresult != value[0] / 48) {
            Log.e(TAG, "getAgcAttrackTimeStatus erro get: " + value[0] + ", saved: " + saveresult);
        } else if (CanDebug()) {
            Log.d(TAG, "getAgcAttrackTimeStatus = " + saveresult);
        }
        //value may be changed realtime
        return value[0] / 48;
    }

    public int getAgcReleaseTimeStatus () {
        int saveresult = -1;
        if (!creatAgcAudioEffects()) {
            Log.e(TAG, "getAgcReleaseTimeStatus mAgc creat fail");
            return DEFAULT_AGC_RELEASE_TIME;
        }
        int[] value = new int[1];
        mAgc.getParameter(PARAM_AGC_RELEASE_TIME, value);
        saveresult = getSavedAudioParameters(AudioEffectManager.SET_AGC_RELEASE_TIME);
        if (saveresult != value[0]) {
            Log.e(TAG, "getAgcReleaseTimeStatus erro get: " + value[0] + ", saved: " + saveresult);
        } else if (CanDebug()) {
            Log.d(TAG, "getAgcReleaseTimeStatus = " + saveresult);
        }
        //value may be changed realtime
        return value[0];
    }

    public int getAgcSourceIdStatus () {
        int saveresult = -1;
        if (!creatAgcAudioEffects()) {
            Log.e(TAG, "getAgcSourceIdStatus mAgc creat fail");
            return DEFAULT_AGC_RELEASE_TIME;
        }
        int[] value = new int[1];
        mAgc.getParameter(PARAM_AGC_SOURCE_ID, value);
        saveresult = getSavedAudioParameters(AudioEffectManager.SET_AGC_SOURCE_ID);
        if (saveresult != value[0]) {
            Log.e(TAG, "getAgcSourceIdStatus erro get: " + value[0] + ", saved: " + saveresult);
        } else if (CanDebug()) {
            Log.d(TAG, "getAgcSourceIdStatus = " + saveresult);
        }
        //value may be changed realtime
        return value[0];
    }

    // 0 1 ~ off on
    public int getVirtualSurroundStatus() {
        int saveresult = -1;
        if (!creatVirtualSurroundAudioEffects()) {
            Log.e(TAG, "getVirtualSurroundStatus mVirtualSurround creat fail");
            return OutputModeManager.VIRTUAL_SURROUND_OFF;
        }
        int[] value = new int[1];
        mVirtualSurround.getParameter(PARAM_VIRTUALSURROUND, value);
        saveresult = getSavedAudioParameters(AudioEffectManager.SET_VIRTUAL_SURROUND);
        if (saveresult != value[0]) {
            Log.e(TAG, "getVirtualSurroundStatus erro get: " + value[0] + ", saved: " + saveresult);
        } else if (CanDebug()) {
            Log.d(TAG, "getVirtualSurroundStatus = " + saveresult);
        }
        return saveresult;
    }

    //set sound mode except customed one
    public void setSoundMode (int mode) {
        //need to set sound mode by observer listener
        saveAudioParameters(AudioEffectManager.SET_SOUND_MODE, mode);
    }

    public void setSoundModeByObserver (int mode) {
        if (!creatSoundModeAudioEffects()) {
            Log.e(TAG, "setSoundMode creat fail");
            return;
        }
        int result = mSoundMode.setEnabled(true);
        if (result == AudioEffect.SUCCESS) {
            if (CanDebug()) Log.d(TAG, "setSoundMode = " + mode);
            if ((mSoundModule == AudioEffectManager.EQ_MODULE && mode == AudioEffectManager.MODE_CUSTOM) ||
                    (mSoundModule == AudioEffectManager.DAP_MODULE && mode == AudioEffectManager.EXTEND_MODE_CUSTOM)) {
                //for (int i = AudioEffectManager.SET_EFFECT_BAND1; i <= AudioEffectManager.SET_EFFECT_BAND5; i++) {
                    //set one band, at the same time the others will be set
                    setDifferentBandEffects(AudioEffectManager.SET_EFFECT_BAND1, getSavedAudioParameters(AudioEffectManager.SET_EFFECT_BAND1), false);
                //}
            } else {
                mSoundMode.setParameter(PARAM_EQ_EFFECT, mode);
            }
            //need to set sound mode by observer listener
            //saveAudioParameters(AudioEffectManager.SET_SOUND_MODE, mode);
        }
    }

    public void setDifferentBandEffects(int bandnum, int value, boolean needsave) {
        if (!creatSoundModeAudioEffects()) {
            Log.e(TAG, "setDifferentBandEffects creat fail");
            return;
        }
        int result = mSoundMode.setEnabled(true);
        if (result == AudioEffect.SUCCESS) {
            if (CanDebug()) Log.d(TAG, "setDifferentBandEffects: NO." + bandnum + " = " + value);
            byte[] fiveband = new byte[5];
            for (int i = AudioEffectManager.SET_EFFECT_BAND1; i <= AudioEffectManager.SET_EFFECT_BAND5; i++) {
                if (bandnum == i) {
                    fiveband[i - AudioEffectManager.SET_EFFECT_BAND1] = (byte)MappingLine(value, true);
                    continue;
                }
                fiveband[i - AudioEffectManager.SET_EFFECT_BAND1] = (byte)MappingLine(getParameters(i), true);
            }
            mSoundMode.setParameter(PARAM_EQ_CUSTOM, fiveband);
            if (needsave) {
                saveAudioParameters(bandnum, value);
            }
        }
    }
    //convert -10~10 to 0~100 controled by need or not
    private int unMappingLine(int mapval, boolean need) {
        if (!need) {
            return mapval;
        }

        final int MIN_UI_VAL = -10;
        final int MAX_UI_VAL = 10;
        final int MIN_VAL = 0;
        final int MAX_VAL = 100;
        if (mapval > MAX_UI_VAL || mapval < MIN_UI_VAL) {
            Log.e(TAG, "unMappingLine: map value:" + mapval + " invalid. set default value:" + (MAX_VAL - MIN_VAL) / 2);
            return (MAX_VAL - MIN_VAL) / 2;
        }
        return (mapval - MIN_UI_VAL) * (MAX_VAL - MIN_VAL) / (MAX_UI_VAL - MIN_UI_VAL);
    }

    //convert 0~100 to -10~10 controled by need or not
    private int MappingLine(int mapval, boolean need) {
        if (!need) {
            return mapval;
        }
        final int MIN_UI_VAL = 0;
        final int MAX_UI_VAL = 100;
        final int MIN_VAL = -10;
        final int MAX_VAL = 10;
        if (MIN_VAL < 0) {
            return (mapval - (MAX_UI_VAL + MIN_UI_VAL) / 2) * (MAX_VAL - MIN_VAL)
                   / (MAX_UI_VAL - MIN_UI_VAL);
        } else {
            return (mapval - MIN_UI_VAL) * (MAX_VAL - MIN_VAL) / (MAX_UI_VAL - MIN_UI_VAL);
        }
    }

    public void setTreble (int step) {
        if (!creatTrebleBassAudioEffects()) {
            Log.e(TAG, "setTreble mTrebleBass creat fail");
            return;
        }
        int result = mTrebleBass.setEnabled(true);
        if (result == AudioEffect.SUCCESS) {
            if (CanDebug()) Log.d(TAG, "setTreble = " + step);
            mTrebleBass.setParameter(PARAM_TREBLE_LEVEL, step);
            saveAudioParameters(AudioEffectManager.SET_TREBLE, step);
        }
    }

    public void setBass (int step) {
        if (!creatTrebleBassAudioEffects()) {
            Log.e(TAG, "setBass mTrebleBass creat fail");
            return;
        }
        int result = mTrebleBass.setEnabled(true);
        if (result == AudioEffect.SUCCESS) {
            if (CanDebug()) Log.d(TAG, "setBass = " + step);
            mTrebleBass.setParameter(PARAM_BASS_LEVEL, step);
            saveAudioParameters(AudioEffectManager.SET_BASS, step);
        }
    }

    public void setBalance (int step) {
        if (!creatBalanceAudioEffects()) {
            Log.e(TAG, "setBalance mBalance creat fail");
            return;
        }
        int result = mBalance.setEnabled(true);
        if (result == AudioEffect.SUCCESS) {
            if (CanDebug()) Log.d(TAG, "setBalance = " + step);
            mBalance.setParameter(PARAM_BALANCE_LEVEL, step);
            saveAudioParameters(AudioEffectManager.SET_BALANCE, step);
        }
    }

    public void setSurroundEnable(boolean enable) {
        if (null == mTruSurround) {
            Log.e(TAG, "The Dts TruSurround effect is not created, the mode cannot be setSurroundEnable.");
            return;
        }
        if (CanDebug()) Log.d(TAG, "setSurroundEnable = " + enable);
        int dbSwitch = enable ? 1 : 0;
        mTruSurround.setParameter(PARAM_SRS_PARAM_SURROUND_MODE, dbSwitch);
        saveAudioParameters(AudioEffectManager.SET_SURROUND_ENABLE, dbSwitch);
    }

    public boolean getSurroundEnable() {
        int dbSwitch = getSavedAudioParameters(AudioEffectManager.SET_SURROUND_ENABLE);
        boolean enable = (1 == dbSwitch);
        if (dbSwitch != 1 && dbSwitch != 0) {
            Log.w(TAG, "DTS Surround enable db value invalid, db:" + dbSwitch + ", return default false");
        }
        return enable;
    }

    public void setDialogClarityMode(int mode) {
        if (null == mTruSurround) {
            Log.e(TAG, "The DTS TruSurround effect is not created, the mode cannot be setDialogClarityMode.");
            return;
        }
        if (CanDebug()) Log.d(TAG, "setDialogClarityMode = " + mode);
        mTruSurround.setParameter(PARAM_SRS_PARAM_DIALOGCLARTY_MODE, mode);
        saveAudioParameters(AudioEffectManager.SET_DIALOG_CLARITY_MODE, mode);
    }

    public int getDialogClarityMode() {
        return getSavedAudioParameters(AudioEffectManager.SET_DIALOG_CLARITY_MODE);
    }

    public void setTruBassEnable(boolean enable) {
        if (null == mTruSurround) {
            Log.e(TAG, "The DTS TruSurround effect is not created, the mode cannot be setTruBassEnable.");
            return;
        }
        if (CanDebug()) Log.d(TAG, "setTruBassEnable = " + enable);
        int dbSwitch = enable ? 1 : 0;
        mTruSurround.setParameter(PARAM_SRS_PARAM_TRUEBASS_ENABLE, dbSwitch);
        saveAudioParameters(AudioEffectManager.SET_TRUBASS_ENABLE, dbSwitch);
    }

    public boolean getTruBassEnable() {
        int dbSwitch = getSavedAudioParameters(AudioEffectManager.SET_TRUBASS_ENABLE);
        boolean enable = (1 == dbSwitch);
        if (dbSwitch != 1 && dbSwitch != 0) {
            Log.w(TAG, "DTS TreBass db value invalid, db:" + dbSwitch + ", return default false");
        }
        return enable;
    }

    public void setAgsEnable (int mode) {
        if (!creatAgcAudioEffects()) {
            Log.e(TAG, "setAgsEnable mAgc creat fail");
            return;
        }
        int result = mAgc.setEnabled(true);
        if (result == AudioEffect.SUCCESS) {
            if (CanDebug()) Log.d(TAG, "setAgsEnable = " + mode);
            mAgc.setParameter(PARAM_AGC_ENABLE, mode);
            saveAudioParameters(AudioEffectManager.SET_AGC_ENABLE, mode);
        }
    }

    public void setAgsMaxLevel (int step) {
        if (!creatAgcAudioEffects()) {
            Log.e(TAG, "setAgsMaxLevel mAgc creat fail");
            return;
        }
        int result = mAgc.setEnabled(true);
        if (result == AudioEffect.SUCCESS) {
            if (CanDebug()) Log.d(TAG, "setAgsMaxLevel = " + step);
            mAgc.setParameter(PARAM_AGC_MAX_LEVEL, step);
            saveAudioParameters(AudioEffectManager.SET_AGC_MAX_LEVEL, step);
        }
    }

    public void setAgsAttrackTime (int step) {
        if (!creatAgcAudioEffects()) {
            Log.e(TAG, "setAgsAttrackTime mAgc creat fail");
            return;
        }
        int result = mAgc.setEnabled(true);
        if (result == AudioEffect.SUCCESS) {
            if (CanDebug()) Log.d(TAG, "setAgsAttrackTime = " + step);
            mAgc.setParameter(PARAM_AGC_ATTRACK_TIME, step * 48);
            saveAudioParameters(AudioEffectManager.SET_AGC_ATTRACK_TIME, step);
        }
    }

    public void setAgsReleaseTime (int step) {
        if (!creatAgcAudioEffects()) {
            Log.e(TAG, "setAgsReleaseTime mAgc creat fail");
            return;
        }
        int result = mAgc.setEnabled(true);
        if (result == AudioEffect.SUCCESS) {
            if (CanDebug()) Log.d(TAG, "setAgsReleaseTime = " + step);
            mAgc.setParameter(PARAM_AGC_RELEASE_TIME, step);
            saveAudioParameters(AudioEffectManager.SET_AGC_RELEASE_TIME, step);
        }
    }

    public void setSourceIdForAvl (int step) {
        if (!creatAgcAudioEffects()) {
            Log.e(TAG, "setSourceIdForAvl mAgc creat fail");
            return;
        }
        int result = mAgc.setEnabled(true);
        if (result == AudioEffect.SUCCESS) {
            if (CanDebug()) Log.d(TAG, "setSourceIdForAvl = " + step);
            mAgc.setParameter(PARAM_AGC_SOURCE_ID, step);
            saveAudioParameters(AudioEffectManager.SET_AGC_SOURCE_ID, step);
        }
    }

    public void setVirtualSurround (int mode) {
        if (!creatVirtualSurroundAudioEffects()) {
            Log.e(TAG, "setVirtualSurround mVirtualSurround creat fail");
            return;
        }
        int result = mVirtualSurround.setEnabled(true);
        if (result == AudioEffect.SUCCESS) {
            if (CanDebug()) Log.d(TAG, "setVirtualSurround = " + mode);
            mVirtualSurround.setParameter(PARAM_VIRTUALSURROUND, mode);
            saveAudioParameters(AudioEffectManager.SET_VIRTUAL_SURROUND, mode);
        }
    }

    public void setParameters(int order, int value) {
        switch (order) {
            case AudioEffectManager.SET_BASS:
                setBass(value);
                break;
            case AudioEffectManager.SET_TREBLE:
                setTreble(value);
                break;
            case AudioEffectManager.SET_BALANCE:
                setBalance(value);
                break;
            case AudioEffectManager.SET_DIALOG_CLARITY_MODE:
                setDialogClarityMode(value);
                break;
            case AudioEffectManager.SET_SURROUND_ENABLE:
                setSurroundEnable(value == 1);
                break;
            case AudioEffectManager.SET_TRUBASS_ENABLE:
                setTruBassEnable(value == 1);
                break;
            case AudioEffectManager.SET_SOUND_MODE:
                setSoundMode(value);
                break;
            case AudioEffectManager.SET_EFFECT_BAND1:
            case AudioEffectManager.SET_EFFECT_BAND2:
            case AudioEffectManager.SET_EFFECT_BAND3:
            case AudioEffectManager.SET_EFFECT_BAND4:
            case AudioEffectManager.SET_EFFECT_BAND5:
                setDifferentBandEffects(order, value, true);
                break;
            case AudioEffectManager.SET_AGC_ENABLE:
                setAgsEnable(value);
                break;
            case AudioEffectManager.SET_AGC_MAX_LEVEL:
                setAgsMaxLevel(value);
                break;
            case AudioEffectManager.SET_AGC_ATTRACK_TIME:
                setAgsAttrackTime(value);
                break;
            case AudioEffectManager.SET_AGC_RELEASE_TIME:
                setAgsReleaseTime(value);
                break;
            case AudioEffectManager.SET_AGC_SOURCE_ID:
                setSourceIdForAvl(value);
                break;
            case AudioEffectManager.SET_VIRTUAL_SURROUND:
                setVirtualSurround(value);
                break;
            default:
                break;
        }
    }

    public int getParameters(int order) {
        int value = -1;
        if (order < AudioEffectManager.SET_BASS || order > AudioEffectManager.SET_VIRTUAL_SURROUND) {
            Log.e(TAG, "getParameters order erro");
            return value;
        }
        value = getSavedAudioParameters(order);
        return value;
    }

    private void saveAudioParameters(int id, int value) {
        switch (id) {
            case AudioEffectManager.SET_BASS:
                int soundModeBass = getSoundModeFromDb();
                if (AudioEffectManager.MODE_CUSTOM == soundModeBass) {
                    Settings.Global.putInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_BASS, value);
                }
                break;
            case AudioEffectManager.SET_TREBLE:
                int soundModeTreble = getSoundModeFromDb();
                if (AudioEffectManager.MODE_CUSTOM == soundModeTreble) {
                    Settings.Global.putInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_TREBLE, value);
                }
                break;
            case AudioEffectManager.SET_BALANCE:
                Settings.Global.putInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_BALANCE, value);
                break;
            case AudioEffectManager.SET_DIALOG_CLARITY_MODE:
                Settings.Global.putInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_DIALOG_CLARITY, value);
                break;
            case AudioEffectManager.SET_SURROUND_ENABLE:
                Settings.Global.putInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_SURROUND, value);
                break;
            case AudioEffectManager.SET_TRUBASS_ENABLE:
                Settings.Global.putInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_TRUBASS, value);
                break;
            case AudioEffectManager.SET_SOUND_MODE:
                String soundmodetype = Settings.Global.getString(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_SOUND_MODE_TYPE);
                if (soundmodetype == null || AudioEffectManager.DB_ID_SOUND_EFFECT_SOUND_MODE_TYPE_EQ.equals(soundmodetype)) {
                    Settings.Global.putInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_SOUND_MODE_EQ_VALUE, value);
                } else if ((AudioEffectManager.DB_ID_SOUND_EFFECT_SOUND_MODE_TYPE_DAP.equals(soundmodetype))) {
                    Settings.Global.putInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_SOUND_MODE_DAP_VALUE, value);
                } else {
                    Settings.Global.putInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_SOUND_MODE, value);
                }
                break;
            case AudioEffectManager.SET_EFFECT_BAND1:
                Settings.Global.putInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_BAND1, value);
                break;
            case AudioEffectManager.SET_EFFECT_BAND2:
                Settings.Global.putInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_BAND2, value);
                break;
            case AudioEffectManager.SET_EFFECT_BAND3:
                Settings.Global.putInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_BAND3, value);
                break;
            case AudioEffectManager.SET_EFFECT_BAND4:
                Settings.Global.putInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_BAND4, value);
                break;
            case AudioEffectManager.SET_EFFECT_BAND5:
                Settings.Global.putInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_BAND5, value);
                break;
            case AudioEffectManager.SET_AGC_ENABLE:
                Settings.Global.putInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_AGC_ENABLE, value);
                break;
            case AudioEffectManager.SET_AGC_MAX_LEVEL:
                Settings.Global.putInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_AGC_MAX_LEVEL, value);
                break;
            case AudioEffectManager.SET_AGC_ATTRACK_TIME:
                Settings.Global.putInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_AGC_ATTRACK_TIME, value);
                break;
            case AudioEffectManager.SET_AGC_RELEASE_TIME:
                Settings.Global.putInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_AGC_RELEASE_TIME, value);
                break;
            case AudioEffectManager.SET_AGC_SOURCE_ID:
                Settings.Global.putInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_AGC_SOURCE_ID, value);
                break;
            case AudioEffectManager.SET_VIRTUAL_SURROUND:
                Settings.Global.putInt(mContext.getContentResolver(), OutputModeManager.VIRTUAL_SURROUND, value);
                break;
            case AudioEffectManager.SET_VIRTUALX_MODE:
                Settings.Global.putInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_VIRTUALX_MODE, value);
                break;
            case AudioEffectManager.SET_TRUVOLUME_HD_ENABLE:
                Settings.Global.putInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_TREVOLUME_HD, value);
                break;
            default:
                break;
        }
    }

    private int getSoundModeFromDb() {
        String soundmodetype = Settings.Global.getString(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_SOUND_MODE_TYPE);
        if (soundmodetype == null || AudioEffectManager.DB_ID_SOUND_EFFECT_SOUND_MODE_TYPE_EQ.equals(soundmodetype)) {
            return Settings.Global.getInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_SOUND_MODE_EQ_VALUE, AudioEffectManager.MODE_STANDARD);
        } else if ((AudioEffectManager.DB_ID_SOUND_EFFECT_SOUND_MODE_TYPE_DAP.equals(soundmodetype))) {
            return Settings.Global.getInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_SOUND_MODE_DAP_VALUE, AudioEffectManager.MODE_STANDARD);
        } else {
            return Settings.Global.getInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_SOUND_MODE, AudioEffectManager.MODE_STANDARD);
        }
    }

    private int getSavedAudioParameters(int id) {
        int result = -1;
        switch (id) {
            case AudioEffectManager.SET_BASS:
                int soundModeBass = getSoundModeFromDb();
                if (AudioEffectManager.MODE_CUSTOM == soundModeBass) {
                    result = Settings.Global.getInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_BASS, AudioEffectManager.EFFECT_BASS_DEFAULT);
                } else {
                    result = AudioEffectManager.EFFECT_BASS_DEFAULT;
                }
                break;
            case AudioEffectManager.SET_TREBLE:
                int soundModeTreble = getSoundModeFromDb();
                if (AudioEffectManager.MODE_CUSTOM == soundModeTreble) {
                    result = Settings.Global.getInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_TREBLE, AudioEffectManager.EFFECT_TREBLE_DEFAULT);
                } else {
                    result = AudioEffectManager.EFFECT_TREBLE_DEFAULT;
                }
                break;
            case AudioEffectManager.SET_BALANCE:
                result = Settings.Global.getInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_BALANCE, AudioEffectManager.EFFECT_BALANCE_DEFAULT);
                break;
            case AudioEffectManager.SET_DIALOG_CLARITY_MODE:
                result = Settings.Global.getInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_DIALOG_CLARITY, AudioEffectManager.SOUND_EFFECT_DIALOG_CLARITY_ENABLE_DEFAULT);
                break;
            case AudioEffectManager.SET_SURROUND_ENABLE:
                result = Settings.Global.getInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_SURROUND, AudioEffectManager.SOUND_EFFECT_SURROUND_ENABLE_DEFAULT);
                break;
            case AudioEffectManager.SET_TRUBASS_ENABLE:
                result = Settings.Global.getInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_TRUBASS, AudioEffectManager.SOUND_EFFECT_TRUBASS_ENABLE_DEFAULT);
                break;
            case AudioEffectManager.SET_SOUND_MODE:
                result = getSoundModeFromDb();
                Log.d(TAG, "getSavedAudioParameters SET_SOUND_MODE = " + result);
                break;
            case AudioEffectManager.SET_EFFECT_BAND1:
                result = Settings.Global.getInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_BAND1, EFFECT_SOUND_MODE_USER_BAND[PARAM_BAND1 - PARAM_BAND1]);
                break;
            case AudioEffectManager.SET_EFFECT_BAND2:
                result = Settings.Global.getInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_BAND2, EFFECT_SOUND_MODE_USER_BAND[PARAM_BAND2 - PARAM_BAND1]);
                break;
            case AudioEffectManager.SET_EFFECT_BAND3:
                result = Settings.Global.getInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_BAND3, EFFECT_SOUND_MODE_USER_BAND[PARAM_BAND3 - PARAM_BAND1]);
                break;
            case AudioEffectManager.SET_EFFECT_BAND4:
                result = Settings.Global.getInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_BAND4, EFFECT_SOUND_MODE_USER_BAND[PARAM_BAND4 - PARAM_BAND1]);
                break;
            case AudioEffectManager.SET_EFFECT_BAND5:
                result = Settings.Global.getInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_BAND5, EFFECT_SOUND_MODE_USER_BAND[PARAM_BAND5 - PARAM_BAND1]);
                break;
            case AudioEffectManager.SET_AGC_ENABLE:
                result = Settings.Global.getInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_AGC_ENABLE, DEFAULT_AGC_ENABLE ? 1 : 0);
                break;
            case AudioEffectManager.SET_AGC_MAX_LEVEL:
                result = Settings.Global.getInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_AGC_MAX_LEVEL, DEFAULT_AGC_MAX_LEVEL);
                break;
            case AudioEffectManager.SET_AGC_ATTRACK_TIME:
                result = Settings.Global.getInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_AGC_ATTRACK_TIME, DEFAULT_AGC_ATTRACK_TIME);
                break;
            case AudioEffectManager.SET_AGC_RELEASE_TIME:
                result = Settings.Global.getInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_AGC_RELEASE_TIME, DEFAULT_AGC_RELEASE_TIME);
                break;
            case AudioEffectManager.SET_AGC_SOURCE_ID:
                result = Settings.Global.getInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_AGC_SOURCE_ID, DEFAULT_AGC_SOURCE_ID);
                break;
            case AudioEffectManager.SET_VIRTUAL_SURROUND:
                result = Settings.Global.getInt(mContext.getContentResolver(), OutputModeManager.VIRTUAL_SURROUND, OutputModeManager.VIRTUAL_SURROUND_OFF);
                break;
            case AudioEffectManager.SET_VIRTUALX_MODE:
                result = Settings.Global.getInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_VIRTUALX_MODE, AudioEffectManager.SOUND_EFFECT_VIRTUALX_MODE_DEFAULT);
                break;
            case AudioEffectManager.SET_TRUVOLUME_HD_ENABLE:
                result = Settings.Global.getInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_TREVOLUME_HD, AudioEffectManager.SOUND_EFFECT_TRUVOLUME_HD_ENABLE_DEFAULT);
                break;
            default:
                break;
        }
        return result;
    }

    public void cleanupAudioEffects() {
        if (mBalance!= null) {
            mBalance.setEnabled(false);
            mBalance.release();
            mBalance = null;
        }
        if (mTruSurround!= null) {
            mTruSurround.setEnabled(false);
            mTruSurround.release();
            mTruSurround = null;
        }
        if (mTrebleBass!= null) {
            mTrebleBass.setEnabled(false);
            mTrebleBass.release();
            mTrebleBass = null;
        }
        if (mSoundMode!= null) {
            mSoundMode.setEnabled(false);
            mSoundMode.release();
            mSoundMode = null;
        }
        if (mAgc!= null) {
            mAgc.setEnabled(false);
            mAgc.release();
            mAgc = null;
        }
        if (mVirtualSurround != null) {
            mVirtualSurround.setEnabled(false);
            mVirtualSurround.release();
            mVirtualSurround = null;
        }
    }

    public void initSoundEffectSettings() {
        if (Settings.Global.getInt(mContext.getContentResolver(), "set_five_band", 0) == 0) {
            if (mSoundMode != null) {
                byte[] fiveBandNum = new byte[5];
                mSoundMode.getParameter(PARAM_EQ_CUSTOM, fiveBandNum);
                for (int i = AudioEffectManager.SET_EFFECT_BAND1; i <= AudioEffectManager.SET_EFFECT_BAND5; i++) {
                    saveAudioParameters(i, unMappingLine(fiveBandNum[i - AudioEffectManager.SET_EFFECT_BAND1], true));
                }
            } else {
                for (int i = AudioEffectManager.SET_EFFECT_BAND1; i <= AudioEffectManager.SET_EFFECT_BAND5; i++) {
                    saveAudioParameters(i, EFFECT_SOUND_MODE_USER_BAND[i - AudioEffectManager.SET_EFFECT_BAND1]);
                }
                Log.w(TAG, "get default band value fail, set default value, mSoundMode == null");
            }
            Settings.Global.putInt(mContext.getContentResolver(), "set_five_band", 1);
        }
        for (int i = 0; i < AudioEffectManager.SET_EFFECT_BAND1; i++) {
            int value = getSavedAudioParameters(i);
            setParameters(i, value);
            Log.d(TAG, "initSoundEffectSettings NO." + i + "=" + value);
        }

        int soundMode = getSavedAudioParameters(AudioEffectManager.SET_SOUND_MODE);
        setSoundModeByObserver(soundMode);

        for (int i = AudioEffectManager.SET_AGC_ENABLE; i < AudioEffectManager.SET_VIRTUAL_SURROUND + 1; i++) {
            int value = getSavedAudioParameters(i);
            setParameters(i, value);
            Log.d(TAG, "initSoundEffectSettings NO." + i + "=" + value);
        }
        OutputModeManager opm = new OutputModeManager(mContext);
        int audioOutPutLatency = Settings.Global.getInt(mContext.getContentResolver(), OutputModeManager.DB_FIELD_AUDIO_OUTPUT_LATENCY,
                OutputModeManager.AUDIO_OUTPUT_LATENCY_DEFAULT);
        opm.setAudioOutputLatency(audioOutPutLatency);
        //init sound parameter at the same time
        /*SoundParameterSettingManager soundparameter = new SoundParameterSettingManager(mContext);
        if (soundparameter != null) {
            soundparameter.initParameterAfterBoot();
        }
        soundparameter = null;*/
        applyAudioEffectByPlayEmptyTrack();

        if (isSupportVirtualX()) {
            setDtsVirtualXMode(getDtsVirtualXMode());
            setDtsTruVolumeHdEnable(getDtsTruVolumeHdEnable());
        } else {
            setSurroundEnable(getSurroundEnable());
            setDialogClarityMode(getDialogClarityMode());
            setTruBassEnable(getTruBassEnable());
        }
    }

    public void resetSoundEffectSettings() {
        Log.d(TAG, "resetSoundEffectSettings");
        cleanupAudioEffects();
        Settings.Global.putInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_BASS, AudioEffectManager.EFFECT_BASS_DEFAULT);
        Settings.Global.putInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_TREBLE, AudioEffectManager.EFFECT_TREBLE_DEFAULT);
        Settings.Global.putInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_BALANCE, AudioEffectManager.EFFECT_BALANCE_DEFAULT);
        Settings.Global.putInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_DIALOG_CLARITY, AudioEffectManager.SOUND_EFFECT_DIALOG_CLARITY_ENABLE_DEFAULT);
        Settings.Global.putInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_SURROUND, AudioEffectManager.SOUND_EFFECT_SURROUND_ENABLE_DEFAULT);
        Settings.Global.putInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_TRUBASS, AudioEffectManager.SOUND_EFFECT_TRUBASS_ENABLE_DEFAULT);
        Settings.Global.putString(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_SOUND_MODE_TYPE, AudioEffectManager.DB_ID_SOUND_EFFECT_SOUND_MODE_TYPE_EQ);
        Settings.Global.putInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_SOUND_MODE, AudioEffectManager.MODE_STANDARD);
        Settings.Global.putInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_SOUND_MODE_DAP_VALUE, AudioEffectManager.MODE_STANDARD);
        Settings.Global.putInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_SOUND_MODE_EQ_VALUE, AudioEffectManager.MODE_STANDARD);
        Settings.Global.putInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_BAND1, EFFECT_SOUND_MODE_USER_BAND[PARAM_BAND1 - PARAM_BAND1]);
        Settings.Global.putInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_BAND2, EFFECT_SOUND_MODE_USER_BAND[PARAM_BAND2 - PARAM_BAND1]);
        Settings.Global.putInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_BAND3, EFFECT_SOUND_MODE_USER_BAND[PARAM_BAND3 - PARAM_BAND1]);
        Settings.Global.putInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_BAND4, EFFECT_SOUND_MODE_USER_BAND[PARAM_BAND4 - PARAM_BAND1]);
        Settings.Global.putInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_BAND5, EFFECT_SOUND_MODE_USER_BAND[PARAM_BAND5 - PARAM_BAND1]);
        Settings.Global.putInt(mContext.getContentResolver(), "set_five_band", 0);
        Settings.Global.putInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_AGC_ENABLE, DEFAULT_AGC_ENABLE ? 1 : 0);
        Settings.Global.putInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_AGC_MAX_LEVEL, DEFAULT_AGC_MAX_LEVEL);
        Settings.Global.putInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_AGC_ATTRACK_TIME, DEFAULT_AGC_ATTRACK_TIME);
        Settings.Global.putInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_AGC_RELEASE_TIME, DEFAULT_AGC_RELEASE_TIME);
        Settings.Global.putInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_AGC_SOURCE_ID, DEFAULT_AGC_SOURCE_ID);
        Settings.Global.putInt(mContext.getContentResolver(), OutputModeManager.VIRTUAL_SURROUND, OutputModeManager.VIRTUAL_SURROUND_OFF);
        Settings.Global.putInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_VIRTUALX_MODE, AudioEffectManager.SOUND_EFFECT_VIRTUALX_MODE_DEFAULT);
        Settings.Global.putInt(mContext.getContentResolver(), AudioEffectManager.DB_ID_SOUND_EFFECT_TREVOLUME_HD, AudioEffectManager.SOUND_EFFECT_TRUVOLUME_HD_ENABLE_DEFAULT);
        initSoundEffectSettings();
    }

    private void applyAudioEffectByPlayEmptyTrack() {
        int bufsize = AudioTrack.getMinBufferSize(8000, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        byte data[] = new byte[bufsize];
        AudioTrack trackplayer = new AudioTrack(AudioManager.STREAM_MUSIC, 8000, AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT, bufsize, AudioTrack.MODE_STREAM);
        trackplayer.play();
        trackplayer.write(data, 0, data.length);
        trackplayer.stop();
        trackplayer.release();
    }
}

