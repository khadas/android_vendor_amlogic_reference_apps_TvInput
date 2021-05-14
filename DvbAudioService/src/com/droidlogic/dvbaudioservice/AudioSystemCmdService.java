/*
 * Copyright (c) 2014 Amlogic, Inc. All rights reserved.
 *
 * This source code is subject to the terms and conditions defined in the
 * file 'LICENSE' which is part of this source code package.
 *
 * Description:
 *     AMLOGIC AudioSystemCmdService
 */

package com.droidlogic.dvbaudioservice;

import java.util.ArrayList;
import java.util.List;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Slog;
import android.media.AudioDevicePort;
import android.media.AudioFormat;
import android.media.AudioGain;
import android.media.AudioGainConfig;
import android.media.AudioManager;
import android.media.AudioPatch;
import android.media.AudioPort;
import android.media.AudioPortConfig;
import android.media.AudioRoutesInfo;
import android.media.AudioSystem;
import android.media.IAudioRoutesObserver;
import android.media.IAudioService;
import android.media.tv.TvInputManager;

import com.droidlogic.app.SystemControlManager;

//this service used to call audio system commands
public class AudioSystemCmdService extends Service implements SystemControlManager.AudioListener{
    private static final String TAG = "AudioSystemCmdService";

    private SystemControlManager mSystemControlManager;
    private AudioManager mAudioManager = null;
    private AudioPatch mAudioPatch = null;
    private Context mContext;
    private int mCurrentIndex = 0;
    private int mCommitedIndex = -1;
    private int mCurrentMaxIndex = 0;
    private int mCurrentMinIndex = 0;
    private final Object mLock = new Object();
    private final Handler mHandler = new Handler();
    private AudioDevicePort mAudioSource;
    private List<AudioDevicePort> mAudioSink = new ArrayList<>();
    private float mCommittedSourceVolume = -1f;
    private float mSourceVolume = 1.0f;
    private int mDesiredSamplingRate = 0;
    private int mDesiredChannelMask = AudioFormat.CHANNEL_OUT_DEFAULT;
    private int mDesiredFormat = AudioFormat.ENCODING_DEFAULT;
    private int mCurrentFmt;
    private int mCurrentSubFmt = -1;
    private int mCurrentSubPid = -1;
    private int mCurrentHasDtvVideo;
    private TvInputManager mTvInputManager;

    private static final int ADEC_START_DECODE                          = 1;
    private static final int ADEC_PAUSE_DECODE                          = 2;
    private static final int ADEC_RESUME_DECODE                         = 3;
    private static final int ADEC_STOP_DECODE                           = 4;
    private static final int ADEC_SET_DECODE_AD                         = 5;
    private static final int ADEC_SET_VOLUME                            = 6;
    private static final int ADEC_SET_MUTE                              = 7;
    private static final int ADEC_SET_OUTPUT_MODE                       = 8;
    private static final int ADEC_SET_PRE_GAIN                          = 9;
    private static final int ADEC_SET_PRE_MUTE                          = 10;
    private static final int ADEC_OPEN_DECODER                          = 12;
    private static final int ADEC_CLOSE_DECODER                         = 13;
    private static final int ADEC_SET_DEMUX_INFO                        = 14;
    private static final int ADEC_SET_SECURITY_MEM_LEVEL                = 15;

    //audio ad
    public static final int MSG_MIX_AD_DUAL_SUPPORT                     = 20;
    public static final int MSG_MIX_AD_MIX_SUPPORT                      = 21;
    public static final int MSG_MIX_AD_MIX_LEVEL                        = 22;
    public static final int MSG_MIX_AD_SET_MAIN                         = 23;
    public static final int MSG_MIX_AD_SET_ASSOCIATE                    = 24;

    private final BroadcastReceiver mVolumeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            handleVolumeChange(context, intent);
        }
    };

    private boolean mHasStartedDecoder;
    private boolean mHasOpenedDecoder;
    private boolean mHasReceivedStartDecoderCmd;
    private boolean  mMixAdSupported;
    private boolean  mNotImptTvHardwareInputService;
    private IAudioService mAudioService;
    private AudioRoutesInfo mCurAudioRoutesInfo;
    private Runnable mHandleAudioSinkUpdatedRunnable;
    final IAudioRoutesObserver.Stub mAudioRoutesObserver = new IAudioRoutesObserver.Stub() {
        @Override
        public void dispatchAudioRoutesChanged(final AudioRoutesInfo newRoutes) {
            Log.i(TAG, "dispatchAudioRoutesChanged");
            mCurAudioRoutesInfo = newRoutes;
            mHasStartedDecoder = false;
            mHandler.removeCallbacks(mHandleAudioSinkUpdatedRunnable);
            mHandleAudioSinkUpdatedRunnable = new Runnable() {
                public void run() {
                    synchronized (mLock) {
                        if (mNotImptTvHardwareInputService)
                            handleAudioSinkUpdated();
                        reStartAdecDecoderIfPossible();
                    }
                }
            };

            if (mTvInputManager.getHardwareList() == null) {
                mHandler.post(mHandleAudioSinkUpdatedRunnable);
            } else {
                try {
                    mHandler.postDelayed(mHandleAudioSinkUpdatedRunnable,
                        mAudioService.isBluetoothA2dpOn() ? 2500 : 500);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate");
        super.onCreate();
        mContext = getApplicationContext();
        mSystemControlManager = SystemControlManager.getInstance();
        mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        mTvInputManager = (TvInputManager) getSystemService(Context.TV_INPUT_SERVICE);
        mSystemControlManager.setAudioListener(this);
        IBinder b = ServiceManager.getService(Context.AUDIO_SERVICE);
        mAudioService = IAudioService.Stub.asInterface(b);
        try {
            mCurAudioRoutesInfo = mAudioService.startWatchingRoutes(mAudioRoutesObserver);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        final IntentFilter filter = new IntentFilter();
        filter.addAction(AudioManager.VOLUME_CHANGED_ACTION);
        filter.addAction(AudioManager.STREAM_MUTE_CHANGED_ACTION);
        mContext.registerReceiver(mVolumeReceiver, filter);
        mNotImptTvHardwareInputService = (mTvInputManager.getHardwareList() == null) || (mTvInputManager.getHardwareList().isEmpty());
        updateVolume();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private String AudioCmdToString(int cmd) {
        String temp = "["+cmd+"]";
        switch (cmd) {
            case ADEC_START_DECODE:
                return temp + "START_DECODE";
            case ADEC_PAUSE_DECODE:
                return temp + "PAUSE_DECODE";
            case ADEC_RESUME_DECODE:
                return temp + "RESUME_DECODE";
            case ADEC_STOP_DECODE:
                return temp + "STOP_DECODE";
            case ADEC_SET_DECODE_AD:
                return temp + "SET_DECODE_AD";
            case ADEC_SET_VOLUME:
                return temp + "SET_VOLUME";
            case ADEC_SET_MUTE:
                return temp + "SET_MUTE";
            case ADEC_SET_OUTPUT_MODE:
                return temp + "SET_OUTPUT_MODE";
            case ADEC_SET_PRE_GAIN:
                return temp + "SET_PRE_GAIN";
            case ADEC_SET_PRE_MUTE:
                return temp + "SET_PRE_MUTE";
            case ADEC_OPEN_DECODER:
                return temp + "OPEN_DECODER";
            case ADEC_CLOSE_DECODER:
                return temp + "CLOSE_DECODER";
            case ADEC_SET_DEMUX_INFO:
                return temp + "SET_DEMUX_INFO";
            case ADEC_SET_SECURITY_MEM_LEVEL:
                return temp + "SET_SECURITY_MEM_LEVEL";

            case MSG_MIX_AD_DUAL_SUPPORT:
                return temp + "AD_DUAL_SUPPORT";
            case MSG_MIX_AD_MIX_SUPPORT:
                return temp + "AD_MIX_SUPPORT";
            case MSG_MIX_AD_MIX_LEVEL:
                return temp + "AD_MIX_LEVEL";
            case MSG_MIX_AD_SET_MAIN:
                return temp + "AD_SET_MAIN";
            case MSG_MIX_AD_SET_ASSOCIATE:
                return temp + "AD_SET_ASSOCIATE";
            default:
                return temp + "invalid cmd";
        }
    }

    private boolean setAdFunction(int msg, int param1) {
        boolean result = false;
        if (mAudioManager == null) {
            Log.i(TAG, "setAdFunction null audioManager");
            return result;
        }
        //Log.d(TAG, "setAdFunction msg = " + msg + ", param1 = " + param1);
        switch (msg) {
            case MSG_MIX_AD_DUAL_SUPPORT://dual_decoder_surport for ad & main mix on/off
                if (param1 > 0) {
                    mAudioManager.setParameters("dual_decoder_support=1");
                } else {
                    mAudioManager.setParameters("dual_decoder_support=0");
                }
                Log.d(TAG, "setAdFunction MSG_MIX_AD_DUAL_SUPPORT setParameters:"
                        + "dual_decoder_support=" + (param1 > 0 ? 1 : 0));
                result = true;
                break;
            case MSG_MIX_AD_MIX_SUPPORT://Associated audio mixing on/off
                if (param1 > 0) {
                    mAudioManager.setParameters("associate_audio_mixing_enable=1");
                } else {
                    mAudioManager.setParameters("associate_audio_mixing_enable=0");
                }
                Log.d(TAG, "setAdFunction MSG_MIX_AD_MIX_SUPPORT setParameters:"
                        + "associate_audio_mixing_enable=" + (param1 > 0 ? 1 : 0));
                result = true;
                break;

            case MSG_MIX_AD_MIX_LEVEL://Associated audio mixing level
                mAudioManager.setParameters("dual_decoder_mixing_level=" + param1 + "");
                Log.d(TAG, "setAdFunction MSG_MIX_AD_MIX_LEVEL setParameters:"
                        + "dual_decoder_mixing_level=" + param1);
                result = true;
                break;
            /*case MSG_MIX_AD_SET_MAIN://set Main Audio by handle
                result = playerSelectAudioTrack(param1);
                Log.d(TAG, "setAdFunction MSG_MIX_AD_SET_MAIN result=" + result
                        + ", setAudioStream " + param1);
                break;
            case MSG_MIX_AD_SET_ASSOCIATE://set Associate Audio by handle
                result = playersetAudioDescriptionOn(param1 == 1);
                Log.d(TAG, "setAdFunction MSG_MIX_AD_SET_ASSOCIATE result=" + result
                        + "setAudioDescriptionOn " + (param1 == 1));
                break;*/
            default:
                Log.i(TAG,"setAdFunction unkown  msg:" + msg + ", param1:" + param1);
                break;
        }
              return result;
    }

    @Override
    public void OnAudioEvent(int cmd, int param1, int param2) {
        Log.d(TAG, "OnAudioEvent cmd:"+ AudioCmdToString(cmd) + ", param1:" + param1 + ", param2:" + param2);
        if (mAudioManager == null) {
            Log.e(TAG, "OnAudioEvent mAudioManager is null");
        } else {
            switch (cmd) {
                case ADEC_SET_DEMUX_INFO:
                    mAudioManager.setParameters("pid="+param1);
                    mAudioManager.setParameters("demux_id="+param2);
                    mAudioManager.setParameters("cmd="+cmd);
                    break;
                case ADEC_SET_SECURITY_MEM_LEVEL:
                    mAudioManager.setParameters("security_mem_level="+param1);
                    break;
                case ADEC_START_DECODE:
                    mHasReceivedStartDecoderCmd = true;
                    mCurrentFmt = param1;
                    mCurrentHasDtvVideo = param2;
                    mHasStartedDecoder = true;
                    if (mMixAdSupported == false) {
                        setAdFunction(MSG_MIX_AD_DUAL_SUPPORT, 0);
                        setAdFunction(MSG_MIX_AD_MIX_SUPPORT, 0);
                        mAudioManager.setParameters("subafmt=-1");
                        mAudioManager.setParameters("subapid=-1");
                    } else if (mMixAdSupported == true){
                        mAudioManager.setParameters("subafmt="+mCurrentSubFmt);
                        mAudioManager.setParameters("subapid="+mCurrentSubPid);
                        setAdFunction(MSG_MIX_AD_DUAL_SUPPORT, 1);
                        setAdFunction(MSG_MIX_AD_MIX_SUPPORT, 1);
                    }
                    mAudioManager.setParameters("fmt="+param1);
                    mAudioManager.setParameters("has_dtv_video="+param2);
                    mAudioManager.setParameters("cmd="+cmd);
                    break;
                case ADEC_PAUSE_DECODE:
                    mAudioManager.setParameters("cmd="+cmd);
                    break;
                case ADEC_RESUME_DECODE:
                    mAudioManager.setParameters("cmd="+cmd);
                    break;
                case ADEC_STOP_DECODE:
                    mHasReceivedStartDecoderCmd = false;
                    mAudioManager.setParameters("cmd="+cmd);
                    break;
                case ADEC_SET_DECODE_AD:
                    mCurrentSubFmt = param1;
                    mCurrentSubPid = param2;
                    mAudioManager.setParameters("cmd="+cmd);
                    mAudioManager.setParameters("subafmt="+param1);
                    mAudioManager.setParameters("subapid="+param2);
                    Log.d(TAG, "OnAudioEvent subafmt:" + param1 + ", subapid:" + param2);
                    break;
                case MSG_MIX_AD_MIX_SUPPORT://Associated audio mixing on/off
                    if (param1 == 0) {
                        mMixAdSupported = false;
                        //setAdFunction(MSG_MIX_AD_DUAL_SUPPORT, 0);
                        //setAdFunction(MSG_MIX_AD_MIX_SUPPORT, 0);
                    } else if (param1 == 1) {
                        mMixAdSupported = true;
                        //setAdFunction(MSG_MIX_AD_DUAL_SUPPORT, 1);
                        //setAdFunction(MSG_MIX_AD_MIX_SUPPORT, 1);
                    }
                    Log.d(TAG, "OnAudioEvent associate_audio_mixing_enable=" + (param1 > 0 ? 1 : 0));
                break;
                case MSG_MIX_AD_MIX_LEVEL:
                     setAdFunction(MSG_MIX_AD_MIX_LEVEL, param2);
                break;
                case ADEC_SET_VOLUME:
                    //mAudioManager.setParameters("cmd="+cmd);
                    //mAudioManager.setParameters("vol="+param1);
                    /*if (mHasOpenedDecoder) {
                        updateVolume();
                        updateAudioSourceAndAudioSink();
                        handleAudioSinkUpdated();
                       }*/
                    Log.d(TAG,"SET_VOLUME now triggered by AudioManager.VOLUME_CHANGED_ACTION");
                    break;
                case ADEC_SET_MUTE:
                    //mAudioManager.setParameters("cmd="+cmd);
                    mAudioManager.setParameters("TV-Mute="+param1);
                    //updateVolume();
                    //updateAudioSourceAndAudioSink();
                    //updateAudioConfigLocked();
                    break;
                case ADEC_SET_OUTPUT_MODE:
                    mAudioManager.setParameters("cmd="+cmd);
                    mAudioManager.setParameters("mode="+param1);
                    break;
                case ADEC_SET_PRE_GAIN:
                    mAudioManager.setParameters("cmd="+cmd);
                    mAudioManager.setParameters("gain="+param1);
                    break;
                case ADEC_SET_PRE_MUTE:
                    mAudioManager.setParameters("cmd="+cmd);
                    mAudioManager.setParameters("mute="+param1);
                    break;
                case ADEC_OPEN_DECODER:
                    updateAudioSourceAndAudioSink();
                    if (mNotImptTvHardwareInputService)
                        handleAudioSinkUpdated();
                    mHasOpenedDecoder = true;
                    reStartAdecDecoderIfPossible();
                    break;
                case ADEC_CLOSE_DECODER://
                    if (mAudioPatch != null) {
                       Log.d(TAG, "ADEC_CLOSE_DECODER mAudioPatch:"
                            + mAudioPatch);
                        mAudioManager.releaseAudioPatch(mAudioPatch);
                    }
                    mAudioPatch = null;
                    mAudioSource = null;
                    mHasStartedDecoder = false;
                    mHasOpenedDecoder = false;
                    mMixAdSupported = false;
                    mCurrentSubFmt = -1;
                    mCurrentSubPid = -1;
                    break;
                default:
                    Log.w(TAG,"OnAudioEvent unkown audio cmd!");
                    break;
            }
        }
    }

    private void updateAudioSourceAndAudioSink() {
        mAudioSource = findAudioDevicePort(AudioManager.DEVICE_IN_TV_TUNER, "");
        findAudioSinkFromAudioPolicy(mAudioSink);
    }

    /**
     * Convert volume from float [0.0 - 1.0] to media volume UI index
     */
    private int volumeToMediaIndex(float volume) {
        return mCurrentMinIndex + (int)(volume * (mCurrentMaxIndex - mCurrentMinIndex));
    }

    /**
     * Convert media volume UI index to Milli Bells for a given output device type
     * and gain controller
     */
    private int indexToGainMbForDevice(int index, int device, AudioGain gain) {
        float gainDb = AudioSystem.getStreamVolumeDB(AudioManager.STREAM_MUSIC,
                                                       index,
                                                       device);
        float maxGainDb = AudioSystem.getStreamVolumeDB(AudioManager.STREAM_MUSIC,
                                                        mCurrentMaxIndex,
                                                        device);
        float minGainDb = AudioSystem.getStreamVolumeDB(AudioManager.STREAM_MUSIC,
                                                        mCurrentMinIndex,
                                                        device);

        // Rescale gain from dB to mB and within gain conroller range and snap to steps
        int gainMb = (int)((float)(((gainDb - minGainDb) * (gain.maxValue() - gain.minValue()))
                        / (maxGainDb - minGainDb)) + gain.minValue());
        gainMb = (int)(((float)gainMb / gain.stepValue()) * gain.stepValue());

        return gainMb;
    }

    private void updateVolume() {
        mCurrentMaxIndex = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        mCurrentMinIndex = mAudioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC);
        mCurrentIndex = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        Log.d(TAG, "updateVolume mCurrentIndex:"+ mCurrentIndex + ", mCommitedIndex:" + mCommitedIndex);
    }

    private void handleVolumeChange(Context context, Intent intent) {
        String action = intent.getAction();
        switch (action) {
            case AudioManager.VOLUME_CHANGED_ACTION: {
                int streamType = intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_TYPE, -1);
                if (streamType != AudioManager.STREAM_MUSIC) {
                    return;
                }
                int index = intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_VALUE, 0);
                if (index == mCurrentIndex) {
                    return;
                }
                Log.i(TAG, "handleVolumeChange VOLUME_CHANGED index:" + index);
                mCurrentIndex = index;
                break;
            }
            case AudioManager.STREAM_MUTE_CHANGED_ACTION: {
                int streamType = intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_TYPE, -1);
                if (streamType != AudioManager.STREAM_MUSIC) {
                    return;
                }
                Log.i(TAG, "handleVolumeChange MUTE_CHANGED");
                // volume index will be updated at onMediaStreamVolumeChanged() through
                // updateVolume().
                break;
            }
            default:
                Slog.w(TAG, "handleVolumeChange action:" + action + ", Unrecognized intent: " + intent);
                return;
        }
        synchronized (mLock) {
            if (mNotImptTvHardwareInputService) {
                updateAudioConfigLocked();
            }
        }
    }

    private float getMediaStreamVolume() {
        return (float) mCurrentIndex / (float) mCurrentMaxIndex;
    }

    private void handleAudioSinkUpdated() {
        synchronized (mLock) {
            updateAudioConfigLocked();
        }
    }

    private boolean updateAudioSinkLocked() {
        List<AudioDevicePort> previousSink = mAudioSink;
        mAudioSink = new ArrayList<>();
        findAudioSinkFromAudioPolicy(mAudioSink);

        // Returns true if mAudioSink and previousSink differs.
        if (mAudioSink.size() != previousSink.size()) {
            return true;
        }
        previousSink.removeAll(mAudioSink);
        return !previousSink.isEmpty();
    }

    private void findAudioSinkFromAudioPolicy(List<AudioDevicePort> sinks) {
        sinks.clear();
        ArrayList<AudioPort> audioPorts = new ArrayList<>();
        if (AudioManager.listAudioPorts(audioPorts) != AudioManager.SUCCESS) {
            Log.w(TAG, "findAudioSinkFromAudioPolicy listAudioPorts failed");
            return;
        }
        int sinkDevice = mAudioManager.getDevicesForStream(AudioManager.STREAM_MUSIC);
        AudioDevicePort port;
        for (AudioPort audioPort : audioPorts) {
            if (audioPort instanceof AudioDevicePort) {
                port = (AudioDevicePort)audioPort;
                if ((port.type() & sinkDevice) != 0 &&
                        (port.type() & AudioSystem.DEVICE_BIT_IN) == 0) {
                    sinks.add(port);
                }
            }
        }
    }

    private AudioDevicePort findAudioDevicePort(int type, String address) {
        if (type == AudioManager.DEVICE_NONE) {
            return null;
        }
        ArrayList<AudioPort> audioPorts = new ArrayList<>();
        if (AudioManager.listAudioPorts(audioPorts) != AudioManager.SUCCESS) {
            Log.w(TAG, "findAudioDevicePort listAudioPorts failed");
            return null;
        }
        AudioDevicePort port;
        for (AudioPort audioPort : audioPorts) {
            if (audioPort instanceof AudioDevicePort) {
                port = (AudioDevicePort)audioPort;
                if (port.type() == type && port.address().equals(address)) {
                    return port;
                }
            }
        }
        return null;
    }

    private void reStartAdecDecoderIfPossible() {

        Log.i(TAG, "reStartAdecDecoderIfPossible StartDecoderCmd:" + mHasReceivedStartDecoderCmd +
                ", mMixAdSupported:" + mMixAdSupported);

        if (mAudioSource != null &&!mAudioSink.isEmpty() &&
            mHasOpenedDecoder && !mHasStartedDecoder) {
            mAudioManager.setParameters("tuner_in=dtv");
            if (mHasReceivedStartDecoderCmd) {
               if (mMixAdSupported == false) {
                    setAdFunction(MSG_MIX_AD_DUAL_SUPPORT, 0);
                    setAdFunction(MSG_MIX_AD_MIX_SUPPORT, 0);
                    mAudioManager.setParameters("subafmt=-1");
                    mAudioManager.setParameters("subapid=-1");
                } else if (mMixAdSupported == true) {
                    mAudioManager.setParameters("subafmt="+mCurrentSubFmt);
                    mAudioManager.setParameters("subapid="+mCurrentSubPid);
                    setAdFunction(MSG_MIX_AD_DUAL_SUPPORT, 1);
                    setAdFunction(MSG_MIX_AD_MIX_SUPPORT, 1);
                }
                mAudioManager.setParameters("fmt="+mCurrentFmt);
                mAudioManager.setParameters("has_dtv_video="+mCurrentHasDtvVideo);
                mAudioManager.setParameters("cmd=1");
                mHasStartedDecoder = true;
            }
        }
    }

    private void updateAudioConfigLocked() {

        boolean sinkUpdated = updateAudioSinkLocked();

        if (mAudioSource == null || mAudioSink.isEmpty()) {
            Log.i(TAG, "updateAudioConfigLocked return, mAudioSource:" +
                    mAudioSource + ", mAudioSink empty:" +  mAudioSink.isEmpty());
            if (mAudioPatch != null) {
                mAudioManager.releaseAudioPatch(mAudioPatch);
                mAudioPatch = null;
                mHasStartedDecoder = false;
            }
            mCommittedSourceVolume = -1f;
            mCommitedIndex = -1;
            return;
        }

        AudioPortConfig sourceConfig = mAudioSource.activeConfig();
        List<AudioPortConfig> sinkConfigs = new ArrayList<>();
        AudioPatch[] audioPatchArray = new AudioPatch[] { mAudioPatch };
        boolean shouldRecreateAudioPatch = sinkUpdated;
        boolean shouldApplyGain = false;

        Log.i(TAG, "updateAudioConfigLocked sinkUpdated:" + sinkUpdated + ", mAudioPatch is empty:"
                + (mAudioPatch == null));
         //mAudioPatch should not be null when current hardware is active.
        if (mAudioPatch == null) {
            shouldRecreateAudioPatch = true;
            mHasStartedDecoder = false;
        }

        for (AudioDevicePort audioSink : mAudioSink) {
            AudioPortConfig sinkConfig = audioSink.activeConfig();
            int sinkSamplingRate = mDesiredSamplingRate;
            int sinkChannelMask = mDesiredChannelMask;
            int sinkFormat = mDesiredFormat;
            // If sinkConfig != null and values are set to default,
            // fill in the sinkConfig values.
            if (sinkConfig != null) {
                if (sinkSamplingRate == 0) {
                    sinkSamplingRate = sinkConfig.samplingRate();
                }
                if (sinkChannelMask == AudioFormat.CHANNEL_OUT_DEFAULT) {
                    sinkChannelMask = sinkConfig.channelMask();
                }
                if (sinkFormat == AudioFormat.ENCODING_DEFAULT) {
                    sinkFormat = sinkConfig.format();
                }
            }

            if (sinkConfig == null
                    || sinkConfig.samplingRate() != sinkSamplingRate
                    || sinkConfig.channelMask() != sinkChannelMask
                    || sinkConfig.format() != sinkFormat) {
                // Check for compatibility and reset to default if necessary.
                if (!intArrayContains(audioSink.samplingRates(), sinkSamplingRate)
                        && audioSink.samplingRates().length > 0) {
                    sinkSamplingRate = audioSink.samplingRates()[0];
                }
                if (!intArrayContains(audioSink.channelMasks(), sinkChannelMask)) {
                    sinkChannelMask = AudioFormat.CHANNEL_OUT_DEFAULT;
                }
                if (!intArrayContains(audioSink.formats(), sinkFormat)) {
                    sinkFormat = AudioFormat.ENCODING_DEFAULT;
                }
                sinkConfig = audioSink.buildConfig(sinkSamplingRate, sinkChannelMask,
                        sinkFormat, null);
                shouldRecreateAudioPatch = true;
            }
            sinkConfigs.add(sinkConfig);
        }

        // Set source gain according to media volume
        // We apply gain on the source but use volume curve corresponding to the sink to match
        // what is done for software source in audio policy manager
//        updateVolume();
//        float volume = mSourceVolume * getMediaStreamVolume();
//        AudioGainConfig sourceGainConfig = null;
//        if (mAudioSource.gains().length > 0 && volume != mCommittedSourceVolume) {
//            AudioGain sourceGain = null;
//            for (AudioGain gain : mAudioSource.gains()) {
//                if ((gain.mode() & AudioGain.MODE_JOINT) != 0) {
//                    sourceGain = gain;
//                    break;
//                }
//            }
//            // NOTE: we only change the source gain in MODE_JOINT here.
//            if (sourceGain != null) {
//                int steps = (sourceGain.maxValue() - sourceGain.minValue())
//                        / sourceGain.stepValue();
//                int gainValue = sourceGain.minValue();
//                if (volume < 1.0f) {
//                    gainValue += sourceGain.stepValue() * (int) (volume * steps + 0.5);
//                } else {
//                    gainValue = sourceGain.maxValue();
//                }
//                // size of gain values is 1 in MODE_JOINT
//                int[] gainValues = new int[] { gainValue };
//                sourceGainConfig = sourceGain.buildConfig(AudioGain.MODE_JOINT,
//                        sourceGain.channelMask(), gainValues, 0);
//            } else {
//                Slog.w(TAG, "No audio source gain with MODE_JOINT support exists.");
//            }
//        }
        updateVolume();

        AudioGainConfig sourceGainConfig = null;
        if (mAudioSource.gains().length > 0) {
            AudioGain sourceGain = null;
            for (AudioGain gain : mAudioSource.gains()) {
                if ((gain.mode() & AudioGain.MODE_JOINT) != 0) {
                    sourceGain = gain;
                    break;
                }
            }
            if (sourceGain != null && ((mSourceVolume != mCommittedSourceVolume) ||
                                       (mCurrentIndex != mCommitedIndex))) {
                // use first sink device as referrence for volume curves
                int deviceType = mAudioSink.get(0).type();

                // first convert source volume to mBs
                int sourceIndex = volumeToMediaIndex(mSourceVolume);
                int sourceGainMb = indexToGainMbForDevice(sourceIndex, deviceType, sourceGain);

                // then convert media volume index to mBs
                int indexGainMb = indexToGainMbForDevice(mCurrentIndex, deviceType, sourceGain);

                Log.d(TAG, "updateAudioConfigLocked mCurrentIndex= "+ mCurrentIndex + ",mCommitedIndex="+mCommitedIndex+",indexGainMb="+indexGainMb);

                // apply combined gains
                int gainValueMb = sourceGainMb + indexGainMb;
                gainValueMb = Math.max(sourceGain.minValue(),
                                       Math.min(sourceGain.maxValue(), gainValueMb));

                // NOTE: we only change the source gain in MODE_JOINT here.
                // size of gain values is 1 in MODE_JOINT
                int[] gainValues = new int[] { gainValueMb };
                sourceGainConfig = sourceGain.buildConfig(AudioGain.MODE_JOINT,
                        sourceGain.channelMask(), gainValues, 0);
            } else {
                Slog.w(TAG, "updateAudioConfigLocked No audio source gain with MODE_JOINT support exists.");
            }
        }

        // sinkConfigs.size() == mAudioSink.size(), and mAudioSink is guaranteed to be
        // non-empty at the beginning of this method.
        AudioPortConfig sinkConfig = sinkConfigs.get(0);
        if (sourceConfig == null || sourceGainConfig != null) {
            int sourceSamplingRate = 0;
            if (intArrayContains(mAudioSource.samplingRates(), sinkConfig.samplingRate())) {
                sourceSamplingRate = sinkConfig.samplingRate();
            } else if (mAudioSource.samplingRates().length > 0) {
                // Use any sampling rate and hope audio patch can handle resampling...
                sourceSamplingRate = mAudioSource.samplingRates()[0];
            }
            int sourceChannelMask = AudioFormat.CHANNEL_IN_DEFAULT;
            for (int inChannelMask : mAudioSource.channelMasks()) {
                if (AudioFormat.channelCountFromOutChannelMask(sinkConfig.channelMask())
                        == AudioFormat.channelCountFromInChannelMask(inChannelMask)) {
                    sourceChannelMask = inChannelMask;
                    break;
                }
            }
            int sourceFormat = AudioFormat.ENCODING_DEFAULT;
            if (intArrayContains(mAudioSource.formats(), sinkConfig.format())) {
                sourceFormat = sinkConfig.format();
            }
            sourceConfig = mAudioSource.buildConfig(sourceSamplingRate, sourceChannelMask,
                    sourceFormat, sourceGainConfig);

            if (mAudioPatch != null) {
                shouldApplyGain = true;
            } else {
                shouldRecreateAudioPatch = true;
            }
        }
        Log.i(TAG, "updateAudioConfigLocked recreatePatch:" + shouldRecreateAudioPatch);
        if (shouldRecreateAudioPatch) {
            //mCommittedSourceVolume = volume;
            if (mAudioPatch != null) {
                mAudioManager.releaseAudioPatch(mAudioPatch);
                audioPatchArray[0] = null;
                mHasStartedDecoder = false;
            }
            mAudioManager.createAudioPatch(
                    audioPatchArray,
                    new AudioPortConfig[] { sourceConfig },
                    sinkConfigs.toArray(new AudioPortConfig[sinkConfigs.size()]));
            mAudioPatch = audioPatchArray[0];
            Log.d(TAG,"createAudioPatch end" + mAudioPatch);
            if (sourceGainConfig != null) {
                mCommitedIndex = mCurrentIndex;
                mCommittedSourceVolume = mSourceVolume;
            }
        }
        if (sourceGainConfig != null &&
                (shouldApplyGain || shouldRecreateAudioPatch)) {
            //mCommittedSourceVolume = volume;
            mAudioManager.setAudioPortGain(mAudioSource, sourceGainConfig);
            mCommitedIndex = mCurrentIndex;
            mCommittedSourceVolume = mSourceVolume;
        }
    }

    private static boolean intArrayContains(int[] array, int value) {
        for (int element : array) {
            if (element == value) return true;
        }
        return false;
    }
}
