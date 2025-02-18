/*
 * Copyright (c) 2014 Amlogic, Inc. All rights reserved.
 *
 * This source code is subject to the terms and conditions defined in the
 * file 'LICENSE' which is part of this source code package.
 *
 * Description: JAVA file
 */

package com.droidlogic.tvinput.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ContentUris;
import android.content.pm.ResolveInfo;
import android.media.PlaybackParams;
import android.media.AudioManager;
import android.media.tv.TvContentRating;
import android.media.tv.TvInputManager;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputHardwareInfo;
import android.media.tv.TvTrackInfo;
import android.media.tv.TvContract;
import android.media.tv.TvStreamConfig;
import android.media.tv.TvInputManager.Hardware;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.accessibility.CaptioningManager;
import android.view.accessibility.CaptioningManager.CaptionStyle;
import android.view.accessibility.CaptioningManager.CaptioningChangeListener;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.Rect;
import android.widget.Toast;
import java.lang.reflect.Method;
import java.lang.reflect.Field;

import com.droidlogic.app.AudioSystemCmdManager;
import com.droidlogic.tvinput.Utils;

import com.droidlogic.app.tv.TvDataBaseManager;
import com.droidlogic.app.tv.TvChannelParams;
import com.droidlogic.app.tv.DroidLogicTvUtils;
import com.droidlogic.app.tv.ChannelInfo;
import com.droidlogic.app.tv.Program;
import com.droidlogic.app.tv.TvMultilingualText;
import com.droidlogic.app.tv.TvTime;
import com.droidlogic.app.tv.TvStoreManager;
import com.droidlogic.app.tv.TvInSignalInfo;
import com.droidlogic.app.tv.DroidContentRatingsParser;
import com.droidlogic.app.tv.EasEvent;
import com.droidlogic.app.SystemControlManager;
import com.droidlogic.app.DataProviderManager;
import com.droidlogic.tvinput.widget.DTVSubtitleView;

import com.droidlogic.tvinput.R;
import com.droidlogic.tvinput.customer.CustomerOps;

import java.nio.channels.Channel;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import com.droidlogic.app.tv.TvControlManager;
import com.droidlogic.app.tv.TvControlManager.RrtSearchInfo;

import android.net.Uri;
import android.view.Surface;
import android.os.SystemProperties;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import java.lang.Long;


//tmp for RecorderSession
import android.os.Looper;
import android.content.ContentResolver;
import android.database.Cursor;
import android.media.tv.TvInputService;
import java.io.File;
import java.io.IOException;

import com.droidlogic.tvinput.database.Rrt5DataBaseManager;
import com.droidlogic.app.tv.RrtEvent;


public class DTVInputService extends DroidLogicTvInputService implements TvControlManager.EasEventListener {

    private static final String TAG = "DTVInputService";
    private static final boolean DEBUG = Log.isLoggable("DroidInputService", Log.DEBUG);
    protected static final String DTV_AUTO_RESCAN_SERVICE = "vendor.tv.dtv.auto_rescan_service";
    protected static final String DTV_AUTO_RESCAN_TIME = "vendor.tv.dtv.auto_rescan_time";
    protected static final String DTV_AUDIO_AD_DISABLE = "vendor.tv.dtv.ad.disable";
    protected static final String DTV_CHANNEL_NUMBER_START = "vendor.tv.channel.number.start";

    protected static final String DTV_TSPLAYER_ENABLE = "vendor.tv.dtv.tsplayer.enable";

    protected static final String DTV_AUDIO_TRACK_IDX = "vendor.tv.dtv.audio_track_idx";
    protected static final String DTV_AUDIO_AD_TRACK_IDX = "vendor.tv.dtv.audio_ad_track_idx";
    protected static final String DTV_SUBTITLE_TRACK_IDX = "vendor.tv.dtv.subtitle_track_idx";
    protected static final String DTV_AUDIO_TRACK_ID = "vendor.tv.dtv.audio_track_id";

    protected static final String DTV_SUBTITLE_AUTO_START = "vendor.tv.dtv.subtitle.autostart";
    protected static final String DTV_SUBTITLE_DTV_XDS = "vendor.tv.dtv.subtitle.xds";
    protected static final String DTV_SUBTITLE_TIF_COMPATIABLE = "vendor.tv.dtv.subtitle.tif";

    protected static final String DTV_SUBTITLE_CS_PREFER = "vendor.tv.persist.sys.cs.prefer";
    protected static final String DTV_SUBTITLE_CC_PREFER = "vendor.tv.persist.sys.cc.prefer";

    protected static final String DTV_SUBTITLE_CAPTION_EXIST = "vendor.tv.dtv.caption.exist";

    protected static final String DTV_TYPE_DEFAULT = "vendor.tv.dtv.type.default";
    protected static final String DTV_STANDARD_FORCE = "vendor.tv.dtv.standard.force";
    protected static final String DTV_MONITOR_MODE_FORCE = "vendor.tv.dtv.monitor.mode.force";
    protected static final String ACTION_AD_VOLUME_LEVEL = "android.intent.action.ad_volume_level";

    protected static final String KEY_TVINPUTINFO_AUDIO_AD = "audio_ad";
    protected static final String EVENT_SIGNAL_INFO = "event_signal_info";
    protected static final String KEY_SIGNAL_STRENGTH = "signal_strength";

    protected static final int KEY_RED = 183;
    protected static final int KEY_GREEN = 184;
    protected static final int KEY_YELLOW = 185;
    protected static final int KEY_BLUE = 186;
    protected static final int KEY_MIX = 85;
    protected static final int KEY_NEXT_PAGE = 166;
    protected static final int KEY_PRIOR_PAGE = 167;
    protected static final int KEY_CLOCK = 89;
    protected static final int KEY_GO_HOME = 90;
    protected static final int KEY_ZOOM = 88;
    protected static final int KEY_SUBPG = 86;
    protected static final int KEY_REGION = 87;
    protected static final int KEY_LOCK_SUBPG = 251;
    protected static final int KEY_TELETEXT_SWITCH = 169;
    protected static final int KEY_SUB_PAGE = 206;
    protected static final int KEY_REVEAL = 246;
    protected static final int KEY_CANCEL = 247;
    protected static final int KEY_SUBTITLE = 175;
    protected static final int KEY_UP = 19;
    protected static final int KEY_DOWN = 20;
    protected static final int KEY_LEFT = 21;
    protected static final int KEY_RIGHT = 22;

    protected static final int DTV_COLOR_WHITE = 1;
    protected static final int DTV_COLOR_BLACK = 2;
    protected static final int DTV_COLOR_RED = 3;
    protected static final int DTV_COLOR_GREEN = 4;
    protected static final int DTV_COLOR_BLUE = 5;
    protected static final int DTV_COLOR_YELLOW = 6;
    protected static final int DTV_COLOR_MAGENTA = 7;
    protected static final int DTV_COLOR_CYAN = 8;

    protected static final int DTV_OPACITY_TRANSPARENT = 1;
    protected static final int DTV_OPACITY_TRANSLUCENT = 2;
    protected static final int DTV_OPACITY_SOLID = 3;

    protected static final int DTV_CC_STYLE_WHITE_ON_BLACK = 0;
    protected static final int DTV_CC_STYLE_BLACK_ON_WHITE = 1;
    protected static final int DTV_CC_STYLE_YELLOW_ON_BLACK = 2;
    protected static final int DTV_CC_STYLE_YELLOW_ON_BLUE = 3;
    protected static final int DTV_CC_STYLE_USE_DEFAULT = 4;
    protected static final int DTV_CC_STYLE_USE_CUSTOM = -1;

    private static final int DECODE_ID_TYPE = 0;
    private static final int SYNC_ID_TYPE = 1;

    public static final String MAX_CACHE_SIZE_KEY = "tv.dtv.tf.max.size";
    public static final int MAX_CACHE_SIZE_DEF = 2 * 1024;  // 2GB
    public static final int MIN_CACHE_SIZE_DEF = 256;  // 256MB
    public static final String PVR_DEFAULT_PATH = "/data/vendor/tvserver";

    private static ChannelInfo.Subtitle pal_teletext_subtitle = null;

    //private static boolean DEBUG = false;
    private EASProcessManager mEASProcessManager;
    private String mEasText = null;
    private boolean pal_teletext = false;
    private boolean isEasTextChannged = false;

    private int player_instance_id = 0;
    private int sync_instance_id = 0;

    protected DTVSessionImpl mCurrentSession;
    protected int id = 0;
    protected TvControlManager mTvControlManager;
    private AudioSystemCmdManager mAudioSystemCmdManager;
    protected static boolean isTvPlaying = false;
    protected TvTime mTvTime = null;
    private boolean mHasPreferLanguageFeature = false;

    public boolean is_subtitle_enabled;
    protected boolean mIsChannelScrambled = false;
    // cur channnel ratings
    private TvContentRating[] mCurChannelRatings = null;
    private Uri mCurrentChannelUri = null;
    protected final Set<TvContentRating> mUnblockedRatingSet = new HashSet<>();

    protected Map<Integer, DTVSessionImpl> sessionMap = new HashMap<>();
    protected final BroadcastReceiver mChannelScanStartReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DEBUG) Log.d(TAG, "-----onReceive:"+action);
            if (mCurrentSession != null) {
                mCurrentSession.notifyUpdateUnblockRatingSet();
                //mCurrentSession.doRelease();
            }
        }
    };

    protected final BroadcastReceiver mParentalControlsBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mCurrentSession != null) {
                String action = intent.getAction();
                if (action.equals(TvInputManager.ACTION_BLOCKED_RATINGS_CHANGED)
                    || action.equals(TvInputManager.ACTION_PARENTAL_CONTROLS_ENABLED_CHANGED)) {
                    if (DEBUG) Log.d(TAG, "BLOCKED_RATINGS_CHANGED");
                    mCurrentSession.checkIsNeedClearUnblockRating();
                    mCurrentSession.checkCurrentContentBlockNeeded();
                } else if (action.equals(Intent.ACTION_TIME_CHANGED)) {
                    if (DEBUG) Log.d(TAG, "SysTime changed.");
                    mCurrentSession.restartMonitorTime();
                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        initInputService(DroidLogicTvUtils.DEVICE_ID_DTV, DTVInputService.class.getName());

        registerChannelScanStartReceiver();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TvInputManager.ACTION_BLOCKED_RATINGS_CHANGED);
        intentFilter
                .addAction(TvInputManager.ACTION_PARENTAL_CONTROLS_ENABLED_CHANGED);
        intentFilter.addAction(Intent.ACTION_TIME_CHANGED);
        registerReceiver(mParentalControlsBroadcastReceiver, intentFilter,
            2/*Context.RECEIVER_EXPORTED*/ | Context.RECEIVER_VISIBLE_TO_INSTANT_APPS);

        IntentFilter filter= new IntentFilter();
        filter.addAction(DroidLogicTvUtils.ACTION_DTV_AUTO_SCAN);
        filter.addAction(DroidLogicTvUtils.ACTION_DTV_MANUAL_SCAN);
        registerReceiver(mChannelScanStartReceiver, filter,
            2/*Context.RECEIVER_EXPORTED*/ | Context.RECEIVER_VISIBLE_TO_INSTANT_APPS);

        mTvControlManager = TvControlManager.getInstance();
        if (DEBUG) Log.d(TAG,"oncreate:Set EAS listener as TvInput");
        mAudioSystemCmdManager = AudioSystemCmdManager.getInstance(this);
        mEASProcessManager = new EASProcessManager(this);
        mTvControlManager.setEasListener(this);
        mTvTime = new TvTime(this);

        //language settings, default disable
        String value =  mTvControlManager.TvMiscConfigGet("dtv.prefer.language.en", "0");
        int ret = 0;
        try {
            ret = Integer.valueOf(value);
        } catch (Exception ignore) {
        }
        mHasPreferLanguageFeature = (ret == 1);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mParentalControlsBroadcastReceiver);
        unregisterReceiver(mChannelScanStartReceiver);
        unRegisterChannelScanStartReceiver();
    }

    @Override
    public Session onCreateSession(String inputId) {
        super.onCreateSession(inputId);

        mCurrentSession = new DTVSessionImpl(this, inputId, getHardwareDeviceId(inputId));
        mCurrentSession.setSessionId(id);
        registerInputSession(mCurrentSession);
        sessionMap.put(id, mCurrentSession);
        id++;

        return mCurrentSession;
    }

    @Override
    public RecordingSession onCreateRecordingSession(String inputId) {
        return new DTVRecordingSession(this, inputId);
    }

    @Override
    public void tvPlayStopped(int sessionId) {
        DTVSessionImpl session = sessionMap.get(sessionId);
        if (session != null) {
            session.stopSubtitle();
            //releasePlayer();
            session.setMonitor(null);
            session.updateEasState();
        }
    }

    @Override
    public void setCurrentSessionById(int sessionId) {
        Utils.logd(TAG, "setCurrentSessionById:"+sessionId);
        DTVSessionImpl session = sessionMap.get(sessionId);
        if (session != null) {
            mCurrentSession = session;
        }
    }

    @Override
    public void doTuneFinish(int result, Uri uri, int sessionId) {
        if (DEBUG) Log.d(TAG, "doTuneFinish,result:"+result+"sessionId:"+sessionId);
        if (result == ACTION_SUCCESS) {
            DTVSessionImpl session = sessionMap.get(sessionId);
            if (session != null)
                session.switchToSourceInput(uri);
        }
    }

    @Override
    public void onSigChanged(TvInSignalInfo signal_info) {
        if (mTvControlManager.GetCurrentSourceInput() == DroidLogicTvUtils.DEVICE_ID_DTV
            || mTvControlManager.GetCurrentVirtualSourceInput() == DroidLogicTvUtils.DEVICE_ID_ADTV) {

            TvInSignalInfo.SignalStatus status = signal_info.sigStatus;

            if (DEBUG) Log.d(TAG, "onSigChanged status: id["+status.ordinal()+"]["+status.toString()+"]");

            //if a/v runs well, notify
            if (DEBUG) Log.d(TAG, "video fps:"+mTvControlManager.DtvGetVideoFormatInfo().fps);
            if (status == TvInSignalInfo.SignalStatus.TVIN_SIG_STATUS_STABLE
                    && mCurrentSession != null
                    && mCurrentSession.mCurrentChannel != null
                    && !mCurrentSession.mCurrentChannel.isAnalogChannel()) {

                if (mTvControlManager.DtvGetVideoFormatInfo().fps != 0) {
                    if (DEBUG) Log.d(TAG, "Signal and video look well");
                    mCurrentSession.notifyVideoAvailable();
                } else if (ChannelInfo.isRadioChannel(mCurrentSession.mCurrentChannel)
                        && mCurrentSession.mVideoUnavailableReason != TvInputManager.VIDEO_UNAVAILABLE_REASON_UNKNOWN - 1 /* Video status remains unchanged */) {
                    //for plug/unplug cable quickly case
                    //for radio channel, if display picture, needn't display audio only
                    Log.d(TAG, "current is radio Channel");
                    mCurrentSession.notifyVideoAvailable();
                }


                if (DEBUG)
                    Log.d(TAG, "onSigChange" + status.ordinal() + status.toString());
            }
        }
    }

    @Override
    protected void releasePlayer() {
        mTvControlManager.stopPlay("atsc", null);
    }

    protected void stopTv() {
        mTvControlManager.StopTv();
    }

    public String getCacheStoragePath() {
        File baseDir;
        int maxCacheSizeMb = SystemProperties.getInt(MAX_CACHE_SIZE_KEY, MAX_CACHE_SIZE_DEF);
        if (maxCacheSizeMb >= MIN_CACHE_SIZE_DEF) {
            boolean useExternalStorage = Environment.MEDIA_MOUNTED.equals(
                    Environment.getExternalStorageState()) &&
                    Environment.isExternalStorageRemovable();
            boolean allowToUseInternalStorage = true;
            if (useExternalStorage || allowToUseInternalStorage) {
                baseDir = useExternalStorage ? getExternalCacheDir() : getCacheDir();
                Log.d(TAG, "getCacheStoragePath baseDir: " + baseDir.getAbsolutePath());
                Log.d(TAG, "getCacheStoragePath PVR_DEFAULT_PATH: " + PVR_DEFAULT_PATH);
                return PVR_DEFAULT_PATH;
                //return baseDir.getAbsolutePath();
            }
        }
        return "/storage";
    }

    protected List<ChannelInfo.Subtitle> getChannelSubtitles(ChannelInfo ch) {
        ArrayList<ChannelInfo.Subtitle> SubtitleList = new ArrayList<ChannelInfo.Subtitle>();

        int[] subPids = ch.getSubtitlePids();
        int SubTracksCount = (subPids == null) ? 0 : subPids.length;
        if (SubTracksCount == 0)
            return null;
        String[] subLanguages = ch.getSubtitleLangs();
        int[] subTypes = ch.getSubtitleTypes();
        int[] subStypes = ch.getSubtitleStypes();
        int[] subId1s = ch.getSubtitleId1s();
        int[] subId2s = ch.getSubtitleId2s();

        for (int i = 0; i < SubTracksCount; i++) {
            ChannelInfo.Subtitle s
                = new ChannelInfo.Subtitle(subTypes[i],
                                        subPids[i],
                                        subStypes[i],
                                        subId1s[i],
                                        subId2s[i],
                                        subLanguages[i],
                                        i);
            SubtitleList.add(s);
        }
        return (SubtitleList.size() == 0 ? null : SubtitleList);
    }

    protected List<ChannelInfo.Audio> getChannelAudios(ChannelInfo ch) {
        ArrayList<ChannelInfo.Audio> AudioList = new ArrayList<ChannelInfo.Audio>();
        int[] audioPids = ch.getAudioPids();
        int AudioTracksCount = (audioPids == null) ? 0 : audioPids.length;
        if (AudioTracksCount == 0)
            return null;
        String[] audioLanguages = ch.getAudioLangs();
        int[] audioFormats = ch.getAudioFormats();
        int[] audioExts = ch.getAudioExts();

        for (int i = 0; i < AudioTracksCount; i++) {
            ChannelInfo.Audio a
                = new ChannelInfo.Audio(audioPids[i],
                                        audioFormats[i],
                                        audioExts[i],
                                        audioLanguages[i],
                                        i);
            AudioList.add(a);
        }
        return (AudioList.size() == 0 ? null : AudioList);
    }

    /*set below 3 vars true to enable tracks-auto-select in this service.*/
    protected static boolean subtitleAutoSave = true;
    protected static boolean audioAutoSave = true;
    protected static boolean subtitleAutoStart = true;
    protected static boolean subtitleTifMode = true;

    /*associate audio*/
    protected static boolean audioADAutoStart = false;

    /*only one monitor instance for all sessions*/
    protected static DTVSessionImpl.DTVMonitor mDTVMonitor = null;
    protected final Object mLock = new Object();
    protected final Object mSubtitleLock = new Object();
    protected final Object mEpgUpdateLock = new Object();
    protected final Object mEpgQueueLock = new Object();
    protected final Object mRatingsUpdatelLock = new Object();



    private static class MiniRecordedProgram {
        private long mChannelId;
        private String mDataUri;

        private static final String[] PROJECTION = {
            TvContract.Programs.COLUMN_CHANNEL_ID,
            TvContract.RecordedPrograms.COLUMN_RECORDING_DATA_URI,
        };

        public MiniRecordedProgram(Cursor cursor) {
            int index = 0;
            mChannelId = cursor.getLong(index++);
            mDataUri = cursor.getString(index++);
        }

        public MiniRecordedProgram(long channelId, String dataUri) {
            mChannelId = channelId;
            mDataUri = dataUri;
        }

        public static MiniRecordedProgram onQuery(Cursor c) {
            MiniRecordedProgram recording = null;
            if (c != null && c.moveToNext()) {
                recording = new MiniRecordedProgram(c);
            }
            return recording;
        }

        public String getDataUri() {
            return mDataUri;
        }
    }

    protected boolean enableChannelBlockInServer() {
        String value =  mTvControlManager.TvMiscConfigGet("tv.channel.block.inserver.en", "0");
        if (DEBUG) Log.d(TAG, "get block switch: " + value);
        int ret = 0;
        try {
            ret = Integer.valueOf(value);
        } catch (Exception e) {
        }
        return (ret == 1);
    }

    public class DTVSessionImpl extends TvInputBaseSession
            implements TvControlManager.AVPlaybackListener, DTVSubtitleView.SubtitleDataListener, TvControlManager.PlayerInstanceNoListener, TvControlManager.RrtEventListener{
        protected final Context mContext;
        protected TvInputManager mTvInputManager;
        protected TvDataBaseManager mTvDataBaseManager;
        protected TvContentRating mLastBlockedRating;
        //protected final Set<TvContentRating> mUnblockedRatingSet = new HashSet<>();//declare in dtvinputservice
        protected ChannelInfo mCurrentChannel;
        protected List<ChannelInfo.Subtitle> mCurrentSubtitles;
        protected ChannelInfo.Subtitle mCurrentSubtitle;
        protected List<ChannelInfo.Audio> mCurrentAudios;
        protected SystemControlManager mSystemControlManager;
        protected CaptioningManager mCaptioningManager = null;
        private String mRecordingId = null;
        private final CountDownLatch mReleaseLatch = new CountDownLatch(1);
        protected final static int AD_MIXING_LEVEL_DEF = 50;
        protected final static int AD_VOLUME_DEF = 100;
        protected int mAudioADMixingLevel = -1;
        protected int mAudioADVolume = -1;

        protected String mDtvType = TvContract.Channels.TYPE_DTMB;

        private int mChannelBlocked = -1;
        protected Uri  mCurrentUri;
        private boolean mIsBlocked = false;
        private boolean mIsChannelBlocked = false;
        private boolean mIsPreviousChannelBlocked = false;

        private boolean mUpdateTsFlag = false;

        protected HandlerThread mHandlerThread = null;
        protected Handler mHandler = null;
        protected Handler mMainHandler = null;

        private TvContentRating[] mCurrentPmtContentRatings = null;
        private TvContentRating[] mCurrentCCContentRatings = null;
        private boolean mIsSessionReleasing = false;
        private int mVideoUnavailableReason = TvInputManager.VIDEO_UNAVAILABLE_REASON_UNKNOWN - 1;
        private Rrt5DataBaseManager mRrt5DataBaseManager = null;

        protected DTVSessionImpl(Context context, String inputId, int deviceId) {
            super(context, inputId, deviceId);
            if (DEBUG) Log.d(TAG, "create:" + this);

            mContext = context;
            mTvDataBaseManager = new TvDataBaseManager(mContext);
            mSystemControlManager = SystemControlManager.getInstance();
            mRrt5DataBaseManager = new Rrt5DataBaseManager(mContext);
            //TODO: for fireos, maybe new Rrt5DataBaseManager(mContext, String auth, String table);
            mTvControlManager.setRrtListener(this);
            mLastBlockedRating = null;
            mCurrentChannel = null;
            mCurrentSubtitles = null;
            mCurrentAudios = null;
            mCurrentUri = null;
            initWorkThread();

            initOverlayView(R.layout.layout_overlay);
            if (DEBUG) Log.d(TAG,"init overlay view");
            if (mOverlayView != null) {
                //mOverlayView.setImage(R.drawable.bg_no_signal);
                mOverlayView.setImageVisibility(false);
                mSubtitleView = (DTVSubtitleView)mOverlayView.getSubtitleView();
                mSubtitleView.setSubtitleDataListener(this);
            }
            mCaptioningManager = (CaptioningManager) mContext.getSystemService(Context.CAPTIONING_SERVICE);

            if (getBlockNoRatingEnable()) {
                isBlockNoRatingEnable = true;
            } else {
                isBlockNoRatingEnable = false;
                isUnlockCurrent_NR = false;
            }
            if (DEBUG) Log.d(TAG,"isBlockNoRatingEnable:"+isBlockNoRatingEnable+",isUnlockCurrent_NR:"+isUnlockCurrent_NR);
        }

        private boolean getBlockNoRatingEnable() {
            int status = DataProviderManager.getIntValue(mContext, DroidLogicTvUtils.BLOCK_NORATING, 0) ;
            if (DEBUG) Log.d(TAG,"getBlockNoRatingEnable:"+status);
            return (status == 1) ? true : false;
        }

        @Override
        public boolean onSetSurface(Surface surface) {
            if (surface == null && mSubtitleView != null) {
                mSubtitleView.setVisibility(View.GONE);
            }
            super.onSetSurface(surface);
            return setSurfaceInService(surface,this);
        }


         @Override
        public void onOverlayViewSizeChanged(int width, int height) {
            if (DEBUG) Log.d(TAG, "onOverlayViewSizeChanged: "+width+","+height);
            super.onOverlayViewSizeChanged(width, height);
            if (width < 720 || height < 480) {
                mSubtitleView.setPreviewWindowMode(true);
            } else {
                mSubtitleView.setPreviewWindowMode(false);
            }
        }

        @Override
        public boolean onTune(Uri channelUri) {
            doTuneInService(channelUri, getSessionId());
            return false;
        }

        public void doRelease() {
            if (DEBUG) Log.d(TAG, "doRelease:"+this);
            if (mHandler != null) {
                mIsSessionReleasing = true;
                mHandler.removeCallbacksAndMessages(null);
            }
            if (mMainHandler != null) {
                mMainHandler.removeCallbacksAndMessages(null);
            }
            mHandler.sendEmptyMessage(MSG_RELEASE);
            mTvControlManager.SetRecorderEventListener(null);
            try {
                mReleaseLatch.await();
            } catch (InterruptedException e) {
                Log.e(TAG, "Couldn't wait for finish of MSG_RELEASE");
            } finally {
            }

            super.doRelease();
        }


        public void doFinalRelease() {
            if (getCurrentSessionId() == mId) {
                if (mSubtitleView != null) {
                    mSubtitleView.setSubtitleDataListener(null);
                }
                stopSubtitle();
                mTvControlManager.SetAVPlaybackListener(null);
                mTvControlManager.SetPlayerInstanceNoListener(null);
                //stopTv();
                setMonitor(null);
            }

            isPlayingATVInDTVMode = false;
            mCurrentChannelUri = null;
            releaseWorkThread();
            synchronized(mLock) {
                mCurrentChannel = null;
            }
            if (sessionMap.containsKey(getSessionId())) {
                sessionMap.remove(getSessionId());
            }
            if (mCurrentSession != null && mCurrentSession.getSessionId() == getSessionId()) {
                mCurrentSession = null;
                registerInputSession(null);
            }
        }

        @Override
        public void doAppPrivateCmd(String action, Bundle bundle) {
            if (TextUtils.equals(DroidLogicTvUtils.ACTION_STOP_TV, action)) {
                if (mHardware != null) {
                    mHardware.setSurface(null, null);
                }
            } else if (TextUtils.equals(DroidLogicTvUtils.ACTION_STOP_PLAY, action)) {
                if (DEBUG) Log.d(TAG, "do private cmd: STOP_PLAY");
                stopSubtitle();
                setMonitor(null);
                releasePlayer();
            } else if (TextUtils.equals(DroidLogicTvUtils.ACTION_DTV_AUTO_TRACKS, action)) {
                if (DEBUG) Log.d(TAG, "do private cmd: AUTO_TRACKS");
                subtitleAutoSave = true;
                audioAutoSave = true;
                subtitleAutoStart = true;
                if (mCurrentChannel != null)
                    startSubtitle(mCurrentChannel);
            } /*else if (DroidLogicTvUtils.ACTION_DTV_AUTO_SCAN.equals(action)
                || DroidLogicTvUtils.ACTION_DTV_MANUAL_SCAN.equals(action)) {
                Log.d(TAG, "do private cmd: DTV_XXX_SCAN, stop play...");
                mCurrentUri = null;
                stopSubtitle();
                setMonitor(null);
                //releasePlayer();
                mTvControlManager.PlayDTVProgram(
                    new TvControlManager.TvMode(mDtvType).getMode(), 470000000, 0, 0, 0, 0, -1, -1, 0, 0, false);
                resetScanStoreListener();
            }  */else if (TextUtils.equals(DroidLogicTvUtils.ACTION_DTV_SET_TYPE, action)) {
                mDtvType = bundle.getString(DroidLogicTvUtils.PARA_TYPE);
                if (DEBUG) Log.d(TAG, "do private cmd: DTV_SET_TYPE ["+mDtvType+"]");
            } else if (TextUtils.equals(DroidLogicTvUtils.ACTION_DTV_ENABLE_AUDIO_AD, action)) {
                //don't support to add play thread realtime, add ad by select ad track if supported
                boolean enable = (bundle.getInt(DroidLogicTvUtils.PARA_ENABLE) == 0)? false : true;
                if (DEBUG) Log.d(TAG, "do private cmd: ACTION_DTV_ENABLE_AUDIO_AD ["+enable+"]");
                /*audioADAutoStart = (bundle.getInt(DroidLogicTvUtils.PARA_ENABLE) == 0)? false : true;
                int adTrackIndex = bundle.getInt(DroidLogicTvUtils.PARA_VALUE1);
                Log.d(TAG, "do private cmd: ACTION_DTV_ENABLE_AUDIO_AD enable["+audioADAutoStart+"] track["+adTrackIndex+"]");
                if (TextUtils.equals(mSystemControlManager.getProperty(DTV_AUDIO_AD_DISABLE),"1")) {
                    audioADAutoStart = false;
                    return;
                }
                if (mCurrentChannel != null) {
                    if (audioADAutoStart) {
                        if (adTrackIndex == -1)
                            startAudioADByMain(mCurrentChannel, getAudioAuto(mCurrentChannel));
                        else
                            startAudioAD(mCurrentChannel, adTrackIndex);
                        return;
                    }
                }
                stopAudioAD();*/
            } else if (TextUtils.equals(DroidLogicTvUtils.ACTION_AD_MIXING_LEVEL, action)) {
                mAudioADMixingLevel = bundle.getInt(DroidLogicTvUtils.PARA_VALUE1);
                mHandler.obtainMessage(MSG_MIX_AD_LEVEL, mAudioADMixingLevel, 0).sendToTarget();
                if (DEBUG) Log.d(TAG, "do private cmd: ACTION_AD_MIXING_LEVEL ["+mAudioADMixingLevel+"]");
            } else if (DroidLogicTvUtils.ACTION_BLOCK_NORATING.equals(action)) {
                if (DEBUG) Log.d(TAG, "do private cmd: ACTION_BLOCK_NORATING:"+ bundle.getInt(DroidLogicTvUtils.PARAM_NORATING_ENABLE));
                if (DroidLogicTvUtils.NORATING_OFF == bundle.getInt(DroidLogicTvUtils.PARAM_NORATING_ENABLE)) {
                    isBlockNoRatingEnable = false;
                    isUnlockCurrent_NR = false;
                } else if (DroidLogicTvUtils.NORATING_ON == bundle.getInt(DroidLogicTvUtils.PARAM_NORATING_ENABLE))
                    isBlockNoRatingEnable = true;
                else if (DroidLogicTvUtils.NORATING_UNLOCK_CURRENT == bundle.getInt(DroidLogicTvUtils.PARAM_NORATING_ENABLE))
                    isUnlockCurrent_NR = true;
                checkCurrentContentBlockNeeded();
            } else if (DroidLogicTvUtils.ACTION_BLOCK_CHANNEL.equals(action)) {
                if (bundle.containsKey("is_locked") && bundle.containsKey("channel_id")) {
                    boolean lock = bundle.getBoolean("is_locked", false);
                    long channelId = bundle.getLong("channel_id");
                    mIsPreviousChannelBlocked = mIsChannelBlocked;
                    doChannelBlockToServer(lock, channelId);
                } else {
                    if (DEBUG) Log.d(TAG, "do private cmd: ACTION_BLOCK_CHANNEL:"+ bundle.getBoolean(DroidLogicTvUtils.PARAM_CHANNEL_BLOCK_ENABLE, false));
                    mIsPreviousChannelBlocked = mIsChannelBlocked;
                    if (bundle.getBoolean(DroidLogicTvUtils.PARAM_CHANNEL_BLOCK_ENABLE, false)) {
                        mIsChannelBlocked = true;
                    } else {
                        mIsChannelBlocked = false;
                    }
                }
            }else if ("action_teletext_start".equals(action)) {
                boolean start = bundle.getBoolean("action_teletext_start", false);
                if (DEBUG) Log.d(TAG, "do private cmd: action_teletext_start: "+ start);
            } else if ("action_teletext_up".equals(action)) {
                boolean actionup = bundle.getBoolean("action_teletext_up", false);
                if (DEBUG) Log.d(TAG, "do private cmd: action_teletext_up: "+ actionup);
                if (mSubtitleView != null) {
                    mSubtitleView.previousPage();
                }
            } else if ("action_teletext_down".equals(action)) {
                boolean actiondown = bundle.getBoolean("action_teletext_down", false);
                if (DEBUG) Log.d(TAG, "do private cmd: action_teletext_down: "+ actiondown);
                if (mSubtitleView != null) {
                    mSubtitleView.nextPage();
                }
            } else if ("action_teletext_number".equals(action)) {
                int number = bundle.getInt("action_teletext_number", -1);
                if (DEBUG) Log.d(TAG, "do private cmd: action_teletext_number: "+ number);
                if (mSubtitleView != null) {
                    mSubtitleView.gotoPage(number);
                }

            } else if ("action_teletext_country".equals(action)) {
                int number = bundle.getInt("action_teletext_country", -1);
                if (DEBUG) Log.d(TAG, "do private cmd: action_teletext_country: "+ number);
                if (mSubtitleView != null) {
                    mSubtitleView.setTTRegion(number);
                }
            } else if (DroidLogicTvUtils.ACTION_TIF_BEFORE_TUNE.equals(action)) {
                boolean status = bundle.getBoolean(DroidLogicTvUtils.ACTION_TIF_BEFORE_TUNE, false);
                if (DEBUG) Log.d(TAG, "do private cmd:"+ DroidLogicTvUtils.ACTION_TIF_BEFORE_TUNE + ", status:" + status);
                setTuningScreen(true);
            } else if (DroidLogicTvUtils.ACTION_TIF_AFTER_TUNE.equals(action)) {
                boolean status = bundle.getBoolean(DroidLogicTvUtils.ACTION_TIF_AFTER_TUNE, false);
                if (DEBUG) Log.d(TAG, "do private cmd:"+ DroidLogicTvUtils.ACTION_TIF_AFTER_TUNE + ", status:" + status);
            } else if ("unblockContent".equals(action)) {
                if (!enableChannelBlockInServer()) {
                    mTvControlManager.request("ADTV.UnblockCurrentChannel", "");
                    return;
                }
                mTvControlManager.request("ADTV.unblockContent", "");
                mIsChannelBlocked = false;
            }
        }

        @Override
        public void doUnblockContent(TvContentRating rating) {
            super.doUnblockContent(rating);
            if (rating != null) {
                unblockContent(rating);
            }
        }

        public static final int MSG_PARENTAL_CONTROL = 1;
        public static final int MSG_PLAY = 2;
        public static final int MSG_RELEASE = 3;
        public static final int MSG_TIMESHIFT_PAUSE = 5;
        public static final int MSG_TIMESHIFT_RESUME = 6;
        public static final int MSG_TIMESHIFT_SEEK_TO = 7;
        public static final int MSG_TIMESHIFT_SET_PLAYBACKPARAMS = 8;
        public static final int MSG_REC_PLAY = 9;
        public static final int MSG_TIMESHIFT_AVAILABLE = 10;
        public static final int MSG_UPDATETS_PLAY = 11;
        public static final int MSG_CC_DATA = 12;
        public static final int MSG_CC_TRY_PREFERRED = 14;
        public static final int MSG_UPDATE_UNBLOCK_SET = 15;

        public static final int MSG_MIX_AD_MAIN = 20;
        public static final int MSG_MIX_AD_LEVEL = 21;
        public static final int MSG_MIX_AD_SET_VOLUME = 22;

        public static final int MSG_UPDATE_VIDEO_RESOLUTION = 23;
        public static final int MSG_RRT5_EVENT = 24;
        public static final int MSG_START_AUDIO_AD_MAIN_MIX = 25;
        public static final int MSG_GET_SIGNAL_STRENGTH = 26;
        public static final int MSG_GET_SIGNAL_STRENGTH_PERIOD = 1000;//MS

        protected void initWorkThread() {
            if (DEBUG) Log.d(TAG, "initWorkThread");
            if (mHandlerThread == null) {
                mHandlerThread = new HandlerThread("DtvInputWorker");
                mHandlerThread.start();
                mHandler = new Handler(mHandlerThread.getLooper(), new Handler.Callback() {
                    @Override
                    public boolean handleMessage(Message msg) {
                            if (DEBUG) Log.d(TAG, "handleMessage:"+msg.what);
                            if (mIsSessionReleasing && msg != null && msg.what != MSG_RELEASE) {
                                if (DEBUG) Log.d(TAG, "discard this message because session is releasing:"+ msg.what);
                                return true;
                            }
                            switch (msg.what) {
                                case MSG_RELEASE:
                                    doFinalRelease();
                                    mReleaseLatch.countDown();
                                    break;
                                case MSG_PLAY:
                                    doPlay((Uri)msg.obj);
                                    break;
                                case MSG_PARENTAL_CONTROL:
                                    checkContentBlockNeeded(mCurrentChannel);
                                    break;
                                case MSG_CC_DATA:
                                    doCCData(msg.arg1);
                                    break;
                                case MSG_CC_TRY_PREFERRED:
                                    tryPreferredSubtitleContinue(msg.arg1);
                                    break;
                                case MSG_TIMESHIFT_PAUSE:
                                    doTimeShiftPause();
                                    break;
                                case MSG_TIMESHIFT_RESUME:
                                    doTimeShiftResume();
                                    break;
                                case MSG_TIMESHIFT_SEEK_TO:
                                    doTimeShiftSeekTo((long) msg.obj);
                                    break;
                                case MSG_TIMESHIFT_SET_PLAYBACKPARAMS:
                                    doTimeShiftSetPlaybackParams((PlaybackParams) msg.obj);
                                    break;
                                case MSG_REC_PLAY:
                                    doRecPlay((Uri)msg.obj);
                                    break;
                                case MSG_TIMESHIFT_AVAILABLE:
                                    notifyTimeShiftStatusChanged(TvInputManager.TIME_SHIFT_STATUS_AVAILABLE);
                                    if (DEBUG) Log.d(TAG, "[timeshift] status available");
                                    break;
                                case MSG_UPDATETS_PLAY:
                                    Uri channelUriP = TvContract.buildChannelUri(msg.arg1);
                                    if (DEBUG) Log.d(TAG, "initWorkThread channelUriT:" + channelUriP + "msg.arg1:" + msg.arg1);
                                    mUpdateTsFlag = true;
                                    switchToSourceInput(channelUriP);
                                    break;
                                case MSG_UPDATE_UNBLOCK_SET:
                                    if (DEBUG) Log.d(TAG, "receive MSG_UPDATE_UNBLOCK_SET");
                                    checkIsNeedClearUnblockRating();
                                    break;
                                case MSG_MIX_AD_MAIN:
                                    if (DEBUG) Log.d(TAG, "receive MSG_MIX_AD_MAIN arg1 = " + msg.arg1);
                                    if (msg.arg1 > 0) {
                                        //handleAdtvAudioEvent(AudioSystemCmdManager.AUDIO_SERVICE_CMD_AD_MIX_SUPPORT, 1, 0);
                                        handleAdtvAudioEvent(AudioSystemCmdManager.AUDIO_SERVICE_CMD_AD_MIX_LEVEL, 0, mAudioADMixingLevel);
                                        handleAdtvAudioEvent(AudioSystemCmdManager.AUDIO_SERVICE_CMD_AD_SET_VOLUME, mAudioADVolume, 0);
                                    } else {
                                        //handleAdtvAudioEvent(AudioSystemCmdManager.AUDIO_SERVICE_CMD_AD_MIX_SUPPORT, 0, 0);
                                    }
                                    break;
                                case MSG_MIX_AD_LEVEL:
                                    if (DEBUG) Log.d(TAG, "receive MSG_MIX_AD_LEVEL arg1 = " + msg.arg1);
                                    handleAdtvAudioEvent(AudioSystemCmdManager.AUDIO_SERVICE_CMD_AD_MIX_LEVEL, 0, msg.arg1);
                                    break;
                                case MSG_MIX_AD_SET_VOLUME:
                                    Log.i(TAG,"receive MSG_MIX_AD_SET_VOLUME = " + msg.arg1);
                                    handleAdtvAudioEvent(AudioSystemCmdManager.AUDIO_SERVICE_CMD_AD_SET_VOLUME, msg.arg1, 0);
                                    break;
                                case MSG_UPDATE_VIDEO_RESOLUTION:
                                    //Log.d(TAG,"receive MSG_UPDATE_VIDEO_RESOLUTION");
                                    String new_frame_count =  mSystemControlManager.readSysFs("/sys/module/aml_media/parameters/new_frame_count");
                                    if (TextUtils.isEmpty(new_frame_count) || Integer.parseInt(new_frame_count) == 0) {
                                        mHandler.sendEmptyMessageDelayed(MSG_UPDATE_VIDEO_RESOLUTION, 200);
                                        break;
                                    }
                                    String height = mSystemControlManager.readSysFs("/sys/class/video/frame_height");
                                    String pi = mSystemControlManager.readSysFs("/sys/class/video/frame_original_format");
                                    String format = DroidLogicTvUtils.convertVideoFormat(height, pi);
                                    Log.i(TAG,"height : " + height + " pi " + pi + " currentChannel format : " + mCurrentChannel.getVideoFormat());
                                    if (mCurrentChannel != null) {
                                        if (!TextUtils.equals(format, mCurrentChannel.getVideoFormat())) {
                                            mCurrentChannel.setVideoFormat(format);
                                            mTvDataBaseManager.updateChannelInfo(mCurrentChannel);
                                        }
                                    }
                                    break;
                                case MSG_RRT5_EVENT:
                                    RrtEvent rrtEvent = (RrtEvent)msg.obj;
                                    Log.i(TAG, "rrt5 event: " + (rrtEvent == null ? null :
                                            ("ratingRegion = " + rrtEvent.ratingRegion +
                                             " versionNumber = " + rrtEvent.versionNumber +
                                             " ratingRegionName = " + rrtEvent.ratingRegionName +
                                             " dimensionDefined = " + rrtEvent.dimensionDefined +
                                             " dimensionName = " + rrtEvent.dimensionName +
                                             " valuesDefined = " + rrtEvent.valuesDefined +
                                             " abbrevRatingValue = " + rrtEvent.abbrevRatingValue +
                                             " graduatedScale = " + rrtEvent.graduatedScale)));
                                    if (mCurrentChannel != null) {
                                        mRrt5DataBaseManager.SynchronizedUpdateRrt(
                                            mCurrentChannel.getMajorChannelNumber(), rrtEvent);
                                    }
                                    break;
                                case MSG_START_AUDIO_AD_MAIN_MIX:
                                    startAudioADMainMix((ChannelInfo) msg.obj, msg.arg1);
                                    break;
                                case MSG_GET_SIGNAL_STRENGTH:
                                    if (mCurrentChannel == null || !mCurrentChannel.isDigitalChannel()) {
                                        break;
                                    }
                                    int strength = mTvControlManager.DtvGetSignalStrength();
                                    Bundle signalBundle = new Bundle();
                                    signalBundle.putInt(KEY_SIGNAL_STRENGTH, strength);
                                    notifySessionEvent(EVENT_SIGNAL_INFO, signalBundle);
                                    if (mHandler != null) {
                                        mHandler.removeMessages(MSG_GET_SIGNAL_STRENGTH);
                                        mHandler.sendEmptyMessageDelayed(MSG_GET_SIGNAL_STRENGTH,
                                                MSG_GET_SIGNAL_STRENGTH_PERIOD);
                                    }
                                    break;
                                default:
                                    break;
                            }
                        //Log.d(TAG, "handleMessage:"+msg.what + " finish");
                        return true;
                    }
                });
                mMainHandler = new Handler();
            }
        }

        protected void releaseWorkThread() {
            if (DEBUG) Log.d(TAG, "releaseWorkThread");
            if (mHandlerThread != null) {
                mHandlerThread.quit();
                mHandler.removeCallbacksAndMessages(null);
                mHandlerThread = null;
                mHandler = null;
                mMainHandler = null;
            }
        }

        protected void checkIsNeedClearUnblockRating() {
            if (mTvInputManager == null)
                mTvInputManager = (TvInputManager)getSystemService(Context.TV_INPUT_SERVICE);

            boolean isParentControlEnabled = mTvInputManager.isParentalControlsEnabled();
            if (DEBUG) Log.d(TAG, "checkIsNeedClearUnblockRating  isParentControlEnabled = " + isParentControlEnabled);
            if (isParentControlEnabled) {
                synchronized (mRatingsUpdatelLock) {
                    Iterator<TvContentRating> rateIter = mUnblockedRatingSet.iterator();
                    while (rateIter.hasNext()) {
                        TvContentRating rating = rateIter.next();
                        if (mTvInputManager.isRatingBlocked(rating)) {
                            rateIter.remove();
                        }
                    }
                }
            }
        }

        protected void resetWorkThread() {
            if (mHandler != null)
                mHandler.removeCallbacksAndMessages(null);
            if (mMainHandler != null)
                mMainHandler.removeCallbacksAndMessages(null);
        }

        protected void switchToSourceInput(Uri uri) {
            if (DEBUG) Log.d(TAG, "switchToSourceInput  uri=" + uri + " this:"+ this);
            if (mHandler != null) {
                if (DEBUG) Log.d(TAG, "remove msg_play and start a new one");
                //mHandler.removeMessages(MSG_PLAY);
                resetWorkThread();
                mHandler.obtainMessage(MSG_PLAY, uri).sendToTarget();
            }
        }

        protected void doPlay(Uri uri) {

            float Time= (float) android.os.SystemClock.uptimeMillis() / 1000;
            if (DEBUG) Log.d(TAG, "--doPlay  uri=" + uri + " this:"+ this + "SwitchSourceTime = " + Time);
            if (null == uri) {
                return;
            }
            mCurrentUri = uri;
            stopSubtitle();

            if (uri != null && !uri.equals(mCurrentChannelUri)) {
                mUnblockedRatingSet.clear();//keep set for same channel
            } else if (uri == null) {
                mUnblockedRatingSet.clear();
            }
            mCurrentChannelUri = uri;
            mChannelBlocked = -1;
            isUnlockCurrent_NR = false;
            mIsBlocked = false;//Restore to default

            //isTvPlaying = false;
            if (mIsChannelScrambled
                    && mSystemControlManager.getPropertyBoolean(DroidLogicTvUtils.PROP_NEED_FAST_SWITCH, false)) {
                // scrambled channels need replay to receive scrambled message, so it can't do fast switch
                mSystemControlManager.setProperty(DroidLogicTvUtils.PROP_NEED_FAST_SWITCH, "false");
            }
            mIsChannelScrambled = false;

            subtitleAutoStart = mSystemControlManager.getPropertyBoolean(DTV_SUBTITLE_AUTO_START, true);
            //subtitleAutoSave = subtitleAutoStart;
            subtitleTifMode = mSystemControlManager.getPropertyBoolean(DTV_SUBTITLE_TIF_COMPATIABLE, true);

            if (Utils.getChannelId(uri) < 0) {
                if (false)
                    mTvControlManager.PlayDTVProgram(
                        new TvControlManager.TvMode(mDtvType).getMode(), 470000000, 0, 0, 0, 0, -1, -1, 0, 0, false);
                else {
                    TvControlManager.FEParas fe = new TvControlManager.FEParas();
                    fe.setMode(new TvControlManager.TvMode(mDtvType))
                       .setFrequency(470000000);
                    StringBuilder param = new StringBuilder("{")
                        .append("\"type\":\"dtv\"")
                        .append(","+fe.toNamedString())
                        .append(",\"v\":{\"pid\":"+0+",\"fmt\":"+-1+"}")
                        .append(",\"a\":{\"pid\":"+0+",\"fmt\":"+-1+",\"AudComp\":"+0+"}")
                        .append(",\"p\":{\"pid\":"+0+"}")
                        .append(",\"para\":{"+"\"disableTimeShifting\":1"+"}")
                        .append("}");
                    mTvControlManager.startPlay("atsc", param.toString());
                }
                mCurrentChannel = null;
                mCurrentSubtitles = null;
                mCurrentAudios = null;
                mCurrentCCContentRatings = null;
                mCurrentPmtContentRatings = null;
                return;
            }

            ChannelInfo ch = mTvDataBaseManager.getChannelInfo(uri);

            prepareChannelInfo(ch);

            if (ch != null) {
                if (enableChannelBlockInServer()) {
                    mIsChannelBlocked = ch.isLocked();
                }
                mTvControlManager.request("ADTV.setCurrentChannelBlockStatus", "{\"Blocked\":" + ch.isLocked() + "}");
                int isRadioChannel = ChannelInfo.isRadioChannel(ch) ? 1 : 0;
                int isInvalidService = TvContract.Channels.SERVICE_TYPE_OTHER.equals(ch.getServiceType()) && (ch.getVideoPid() == 0 || ch.getVideoPid() >= 0x1FFF) ? 1 : 0;
                mTvControlManager.request("ADTV.setNoneStaticChangeToCurrentProgram", "{\"Scrambled\":" + ch.getScrambled()
                        + ",\"RadioChannel\":" + isRadioChannel
                        + ",\"invalidService\":" + isInvalidService
                        + "}");
                if (isInvalidService == 1) {
                    updateEasState();
                }
                tryPlayProgram(ch);
            } else {
                Log.w(TAG, "Failed to get channel info for " + uri);
                mTvControlManager.SetAVPlaybackListener(null);
                mTvControlManager.SetPlayerInstanceNoListener(null);
            }
            if (mCurrentChannel != null) {
                mEASProcessManager.SetCurDisplayNum(mCurrentChannel.getDisplayNumber());
                mEASProcessManager.SetCurInputId(getInputId());
                mEASProcessManager.SetCurUri(mCurrentUri);
            }
        }

        protected void prepareChannelInfo(ChannelInfo channel) {
            mCurrentSubtitles = new ArrayList<ChannelInfo.Subtitle>();
            mCurrentAudios = new ArrayList<ChannelInfo.Audio>();
            if (channel != null) {
                prepareSubtitles(mCurrentSubtitles, channel);
                prepareAudios(mCurrentAudios, channel);
            }
        }

        @Override
        public void PlayerInsEvent(int msgType, int type, int Id) {
            if (DEBUG) Log.d(TAG, "PlayerInsEvent = "+msgType+" type = "+type+" Id = "+Id);

            mSubtitleView.DTVSubtitleTune(type, Id);
            if (type == DECODE_ID_TYPE) {
                player_instance_id = Id;
            }

            if (type == SYNC_ID_TYPE) {
                sync_instance_id = Id;
            }
        }

        protected boolean isAtsc(ChannelInfo info) {
            return info.isAtscChannel() || isAtscForcedStandard();
        }

        private ChannelInfo mLastChannel = null;
        protected boolean tryPlayProgram(ChannelInfo info) {
            boolean needstaticframe = mTvControlManager.getBlackoutEnable() == 0;
            boolean needdisablestaticframe = false;
            if (mCurrentChannel != null && info != null) {
                if (DroidLogicTvUtils.isAtscCountry(mContext)) {
                    if ((TvContract.Channels.TYPE_ATSC_T.equals(mCurrentChannel.getSignalType()) ||
                            TvContract.Channels.TYPE_ATSC_T.equals(mCurrentChannel.getSignalType())) &&
                            (TvContract.Channels.TYPE_ATSC_T.equals(info.getSignalType()) ||
                            TvContract.Channels.TYPE_ATSC_T.equals(info.getSignalType()))) {
                        if (!TextUtils.equals(mCurrentChannel.getSignalType(), info.getSignalType())) {
                            if (DEBUG) Log.d(TAG, "disable show last frame as diffrent sigtype");
                            needdisablestaticframe = true;//atsc-t to atsc-c or atsc-c to atsc-t, this case last frame is not needed
                        }
                    }
                }
                if (ChannelInfo.isRadioChannel(mCurrentChannel) != ChannelInfo.isRadioChannel(info)) {
                    if (DEBUG) Log.d(TAG, "disable show last frame as diffrent video type");
                    needdisablestaticframe = true;//radio to video or video to radio, this case last frame is not needed
                }
                boolean lastRatingBlockStatus = DataProviderManager.getBooleanValue(mContext, DroidLogicTvUtils.TV_CURRENT_BLOCK_STATUS, false);
                boolean lastChannelBlockStatus = DataProviderManager.getBooleanValue(mContext, DroidLogicTvUtils.TV_CURRENT_CHANNELBLOCK_STATUS, false);
                if (/*mIsBlocked || mIsPreviousChannelBlocked || */lastRatingBlockStatus || lastChannelBlockStatus) {
                    if (DEBUG) Log.d(TAG, "disable show last frame as previous blocked");
                    needdisablestaticframe = true;//blocked channel to non blocked, this case last frame is not needed
                }
            }
            if (needstaticframe && !needdisablestaticframe) {
                if (DEBUG) Log.d(TAG, "enable show last frame");
                mTvControlManager.setBlackoutEnable(0, 0);
            } else {
                if (DEBUG) Log.d(TAG, "disable show last frame");
                mTvControlManager.setBlackoutEnable(1, 0);
                //setTuningScreen(true);
            }
            mCurrentChannel = info;
            isTvPlaying = false;
            if (DEBUG) Log.d(TAG,"mCurrentChannel : " + mCurrentChannel);
            if (DroidLogicTvUtils.isAtscCountry(mContext)) {
                int signalType = DroidLogicTvUtils.getSigType(mCurrentChannel);
                Log.i(TAG, "signalType = " + signalType + " isPlayingATVInDTVMode = " + isPlayingATVInDTVMode);
                boolean needResetSurface = false;
                if (signalType == DroidLogicTvUtils.SIG_INFO_TYPE_ATV && !isPlayingATVInDTVMode) {
                    needResetSurface = true;
                } else if (signalType != DroidLogicTvUtils.SIG_INFO_TYPE_ATV && isPlayingATVInDTVMode){
                    needResetSurface = true;
                }
                isPlayingATVInDTVMode = signalType == DroidLogicTvUtils.SIG_INFO_TYPE_ATV;
                if (mSurface != null && needResetSurface) {
                    setSurfaceInService(mSurface, this);
                }
            }
            mCurrentPmtContentRatings = null;

            if (!TextUtils.isEmpty(info.getContentRatings())) {
                mCurrentCCContentRatings = Program.stringToContentRatings(info.getContentRatings());
            } else {
                mCurrentCCContentRatings = null;
            }

            if (info.isAnalogChannel()) {
//                mCurrentCCContentRatings = null;
                saveCurrentChannelRatings();
                if (CustomerOps.getInstance(mContext).shouldSendTimeShiftStatusToAtv()) {
                    notifyTimeShiftStatusChanged(TvInputManager.TIME_SHIFT_STATUS_UNAVAILABLE);
                }
            }

            if ((mLastChannel != null) && (mCurrentChannel != null)) {
                if ((mLastChannel.getFrequency() != mCurrentChannel.getFrequency()) || mUpdateTsFlag) {
                    setMonitor(null);
                    mUpdateTsFlag = false;
                }
            }

            if (!mSystemControlManager.getPropertyBoolean(DroidLogicTvUtils.PROP_NEED_FAST_SWITCH, false)) {
                boolean needResumeBlackout = false;
                if (mLastChannel != null && mLastChannel.isAnalogChannel() != mCurrentChannel.isAnalogChannel()
                        && mTvControlManager.getBlackoutEnable() != 1 && !DroidLogicTvUtils.isAtscCountry(mContext)) {
                    // need clear static frame when switching source between ATV and DTV
                    mTvControlManager.setBlackoutEnable(1, 0);
                    needResumeBlackout = true;
                }
                if (!isSurfaceAlive) {
                    return true;
                }
                mTvControlManager.TvSetFrontEnd(new TvControlManager.FEParas(info.getFEParas()));

                if (needResumeBlackout) {
                    mTvControlManager.setBlackoutEnable(0, 0);
                }
            } else if (isSurfaceAlive){
                mSessionHandler.post(new Runnable() {
                        public void run() {
                            if (isRadioChannel()) {
                                notifyVideoAvailable();
                            } else {
                                onSigChange(mTvControlManager.GetCurrentSignalInfo());
                            }
                        }
                });
            }

            setMonitor(info);
            mLastChannel = mCurrentChannel;
            checkContentBlockNeeded(info);
            //dealt in TvControlManager.EVENT_AV_PLAYBACK_RESUME
            //showRadioPicture(1000);//display after 1s
            return true;
        }

        protected int tryStartSubtitle(ChannelInfo info) {
            if (isAtsc(info) && !subtitleTifMode) {
                mCurrentCCExist = 0;
                mSystemControlManager.setProperty(DTV_SUBTITLE_CAPTION_EXIST, String.valueOf(mCurrentCCExist));
                startSubtitleCCBackground(info);
            }
            mCurrentCCEnabled = mCaptioningManager == null? false : mCaptioningManager.isEnabled();
            Log.e(TAG, "start subtitle " + "isAnalogChannel " + info.isAnalogChannel() + " isNtscChannel " + info.isNtscChannel());

            if (info != null && info.isAnalogChannel() && !info.isNtscChannel())
            {
                Log.e(TAG, "PAL start subtitle");
                for (ChannelInfo.Subtitle s : mCurrentSubtitles) {
                    if (s.mType == ChannelInfo.Subtitle.TYPE_ATV_TELETEXT) {
                        Log.e(TAG, "Found track");
                        mSubtitleView.setTTSwitch(false);
                        pal_teletext_subtitle = s;
                        break;
                    }
                }

                pal_teletext = true;
                if (teletext_switch) {
                    start_teletext();
                }
            } else if (subtitleAutoStart) {
                startSubtitle(info);
                pal_teletext = false;
            }

            return 0;
        }

        protected boolean playProgram(ChannelInfo info) {
            //override by child class
            return true;
        }

        private void updateChannelBlockStatus(boolean channelBlocked,
                TvContentRating contentRating, ChannelInfo channelInfo) {
            if (channelInfo == null) {
                if (DEBUG) Log.d(TAG,"channelInfo is null ,exit updateChannelBlockStatus");
                return;
            }
            if (DEBUG) Log.d(TAG, "updateBlock:"+channelBlocked + " curBlock:"+mChannelBlocked + " channel:"+channelInfo.getId());
            TvContentRating tcr = TvContentRating.createRating("com.android.tv", "block_norating", "block_norating", "");//only for block canonical function
            //maybe from the previous channel
            if (TvContract.buildChannelUri(channelInfo.getId()).compareTo(mCurrentUri) != 0)
                return;

            boolean needChannelBlock = channelBlocked;
            if (DEBUG) Log.d(TAG, "isBlockNoRatingEnable:"+isBlockNoRatingEnable+",isUnlockCurrent_NR:"+isUnlockCurrent_NR);
            //add for no-rating block
            boolean isParentControlEnabled = mTvInputManager.isParentalControlsEnabled();
            TvContentRating ratings[] = getContentRatingsOfCurrentProgram(channelInfo);
            if (ratings == null && isBlockNoRatingEnable && !isUnlockCurrent_NR)
                needChannelBlock = true;

            if (DEBUG) Log.d(TAG, "needChannelBlock:"+needChannelBlock);
            needChannelBlock = isParentControlEnabled & needChannelBlock;
            if (DEBUG) Log.d(TAG, "updated needChannelBlock:"+needChannelBlock);
            if ((mChannelBlocked != -1) && (mChannelBlocked == 1) == needChannelBlock
                    && (!needChannelBlock || (needChannelBlock && contentRating != null && contentRating.equals(mLastBlockedRating))))
                //if(!isBlockNoRatingEnable)
                return;

            mChannelBlocked = (needChannelBlock ? 1 : 0);
            if (needChannelBlock) {
                //stopSubtitleBlock(channelInfo);
                if (contentRating != null) {
                    if (DEBUG) Log.d(TAG, "notifyBlock:"+contentRating.flattenToString());
                    if (!mUnblockedRatingSet.contains(contentRating)) {
                        notifyContentBlocked(contentRating);
                    }
                } else if (isBlockNoRatingEnable) {
                    if (DEBUG) Log.d(TAG, "notifyBlock because of block_norating:"+tcr.flattenToString());
                    notifyContentBlocked(tcr);
                }
                mLastBlockedRating = contentRating;
                if (!isTvPlaying) {
                    playProgram(mCurrentChannel);//play after notifyContentBlocked to avoid paly after unblock
                }
            } else {
                synchronized(mLock) {
                    if (mCurrentChannel != null) {
                        if (!isTvPlaying) {
                            playProgram(mCurrentChannel);
                        }
                        //if channel is Blocked, don't send notifyAllowed
                        if (!mCurrentChannel.isLocked() || !mIsChannelBlocked) {
                            if (!enableChannelBlockInServer()) {
                                if (DEBUG) Log.d(TAG, "notifyAllowed");
                                notifyContentAllowed();
                            }
                        }
                    }
                }
            }
        }

        // if 3rd cc ratings is not null, the save it to channel's ratings
        private void saveCurrentChannelRatings() {
            if (mCurrentChannel != null) {
                String rstr = null;
                final String DELIMITER = ",";
                if ((mCurrentCCContentRatings != null) && (mCurrentCCContentRatings.length > 0)) {
                    rstr = Program.contentRatingsToString(mCurrentCCContentRatings);
                }
                //first used eit ratings, if eit rating is not exist, used pmt ratings,
                //if both eit and pmt rating not exist, used cc ratings.
                if ((mCurrentPmtContentRatings != null) && (mCurrentPmtContentRatings.length > 0)) {
                     StringBuilder ratings = new StringBuilder(mCurrentPmtContentRatings[0].flattenToString());
                     for (int i = 1; i < mCurrentPmtContentRatings.length; ++i) {
                          ratings.append(DELIMITER);
                          ratings.append(mCurrentPmtContentRatings[i].flattenToString());
                     }
                     rstr = ratings.toString();
                }
                if (!TextUtils.equals(rstr == null ? "" : rstr, mCurrentChannel.getContentRatings() == null ? "" : mCurrentChannel.getContentRatings())) {
                    if (DEBUG) Log.d(TAG, "rating:updateChannel:"+rstr);
                    mCurrentChannel.setContentRatings(rstr == null ? "" : rstr);
                    mTvDataBaseManager.updateChannelInfo(mCurrentChannel);
                }
            }
        }

        protected long getCurrentProgramTime() {
            if (mTvTime != null) {
                if (mRecordingId != null && mCurrentTimeMs != 0)
                    return mCurrentTimeMs + mTvTime.getDiffTime();
                else
                    return mTvTime.getTime();
            }
            return 0;
        }

        protected TvContentRating[] getContentRatingsOfCurrentProgram(ChannelInfo channelInfo) {
            long currentProgramTime = 0;
            if (channelInfo == null)
                return null;
            currentProgramTime = getCurrentProgramTime();
            if (currentProgramTime == 0)
                return null;
            Program mCurrentProgram = mTvDataBaseManager.getProgram(TvContract.buildChannelUri(channelInfo.getId()), currentProgramTime);
            if (DEBUG) Log.d(TAG, "TvTime:"+getDateAndTime(currentProgramTime)+" ("+currentProgramTime+")");

            TvContentRating[] ratings = mCurrentProgram == null ? null : mCurrentProgram.getContentRatings();
            String json = null;
            TvContentRating[] newParseRatings = null;
            if (mCurrentProgram != null) {
                json = mCurrentProgram.getInternalProviderData();
                if (DEBUG) Log.d(TAG, "getContentRatingsOfCurrentProgram programid = " + mCurrentProgram.getId() + ", channel json = " + json);
                newParseRatings = parseMultiRatings(json, channelInfo.getDisplayNumber(), mCurrentProgram.getTitle());
            } else if (DEBUG) {
                Log.d(TAG, "getContentRatingsOfCurrentProgram mCurrentProgram = null");
            }
            if (DEBUG) {
                Log.d(TAG, "getContentRatingsOfCurrentProgram newrating = " + Program.contentRatingsToString(newParseRatings) + ", ratings = " + Program.contentRatingsToString(ratings));
            }
            if (newParseRatings != null && !TextUtils.equals(Program.contentRatingsToString(newParseRatings), Program.contentRatingsToString(ratings))) {
                ratings = newParseRatings;
                mCurrentProgram.setContentRatings(ratings);
                mTvDataBaseManager.updateProgram(mCurrentProgram);
                Log.d(TAG, "getContentRatingsOfCurrentProgram update ratings:" +mCurrentProgram.getTitle());
            }

            /*pmt ratings 2nd*/
            if (ratings == null) {
                ratings = mCurrentPmtContentRatings;
                if (ratings != null && ratings.length > 0) {
                    if (DEBUG) Log.d(TAG, "mCurrentPmtContentRatings = " + Program.contentRatingsToString(ratings));
                    saveCurrentChannelRatings();
                }
            }
            /*cc ratings 3rd*/
            if (ratings == null) {
                ratings = mCurrentCCContentRatings;
                if (ratings != null && ratings.length > 0) {
                    if (DEBUG) Log.d(TAG, "mCurrentCCContentRatings = " + Program.contentRatingsToString(ratings));
                    saveCurrentChannelRatings();
                }
            }
            if (ratings == null && mCurrentChannel != null) {
                ratings = Program.stringToContentRatings(mCurrentChannel.getContentRatings());
                if (ratings != null && ratings.length > 0) {
                    if (DEBUG) Log.d(TAG, "mCurrentChannel = " + Program.contentRatingsToString(ratings));
                }
            }

            /*NR ratings*/
            if (ratings == null || ratings.length == 0) {
                ratings = CustomerOps.getInstance(mContext).getCustomerNoneRatings();
            }

            return ratings;
        }

        protected TvContentRating getContentRatingOfCurrentProgramBlocked(ChannelInfo channelInfo) {
            TvContentRating ratings[] = getContentRatingsOfCurrentProgram(channelInfo);
            if (ratings == null)
                return null;
            mCurChannelRatings = ratings;
            Log.d(TAG, "current Ratings:"+Program.contentRatingsToString(ratings));

            for (TvContentRating rating : ratings) {
                if (!mUnblockedRatingSet.contains(rating) && mTvInputManager
                        .isRatingBlocked(rating)) {
                    return rating;
                }
            }
            return null;
        }

        public int mParentControlDelay = 2000;

        protected void doParentalControls(ChannelInfo channelInfo) {
            if (mHandler != null)
                mHandler.removeMessages(MSG_PARENTAL_CONTROL);
            if (mTvInputManager == null)
                mTvInputManager = (TvInputManager)getSystemService(Context.TV_INPUT_SERVICE);

            //Log.d(TAG, "doPC:"+this);
            boolean isParentalControlsEnabled = mTvInputManager.isParentalControlsEnabled();
            if (isParentalControlsEnabled) {
                TvContentRating blockContentRating = getContentRatingOfCurrentProgramBlocked(channelInfo);
                if (blockContentRating != null) {
                    if (DEBUG) Log.d(TAG, "Check parental controls: blocked by content rating - "
                            + blockContentRating.flattenToString());
                } else {
                    //Log.d(TAG, "Check parental controls: available");
                }
                updateChannelBlockStatus(blockContentRating != null, blockContentRating, channelInfo);
            } else {
                if (DEBUG) Log.d(TAG, "Check parental controls: disabled");
                updateChannelBlockStatus(false, null, channelInfo);
            }

            if (mHandler != null) {
                    mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_PARENTAL_CONTROL, this), mParentControlDelay);
                    if (DEBUG) Log.d(TAG, "doPC next:"+mParentControlDelay);
            }
        }

        protected void checkContentBlockNeeded(ChannelInfo channelInfo) {
            //this is DTVInputService, no ParentControl, just unlock all channels
            //ParentControl will be exec in ADTVInputService
            //doParentalControls(channelInfo);
            updateChannelBlockStatus(false, null, channelInfo);
        }
        public void notifyUpdateUnblockRatingSet() {
            // isUnlockCurrent_NR = false;
            if (mHandler != null) {
                mHandler.removeMessages(MSG_UPDATE_UNBLOCK_SET);
                mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_UNBLOCK_SET, this));
            }
        }
        protected void checkCurrentContentBlockNeeded() {
            checkContentBlockNeeded(mCurrentChannel);
        }

        protected void unblockContent(TvContentRating rating) {
            // TIS should unblock content only if unblock request is legitimate.
            if (DEBUG) Log.d(TAG, "unblockContent " + rating.flattenToString());
            int index = 0;
            if (rating == null
                || mLastBlockedRating == null
                || (mLastBlockedRating != null && rating.equals(mLastBlockedRating))) {
                mLastBlockedRating = null;

                synchronized (mRatingsUpdatelLock) {
                    if (rating != null) {
                        //unblock all other ratings
                        TvContentRating[] allratings = getContentRatingsOfCurrentProgram(mCurrentChannel);
                        if (allratings != null) {
                            for (TvContentRating single : allratings) {
                                mUnblockedRatingSet.add(single);
                            }
                        } else {
                            mUnblockedRatingSet.add(rating);
                        }
                        isUnlockCurrent_NR = true;
                        TvContentRating ratings[] = DroidLogicTvUtils.unblockLowRating(rating);
                        if (ratings != null && ratings.length > 0) {
                            for (index = 0; index < ratings.length; index ++) {
                                if (!mUnblockedRatingSet.contains(ratings[index]))
                                    mUnblockedRatingSet.add(ratings[index]);
                            }
                        }
                    }
                    //Unlock lower rating for other ratings
                    if (mCurChannelRatings != null && mCurChannelRatings.length > 1) {
                        for (TvContentRating mrating : mCurChannelRatings) {
                            if (!rating.equals(mrating)) {
                                mUnblockedRatingSet.add(mrating);
                                TvContentRating ratings[] = DroidLogicTvUtils.unblockLowRating(mrating);
                                if (ratings != null && ratings.length > 0) {
                                    for (index = 0; index < ratings.length; index ++) {
                                        if (!mUnblockedRatingSet.contains(ratings[index]))
                                            mUnblockedRatingSet.add(ratings[index]);
                                    }
                                }
                            }
                        }
                    }
                }
                mCurChannelRatings = null;
                if (DEBUG) Log.d(TAG, "notifyContentAllowed");
                notifyContentAllowed();
            }
        }

        boolean teletext_switch = false;
        int tt_display_mode = DTVSubtitleView.TTX_MIX_MODE_NORAML;

        @Override
        public boolean onKeyUp(int keyCode, KeyEvent event) {
            if (!pal_teletext || (!teletext_switch && keyCode != KEY_TELETEXT_SWITCH))
                return super.onKeyUp(keyCode, event);
            else {
                switch (keyCode) {
                    case 7:
                    case 8:
                    case 9:
                    case 10:
                    case 11:
                    case 12:
                    case 13:
                    case 14:
                    case 15:
                    case 16:
                    case KEY_RED: //Red
                    case KEY_GREEN: //Green
                    case KEY_YELLOW: //Yellow
                    case KEY_BLUE: //Blue
                    case KEY_MIX: //Play/Pause
                    case KEY_NEXT_PAGE: //Next program
                    case KEY_PRIOR_PAGE: //Prior program
                    case KEY_CLOCK: //Fast backward
                    case KEY_GO_HOME: //Fast forward
                    case KEY_ZOOM: //Zoom in
                    case KEY_SUBPG: //Stop
                    case KEY_LOCK_SUBPG: //DUIDE
                    case KEY_TELETEXT_SWITCH: //Zoom out
                    case KEY_REVEAL: //FAV
                    case KEY_CANCEL: //List
                    case KEY_SUBTITLE: //Subtitle
                        break;
                    default:
                        return super.onKeyUp(keyCode, event);
                }
                return true;
            }
        }

        public void reset_atv_status()
        {
            if (tt_display_mode == DTVSubtitleView.TTX_MIX_MODE_LEFT_RIGHT) {
                Rect rect = new Rect();
                mSubtitleView.getGlobalVisibleRect(rect);
                layoutSurface(rect.left, rect.top, rect.right, rect.bottom);
            }
            tt_display_mode = DTVSubtitleView.TTX_MIX_MODE_NORAML;
            mSubtitleView.reset_atv_status();
            Log.e(TAG, "reset_atv_status done");
        }

        private int reg_id = 0;
        @Override
        public boolean onKeyDown(int keyCode, KeyEvent event) {
            float Time= (float) android.os.SystemClock.uptimeMillis() / 1000;
            if (DEBUG) Log.d(TAG, "keycode down: " + keyCode + " tt_switch " + teletext_switch + "SwitchSourceTime = " + Time);
            //Teletext is not opened.
            if (!pal_teletext || (!teletext_switch && keyCode != KEY_TELETEXT_SWITCH)) {
                if (keyCode == KEY_TELETEXT_SWITCH) {
                    Toast.makeText(mContext, "No teletext", Toast.LENGTH_SHORT).show();
                }
                return super.onKeyDown(keyCode, event);
             }

            switch (keyCode) {
                case 7:
                case 8:
                case 9:
                case 10:
                case 11:
                case 12:
                case 13:
                case 14:
                case 15:
                case 16:
                    mSubtitleView.tt_handle_number_event(keyCode - 7);
                    break;
                case KEY_RED: //Red
                    mSubtitleView.colorLink(0);
                    break;
                case KEY_GREEN: //Green
                    mSubtitleView.colorLink(1);
                    break;
                case KEY_YELLOW: //Yellow
                    mSubtitleView.colorLink(2);
                    break;
                case KEY_BLUE: //Blue
                    mSubtitleView.colorLink(3);
                    break;
                case KEY_MIX: //Play/Pause
                    switch (tt_display_mode)
                    {
                        case DTVSubtitleView.TTX_MIX_MODE_NORAML:
                            tt_display_mode = DTVSubtitleView.TTX_MIX_MODE_TRANSPARENT;
                            break;
                        case DTVSubtitleView.TTX_MIX_MODE_TRANSPARENT:
                            tt_display_mode = DTVSubtitleView.TTX_MIX_MODE_LEFT_RIGHT;
                            break;
                        case DTVSubtitleView.TTX_MIX_MODE_LEFT_RIGHT:
                            tt_display_mode = DTVSubtitleView.TTX_MIX_MODE_NORAML;
                            break;
                        default:
                            tt_display_mode = DTVSubtitleView.TTX_MIX_MODE_NORAML;
                            break;
                    };
                    mSubtitleView.tt_switch_mix_mode(tt_display_mode);
                    Rect rect = new Rect();
                    mSubtitleView.getGlobalVisibleRect(rect);
                    if (tt_display_mode == DTVSubtitleView.TTX_MIX_MODE_LEFT_RIGHT) {
                        layoutSurface(rect.left, rect.top, (rect.left + rect.right) / 2,  rect.top + rect.bottom);
                    } else {
                        layoutSurface(rect.left, rect.top, rect.right,  rect.bottom);
                    }
                    break;
                case KEY_UP:
                case KEY_NEXT_PAGE: //Next program
                    reset_atv_status();
                    mSubtitleView.nextPage();
                    break;
                case KEY_DOWN:
                case KEY_PRIOR_PAGE: //Prior program
                     reset_atv_status();
                     mSubtitleView.previousPage();
                    break;
                case KEY_CLOCK: //Fast backward
                    mSubtitleView.tt_switch_clock_mode();
                    break;
                case KEY_GO_HOME: //Fast forward
                    reset_atv_status();
                    mSubtitleView.goHome();
                    break;
                case KEY_SUB_PAGE: // TT SubPage
                    mSubtitleView.tt_goto_subpage();
                    break;
                case KEY_ZOOM: //Zoom in
                    mSubtitleView.tt_zoom_in();
                    break;
                case KEY_LEFT:
                    mSubtitleView.tt_get_previousSubpage();
                    break;
                case KEY_RIGHT:
                case KEY_SUBPG: //Stop
                    mSubtitleView.tt_get_nextsubpage();
                    break;
                case KEY_LOCK_SUBPG: //DUIDE
                    mSubtitleView.setTTSubpgLock();
                    break;
                case KEY_TELETEXT_SWITCH: //Zoom out
                    teletext_switch = !teletext_switch;
                    if (teletext_switch) {
                        start_teletext();
                    } else {
                        stop_teletext();
                    }
                    break;
                case KEY_REVEAL: //FAV
                    mSubtitleView.setTTRevealMode();
                    break;
                case KEY_CANCEL: //List
                    mSubtitleView.tt_clear();
                    break;
                case KEY_SUBTITLE: //Subtitle
                    reset_atv_status();
                    mSubtitleView.tt_goto_subtitle();
                    break;
                case KEY_REGION:
                    switch (reg_id)
                    {
                        case 0:
                            reg_id = 8;
                            break;
                        case 8:
                            reg_id = 16;
                            break;
                        case 16:
                            reg_id = 24;
                            break;
                        case 24:
                            reg_id = 32;
                            break;
                        case 32:
                            reg_id = 48;
                            break;
                        case 48:
                            reg_id = 64;
                            break;
                        case 64:
                            reg_id = 80;
                            break;
                        default:
                            reg_id = 0;
                            break;
                    };
                    Log.e(TAG, "regid " + reg_id);
                    mSubtitleView.setTTRegion(reg_id);
                    break;
                default:
                    return super.onKeyDown(keyCode, event);
            }
            return true;
        }
        private void start_teletext()
        {
            reset_atv_status();
            if (mCurrentChannel != null && pal_teletext_subtitle != null) {
//                            Toast.makeText(mContext, "Searching teletext", Toast.LENGTH_SHORT).show();
                setSubtitleParam(mCurrentChannel.getVfmt(),
                        ChannelInfo.Subtitle.TYPE_ATV_TELETEXT,
                        0,
                        0,
                        mCurrentChannel.getFrequency(),
                        0,
                        "");
                mSubtitleView.setActive(true);
                mSubtitleView.startSub();
                mSubtitleView.setTTSwitch(false);
                enableSubtitleShow(true);
                Log.e(TAG, "teletext_switch " + teletext_switch + " id");
            } else {
                Toast.makeText(mContext, "No teletext, no channel info", Toast.LENGTH_SHORT).show();
                //TODO: Remove subtitle notification.
            }
        }

        private void stop_teletext()
        {
            Log.e(TAG, "stop_teletext");
            teletext_switch = false;
            reset_atv_status();
            mSubtitleView.stop();
            mSubtitleView.setTTSwitch(false);
            notifyTrackSelected(TvTrackInfo.TYPE_SUBTITLE, null);
            enableSubtitleShow(false);
        }

        @Override
        public void onEvent(int msgType, int programID) {
            Log.d(TAG, "AV evt:" + msgType);
            switch (msgType) {
                case TvControlManager.EVENT_AV_SCRAMBLED:
                    if (mOverlayView != null) {
                        mOverlayView.setText(R.string.av_scrambled);
                        mOverlayView.setTextVisibility(true);
                    }
                    notifySessionEvent(DroidLogicTvUtils.AV_SIG_SCRAMBLED, null);
                    mIsChannelScrambled = true;
                    break;
                case TvControlManager.EVENT_AV_PLAYBACK_NODATA:
                    break;
                case TvControlManager.EVENT_AV_PLAYBACK_RESUME:
                    if (mCurrentChannel != null && ChannelInfo.isRadioChannel(mCurrentChannel)) {
                        //for radio channel, if display picture, needn't display audio only
                        //showRadioPicture(1000);
                        //notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_AUDIO_ONLY);
                        mCurrentSession.notifyVideoAvailable();
                    } else if (mCurrentChannel != null && mOverlayView != null){
                        //Hide SCRAMBLED when receiving "EVENT_AV_PLAYBACK_RESUME" request
                        mOverlayView.setTextVisibility(false);
                    }
                    break;
                case TvControlManager.EVENT_AV_VIDEO_AVAILABLE:
                    notifyVideoAvailable();
                    if (mHandler != null) {
                        mHandler.sendEmptyMessageDelayed(MSG_UPDATE_VIDEO_RESOLUTION, 200);
                    }
                    // TODO: audioinfo only for test here, should be used by app
//                    TvControlManager.AudioFormatInfo audioInfo = mTvControlManager.DtvGetAudioFormatInfo();
//                    mSystemControlManager.setProperty("tv.dtv.audio.channels",
//                        String.valueOf(audioInfo.ChannelsOriginal)+"."+String.valueOf(audioInfo.LFEPresentOriginal));
                    break;
                case TvControlManager.EVENT_AV_TIMESHIFT_PLAY_FAIL:
                case TvControlManager.EVENT_AV_TIMESHIFT_REC_FAIL:
                case TvControlManager.EVENT_AV_TIMESHIFT_START_TIME_CHANGED:
                case TvControlManager.EVENT_AV_TIMESHIFT_CURRENT_TIME_CHANGED:
                    onTimeShiftEvent(msgType, programID);
                    break;
                case TvControlManager.EVENT_AV_PLAYER_BLOCKED:
                    if (enableChannelBlockInServer()) {
                        super.onSetStreamVolume(0.0f);
                        TvContentRating tcr =
                            TvContentRating.createRating("com.android.tv",
                                "block_norating", "block_norating", "");//only for block canonical function
                        notifyContentBlocked(tcr);
                    }
                    break;
                case TvControlManager.EVENT_AV_PLAYER_UNBLOCK:
                    if (enableChannelBlockInServer()) {
                        super.onSetStreamVolume(1.0f);
                        notifyContentAllowed();
                    }
                    break;
                case TvControlManager.EVENT_AV_UNSUPPORT:
                    Log.d(TAG, "EVENT_AV_UNSUPPORT");
                    if (mOverlayView != null) {
                        mOverlayView.setText(R.string.av_Unsuppot);
                        mOverlayView.setTextVisibility(true);
                    }
                     notifyVideoUnavailable(DroidLogicTvUtils.VIDEO_UNAVAILABLE_NOT_SUPPORT);
                    break;
            }
        }

        @Override
        public void processDetailsChannelAlert(RrtEvent ev) {
            if (mHandler != null)
                mHandler.obtainMessage(MSG_RRT5_EVENT, ev).sendToTarget();
        }

        @Override
        public boolean onSelectTrack(int type, String trackId) {
            if (DEBUG) Log.d(TAG, "onSelectTrack: [type:" + type + "] [id:" + trackId + "]");

            if (mCurrentChannel == null)
                return false;

            if (type == TvTrackInfo.TYPE_AUDIO) {
                int index = -1;
                if (trackId == null) {
                    //TODO
                    //close audio track
                    index = -2;
                } else {
                    String oldId = mSystemControlManager.getProperty(DTV_AUDIO_TRACK_ID);
                    if (DEBUG) Log.d(TAG, "oldId:"+oldId);
                    if (!trackId.equals(oldId)) {
                        int findMainTrackIndex = -1;
                        int findAdTrackId = -1;
                        boolean needMixAdMain = false;
                        ChannelInfo.Audio audio = parseAudioIdString(trackId);
                        stopAudioAD();
                        //save current selection firstly as may be changed when main audio track found
                        mSystemControlManager.setProperty(DTV_AUDIO_TRACK_IDX, ""+audio.id);
                        mSystemControlManager.setProperty(DTV_AUDIO_TRACK_ID, trackId);
                        index = audio.id;
                        //find main track by ad track
                        if (audioADAutoStart && mCurrentChannel != null && hasDollbyAudioAD(mCurrentChannel)) {
                            int[] adList = DroidLogicTvUtils.getAudioADTracks(mCurrentChannel, 0);//get first ad
                            int firstAdTrackIndex = -1;
                            if (adList != null && adList.length > 0 && adList[0] > 0) {
                                firstAdTrackIndex = adList[0];
                            }
                            if (DEBUG) Log.d(TAG, "onSelectTrack firstAdTrackIndex  =" + firstAdTrackIndex + "audio.id = " + audio.id);
                            if (firstAdTrackIndex > 0 && audio.id >= firstAdTrackIndex) {
                                Log.i(TAG, "find ad audio successfully");
                                findAdTrackId = index;
                                findMainTrackIndex = audio.id - firstAdTrackIndex;//play mix audio if ad support
                                if (findMainTrackIndex >= 0 && mCurrentAudios != null) {//to ensure nomal size of mCurrentAudios
                                    Iterator<ChannelInfo.Audio> iter = mCurrentAudios.iterator();
                                    while (iter.hasNext()) {
                                        ChannelInfo.Audio a = iter.next();
                                        if (a != null && a.id == findMainTrackIndex) {
                                            audio = a;//find main audio track and replace ad audio track
                                            needMixAdMain = true;
                                            Log.d(TAG, "find main audio by ad audio and replace successfully");
                                            break;
                                        }
                                    }
                                }
                            } /*else {
                                findAdTrackId = audio.mPid + firstAdTrackIndex;
                                needMixAdMain = true;
                                if (DEBUG) Log.d(TAG, "current track need to mix AdMain, findAdTrackId : " + findAdTrackId);
                            }*/
                        }
                        /* Set ad audio first if available then start playback */
                        if (findAdTrackId != -1) {
                            startAudioADByMain(mCurrentChannel, audio.id);
                        }
                        mTvControlManager.DtvSwitchAudioTrack(audio.mPid, audio.mFormat, 0);
                        if (mHandler != null) {
                            if (DEBUG) Log.d(TAG, "send MSG_MIX_AD_MAIN status = " + needMixAdMain);
                            mHandler.obtainMessage(MSG_MIX_AD_MAIN, (needMixAdMain ? 1 : 0), 0).sendToTarget();
                        }
                    } else {
                        if (DEBUG) Log.d(TAG, "same audio track");
                    }
                }

                notifyTrackSelected(type, trackId);

                if (audioAutoSave) {
                    if (mCurrentChannel != null) {
                        if (DEBUG) Log.d(TAG, "audioAutoSave: idx=" + index);
                        mCurrentChannel.setAudioTrackIndex(index);
                        mTvDataBaseManager.updateSingleChannelInternalProviderData(mCurrentChannel.getId(),
                                ChannelInfo.KEY_AUDIO_TRACK_INDEX, String.valueOf(index));
                    }
                }

                return true;

            } else if (type == TvTrackInfo.TYPE_SUBTITLE) {
                int index = -1;
                if (trackId == null) {
                    if (isAtsc(mCurrentChannel) && !subtitleTifMode) {
                        mSystemControlManager.setProperty(DTV_SUBTITLE_CS_PREFER, String.valueOf(-1));
                        mSystemControlManager.setProperty(DTV_SUBTITLE_CC_PREFER, String.valueOf(-1));
                    }
                    stopSubtitleUser(mCurrentChannel);
                    index = -2;
                } else {
                    ChannelInfo.Subtitle subtitle = parseSubtitleIdString(trackId);
                    if (isAtsc(mCurrentChannel) && !subtitleTifMode) {
                        mSystemControlManager.setProperty(DTV_SUBTITLE_CS_PREFER, String.valueOf(subtitle.mPid));
                        mSystemControlManager.setProperty(DTV_SUBTITLE_CC_PREFER, String.valueOf(-1));
                    } else {
                        stopSubtitle();
                        startSubtitle(subtitle, mCurrentChannel.getVfmt());
                        mSystemControlManager.setProperty(DTV_SUBTITLE_TRACK_IDX, String.valueOf(subtitle.id));
                    }
                    index = subtitle.id;
                }

                notifyTrackSelected(type, trackId);

                if (subtitleAutoSave) {
                    if (mCurrentChannel != null) {
                        if (DEBUG) Log.d(TAG, "subtitleAutoSave: idx=" + index);
                        mCurrentChannel.setSubtitleTrackIndex(index);
                        mTvDataBaseManager.updateSingleChannelInternalProviderData(mCurrentChannel.getId(),
                                ChannelInfo.KEY_SUBT_TRACK_INDEX, String.valueOf(index));
                        CustomerOps.getInstance(mContext).saveTvClosedCaptionIndex(mCurrentChannel, index);
                    }
                }

                return true;
            }
            return false;
        }

        @Override
        public void notifyVideoAvailable() {
            if (DEBUG) Log.d(TAG, "notifyVideoAvailable "+getSessionId());
            if (enableChannelBlockInServer()) {
                if (mTvControlManager.isBlockedByChannelLock()) {
                    return;
                } else {
                    super.onSetStreamVolume(1.0f);
                }
            }
            super.notifyVideoAvailable();
            mVideoUnavailableReason = TvInputManager.VIDEO_UNAVAILABLE_REASON_UNKNOWN - 1;

            enableSubtitleShow(true);
            updateEasState();

            if (isRadioChannel() && mOverlayView != null) {
                mOverlayView.setImage(R.drawable.bg_radio);
                mOverlayView.setImageVisibility(true);
            } else {
                if (DEBUG) Log.d(TAG, "notifyVideoAvailable not radio case");
            }

            if (mHandler != null) {
                mHandler.removeMessages(MSG_GET_SIGNAL_STRENGTH);
                mHandler.sendEmptyMessageDelayed(MSG_GET_SIGNAL_STRENGTH, MSG_GET_SIGNAL_STRENGTH_PERIOD);
            }

            if (mIsChannelScrambled && mTvControlManager.DtvGetVideoFormatInfo().fps <= 0
                && mOverlayView != null) {
                mOverlayView.setText(R.string.av_scrambled);
                mOverlayView.setTextVisibility(true);
            } else {
                mIsChannelScrambled = false;
            }
        }

        public void updateEasState() {
            if (mEASProcessManager != null && mEASProcessManager.isEasInProgress()
                    && mEASProcessManager.getEasChannelUri() != null
                    && mEASProcessManager.getEasChannelUri().equals(mCurrentUri)) {
                Log.i(TAG,"return to eas");
                notifyAppEasStatus(true);
                showEasText(true);
            } else {
                Log.i(TAG,"disable eas state");
                notifyAppEasStatus(false);
                if (mOverlayView != null)
                    mOverlayView.setEasTextVisibility(false);
            }
        }

        public void stopEasText() {
            if (mOverlayView != null) {
                if (mOverlayView.isEasTextShown()) {
                    mOverlayView.setEasTextVisibility(false);
                    notifyAppEasStatus(false);
                }
            }
        }

        public final static int MSG_STOP_EAS_TEXT = 0;

        Handler easHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_STOP_EAS_TEXT:
                        stopEasText();
                        break;
                }
            }
        };

        @Override
        public void notifyVideoUnavailable(int reason) {
            if (DEBUG) Log.d(TAG, "notifyVideoUnavailable: "+reason+", "+getSessionId());
            if (mVideoUnavailableReason == TvInputManager.VIDEO_UNAVAILABLE_REASON_UNKNOWN &&
                    reason == DroidLogicTvUtils.VIDEO_UNAVAILABLE_REASON_NODATA) {
                if (DEBUG) Log.d(TAG, "skip decode no signal as front no signal has handled!");
                return;
            }
            super.notifyVideoUnavailable(reason);
            mVideoUnavailableReason = reason;
            stopEasText();
            if (mOverlayView != null) {
                switch (reason) {
                    case TvInputManager.VIDEO_UNAVAILABLE_REASON_AUDIO_ONLY:
                    case DroidLogicTvUtils.VIDEO_UNAVAILABLE_NOT_SUPPORT:
                        /*mOverlayView.setImage(R.drawable.bg_radio);
                        mSystemControlManager.writeSysFs("/sys/class/video/disable_video",
                                "2");*///dealt in TvControlManager.EVENT_AV_PLAYBACK_RESUME
                        break;
                    case TvInputManager.VIDEO_UNAVAILABLE_REASON_TUNING:
                    case TvInputManager.VIDEO_UNAVAILABLE_REASON_BUFFERING:
                    case TvInputManager.VIDEO_UNAVAILABLE_REASON_WEAK_SIGNAL:
                    case TvInputManager.VIDEO_UNAVAILABLE_REASON_UNKNOWN:
                    default:
                        mOverlayView.setImage(R.drawable.bg_no_signal);
                        mOverlayView.setText(R.string.nosignal);
                        mOverlayView.setTextVisibility(true);
                        mOverlayView.setImageVisibility(true);
                        mOverlayView.setTextVisibility(true);
                        enableSubtitleShow(false);
                        break;
                }

                //Pal teletext request that unplug then plug in, close tt
                if (pal_teletext) {
                    teletext_switch = false;
                    mSubtitleView.setTTSwitch(teletext_switch);
                }
            }
        }

        @Override
        public void notifyContentAllowed() {
            if (DEBUG) Log.d(TAG, "notifyContentAllowed ");
            super.notifyContentAllowed();
            setTuningScreen(false);
            if (mIsBlocked) {
                mTvControlManager.request("ADTV.UnblockCurrentChannel", "");
                mIsBlocked = false;
            }
            if (enableChannelBlockInServer()) {
                if (mMainHandler != null) {
                    mMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            notifyVideoAvailable();
                        }
                    });
                }
            }
        }

        @Override
        public void notifyContentBlocked(final TvContentRating rating) {
            if (DEBUG) Log.d(TAG, "notifyContentBlocked: " +rating);
            super.notifyContentBlocked(rating);
            setTuningScreen(true);
            mIsBlocked = true;
            mTvControlManager.request("ADTV.BlockCurrentChannel", "");
            if (enableChannelBlockInServer()) {
                if (mMainHandler != null) {
                    mMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mOverlayView != null) {
                                mOverlayView.setImageVisibility(false);
                                mOverlayView.setTextVisibility(false);
                            }
                        }
                    });
                }
            }
        }

        //update overlay display need run in main handler
        private void showRadioPicture(final int time) {
            if (mMainHandler != null) {
                mMainHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mOverlayView != null && ChannelInfo.isRadioChannel(mCurrentChannel)) {
                            mOverlayView.setImage(R.drawable.bg_radio);
                            mOverlayView.setImageVisibility(true);
                            mOverlayView.setTextVisibility(false);
                            //mSystemControlManager.writeSysFs("/sys/class/video/disable_video", "2");
                        }
                    }
                }, time);
            }
        }

        @Override
        public boolean isRadioChannel() {
            if (mCurrentChannel != null)
                return ChannelInfo.isRadioChannel(mCurrentChannel);
            return false;
        }

        //update overlay display need run in main handler
        private void setTuningScreen(final boolean status) {
            if (mMainHandler != null) {
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mOverlayView != null) {
                            mOverlayView.setTuningImageVisibility(status);
                        }
                    }
                });
            }
        }

        protected String generateAudioIdString(ChannelInfo.Audio audio) {
            if (audio == null)
                return "";

            Map<String, String> map = new HashMap<String, String>();
            map.put("id", String.valueOf(audio.id));
            map.put("pid", String.valueOf(audio.mPid));
            map.put("fmt", String.valueOf(audio.mFormat));
            map.put("ext", String.valueOf(audio.mExt));
            map.put("lang", audio.mLang);
            return DroidLogicTvUtils.mapToString(map);
        }

        protected ChannelInfo.Audio parseAudioIdString(String audioId) {
            if (audioId == null)
                return null;

            Map<String, String> parsedMap = DroidLogicTvUtils.stringToMap(audioId);
            return new ChannelInfo.Audio(
                            Integer.parseInt(parsedMap.get("pid")),
                            Integer.parseInt(parsedMap.get("fmt")),
                            Integer.parseInt(parsedMap.get("ext")),
                            parsedMap.get("lang"),
                            Integer.parseInt(parsedMap.get("id")));
        }


        protected void prepareAudios(List<ChannelInfo.Audio> audios, ChannelInfo channel) {
            List<ChannelInfo.Audio> auds = getChannelAudios(channel);
            if (auds != null)
                audios.addAll(auds);
        }

        protected String addAudioTracks(List <TvTrackInfo> tracks, ChannelInfo ch) {
            if (mCurrentAudios == null || mCurrentAudios.size() == 0)
                return null;

            int auto = getAudioAuto(ch);
            Iterator<ChannelInfo.Audio> iter = mCurrentAudios.iterator();
            int firstAdAudioIndex = -1;
            int[] adAudioList = DroidLogicTvUtils.getAudioADTracks(ch, 0);//get first audio
            if (adAudioList != null && adAudioList.length > 0 && adAudioList[0] > 0) {
                firstAdAudioIndex = adAudioList[0];
            }
            if (DEBUG) Log.d(TAG, "add audio tracks["+mCurrentAudios.size()+"]" + ", firstAdAudioIndex = " + firstAdAudioIndex);
            while (iter.hasNext()) {
                ChannelInfo.Audio a = iter.next();
                if (a == null) continue;
                String Id = generateAudioIdString(a);
                String description = "";
                Bundle bundle = new Bundle();
                bundle.putBoolean(KEY_TVINPUTINFO_AUDIO_AD, false);
                if (a != null && firstAdAudioIndex > 0 && a.id >= firstAdAudioIndex) {//find ad audio
                    description = "ad";
                    bundle.putBoolean(KEY_TVINPUTINFO_AUDIO_AD, true);
                }
                TvTrackInfo AudioTrack =
                    new TvTrackInfo.Builder(TvTrackInfo.TYPE_AUDIO, Id)
                        .setLanguage(a.mLang)
                        .setAudioChannelCount(2)
                        .setDescription(description)
                        .setExtra(bundle)
                        .build();
                tracks.add(AudioTrack);

                if (DEBUG) Log.d(TAG, "\t" + ((auto == a.id) ? ("*"+a.id+":[") : (""+a.id+": [")) + a.mLang + "]"
                    + " [pid:" + a.mPid + "] [fmt:" + a.mFormat + "]" + " [discription:" + description + "]");
                if (DEBUG) Log.d(TAG, "\t" + "   [ext:" + Integer.toHexString(a.mExt) + "]");
            }

            if (auto >= 0)
                return generateAudioIdString(mCurrentAudios.get(auto));

            return null;
        }

        protected List<ChannelInfo.Subtitle> getChannelProgramCaptions(ChannelInfo channelInfo) {
            long currentProgramTime = 0;
            currentProgramTime = getCurrentProgramTime();
            if (currentProgramTime == 0)
                return null;
            Program mCurrentProgram = mTvDataBaseManager.getProgram(TvContract.buildChannelUri(channelInfo.getId()), currentProgramTime);
            if (DEBUG) Log.d(TAG, "TvTime:"+getDateAndTime(currentProgramTime));
            if (DEBUG) Log.d(TAG, "caption:"+(mCurrentProgram == null?"null":mCurrentProgram.getInternalProviderData()));
            return DroidLogicTvUtils.parseAtscCaptions(mCurrentProgram == null ? null : mCurrentProgram.getInternalProviderData());
        }

        protected List<ChannelInfo.Subtitle> getChannelFixedCaptions(ChannelInfo channel) {
            ArrayList<ChannelInfo.Subtitle> SubtitleList = new ArrayList<ChannelInfo.Subtitle>();
            int CaptionCSMax = 6;
            int CaptionCCMax = 4;
            int CaptionTXMax = 4;
            boolean isAnalog = channel.isAnalogChannel();
            int CaptionType
                = isAnalog ? ChannelInfo.Subtitle.TYPE_ATV_CC : ChannelInfo.Subtitle.TYPE_DTV_CC;
            int count = 0;
            //stype indicates the source type, where cc comes from
            if (!isAnalog) {
                for (int i = 0; i < CaptionCSMax; i++) {
                    ChannelInfo.Subtitle s
                        = new ChannelInfo.Subtitle(CaptionType,
                                                ChannelInfo.Subtitle.CC_CAPTION_SERVICE1 + i,
                                                CaptionType,
                                                0,
                                                0,
                                                "CS"+(i+1),
                                                count++);
                    SubtitleList.add(s);
                }
            }
            for (int i = 0; i < CaptionCCMax; i++) {
                ChannelInfo.Subtitle s
                    = new ChannelInfo.Subtitle(CaptionType,
                                            ChannelInfo.Subtitle.CC_CAPTION_CC1 + i,
                                            CaptionType,
                                            0,
                                            0,
                                            "CC"+(i+1),
                                            count++);
                SubtitleList.add(s);
            }
            for (int i = 0; i < CaptionTXMax; i++) {
                ChannelInfo.Subtitle s
                    = new ChannelInfo.Subtitle(CaptionType,
                                            ChannelInfo.Subtitle.CC_CAPTION_TEXT1 + i,
                                            CaptionType,
                                            0,
                                            0,
                                            "TX"+(i+1),
                                            count++);
                SubtitleList.add(s);
            }
            return SubtitleList;
        }

        protected List<ChannelInfo.Subtitle> getChannelFixedTeletext(ChannelInfo channel) {
            ArrayList<ChannelInfo.Subtitle> SubtitleList = new ArrayList<ChannelInfo.Subtitle>();
            boolean notNtsc = channel.isAnalogChannel() && !channel.isNtscChannel();
            if (!notNtsc) {
                return SubtitleList;
            }
            ChannelInfo.Subtitle s
                    = new ChannelInfo.Subtitle(ChannelInfo.Subtitle.TYPE_ATV_TELETEXT,
                    0,
                    0,
                    0,
                    0,
                    "ATV_TELETEXT",
                    0);
            SubtitleList.add(s);
            return SubtitleList;
        }

        protected ChannelInfo.Subtitle getExistSubtitleFromList(
                List<ChannelInfo.Subtitle> subtitles, ChannelInfo.Subtitle sub) {

            if (subtitles == null)
                return null;

            Iterator<ChannelInfo.Subtitle> iter = subtitles.iterator();
            while (iter.hasNext()) {
                ChannelInfo.Subtitle s = iter.next();
                if (s.mType == sub.mType && s.mPid == sub.mPid)
                    {
                    Log.e(TAG, "pid "+s.mPid + " id1 "+s.mId1 + " id2 "+s.mId2);
                    return s;
                    }
            }
            return null;
        }

        protected void prepareAtscCaptions(List<ChannelInfo.Subtitle> subtitles, ChannelInfo channel) {
            if (subtitles == null)
                return;

            List<ChannelInfo.Subtitle> fixedSubs = getChannelFixedCaptions(channel);
            List<ChannelInfo.Subtitle> channelSubs = getChannelSubtitles(channel);
            List<ChannelInfo.Subtitle> programSubs = getChannelProgramCaptions(channel);

            if (DEBUG) Log.d(TAG, "cc fixedSubs:"+ (fixedSubs == null? "null" : fixedSubs.size()));
            if (DEBUG) Log.d(TAG, "cc channelSubs:"+(channelSubs == null? "null" : channelSubs.size()));
            if (DEBUG) Log.d(TAG, "cc programSubs:"+(programSubs == null? "null" : programSubs.size()));

            Iterator<ChannelInfo.Subtitle> iter = fixedSubs.iterator();
            while (iter.hasNext()) {
                ChannelInfo.Subtitle s = iter.next();
                ChannelInfo.Subtitle sub = null;

                sub = getExistSubtitleFromList(channelSubs, s);
                if (sub != null)
                    s = new ChannelInfo.Subtitle.Builder(sub).setId(s.id).setLang(s.mLang + "-" +
                        sub.mLang).setId1(sub.mId1).build();
                sub = getExistSubtitleFromList(programSubs, s);
                if (sub != null)
                    s = new ChannelInfo.Subtitle.Builder(sub).setId(s.id).setLang(s.mLang).setId1(sub.mId1).build();
                subtitles.add(s);
            }
            //Add scte27 subtitle
            if (channelSubs != null) {
                iter = channelSubs.iterator();
                while (iter.hasNext()) {
                    ChannelInfo.Subtitle s = iter.next();
                    Log.e(TAG, "channelSub " + s.mType);
                    if (s.mType == ChannelInfo.Subtitle.TYPE_SCTE27_SUB)
                        subtitles.add(s);
                }
            }

            if (programSubs != null) {
                iter = programSubs.iterator();
                while (iter.hasNext()) {
                    ChannelInfo.Subtitle s = iter.next();
                    Log.e(TAG, "programSubs " + s.mType);
                    if (s.mType == ChannelInfo.Subtitle.TYPE_SCTE27_SUB)
                        subtitles.add(s);
                }
            }
        }

        protected void prepareTeleTextCaptions(List<ChannelInfo.Subtitle> subtitles, ChannelInfo channel) {
            if (subtitles == null)
                return;

            List<ChannelInfo.Subtitle> fixedSubs = getChannelFixedTeletext(channel);
            List<ChannelInfo.Subtitle> channelSubs = getChannelSubtitles(channel);
            List<ChannelInfo.Subtitle> programSubs = getChannelProgramCaptions(channel);

            if (DEBUG) Log.d(TAG, "teletext fixedSubs:"+ (fixedSubs == null? "null" : fixedSubs.size()));
            if (DEBUG) Log.d(TAG, "teletext channelSubs:"+(channelSubs == null? "null" : channelSubs.size()));
            if (DEBUG) Log.d(TAG, "teletext programSubs:"+(programSubs == null? "null" : programSubs.size()));

            Iterator<ChannelInfo.Subtitle> iter = fixedSubs.iterator();
            while (iter.hasNext()) {
                ChannelInfo.Subtitle s = iter.next();
                ChannelInfo.Subtitle sub = null;

                sub = getExistSubtitleFromList(channelSubs, s);
                if (sub != null)
                    s = new ChannelInfo.Subtitle.Builder(sub).setId(s.id).setLang(s.mLang + "-" +
                        sub.mLang).setId1(sub.mId1).build();
                sub = getExistSubtitleFromList(programSubs, s);
                if (sub != null)
                    s = new ChannelInfo.Subtitle.Builder(sub).setId(s.id).setLang(s.mLang).setId1(sub.mId1).build();
                subtitles.add(s);
            }
        }

        protected int getAtscCaptionDefault(ChannelInfo channel) {
            if (mCurrentSubtitles == null || mCurrentSubtitles.size() == 0)
                return 0;

            List<ChannelInfo.Subtitle> fixedSubs = getChannelFixedCaptions(channel);
            List<ChannelInfo.Subtitle> channelSubs = getChannelSubtitles(channel);
            List<ChannelInfo.Subtitle> programSubs = getChannelProgramCaptions(channel);

            //defult: cs(exist) > cc(exist) > cs1(fixed) > cc1(fixed) > 1st
            //        event > channel > fixed
            ChannelInfo.Subtitle defaultSub = null;
            Iterator<ChannelInfo.Subtitle> iter = null;

            if (programSubs != null) {
                iter = programSubs.iterator();
                while (iter.hasNext()) {
                    ChannelInfo.Subtitle s = iter.next();
                    if (defaultSub == null
                        || (defaultSub.mStype == ChannelInfo.Subtitle.TYPE_ATV_CC
                            && s.mStype == ChannelInfo.Subtitle.TYPE_DTV_CC )) {
                        defaultSub = s;
                        if (DEBUG) Log.d(TAG, "cc default to pid:"+s.mPid+" in program");
                    }
                }
            }
            if (channelSubs != null) {
                iter = channelSubs.iterator();
                while (iter.hasNext()) {
                    ChannelInfo.Subtitle s = iter.next();
                    if (defaultSub == null
                        || (defaultSub.mStype == ChannelInfo.Subtitle.TYPE_ATV_CC
                            && s.mStype == ChannelInfo.Subtitle.TYPE_DTV_CC )) {
                        defaultSub = s;
                        if (DEBUG) Log.d(TAG, "cc default to pid:"+s.mPid+" in channel");
                    }
                }
            }
            if (fixedSubs != null) {
                iter = fixedSubs.iterator();
                while (iter.hasNext()) {
                    ChannelInfo.Subtitle s = iter.next();
                    if (defaultSub == null
                        || (defaultSub.mStype == ChannelInfo.Subtitle.TYPE_ATV_CC
                            && s.mStype == ChannelInfo.Subtitle.TYPE_DTV_CC )) {
                        defaultSub = s;
                        if (DEBUG) Log.d(TAG, "cc default to pid:"+s.mPid+" in fixed");
                    }
                }
            }
            if (defaultSub == null)
                return 0;

            iter = mCurrentSubtitles.iterator();
            while (iter.hasNext()) {
                ChannelInfo.Subtitle s = iter.next();
                if (s.mPid == defaultSub.mPid
                    && s.mType == defaultSub.mType)
                    return s.id;
            }
            if (DEBUG) Log.d(TAG, "cc FATAL, not found default");
            return 0;
        }
        public boolean isAtscForcedStandard() {
            String forcedStandard = mSystemControlManager.getProperty(DTV_STANDARD_FORCE);
            return TextUtils.equals(forcedStandard, "atsc");
        }

        protected String generateSubtitleIdString(ChannelInfo.Subtitle subtitle) {
            if (subtitle == null)
                return null;

            Map<String, String> map = new HashMap<String, String>();
            map.put("id", String.valueOf(subtitle.id));
            map.put("pid", String.valueOf(subtitle.mPid));
            map.put("type", String.valueOf(subtitle.mType));
            map.put("stype", String.valueOf(subtitle.mStype));
            map.put("uid1", String.valueOf(subtitle.mId1));
            map.put("uid2", String.valueOf(subtitle.mId2));
            map.put("lang", subtitle.mLang);
            return DroidLogicTvUtils.mapToString(map);
        }

        protected ChannelInfo.Subtitle parseSubtitleIdString(String subtitleId) {
            if (subtitleId == null)
                return null;

            Map<String, String> parsedMap = DroidLogicTvUtils.stringToMap(subtitleId);
            return new ChannelInfo.Subtitle(Integer.parseInt(parsedMap.get("type")),
                            Integer.parseInt(parsedMap.get("pid")),
                            Integer.parseInt(parsedMap.get("stype")),
                            Integer.parseInt(parsedMap.get("uid1")),
                            Integer.parseInt(parsedMap.get("uid2")),
                            parsedMap.get("lang"),
                            Integer.parseInt(parsedMap.get("id")));
        }

        protected void prepareSubtitles(List<ChannelInfo.Subtitle> subtitles, ChannelInfo channel) {
            if (channel.isAtscChannel() || channel.isNtscChannel() || isAtscForcedStandard()) {
                prepareAtscCaptions(subtitles, channel);
            } else if (channel.isAnalogChannel() && !channel.isNtscChannel()) {//add teletext track for PAL & SECAM atv
                prepareTeleTextCaptions(subtitles, channel);
            } else {
                List<ChannelInfo.Subtitle> subs = getChannelSubtitles(channel);
                if (subs != null)
                    subtitles.addAll(subs);
            }
        }

        protected String addSubtitleTracks(List <TvTrackInfo> tracks, ChannelInfo ch) {
            if (mCurrentSubtitles == null || mCurrentSubtitles.size() == 0)
                return null;

            if (DEBUG) Log.d(TAG, "add subtitle tracks["+mCurrentSubtitles.size()+"]");

            int auto = (subtitleAutoStart? getSubtitleAuto(ch) : -1);
            Iterator<ChannelInfo.Subtitle> iter = mCurrentSubtitles.iterator();
            while (iter.hasNext()) {
                ChannelInfo.Subtitle s = iter.next();
                String Id = generateSubtitleIdString(s);
                TvTrackInfo SubtitleTrack =
                    new TvTrackInfo.Builder(TvTrackInfo.TYPE_SUBTITLE, Id)
                        .setLanguage(s.mLang)
                        .build();
                tracks.add(SubtitleTrack);

                if (DEBUG) Log.d(TAG, "\t" + ((subtitleAutoStart && (auto == s.id))? ("*"+s.id+":[") : (""+s.id+": [")) + s.mLang + "]"
                    + " [pid:" + s.mPid + "] [type:" + s.mType + "]");
                if (DEBUG) Log.d(TAG, "\t" + "   [id1:" + s.mId1 + "] [id2:" + s.mId2 + "] [stype:" + s.mStype + "]");
            }

            if (auto >= 0 && mCurrentSubtitles.size() > auto)
                return generateSubtitleIdString(mCurrentSubtitles.get(auto));

            return null;
        }

        protected void notifyTracks(ChannelInfo ch) {
            List < TvTrackInfo > tracks = new ArrayList<>();;
            String AudioSelectedId = null;
            String SubSelectedId = null;

            AudioSelectedId = addAudioTracks(tracks, ch);
            SubSelectedId = addSubtitleTracks(tracks, ch);

            if (tracks != null) {
                if (DEBUG) Log.d(TAG, "notify Tracks["+tracks.size()+"]");
                notifyTracksChanged(tracks);
            }

            if (DEBUG) Log.d(TAG, "\tAuto Aud: [" + AudioSelectedId + "]");
            notifyTrackSelected(TvTrackInfo.TYPE_AUDIO, AudioSelectedId);

            if (DEBUG) Log.d(TAG, "\tAuto Sub: [" + SubSelectedId +  "]" + "type " + ch.getSubtitleTypes());
            notifyTrackSelected(TvTrackInfo.TYPE_SUBTITLE, SubSelectedId);
        }

        /*
                Auto rule: channel specified track > default language track > system language > the 1st track > -1
        */
        protected int getAudioAuto(ChannelInfo info) {
            /*keep origin design*/
            boolean useSavedTrack = !mHasPreferLanguageFeature;

            if (mCurrentAudios == null || mCurrentAudios.size() == 0)
                return -1;

            int index = info.getAudioTrackIndex();
            /*off by user*/
            if (index == -2)
                return -2;

            /*if valid*/
            if (useSavedTrack) {
                if (index >= 0 && index < mCurrentAudios.size())
                    return index;
            }

            /*default language track*/
            String firstLanguage = TvMultilingualText.getLocalLang();
            String secondaryLanguage = "none";
            /*prefer language feature*/
            if (info.isAtscChannel()) {
                firstLanguage = mTvControlManager.GetPreferredLanguage(mContext, DroidLogicTvUtils.PREFERRED_ATSC_AUD_DEFAULT, "eng");
                secondaryLanguage =  mTvControlManager.GetPreferredLanguage(mContext, DroidLogicTvUtils.PREFERRED_ATSC_AUD_SECONDARY, "eng");
            } else if (mHasPreferLanguageFeature) {
                firstLanguage = mTvControlManager.GetPreferredLanguage(mContext, DroidLogicTvUtils.PREFERRED_AUD_DEFAULT, null);
                secondaryLanguage =  mTvControlManager.GetPreferredLanguage(mContext, DroidLogicTvUtils.PREFERRED_AUD_SECONDARY, null);
            }
            int firstId = -1;
            int secondId = -1;
            for (ChannelInfo.Audio a : mCurrentAudios) {
                if (!"none".equals(firstLanguage)) {
                    if (TextUtils.equals(a.mLang, firstLanguage)) {
                        firstId = a.id;
                        break;
                    }
                }
                if (!"none".equals(secondaryLanguage)) {
                    if (TextUtils.equals(a.mLang, secondaryLanguage)) {
                    secondId = a.id;
                    }
                }
            }
            if (firstId >= 0)
                return firstId;
            else if (secondId >= 0)
                return secondId;

            /*none match, use the 1st.*/
            return 0;
        }

        protected int getSubtitleAuto(ChannelInfo info) {
            //keep origin logic
            boolean useDynamicCaption = false;
            boolean useLocalLanguage = !mHasPreferLanguageFeature;
            boolean useSavedTrack = !mHasPreferLanguageFeature;

            if (info == null
                || mCurrentSubtitles == null
                || mCurrentSubtitles.size() == 0)
                return -1;

            if ((info.isAtscChannel() || isAtscForcedStandard()) && useDynamicCaption)
                return getAtscCaptionDefault(info);

            if (info.isAtscChannel() || info.isNtscChannel()) {
                int index = CustomerOps.getInstance(mContext).getTvClosedCaptionIndex(info);
                if (index == CustomerOps.INVALID_INDEX) {
                    index = info.getSubtitleTrackIndex();
                }
                /*if valid*/
                if (index >= 0 && index < mCurrentSubtitles.size())
                    return index;
                else
                    return -1;
            } else if (info.isDigitalChannel() && !info.isAtscChannel()) {
                if (useSavedTrack) {
                    int index = info.getSubtitleTrackIndex();
                    if (index >= 0 && index < mCurrentSubtitles.size())
                        return index;
                    else
                        return -1;
                } else {
                    String firstLanguage =
                        mTvControlManager.GetPreferredLanguage(mContext, DroidLogicTvUtils.PREFERRED_SUB_DEFAULT, null);
                    String secondaryLanguage =
                        mTvControlManager.GetPreferredLanguage(mContext, DroidLogicTvUtils.PREFERRED_SUB_SECONDARY, null);
                    if (useLocalLanguage) {
                        firstLanguage = TvMultilingualText.getLocalLang();
                        secondaryLanguage = "none";
                    }
                    int firstId = -1;
                    int secondId = -1;
                    for (ChannelInfo.Subtitle s : mCurrentSubtitles) {
                        if (!"none".equals(firstLanguage)) {
                            if (TextUtils.equals(s.mLang, firstLanguage)) {
                                firstId = s.id;
                                break;
                            }
                        }
                        if (!"none".equals(secondaryLanguage)) {
                            if (TextUtils.equals(s.mLang, secondaryLanguage)) {
                                secondId = s.id;
                            }
                        }
                    }
                    if (firstId >= 0)
                        return firstId;
                    return secondId;
                }
            }

            /*none match, use the 1st.*/
            return 0;
        }

        protected DTVSubtitleView mSubtitleView = null;
        protected int current_cc_pid = -1;

        protected boolean startSubtitle(ChannelInfo channelInfo) {
            int idx = getSubtitleAuto(channelInfo);
            if (mCurrentSubtitles != null && mCurrentSubtitles.size() >  idx && idx >= 0) {
                startSubtitle(mCurrentSubtitles.get(idx), channelInfo.getVfmt());
                mSystemControlManager.setProperty(DTV_SUBTITLE_TRACK_IDX, String.valueOf(idx));
                return true;
            } else if (channelInfo != null
                    && (channelInfo.isAtscChannel() || isAtscForcedStandard())
                    && mSystemControlManager.getPropertyBoolean(DTV_SUBTITLE_DTV_XDS, true)) {
                startSubtitleCCBackground(channelInfo);
                return true;
            } else {
                stopSubtitle();
            }
            return false;
        }

        protected void startSubtitleCCBackground(ChannelInfo channel) {
            if (DEBUG) Log.d(TAG, "start bg cc for xds");
            mCurrentSubtitle = null;
            if (channel == null)
                return;
            startSubtitle(channel.getVfmt(),channel.isAnalogChannel()?
                                ChannelInfo.Subtitle.TYPE_ATV_CC : ChannelInfo.Subtitle.TYPE_DTV_CC,
                            15/*ChannelInfo.Subtitle.CC_CAPTION_XDS*/, 0, 0, 0, "");
            enableSubtitleShow(false);
            mSystemControlManager.setProperty(DTV_SUBTITLE_TRACK_IDX, "-3");
        }

        protected int getTeletextRegionID(String ttxRegionName) {
            final String[] supportedRegions = {"English", "Deutsch", "Svenska/Suomi/Magyar",
                                               "Italiano", "Fran?ais", "Português/Espa?ol",
                                               "Cesky/Slovencina", "Türk?e", "Ellinika", "Alarabia / English" ,
                                               "Russian", "Cyrillic", "Hebrew"
                                              };
            final int[] regionIDMaps = {16, 17, 18, 19, 20, 21, 14, 22, 55 , 64, 36, 32, 80};

            int i;
            for (i = 0; i < supportedRegions.length; i++) {
                if (supportedRegions[i].equals(ttxRegionName))
                    break;
            }

            if (i >= supportedRegions.length) {
                if (DEBUG) Log.d(TAG, "Teletext default region " + ttxRegionName +
                      " not found, using 'English' as default!");
                i = 0;
            }

            if (DEBUG) Log.d(TAG, "Teletext default region id: " + regionIDMaps[i]);
            return regionIDMaps[i];
        }

        public void onSubtitleData(String json) {
            if (DEBUG) Log.d(TAG, "onSubtitleData("+json+")"+
                " channel:("+(mCurrentChannel == null? "null" : mCurrentChannel.getDisplayName())+")");
            if (mCurrentChannel != null) {
                int mask = DroidLogicTvUtils.getObjectValueInt(json, "cc", "data", -1);
                if (mask != -1) {
                    sendCCDataInfoByTif(mask);
                    if (mHandler != null) {
                        Message msg = mHandler.obtainMessage(MSG_CC_DATA, this);
                        msg.arg1 = mask;
                        msg.sendToTarget();
                    }
                    if (DEBUG) Log.d(TAG, "ccc send data");
                    return;
                }

                if (mCurrentChannel.isAtscChannel() || mCurrentChannel.isAnalogChannel() || isAtscForcedStandard()) {
                    mCurrentCCContentRatings = DroidLogicTvUtils.parseARatings(json);
                    saveCurrentChannelRatings();
                    if (mHandler != null)
                        mHandler.sendMessage(mHandler.obtainMessage(MSG_PARENTAL_CONTROL, this));
                }
            }
        }

        public String onReadSysFs(String node) {
            String value = null;
            if (mSystemControlManager != null) {
                value = mSystemControlManager.readSysFs(node);
            }
            return value;
        }

        public void onWriteSysFs(String node, String value) {
            if (mSystemControlManager != null) {
                mSystemControlManager.writeSysFs(node, value);
            }
        }

        public void onStatus(int status) {
            if (status == DTVSubtitleView.TT_NOTIFY_SEARCHING) {
                Toast.makeText(mContext, "Searching teletext", Toast.LENGTH_SHORT).show();
            }else if (status == DTVSubtitleView.TT_NOTIFY_NOSIG) {
                stop_teletext();
                Toast.makeText(mContext, "No teletext", Toast.LENGTH_SHORT).show();
            }
        }

        private void sendCCDataInfoByTif(final int mask) {
            Bundle ratingbundle = new Bundle();
            ratingbundle.putInt(DroidLogicTvUtils.SIG_INFO_CC_DATA_INFO_KEY, mask);
            notifySessionEvent(DroidLogicTvUtils.SIG_INFO_CC_DATA_INFO, ratingbundle);
        }

        protected void onUpdateTsPlay(long mId) {
            if (DEBUG) Log.d(TAG, "onUpdateTsPlay enter");
            if (mHandler != null) {
                Message msg = mHandler.obtainMessage(MSG_UPDATETS_PLAY, this);
                msg.arg1 = (int)mId;
                msg.sendToTarget();
            }
            if (DEBUG) Log.d(TAG, "onUpdateTsPlay exit");
        }

        private int mCurrentCCExist = 0;
        private boolean mCurrentCCEnabled = false;

        public void check_program_pmt_rating_block(int ServiceId, String json)
        {
            if (DEBUG) Log.d(TAG, "check_program_pmt_rating_block cur channel: " + mCurrentChannel.getServiceId() +" PMT ServiceId:" + ServiceId + " ratings:" + json);
            if ((mCurrentChannel != null && mCurrentChannel.isAtscChannel()) || isAtscForcedStandard()) {
                if (DEBUG) Log.d(TAG, "PMT get mCurrentPmtContentRatings");
                mCurrentPmtContentRatings = parseDRatingsT(json, null, "", mCurrentChannel.getUri(), -1, -1);
                if (DEBUG) Log.d(TAG, "PMT save mCurrentPmtContentRatings = " + Program.contentRatingsToString(mCurrentPmtContentRatings));
                saveCurrentChannelRatings();
                if (mHandler != null)
                    mHandler.sendMessage(mHandler.obtainMessage(MSG_PARENTAL_CONTROL, this));
            }
        }
        /*When CC data changed.*/
        public void doCCData(int mask) {
            if (DEBUG) Log.d(TAG, "cc data " + mask);

            /*Check XDS data*/
            if ((mask != (1 << 16)) && (mask & (1 << 15)) == 0) {
                if (mCurrentCCContentRatings != null) {
                    mCurrentCCContentRatings = null;
                    saveCurrentChannelRatings();
                    checkContentBlockNeeded(mCurrentChannel);
                }
            }
            /*Check CC show*/
//            mCurrentCCExist = mask;
//            if (mSystemControlManager != null)
//                mSystemControlManager.setProperty(DTV_SUBTITLE_CAPTION_EXIST, String.valueOf(mCurrentCCExist));

//            if (mHandler != null) {
//                mHandler.removeMessages(MSG_CC_TRY_PREFERRED);
//                mHandler.obtainMessage(MSG_CC_TRY_PREFERRED, mCurrentCCExist, 0, this).sendToTarget();
//            }
        }

        private static final int DELAY_TRY_PREFER_CC = 2000;

        protected void tryPreferredSubtitleContinue(int exist) {
            synchronized (mSubtitleLock) {
                if ((tryPreferredSubtitle(exist) == -1) /* && (mCurrentSubtitle == null)*/) {
                    startSubtitleCCBackground(mCurrentChannel);
                    notifyTrackSelected(TvTrackInfo.TYPE_SUBTITLE, null);
                }
                if (mHandler != null) {
                    mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_CC_TRY_PREFERRED, mCurrentCCExist, 0, this),
                                DELAY_TRY_PREFER_CC);
                }
            }
        }

        /*
            try to show cc according to user preferred
            return [cc channel id/-1] if current will be changed
                    or
                   [0] if nothing changed
        */
        protected int tryPreferredSubtitle(int exist) {
            if (mSystemControlManager != null) {
                int ccPrefer = mSystemControlManager.getPropertyInt(DTV_SUBTITLE_CC_PREFER, -1);
                int csPrefer = mSystemControlManager.getPropertyInt(DTV_SUBTITLE_CS_PREFER, -1);
                int curr = mCurrentSubtitle == null? -1 : mCurrentSubtitle.mPid;
                int to = -1;

                Log.e(TAG, "Prefer mCurrentSubtitle " + mCurrentSubtitle);

                if (csPrefer == -1 && ccPrefer == -1)
                    return -1;

                if (curr != ccPrefer && curr != csPrefer && curr != -1)
                    return -1;

                if ((to != csPrefer) && ((exist & (1 << csPrefer)) != 0) && !mCurrentChannel.isAnalogChannel()) {
                    to = csPrefer;
                } else if ((to != ccPrefer) && ((exist & (1 << ccPrefer)) != 0)) {
                    to = ccPrefer;
                }

                if (DEBUG) Log.d(TAG, "ccc tryPrefer, exist["+exist+"], current["+curr+"] to["+to+"] prefer[cc:"+ccPrefer+" cs:"+csPrefer+"] Enable["+mCurrentCCEnabled+"]" + "mCurrentSubtitle" + mCurrentSubtitle);

                if ((exist & (1 << csPrefer)) == 0 && (exist & (1 << ccPrefer)) == 0)
                    return 0;

                if ((to == -1) && (curr == -1)) {
                    if (mCurrentChannel != null) {
                        if (!mCurrentChannel.isAnalogChannel() && (csPrefer != -1))
                            to = csPrefer;
                        if ((to == -1) && (ccPrefer != -1))
                            to = ccPrefer;
                    }
                }

                if (curr == to)//already show
                    return 0;

                if (to != -1) {
                    for (ChannelInfo.Subtitle s : mCurrentSubtitles) {
                        if (s.mPid == to) {
                            startSubtitle(s, mCurrentChannel.getVfmt());
                            notifyTrackSelected(TvTrackInfo.TYPE_SUBTITLE, generateSubtitleIdString(s));
                            break;
                        }
                    }
                }
                return to;
            }
            return 0;
        }

        private TvContentRating[] parseMultiRatings(String jsonString, String channelname, String programtitle) {
            if (jsonString == null || jsonString.isEmpty() || mRrt5DataBaseManager == null)
                return null;
            //Log.d(TAG, "[parseMultiRatings] channelname = " + channelname + ", programtitle = " + programtitle + ", jsonString = " + jsonString);

            ArrayList<TvContentRating> RatingList = new ArrayList<TvContentRating>();
            int rrt5count = 0;
            int noneRrt5Count = 0;
            JSONArray regionArray;
            JSONObject obj = null;
            try {
                obj = new JSONObject(jsonString);
            } catch (JSONException e) {
                return null;
            }

            try {
                regionArray = obj.getJSONArray("Dratings");
            } catch (JSONException e) {
                return null;
            }

            int ArraySize = regionArray.length();
            for (int i = 0; i < regionArray.length(); i++) {
                JSONObject g = regionArray.optJSONObject(i);
                if (g == null)
                    continue;
                int region = g.optInt("g", -1);
                if (region > 2) {//region > 2, search download rrt
                    rrt5count++;
                } else {
                    noneRrt5Count ++;
                    continue;
                }

                JSONArray ratings = g.optJSONArray("rx");
                if (ratings != null) {
                    for (int j = 0; j < ratings.length(); j++) {
                        JSONObject ratingValues = ratings.optJSONObject(j);
                        int dimension = ratingValues.optInt("d", -1);
                        int value = ratingValues.optInt("r", -1);
                        //Log.d(TAG, "[parseMultiRatings] channelname = " + channelname + ", programtitle = " + programtitle + ", dimension:" + dimension + ",rating_value:" + value);
                        if (dimension == -1 || value == -1)
                            continue;
                        if (mRrt5DataBaseManager != null && mCurrentChannel != null) {
                            String[] rrtResult = mRrt5DataBaseManager.getRrt5Rating(mCurrentChannel.getMajorChannelNumber(), dimension, value);
                            if (rrtResult[0] == null || rrtResult[1] == null || rrtResult[2] == null)
                                continue;
                            TvContentRating r = TvContentRating.createRating(mRrt5DataBaseManager.getRrt5Domain(),
                                                                             rrtResult[1], rrtResult[2]);
                            if (r != null) {
                                RatingList.add(r);
                                Log.d(TAG, "[parseMultiRatings] channelname = " + channelname + ", programtitle = " + programtitle + ", add rating:" + r.flattenToString());
                            }
                        }
                    }
                }
            }

            TvContentRating ratings_vchip[] = null;
            if (noneRrt5Count > 0)
                ratings_vchip = DroidLogicTvUtils.parseDRatings(jsonString);
            TvContentRating ratings_rrt5[] = (RatingList.size() == 0 ? null : RatingList.toArray(new TvContentRating[RatingList.size()]));
            TvContentRating ratings_all[];
            if (ratings_vchip != null && ratings_vchip.length > 0 && RatingList != null && RatingList.size() > 0) {
                ratings_all = new TvContentRating[RatingList.size() + ratings_vchip.length];
                for (int k = 0; k < ratings_vchip.length; k++) {
                    ratings_all[k] = ratings_vchip[k];
                }
                for (int k = ratings_vchip.length; k < ratings_all.length; k++) {
                    ratings_all[k] = RatingList.get(k - ratings_vchip.length);
                }
            } else if (ratings_vchip != null && ratings_vchip.length > 0) {
                ratings_all = ratings_vchip;
            } else if (RatingList != null && RatingList.size() > 0) {
                ratings_all = new TvContentRating[RatingList.size()];
                for (int k = 0; k < ratings_all.length; k++) {
                    ratings_all[k] = RatingList.get(k);
                }
            } else {
                ratings_all = null;
            }

            return ratings_all;
        }

        private class CCStyleParams {
             protected int fg_color;
             protected int fg_opacity;
             protected int bg_color;
             protected int bg_opacity;
             protected int font_style;
             protected float font_size;

             public CCStyleParams(int fg_color, int fg_opacity,
                                int bg_color, int bg_opacity, int font_style, float font_size) {
                 this.fg_color = fg_color;
                 this.fg_opacity = fg_opacity;
                 this.bg_color = bg_color;
                 this.bg_opacity = bg_opacity;
                 this.font_style = font_style;
                 this.font_size = font_size;
             }
        }

        protected int getDbTeletextRegionID() {
            int region_id;
            region_id = DataProviderManager.getIntValue(mContext, mSubtitleView.TT_REGION_DB, getTeletextRegionID("English"));
            Log.e(TAG, "region_id in db " + region_id);
            return region_id;
        }

        protected void setSubtitleParam(int vfmt, int type, int pid, int stype, int id1, int id2, String lang) {
            if (type == ChannelInfo.Subtitle.TYPE_DVB_SUBTITLE) {
                DTVSubtitleView.DVBSubParams params =
                    new DTVSubtitleView.DVBSubParams(0, pid, id1, id2, player_instance_id, sync_instance_id);
                mSubtitleView.setSubParams(params);

            } else if (type == ChannelInfo.Subtitle.TYPE_DTV_TELETEXT) {
                //int pgno;
                //pgno = (id1 == 0) ? 800 : id1 * 100;
                //pgno += (id2 & 15) + ((id2 >> 4) & 15) * 10 + ((id2 >> 8) & 15) * 100;
                DTVSubtitleView.DTVTTParams params =
                    new DTVSubtitleView.DTVTTParams(0, pid, id1, id2, getDbTeletextRegionID(), type, stype, player_instance_id, sync_instance_id);
                mSubtitleView.setSubParams(params);

            } else if (type == ChannelInfo.Subtitle.TYPE_ATV_TELETEXT) {
//                int pgno;
//                pgno = (id1 == 0) ? 800 : id1 * 100;
//                pgno += (id2 & 15) + ((id2 >> 4) & 15) * 10 + ((id2 >> 8) & 15) * 100;
                DTVSubtitleView.AtvTeleTextParams params =
                        new DTVSubtitleView.AtvTeleTextParams(100, 0x3F7F, getDbTeletextRegionID(), getSessionId(), id1);
//                DTVSubtitleView.AtvTeleTextParams params =
//                        new DTVSubtitleView.AtvTeleTextParams(pgno, 0x3F7F, getDbTeletextRegionID());
                mSubtitleView.setSubParams(params);
            } else if (type == ChannelInfo.Subtitle.TYPE_DTV_CC) {
                CCStyleParams ccParam = getCaptionStyle();
                DTVSubtitleView.DTVCCParams params =
                    new DTVSubtitleView.DTVCCParams(vfmt, pid,
                        id1, lang,
                        ccParam.fg_color,
                        ccParam.fg_opacity,
                        ccParam.bg_color,
                        ccParam.bg_opacity,
                        ccParam.font_style,
                        ccParam.font_size,
                        player_instance_id,
                        sync_instance_id);
                mSubtitleView.setSubParams(params);
                mSubtitleView.setMargin(225, 128, 225, 128);
                if (DEBUG) Log.d(TAG, "DTV CC pid="+pid+" dp "+id1+" lang "+lang+ ",fg_color="+ccParam.fg_color+", fg_op="+ccParam.fg_opacity+", bg_color="+ccParam.bg_color+", bg_op="+ccParam.bg_opacity);

            } else if (type == ChannelInfo.Subtitle.TYPE_ATV_CC) {
                CCStyleParams ccParam = getCaptionStyle();
                DTVSubtitleView.ATVCCParams params =
                    new DTVSubtitleView.ATVCCParams(pid, id1, lang,
                        ccParam.fg_color,
                        ccParam.fg_opacity,
                        ccParam.bg_color,
                        ccParam.bg_opacity,
                        ccParam.font_style,
                        ccParam.font_size);

                mSubtitleView.setSubParams(params);
                mSubtitleView.setMargin(225, 128, 225, 128);
                if (DEBUG) Log.d(TAG, "ATV CC pid="+pid+" dp "+id1+ " lang "+lang+",fg_color="+ccParam.fg_color+", fg_op="+ccParam.fg_opacity+", bg_color="+ccParam.bg_color+", bg_op="+ccParam.bg_opacity);
            } else if (type == ChannelInfo.Subtitle.TYPE_DTV_TELETEXT_IMG) {
                //int pgno;
                //pgno = (id1 == 0) ? 800 : id1 * 100;
                //pgno += (id2 & 15) + ((id2 >> 4) & 15) * 10 + ((id2 >> 8) & 15) * 100;
                DTVSubtitleView.DTVTTParams params =
                        new DTVSubtitleView.DTVTTParams(0, pid, id1, id2, getDbTeletextRegionID(), type, stype, player_instance_id, sync_instance_id);
                mSubtitleView.setSubParams(params);
            } else if (type == ChannelInfo.Subtitle.TYPE_ISDB_SUB) {
                DTVSubtitleView.ISDBParams params = new DTVSubtitleView.ISDBParams(0, pid, 0);
                mSubtitleView.setSubParams(params);
            } else if (type == ChannelInfo.Subtitle.TYPE_SCTE27_SUB) {
                DTVSubtitleView.Scte27Params params = new DTVSubtitleView.Scte27Params(0, pid);
                mSubtitleView.setSubParams(params);
            }
        }

        protected int getColor(int color)
        {
        switch (color)
            {
                case 0xFFFFFF:
                    return DTV_COLOR_WHITE;
                case 0x0:
                    return DTV_COLOR_BLACK;
                case 0xFF0000:
                    return DTV_COLOR_RED;
                case 0x00FF00:
                    return DTV_COLOR_GREEN;
                case 0x0000FF:
                    return DTV_COLOR_BLUE;
                case 0xFFFF00:
                    return DTV_COLOR_YELLOW;
                case 0xFF00FF:
                    return DTV_COLOR_MAGENTA;
                case 0x00FFFF:
                    return DTV_COLOR_CYAN;
            }
            return DTV_COLOR_WHITE;
        }
        protected int getOpacity(int opacity)
        {
            if (DEBUG) Log.d(TAG, ">> opacity:"+Integer.toHexString(opacity));
            switch (opacity)
            {
                case 0:
                    return DTV_OPACITY_TRANSPARENT;
                case 0x80000000:
                    return DTV_OPACITY_TRANSLUCENT;
                case 0xFF000000:
                    return DTV_OPACITY_SOLID;
            }
            return DTV_OPACITY_TRANSPARENT;
        }
        protected float getFontSize(float textSize) {
            if (0 <= textSize && textSize < .375) {
                return 1.0f;//AM_CC_FONTSIZE_SMALL
            } else if (textSize < .75) {
                return 1.0f;//AM_CC_FONTSIZE_SMALL
            } else if (textSize < 1.25) {
                return 2.0f;//AM_CC_FONTSIZE_DEFAULT
            } else if (textSize < 1.75) {
                return 3.0f;//AM_CC_FONTSIZE_BIG
            } else if (textSize < 2.5) {
                return 4.0f;//AM_CC_FONTSIZE_MAX
            }else {
                return 2.0f;//AM_CC_FONTSIZE_DEFAULT
            }
        }

        protected void convertStyle() {
        }

        private int getRawUserStyle(){
            //TODO
            /*try {
                Class clazz = ClassLoader.getSystemClassLoader().loadClass("android.view.accessibility.CaptioningManager");
                Method method = clazz.getMethod("getUserStyle");
                Object objInt = method.invoke(clazz);
                return Integer.parseInt(String.valueOf(objInt));
            } catch (Exception e) {
                    // TODO Auto-generated catch block
                e.printStackTrace();
            }*/
            return -1;
        }

        private String getRawTypeface(CaptioningManager.CaptionStyle captionstyle) {
            //TODO
            /*try {
                Class<?> cls = Class.forName("android.view.accessibility.CaptioningManager.CaptionStyle");
                Object obj = cls.newInstance();
                obj = captionstyle;
                Field rawTypeface = cls.getDeclaredField("mRawTypeface");
                return rawTypeface.get(obj).toString();
            } catch(Exception e) {
                e.printStackTrace();
            }*/
            return null;
        }

        protected CCStyleParams getCaptionStyle()
        {
            boolean USE_NEW_CCVIEW = true;

            CCStyleParams params;

            String[] typeface = getResources().getStringArray(R.array.captioning_typeface_selector_values);

            /*
             * Gets CC paramsters by CaptioningManager.
             */
            CaptioningManager.CaptionStyle userStyle = mCaptioningManager.getUserStyle();

            int style = getRawUserStyle();
            float textSize = mCaptioningManager.getFontScale();
            int fg_color = userStyle.foregroundColor & 0x00ffffff;
            int fg_opacity = userStyle.foregroundColor & 0xff000000;
            int bg_color = userStyle.backgroundColor & 0x00ffffff;
            int bg_opacity = userStyle.backgroundColor & 0xff000000;
            int fontStyle = DTVSubtitleView.CC_FONTSTYLE_DEFAULT;

            for (int i = 0; i < typeface.length; ++i) {
                if (typeface[i].equals(getRawTypeface(userStyle))) {
                    fontStyle = i;
                    break;
                }
            }
            if (DEBUG) Log.d(TAG, "get style: " + style + ", fontStyle" + fontStyle + ", typeface: " + getRawTypeface(userStyle));

            int fg = userStyle.foregroundColor;
            int bg = userStyle.backgroundColor;

            int convert_fg_color = USE_NEW_CCVIEW? fg_color : getColor(fg_color);
            int convert_fg_opacity = USE_NEW_CCVIEW? fg_opacity : getOpacity(fg_opacity);
            int convert_bg_color = USE_NEW_CCVIEW? bg_color : getColor(bg_color);
            int convert_bg_opacity = USE_NEW_CCVIEW? bg_opacity : getOpacity(bg_opacity);
            float convert_font_size = USE_NEW_CCVIEW? textSize: getFontSize(textSize);
            if (DEBUG) Log.d(TAG, "Caption font size:"+convert_font_size+" ,fg_color:"+Integer.toHexString(fg)+
                ", fg_opacity:"+Integer.toHexString(fg_opacity)+
                " ,bg_color:"+Integer.toHexString(bg)+", @fg_color:"+convert_fg_color+", @bg_color:"+
                convert_bg_color+", @fg_opacity:"+convert_fg_opacity+", @bg_opacity:"+convert_bg_opacity);

            switch (style)
            {
                case DTV_CC_STYLE_WHITE_ON_BLACK:
                    convert_fg_color = USE_NEW_CCVIEW? Color.WHITE : DTV_COLOR_WHITE;
                    convert_fg_opacity = USE_NEW_CCVIEW? Color.BLACK : DTV_OPACITY_SOLID;
                    convert_bg_color = USE_NEW_CCVIEW? Color.BLACK : DTV_COLOR_BLACK;
                    convert_bg_opacity = USE_NEW_CCVIEW? Color.BLACK : DTV_OPACITY_SOLID;
                    break;

                case DTV_CC_STYLE_BLACK_ON_WHITE:
                    convert_fg_color = USE_NEW_CCVIEW? Color.BLACK : DTV_COLOR_BLACK;
                    convert_fg_opacity = USE_NEW_CCVIEW? Color.BLACK : DTV_OPACITY_SOLID;
                    convert_bg_color = USE_NEW_CCVIEW? Color.WHITE : DTV_COLOR_WHITE;
                    convert_bg_opacity = USE_NEW_CCVIEW? Color.BLACK : DTV_OPACITY_SOLID;
                    break;

                case DTV_CC_STYLE_YELLOW_ON_BLACK:
                    convert_fg_color = USE_NEW_CCVIEW? Color.YELLOW : DTV_COLOR_YELLOW;
                    convert_fg_opacity = USE_NEW_CCVIEW? Color.BLACK : DTV_OPACITY_SOLID;
                    convert_bg_color = USE_NEW_CCVIEW? Color.BLACK : DTV_COLOR_BLACK;
                    convert_bg_opacity = USE_NEW_CCVIEW? Color.BLACK : DTV_OPACITY_SOLID;
                    break;

                case DTV_CC_STYLE_YELLOW_ON_BLUE:
                    convert_fg_color = USE_NEW_CCVIEW? Color.YELLOW : DTV_COLOR_YELLOW;
                    convert_fg_opacity = USE_NEW_CCVIEW? Color.BLACK : DTV_OPACITY_SOLID;
                    convert_bg_color = USE_NEW_CCVIEW? Color.BLUE : DTV_COLOR_BLUE;
                    convert_bg_opacity = USE_NEW_CCVIEW? Color.BLUE : DTV_OPACITY_SOLID;
                    break;

                case DTV_CC_STYLE_USE_DEFAULT:
                    convert_fg_color = USE_NEW_CCVIEW? Color.WHITE : DTVSubtitleView.CC_COLOR_DEFAULT;
                    convert_fg_opacity = USE_NEW_CCVIEW? Color.BLACK : DTVSubtitleView.CC_OPACITY_DEFAULT;
                    convert_bg_color = USE_NEW_CCVIEW? Color.BLACK : DTVSubtitleView.CC_COLOR_DEFAULT;
                    convert_bg_opacity = USE_NEW_CCVIEW? Color.BLACK : DTVSubtitleView.CC_OPACITY_DEFAULT;
                    break;

                case DTV_CC_STYLE_USE_CUSTOM:
                    break;
            }
            params = new CCStyleParams(convert_fg_color,
                convert_fg_opacity,
                convert_bg_color,
                convert_bg_opacity,
                fontStyle,
                convert_font_size);

            return params;
        }

        protected void startSubtitle(int vfmt, int type, int pid, int stype, int id1, int id2, String lang) {
            synchronized (mSubtitleLock) {
                if (DEBUG) Log.d(TAG, "start Subtitle [" + type + " " + pid + " " + stype + " " + id1 + " " + id2 + "]");
                if (mSubtitleView == null) {
                    if (DEBUG) Log.d(TAG, "subtitle view is null");
                    return;
                }
                //parameter pid will not be -1
                if (current_cc_pid == pid) {
                    if (DEBUG) Log.d(TAG, "Pid " + pid + " is playing now, no need to change");
                    return;
                }

                current_cc_pid = pid;

                setSubtitleParam(vfmt, type, pid, stype, id1, id2, lang);

                mSubtitleView.setActive(true);
                mSubtitleView.startSub();
            }
        }

        protected void startSubtitle(ChannelInfo.Subtitle subtitle, int vfmt) {
            mCurrentSubtitle = subtitle;
            if (subtitle != null) {
                enableSubtitleShow(true);
                startSubtitle(vfmt, subtitle.mType, subtitle.mPid, subtitle.mStype, subtitle.mId1, subtitle.mId2,
                subtitle.mLang);
            }
        }

        protected void stopSubtitle(boolean stopRetry) {
            if (DEBUG) Log.d(TAG, "stop Subtitle[stopRetry:"+stopRetry+"],session:"+this);

            synchronized (mSubtitleLock) {

                if (stopRetry && mHandler != null)
                    mHandler.removeMessages(MSG_CC_TRY_PREFERRED);

                if (pal_teletext == true)
                    stop_teletext();

                mCurrentSubtitle = null;

                if (mSubtitleView != null) {
                    enableSubtitleShow(false);
                    mSubtitleView.stop();
                    sendSessionMessage(MSG_SUBTITLE_HIDE);
                }
                mSystemControlManager.setProperty(DTV_SUBTITLE_TRACK_IDX, "-1");
            }
        }

        protected void stopSubtitle() {
            current_cc_pid = -1;
            stopSubtitle(true);
        }

        protected void stopSubtitleBlock(ChannelInfo channel) {
            if ((channel.isAtscChannel() || isAtscForcedStandard())
                && mSystemControlManager.getPropertyBoolean(DTV_SUBTITLE_DTV_XDS, true))
                startSubtitleCCBackground(channel);
            else
                stopSubtitle();
        }

        protected void stopSubtitleUser(ChannelInfo channel) {
            stopSubtitleBlock(channel);
        }

        protected void enableSubtitleShow(boolean enable) {
            is_subtitle_enabled = enable;
            if (mSubtitleView != null) {
                mSubtitleView.setVisible(enable);
                if (enable)
                    mSubtitleView.show();
                else
                    mSubtitleView.hide();
            }
            is_subtitle_enabled = enable;
            sendSessionMessage(enable ? MSG_SUBTITLE_SHOW : MSG_SUBTITLE_HIDE);
        }

        protected void startAudioADByMain(ChannelInfo channelInfo, int mainAudioTrackIndex) {

            if (!audioADAutoStart || !hasDollbyAudioAD(channelInfo))
                return ;

            int[] idxs = DroidLogicTvUtils.getAudioADTracks(channelInfo, mainAudioTrackIndex);
            if (DEBUG) Log.d(TAG, "startAudioAD mainAudioTrackIndex["+mainAudioTrackIndex+"], AudioADTrackIndex["+Arrays.toString(idxs)+"]");

            startAudioAD(channelInfo, ((idxs == null)? -1 : idxs[0]));
        }

        protected void startAudioADMainMix(ChannelInfo channelInfo, int mainAudioTrackIndex) {
            //get realtime mix level
            mAudioADMixingLevel = DataProviderManager.getIntValue(mContext, DroidLogicTvUtils.TV_KEY_AD_MIX, AD_MIXING_LEVEL_DEF);
            audioADAutoStart = (DataProviderManager.getIntValue(mContext, DroidLogicTvUtils.TV_KEY_AD_SWITCH, 0) != 0);
            mAudioADVolume = DataProviderManager.getIntValue(mContext, "ad_volume", AD_VOLUME_DEF);
            AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            if (DEBUG) Log.d(TAG, "startAudioADMainMix audioADAutoStart = " + audioADAutoStart + ", mAudioADMixingLevel = " + mAudioADMixingLevel);
            if (audioADAutoStart) {
                //handleAdtvAudioEvent(AudioSystemCmdManager.AUDIO_SERVICE_CMD_AD_DUAL_SUPPORT, 1, 0);
                audioManager.setParameters("ad_switch_enable=" + 1);
            } else {
                //handleAdtvAudioEvent(AudioSystemCmdManager.AUDIO_SERVICE_CMD_AD_DUAL_SUPPORT, 0, 0);
                //handleAdtvAudioEvent(AudioSystemCmdManager.AUDIO_SERVICE_CMD_AD_MIX_SUPPORT, 0, 0);
                audioManager.setParameters("ad_switch_enable=" + 0);
                if (DEBUG) Log.d(TAG, "startAudioADMainMix no need to mix");
            }

            if (!audioADAutoStart || channelInfo == null || mainAudioTrackIndex < 0)
                return ;

            int[] adList = DroidLogicTvUtils.getAudioADTracks(mCurrentChannel, 0);//get first ad
            int firstAdTrackIndex = -1;
            int findMainTrackIndex = -1;
            int findAdTrackIndex = -1;
            boolean needMixAdMain = false;
            if (adList != null && adList.length > 0 && adList[0] > 0) {
                firstAdTrackIndex = adList[0];
            }
            ChannelInfo.Audio main = null;
            ChannelInfo.Audio ad = null;
            if (DEBUG) Log.d(TAG, "startAudioADMainMix firstAdTrackIndex = " + firstAdTrackIndex + ", mainAudioTrackIndex = " + mainAudioTrackIndex);
            if (firstAdTrackIndex > 0) {//has ad audio
                if (mainAudioTrackIndex < firstAdTrackIndex) {//main audio index
                    findMainTrackIndex = mainAudioTrackIndex;
                    int[] adList1 = DroidLogicTvUtils.getAudioADTracks(mCurrentChannel, findMainTrackIndex);
                    if (adList1 != null && adList1.length > 0) {
                        findAdTrackIndex = adList1[0];
                    }
                } else {//ad audio index
                    findAdTrackIndex = mainAudioTrackIndex;
                    findMainTrackIndex = findAdTrackIndex - firstAdTrackIndex;
                }
                if (mCurrentAudios != null) {
                    Iterator<ChannelInfo.Audio> iter = mCurrentAudios.iterator();
                    while (iter.hasNext()) {
                        ChannelInfo.Audio a = iter.next();
                        if (a != null && a.id == findMainTrackIndex) {
                            main = a;
                            if (DEBUG) Log.d(TAG, " startAudioADMainMix find main audio");
                        } else if (a != null && a.id == findAdTrackIndex) {
                            ad = a;
                            if (DEBUG) Log.d(TAG, " startAudioADMainMix find ad audio");
                        }
                    }
                }
            }
            if (main != null && ad != null) {
                if (findAdTrackIndex == mainAudioTrackIndex) {
                    //handleAdtvAudioEvent(AudioSystemCmdManager.AUDIO_SERVICE_CMD_AD_MIX_SUPPORT, 1, 0);
                    //handleAdtvAudioEvent(AudioSystemCmdManager.AUDIO_SERVICE_CMD_AD_MIX_LEVEL, mAudioADMixingLevel, 0);
                } else {
                    //handleAdtvAudioEvent(AudioSystemCmdManager.AUDIO_SERVICE_CMD_AD_MIX_SUPPORT, 0, 0);
                }
                startAudioAD(channelInfo, findAdTrackIndex);
                mTvControlManager.DtvSwitchAudioTrack(main.mPid, main.mFormat, 0);
                mHandler.obtainMessage(MSG_MIX_AD_MAIN, 1, 0).sendToTarget();
                notifyTrackSelected(TvTrackInfo.TYPE_AUDIO, generateAudioIdString(ad));
                mSystemControlManager.setProperty(DTV_AUDIO_TRACK_ID, generateAudioIdString(ad));
                if (mCurrentChannel != null) {
                    if (DEBUG) Log.d(TAG, "audioAutoSave: idx=" + ad.id);
                    mCurrentChannel.setAudioTrackIndex(ad.id);
                    mTvDataBaseManager.updateSingleChannelInternalProviderData(mCurrentChannel.getId(),
                            ChannelInfo.KEY_AUDIO_TRACK_INDEX, String.valueOf(ad.id));
                }

                if (DEBUG) Log.d(TAG, " startAudioADMainMix find ad and main audio");
            } else {
                //handleAdtvAudioEvent(AudioSystemCmdManager.AUDIO_SERVICE_CMD_AD_MIX_SUPPORT, 0, 0);
                if (DEBUG) Log.d(TAG, " startAudioADMainMix not find ad and main audio");
            }
        }

        protected ChannelInfo.Audio findMainAudioTrack() {
            int[] adList = DroidLogicTvUtils.getAudioADTracks(mCurrentChannel, 0);//get first ad
            int firstAdTrackIndex = -1;
            int findMainTrackIndex = -1;
            if (adList != null && adList.length > 0 && adList[0] > 0) {
                firstAdTrackIndex = adList[0];
            }
            if (getAudioAuto(mCurrentChannel) < firstAdTrackIndex) {
                return null;
            }
            if (firstAdTrackIndex > 0) {
                findMainTrackIndex = getAudioAuto(mCurrentChannel) - firstAdTrackIndex;
            }
            ChannelInfo.Audio main = null;
            if (mCurrentAudios != null) {
                Iterator<ChannelInfo.Audio> iter = mCurrentAudios.iterator();
                while (iter.hasNext()) {
                    ChannelInfo.Audio a = iter.next();
                    if (a != null && a.id == findMainTrackIndex) {
                        main = a;
                        if (DEBUG) Log.d(TAG, " findMainAudioTrack find main audio");
                    }
                }
            }
            return main;
        }

        protected boolean hasDollbyAudioAD(ChannelInfo channelInfo) {
            boolean result = false;
            if (channelInfo == null) {
                return result;
            }
            int[] adList = DroidLogicTvUtils.getAudioADTracks(channelInfo, 0);//get first ad
            int audioformats[] = channelInfo.getAudioFormats();
            if (adList != null && adList.length > 0 && adList[0] > 0) {
                if (audioformats != null && audioformats.length > 0) {
                    for (int temp : audioformats) {
                        if (temp == 3 || temp == 21) {//ac3 3, eac3 21
                            result = true;
                            break;
                        }
                    }
                }
            }
            return result;
        }

        protected void startAudioAD(ChannelInfo channelInfo, int adAudioTrackIndex) {
            if (DEBUG) Log.d(TAG, "startAudioAD idx["+adAudioTrackIndex+"]");
            if (adAudioTrackIndex >= 0 && adAudioTrackIndex < channelInfo.getAudioPids().length) {
                mTvControlManager.DtvSetAudioAD(1,
                                channelInfo.getAudioPids()[adAudioTrackIndex],
                                channelInfo.getAudioFormats()[adAudioTrackIndex]);
                mSystemControlManager.setProperty(DTV_AUDIO_AD_TRACK_IDX, String.valueOf(adAudioTrackIndex));
            } else {
                stopAudioAD();
            }
        }

        protected void stopAudioAD() {
            if (DEBUG) Log.d(TAG, "stopAudioAD");
            mTvControlManager.DtvSetAudioAD(0, 0, 0);
            mSystemControlManager.setProperty(DTV_AUDIO_AD_TRACK_IDX, "-1");
        }

        protected DTVMonitorCurrentProgramRunnable mMonitorCurrentProgramRunnable;

        protected static final int MONITOR_FEND = 0;
        protected static final int MONITOR_DMX = 1;
        protected int MONITOR_MODE = DTVMonitor.MODE_UPDATE_SERVICE
                                        | DTVMonitor.MODE_UPDATE_EPG
                                        | DTVMonitor.MODE_UPDATE_TIME
                                        | DTVMonitor.MODE_UPDATE_TS;
        protected static final String EPG_LANGUAGE = "local eng zho chi chs first";
        protected static final String DEF_CODING = "GB2312";//"standard";//force setting for auto-detect fail.

        protected class DTVMonitorCurrentProgramRunnable implements Runnable {
            private final ChannelInfo mChannel;

            public DTVMonitorCurrentProgramRunnable(ChannelInfo channel) {
                mChannel = channel;
            }

            @Override
            public void run() {
                synchronized (mLock) {
                    if (DEBUG) Log.d(TAG, "monitor ch: " + mChannel.getDisplayNumber() + "-" + mChannel.getDisplayName());
                    String standard = (mChannel.isAtscChannel() ? "atsc" : "dvb");
                    String forceStandard = mSystemControlManager.getProperty(DTV_STANDARD_FORCE);
                    Log.d(TAG, "std:"+standard + " forcestd:"+forceStandard);
                    if (forceStandard != null && forceStandard.length() != 0)
                        standard = forceStandard;
                    if (mDTVMonitor != null && !mDTVMonitor.getStandard().equals(standard)) {
                        mDTVMonitor.destroy();
                        mDTVMonitor = null;
                    }
                    int forceMode = mSystemControlManager.getPropertyInt(DTV_MONITOR_MODE_FORCE, MONITOR_MODE);
                    if (DEBUG) Log.d(TAG, "monitor:"+MONITOR_MODE+" force:"+forceMode);
                    MONITOR_MODE = forceMode;
                    if (mDTVMonitor == null) {
                        mDTVMonitor = new DTVMonitor(mContext, getInputId(), DEF_CODING, MONITOR_MODE, standard);
                        mDTVMonitor.reset(MONITOR_FEND, MONITOR_DMX,
                                  new TvControlManager.TvMode(mChannel.getType()).getBase(),
                                  EPG_LANGUAGE.replaceAll("local", TvMultilingualText.getLocalLang()));
                    }

                    mDTVMonitor.enterChannel(getTVChannelParams(mChannel), false);
                    mDTVMonitor.enterService(mChannel);

                    mDTVMonitor.setEpgAutoReset(true);
                }
            }
        }

        protected TvChannelParams getTVChannelParams(ChannelInfo channel) {
            TvChannelParams params = null;
            String type = channel.getType();
            if (TextUtils.equals(type, TvContract.Channels.TYPE_DTMB))
                params = TvChannelParams.dtmbParams(channel.getFrequency(), channel.getBandwidth());
            else if (TextUtils.equals(type, TvContract.Channels.TYPE_DVB_C))
                params = TvChannelParams.dvbcParams(channel.getFrequency(), channel.getModulation(), channel.getSymbolRate());
            else if (TextUtils.equals(type, TvContract.Channels.TYPE_DVB_T))
                params = TvChannelParams.dvbtParams(channel.getFrequency(), channel.getBandwidth());
            /*else if (TextUtils.equals(type, TvContract.Channels.TYPE_DVB_S))
                params = TvChannelParams.dvbsParams(channel.getFrequency(), channel.getModulation(), channel.getSymbolRate());*/
            else if (TextUtils.equals(type, TvContract.Channels.TYPE_DVB_C2))
                params = TvChannelParams.dvbcParams(channel.getFrequency(), channel.getModulation(), channel.getSymbolRate());
            else if (TextUtils.equals(type, TvContract.Channels.TYPE_DVB_T2))
                params = TvChannelParams.dvbt2Params(channel.getFrequency(), channel.getBandwidth());
            /*else if (TextUtils.equals(type, TvContract.Channels.TYPE_DVB_S2))
                params = TvChannelParams.dvbsParams(channel.getFrequency(), channel.getModulation(), channel.getSymbolRate());*/
            else if (TextUtils.equals(type, TvContract.Channels.TYPE_ATSC_T)
                ||TextUtils.equals(type, TvContract.Channels.TYPE_ATSC_C))
                params = TvChannelParams.atscParams(channel.getFrequency(), channel.getModulation());
            else if (TextUtils.equals(type, TvContract.Channels.TYPE_ISDB_T))
                params = TvChannelParams.isdbtParams(channel.getFrequency(), channel.getBandwidth());
           return params;
        }

        protected void setMonitor(ChannelInfo channel) {
            synchronized (mLock) {
                if (mHandler != null)
                    mHandler.removeCallbacks(mMonitorCurrentProgramRunnable);
                if (channel != null) {
                    if (DEBUG) Log.d(TAG, "startMonitor");
                    if (mHandler != null) {
                        mMonitorCurrentProgramRunnable = new DTVMonitorCurrentProgramRunnable(channel);
                        mHandler.post(mMonitorCurrentProgramRunnable);
                    }
                } else {
                    if (DEBUG) Log.d(TAG, "stopMonitor");
                    if (mDTVMonitor != null) {
                        mDTVMonitor.destroy();
                        mDTVMonitor = null;
                    } else
                        if (DEBUG) Log.d(TAG, "monitor is null");
                }
            }
        }

        protected void restartMonitorTime() {
            synchronized (mLock) {
                if (DEBUG) Log.d(TAG, "restartMonitorTime");
                if (mDTVMonitor != null)
                    mDTVMonitor.restartMonitorTime();
            }
        }

        public String getDateAndTime(long dateTime) {
            SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            sDateFormat.setTimeZone(TimeZone.getDefault());
            return sDateFormat.format(new Date(dateTime + 0));
        }



        public class DTVMonitor {
            public static final String TAG = "DTVMonitor";

            public static final String STD_DVB = "dvb";
            public static final String STD_ATSC = "atsc";

            public static final int MODE_UPDATE_EPG = 1;
            public static final int MODE_UPDATE_SERVICE = 2;
            public static final int MODE_UPDATE_TS = 4;
            public static final int MODE_UPDATE_TIME = 8;

            private static final int MSG_MONITOR_EVENT = 1000;
            private static final int MSG_MONITOR_RESCAN_SERVICE = 2000;
            private static final int MSG_MONITOR_RESCAN_TIME = 3000;
            private static final int MSG_MONITOR_EVENT_CLEAR = 4000;
            private static final int MSG_MONITOR_FLUSH_PROGRAMS = 5000;
            private static final int MSG_MONITOR_UPDATE_EPG = 6000;

            private static final int AUTO_RESCAN_NONE = 0;
            private static final int AUTO_RESCAN_ONCE = 1;
            private static final int AUTO_RESCAN_CONTINUOUS = 2;
            /*
                Rescan service info periodically @AUTO_RESCAN_INTERVAL for data changing,
                due to only version-triggered at low-level
            */
            private int auto_rescan_service = AUTO_RESCAN_CONTINUOUS;

            private static final int AUTO_RESCAN_SERVICE_INTERVAL = 2000;//2s

            private int auto_rescan_time = AUTO_RESCAN_CONTINUOUS;

            private static final int AUTO_RESCAN_TIME_INTERVAL = 5000;//5s

            /*Retune current uri if necessary*/
            private boolean auto_retune_service = true;

            private HandlerThread mMonitorHandlerThread;
            private Handler mMonitorHandler;
            private Context mContext;
            private String mInputId;
            private int mMode;
            private String mStandard;
            private DTVEpgScanner epgScanner;
            private TvChannelParams tvchan = null;
            private ChannelInfo tvservice = null;
            private TvDataBaseManager mTvDataBaseManager = null;
            private boolean isAlive =  false;

            private ChannelObserver mChannelObserver;
            private CCStyleObserver mCCObserver;
            private ArrayList<ChannelInfo> channelMap;
            private long maxChannel_ID = 0;

            private int mUpdateFrequency = 0;
            private int fend = 0;
            private int dmx = 0;
            private int src = 0;
            private String[] languages = null;

            private MonitorStoreManager mMonitorStoreManager;

            private int MODE_Epg = 0;
            private int MODE_Service = 0;
            private int MODE_Time = 0;
            private int MODE_Ts = 0;

            private String mVct = null;
            private Map<Integer, Long> mVctMap = null;
            private int mFrequency = 0;
            private ArrayList<DTVEpgScanner.Event> mEpgeventQueue = new ArrayList<DTVEpgScanner.Event>();
            private ArrayList<DTVEpgScanner.Event> mEpgeventBuffer = new ArrayList<DTVEpgScanner.Event>();

            private long mTDTTime = 0;


            private void buildVct () {
                if (mVct == null || channelMap == null) {
                    if (DEBUG) Log.d(TAG, "build VCT map vct null:");
                    return;
                } else {
                    //if (DEBUG) Log.d(TAG, "build VCT map vct :" + mVct);
                }
                String items[] = mVct.split(",");
                if (items == null) {
                    if (DEBUG) Log.d(TAG, "build VCT map items null");
                    return;
                }

                mVctMap = new HashMap<Integer, Long>();
                for (String item : items) {
                    ChannelInfo chan = null;
                    int srcId, major, minor;
                    int p1, p2;

                    p1 = item.indexOf(':');
                    if (p1 == -1)
                        continue;
                    p2 = item.indexOf('-', p1);
                    if (p2 == -1)
                        continue;
                    //if (DEBUG) Log.d(TAG, "build VCT map id :"+item.substring(0, p1)+" maj:" + item.substring(p1 + 1, p2)+" min:" + item.substring(p2 + 1));
                    srcId = Integer.parseInt(item.substring(0, p1));
                    major = Integer.parseInt(item.substring(p1 + 1, p2));
                    minor = Integer.parseInt(item.substring(p2 + 1));

                    for (ChannelInfo c : channelMap) {
                        if ((c.getMajorChannelNumber() == major) && (c.getMinorChannelNumber() == minor)) {
                            if (chan == null || c.getFrequency() == mFrequency)
                                chan = c;
                        }
                    }

                    if (chan == null)
                        continue;

                    mVctMap.put(srcId, chan.getId());
                }
                if (DEBUG) Log.d(TAG, "build VCT map end");
            }

            public DTVMonitor(Context context, String inputId, String coding, int mode, String standard) {
                isAlive = true;
                mContext = context;
                mInputId = inputId;
                mMode = mode;
                mStandard = standard;
                mTvDataBaseManager = new TvDataBaseManager(mContext);

                int channel_number_start = mSystemControlManager.getPropertyInt(DTV_CHANNEL_NUMBER_START, 1);
                mMonitorStoreManager = new MonitorStoreManager(mInputId, channel_number_start);

                if (mCurrentChannel != null) {
                    mVct = mCurrentChannel.getVct();
                    mFrequency = mCurrentChannel.getFrequency();
                }

                mMonitorHandlerThread = new HandlerThread(getClass().getSimpleName());
                mMonitorHandlerThread.start();
                mMonitorHandler = new Handler(mMonitorHandlerThread.getLooper()) {
                    @Override
                    public void handleMessage(Message msg) {
                        if (!isAlive) {
                            Log.e(TAG, "DTVMonitor is destroyed, exit mMonitorHandler handleMessage");
                            return;
                        }
                        switch (msg.what) {
                            case MSG_MONITOR_EVENT:
                                resolveMonitorEvent((DTVEpgScanner.Event)msg.obj);
                                break;
                            case MSG_MONITOR_RESCAN_SERVICE:
                                rescanService(true);
                                break;
                            case MSG_MONITOR_RESCAN_TIME:
                                rescanTime(true);
                                break;
                            case MSG_MONITOR_EVENT_CLEAR:
                                clearChannelProgram((ChannelInfo)msg.obj);
                                break;
                            case MSG_MONITOR_FLUSH_PROGRAMS:
                                if (mTvDataBaseManager != null)
                                    mTvDataBaseManager.flushPrograms();
                                    mContext.getContentResolver().notifyChange(TvContract.Programs.CONTENT_URI,null);
                                break;
                            case MSG_MONITOR_UPDATE_EPG:
                                synchronized(mEpgQueueLock) {
                                    if (mEpgeventQueue != null && mEpgeventQueue.size() > 0) {
                                        if (mEpgeventBuffer != null)
                                            mEpgeventBuffer.addAll(mEpgeventQueue);
                                        mEpgeventQueue.clear();
                                    }
                                }
                                if (mEpgeventBuffer != null && mEpgeventBuffer.size() > 0) {
                                    updateEpgevents(mEpgeventBuffer);
                                    mEpgeventBuffer.clear();
                                }
                                sendEmptyMessageDelayed(MSG_MONITOR_UPDATE_EPG, 200);
                        }
                    }
                };
                mMonitorHandler.sendEmptyMessageDelayed(MSG_MONITOR_UPDATE_EPG, 200);

                int scannerMode = 0;
                if (STD_DVB.equals(standard)) {
                    MODE_Epg = DTVEpgScanner.SCAN_EIT_ALL;
                    MODE_Service = DTVEpgScanner.SCAN_SDT | DTVEpgScanner.SCAN_PAT | DTVEpgScanner.SCAN_PMT;
                    MODE_Time = DTVEpgScanner.SCAN_TDT;
                    MODE_Ts = DTVEpgScanner.SCAN_NIT;
                } else {// (std == ATSC) {
                    MODE_Epg = DTVEpgScanner.SCAN_PSIP_EIT_ALL;
                    MODE_Service = DTVEpgScanner.SCAN_PAT | DTVEpgScanner.SCAN_VCT | DTVEpgScanner.SCAN_PMT;
                    MODE_Time = DTVEpgScanner.SCAN_STT;
                    MODE_Ts = DTVEpgScanner.SCAN_MGT;
                }

                if ((mode & MODE_UPDATE_EPG) == MODE_UPDATE_EPG)
                    scannerMode |= MODE_Epg;
                if ((mode & MODE_UPDATE_SERVICE) == MODE_UPDATE_SERVICE)
                    scannerMode |= MODE_Service;
                if ((mode & MODE_UPDATE_TS) == MODE_UPDATE_TS)
                    scannerMode |= MODE_Ts;
                if ((mode & MODE_UPDATE_TIME) == MODE_UPDATE_TIME)
                    scannerMode |= MODE_Time;

                if (DEBUG) Log.d(TAG, "DTVMonitor std["+standard+"] mode["+scannerMode+"]");

                synchronized(mEpgQueueLock) {
                    if (mEpgeventQueue != null)
                        mEpgeventQueue.clear();
                }
                epgScanner = new DTVEpgScanner(scannerMode) {
                    public void onEvent(DTVEpgScanner.Event event) {
                        if (event.type == DTVEpgScanner.Event.EVENT_TDT_END) {
                            if (DEBUG) Log.d(TAG, "[Time Update]:" + event.time);
                            if (DEBUG) Log.d(TAG, "[Time]:"+getDateAndTime(event.time*1000));

                            if (mTDTTime == event.time) {
                                return;
                            }

                            mTDTTime = event.time;
                            if ((mTvTime.getTime()/1000 - event.time) > 30) {
                                Log.e(TAG, "stream replay, tdt time " + event.time + " now time " + mTvTime.getTime()/1000);
                                //when the stream back, it would trigger this event.
                                //and the the subtitle server will take a long time to restart subtitle
                                //stopSubtitle();
                                //if (mCurrentChannel != null)
                                //    tryStartSubtitle(mCurrentChannel);
                            }

                            if (mTvTime != null) setTime(event.time * 1000);
                        } else if (event.type == DTVEpgScanner.Event.EVENT_PROGRAM_EVENTS_UPDATE) {
                            synchronized(mEpgQueueLock) {
                                if (mEpgeventQueue != null)
                                    mEpgeventQueue.add(event);
                            }
                        } else {
                            if (mMonitorHandler != null)
                                mMonitorHandler.obtainMessage(MSG_MONITOR_EVENT, event).sendToTarget();
                        }
                    }
                };
                epgScanner.setDvbTextCoding(coding);

                if ((mode & MODE_UPDATE_EPG) == MODE_UPDATE_EPG) {
                    if (mChannelObserver == null)
                        mChannelObserver = new ChannelObserver();
                    mContext.getContentResolver().registerContentObserver(TvContract.Channels.CONTENT_URI, true, mChannelObserver);
                }

                if (mCCObserver == null) {
                    if (DEBUG) Log.d(TAG, "new cc style observer");
                    if (DEBUG) Log.d(TAG, "CONTENT_URI: "+TvContract.Channels.CONTENT_URI);
                    mCCObserver = new CCStyleObserver();
                    mCaptioningManager.addCaptioningChangeListener((CaptioningChangeListener) mCCObserver);
                }
            }

            private void setTime(long time){
                Date sys = new Date();
                long diff = time - sys.getTime();
                if (DEBUG) Log.d(TAG, "setTime vendor.sys.tv.stream.realtime = " + String.valueOf(diff));
                mSystemControlManager.setProperty("vendor.sys.tv.stream.realtime", String.valueOf(diff));
            }

            private void clearChannelProgram(ChannelInfo channel) {
                if (mTvDataBaseManager != null && channel != null) {
                    int count = mTvDataBaseManager.deleteProgram(channel);
                    if (DEBUG) Log.d(TAG, "epg delete "+count+" programs");
                }
            }

            private void refreshChannelMap() {
                channelMap = mTvDataBaseManager.getChannelList(mInputId, ChannelInfo.COMMON_PROJECTION, null, null);
                if (channelMap != null) {
                    for (ChannelInfo c : channelMap)
                        if (c.getId() > maxChannel_ID)
                            maxChannel_ID = c.getId();
                }
                if (DEBUG) Log.d(TAG, "channelMap changed. max_ID:" + maxChannel_ID);
            }

            public void reset(int fend, int dmx, int src, String textLanguages) {
                if (DEBUG) Log.d(TAG, "monitor reset all.");
                if ((mMode & MODE_UPDATE_EPG) == MODE_UPDATE_EPG)
                    refreshChannelMap();

                synchronized (this) {
                    if (epgScanner == null)
                        return;

                    epgScanner.setSource(fend, dmx, src, textLanguages);
                    languages = textLanguages.split(" ");

                    this.fend = fend;
                    this.dmx = dmx;
                    this.src = src;
                }
            }

            public void reset() {
                if (DEBUG) Log.d(TAG, "monitor reset.");

                if (epgScanner == null) {
                    if (DEBUG) Log.d(TAG, "monitor may exit, ignore.");
                    return;
                }

                reset(fend, dmx, src, EPG_LANGUAGE.replaceAll("local", TvMultilingualText.getLocalLang()));

                enterChannel(tvchan, true);
                enterService(tvservice);
            }

            public void destroy() {
                isAlive = false;
                setEpgAutoReset(false);

                if (mChannelObserver != null) {
                    mContext.getContentResolver().unregisterContentObserver(mChannelObserver);
                    mChannelObserver = null;
                }

                if (mCCObserver != null) {
                    mCaptioningManager.removeCaptioningChangeListener((CaptioningChangeListener) mCCObserver);
                    mCCObserver = null;
                }

                if (mMonitorHandler != null) {/*take care of rescan befor epgScanner=null*/
                    mMonitorHandler.removeMessages(MSG_MONITOR_RESCAN_SERVICE);
                    mMonitorHandler.removeMessages(MSG_MONITOR_RESCAN_TIME);
                    mMonitorHandler.removeMessages(MSG_MONITOR_UPDATE_EPG);
                }
                synchronized (this) {
                    mTvControlManager.setStorDBListener(null);
                    //mTvControlManager.DtvStopScan();

                    if (epgScanner != null) {
                        epgScanner.destroy();
                        epgScanner = null;
                        if (mEpgeventQueue != null) {
                            mEpgeventQueue.clear();
                        }
                    }
                    if (mMonitorHandler != null) {
                        mMonitorHandler.removeCallbacksAndMessages(null);
                        mMonitorHandler = null;
                    }

                    if (mMonitorHandlerThread != null) {
                        mMonitorHandlerThread.quit();
                        mMonitorHandlerThread = null;
                    }
                    if (mMonitorStoreManager != null)
                        mMonitorStoreManager.releaseHandlerThread();

                    //mTvDataBaseManager = null;
                    //mTvTime = null;
                }
            }

            public void enterChannel(TvChannelParams chan, boolean force) {
                synchronized (this) {
                    enterChannelLocked(chan, force);
                }
            }

            public void enterService(ChannelInfo channel) {
                synchronized (this) {
                    enterServiceLocked(channel);
                }
            }

            private void enterChannelLocked(TvChannelParams chan, boolean force) {
                    if (epgScanner == null)
                        return;
                    if (chan == null)
                        epgScanner.leaveChannel();
                    else if ((tvchan == null) || !tvchan.equals(chan) || force)
                        epgScanner.enterChannel();
                    tvchan = chan;
            }

            private void enterServiceLocked(ChannelInfo channel) {
                    if (epgScanner == null)
                        return;
                    if (channel == null) {
                        rescanTime(false);
                        rescanService(false);
                        epgScanner.leaveProgram();
                    } else {
                        epgScanner.enterProgram(channel);
                        rescanService(true);
                        rescanTime(true);
                    }
                    tvservice = channel;
            }

            public void rescanService(boolean on) {
                auto_rescan_service =
                    mSystemControlManager.getPropertyInt(DTV_AUTO_RESCAN_SERVICE, AUTO_RESCAN_CONTINUOUS);

                if (auto_rescan_service == AUTO_RESCAN_NONE)
                    return;

                if (DEBUG) Log.d(TAG, "rescanService:" + on);
                if (!isAlive) {
                    Log.e(TAG, "DTVMonitor is destroyed, exit rescanService");
                    return;
                }
                synchronized (this) {
                    if (mMonitorHandler != null) {
                        mMonitorHandler.removeMessages(MSG_MONITOR_RESCAN_SERVICE);
                        if (on && (auto_rescan_service == AUTO_RESCAN_CONTINUOUS)) {
                            if (DEBUG) Log.d(TAG, "rescanServiceLater");
                            mMonitorHandler.sendEmptyMessageDelayed(MSG_MONITOR_RESCAN_SERVICE, AUTO_RESCAN_SERVICE_INTERVAL);
                        }
                    }
                    if (on) {
                        if (tvservice != null && epgScanner != null) {
                            if (DEBUG) Log.d(TAG, "rescanService["+MODE_Service+"]"+" rescanTs["+MODE_Ts+"]");
                            epgScanner.stopScan(MODE_Service|MODE_Ts);
                            epgScanner.startScan(MODE_Service|MODE_Ts);
                        }
                    }
                }
            }

            public void rescanTime(boolean on) {
                auto_rescan_time =
                    mSystemControlManager.getPropertyInt(DTV_AUTO_RESCAN_TIME, AUTO_RESCAN_CONTINUOUS);

                if (auto_rescan_time == AUTO_RESCAN_NONE)
                    return;

                if (DEBUG) Log.d(TAG, "rescanTime:" + on);
                if (!isAlive) {
                    Log.e(TAG, "DTVMonitor is destroyed, exit rescanTime");
                    return;
                }
                synchronized (this) {
                    if (mMonitorHandler != null) {
                        mMonitorHandler.removeMessages(MSG_MONITOR_RESCAN_TIME);
                        if (on && (auto_rescan_time == AUTO_RESCAN_CONTINUOUS)) {
                            if (DEBUG) Log.d(TAG, "rescanTimeLater");
                            mMonitorHandler.sendEmptyMessageDelayed(MSG_MONITOR_RESCAN_TIME, AUTO_RESCAN_TIME_INTERVAL);
                        }
                    }
                    if (on) {
                        if (DEBUG) Log.d(TAG, "rescanTime["+MODE_Time+"]");
                        //epgScanner.stopScan(MODE_Time);
                        //epgScanner.startScan(MODE_Time);
                    }
                }
            }

            public void restartMonitorTime(){
                if (DEBUG) Log.d(TAG, "restartMonitorTime["+MODE_Time+"]");
                if (!isAlive) {
                    if (DEBUG) Log.e(TAG, "DTVMonitor is destroyed, exit restartMonitorTime");
                    return;
                }
                synchronized (this) {
                    if (epgScanner == null) {
                        if (DEBUG) Log.d(TAG, "monitor may exit, ignore.");
                        return;
                    }
                    //epgScanner.stopScan(MODE_Time);
                    //epgScanner.startScan(MODE_Time);
                }
            }

            private boolean isAtscEvent(DTVEpgScanner.Event.Evt evt) {
                return (evt.source_id != -1);
            }

            private List<Program> getChannelPrograms(Uri channelUri, ChannelInfo channel,
                    DTVEpgScanner.Event event) {
                List<Program> programs = new ArrayList<>();
                for (DTVEpgScanner.Event.Evt evt : event.evts) {
                    if (isAtscEvent(evt)) {//atsc
                        if (channel.getSourceId() == evt.srv_id) {
                            //if (DEBUG) Log.d(TAG, "evt srv_id:"+evt.srv_id+" channel src_id:"+channel.getSourceId()+",rrt_ratings:"+evt.rrt_ratings);
                            try {
                                long start = evt.start;
                                long end = evt.end;
                                Program p = new Program.Builder()
                                    .setProgramId(evt.evt_id)
                                    .setChannelId(ContentUris.parseId(channelUri))
                                    .setTitle(TvMultilingualText.getText((evt.name == null ? null : new String(evt.name)), languages))
                                    .setDescription(TvMultilingualText.getText((evt.ext_descr == null ? null : new String(evt.ext_descr)), languages))
                                    .setContentRatings(evt.rrt_ratings == null ? null : parseMultiRatings(new String(evt.rrt_ratings), channel.getDisplayNumber(), (evt.name == null ? null : new String(evt.name))))
                                    //.setContentRatings(evt.rrt_ratings == null ? null : DroidLogicTvUtils.parseDRatings(new String(evt.rrt_ratings)))
                                    //.setCanonicalGenres(programInfo.genres)
                                    //.setPosterArtUri(programInfo.posterArtUri)
                                    .setInternalProviderData(evt.rrt_ratings == null ? null : new String(evt.rrt_ratings))
                                    .setStartTimeUtcMillis(start * 1000)
                                    .setEndTimeUtcMillis(end * 1000)
                                    .build();
                                programs.add(p);

                                if (DEBUG) Log.d(TAG, "epg: sid[" + evt.srv_id + "]"
                                      + "eid[" + evt.evt_id + "]"
                                      + "{" + p.getTitle() + "}"
                                      + "[" + (p.getStartTimeUtcMillis() == 0 ? 0 : p.getStartTimeUtcMillis() / 1000)
                                      + "-" + (p.getEndTimeUtcMillis() == 0 ? 0 : p.getEndTimeUtcMillis() / 1000) + "]"
                                      + " R["+ Program.contentRatingsToString(p.getContentRatings())
                                      + "]");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        if ((channel.getTransportStreamId() == evt.ts_id)
                            && (channel.getServiceId() == evt.srv_id)
                            && (channel.getOriginalNetworkId() == evt.net_id)) {
                            try {
                                long start = evt.start;
                                long end = evt.end;
                                String ext_descr = TvMultilingualText.getText((evt.ext_descr == null ? null : new String(evt.ext_descr)), languages);
                                Program p = new Program.Builder()
                                    .setProgramId(evt.evt_id)
                                    .setChannelId(ContentUris.parseId(channelUri))
                                    .setTitle(TvMultilingualText.getText(new String(evt.name), languages))
                                    .setDescription(TvMultilingualText.getText(new String(evt.desc), languages) + (ext_descr == null ? "" : ext_descr))
                                    //.setContentRatings(evt.parental_rating == 0 ? null : parseParentalRatings(evt.parental_rating,(evt.name == null ? null : new String(evt.name))))
                                    //.setCanonicalGenres(programInfo.genres)
                                    //.setPosterArtUri(programInfo.posterArtUri)
                                    //.setInternalProviderData(TvContractUtils.convertVideoInfoToInternalProviderData(
                                    //        programInfo.videoType, programInfo.videoUrl))
                                    .setStartTimeUtcMillis(start * 1000)
                                    .setEndTimeUtcMillis(end * 1000)
                                    .build();
                                programs.add(p);
                                if (DEBUG)
                                    Log.d(TAG, "epg: sid/net/ts[" + evt.srv_id + "/" + evt.net_id + "/" + evt.ts_id + "]"
                                          + "{" + p.getTitle() + "}"
                                          + "[" + p.getStartTimeUtcMillis() / 1000
                                          + "-" + p.getEndTimeUtcMillis() / 1000
                                          + " R["+ Program.contentRatingsToString(p.getContentRatings())
                                          + "]");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                return programs;
            }

            /*Update ATSC programs*/
            private void updateAtscPrograms(List<DTVEpgScanner.Event> epglist) {
                if (!isAlive) {
                    Log.e(TAG, "DTVMonitor is destroyed, exit updateAtscPrograms");
                    return;
                }

                if (epglist == null || epglist.size() == 0)
                    return;

                if (channelMap == null)
                    return;

                if (mVctMap == null) {
                    buildVct();
                    if (mVctMap == null) {
                        return;
                    }
                }

                for (ChannelInfo c : channelMap) {
                    if (c == null) {
                        continue;
                    }
                    Uri channelUri = TvContract.buildChannelUri(c.getId());
                    List<Program> programs = new ArrayList<>();
                    //if (DEBUG) Log.d(TAG," program name :"+c.getDisplayNumber());

                    for (DTVEpgScanner.Event event : epglist) {
                        for (DTVEpgScanner.Event.Evt evt : event.evts) {
                            Long cid = mVctMap.get(evt.source_id);
                            //if (DEBUG) Log.d(TAG, "build VCT map cid " + cid + " c.getId():" + c.getId() + " evt.srv_id" + evt.srv_id + " evt.v:(" + evt.sub_flag + ":" + evt.sub_status +")"+" source_id:"+evt.source_id);
                            if (cid == null)
                                continue;
                            if (c.getId() == cid) {
                                try {
                                    long start = evt.start;
                                    long end = evt.end;
                                    //if (start != 0 && end !=0 && evt.rrt_ratings == null)
                                    //    throw new Exception("Receive EIT data,but rating is NULL!!!");
                                    Program p = new Program.Builder()
                                        .setProgramId(evt.evt_id)
                                        .setChannelId(cid)
                                        .setTitle(TvMultilingualText.getText((evt.name == null ? null : new String(evt.name)), languages))
                                        .setDescription(TvMultilingualText.getText((evt.ext_descr == null ? null : new String(evt.ext_descr)), languages))
                                        .setContentRatings(evt.rrt_ratings == null ? null : parseMultiRatings(new String(evt.rrt_ratings), c.getDisplayNumber(), (evt.name == null ? null : new String(evt.name))))
                                        //.setContentRatings(evt.rrt_ratings == null ? null : DroidLogicTvUtils.parseDRatings(new String(evt.rrt_ratings)))
                                        //.setCanonicalGenres(programInfo.genres)
                                        //.setPosterArtUri(programInfo.posterArtUri)
                                        .setInternalProviderData(evt.rrt_ratings == null ? null : new String(evt.rrt_ratings))
                                        .setStartTimeUtcMillis(start * 1000)
                                        .setEndTimeUtcMillis(end * 1000)
                                        .setVersion(String.valueOf(evt.sub_flag))
                                        .setEitExt(String.valueOf(evt.sub_status))
                                        .setPackageName(c.getPackageName())
                                        .build();
                                    boolean isPexist = false;
                                    for (Program oldProgram : programs) {
                                        if (oldProgram.equals(p)) {
                                            isPexist = true;
                                            break;
                                        }
                                    }
                                    if (isPexist) {
                                        continue;
                                    }
                                    programs.add(p);
                                    if (DEBUG) Log.v(TAG, "epg: sid[" + evt.srv_id + "]"
                                          + "eid[" + evt.evt_id + "]"
                                          + "ver[" + evt.sub_flag + ":" +evt.sub_status + "]"
                                          + "{" + p.getTitle() + "}"
                                          + "{" + p.getDescription() + "}"
                                          + "[" + (p.getStartTimeUtcMillis() == 0 ? 0 : p.getStartTimeUtcMillis() / 1000)
                                          + "-" + (p.getEndTimeUtcMillis() == 0 ? 0 : p.getEndTimeUtcMillis() / 1000) + "]"
                                          + "R["+ Program.contentRatingsToString(p.getContentRatings()) +"]"
                                          + "p[" + p.getPackageName() + "]");
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }

                    synchronized(mEpgUpdateLock) {
                        if (mTvDataBaseManager != null && programs.size() != 0) {
                            boolean updated = mTvDataBaseManager.updatePrograms(channelUri, c.getId(), programs, mTvTime.getTime(), true);
                            if (updated && mCurrentChannel != null && mCurrentChannel.getId() == c.getId()) {
                                if (DEBUG) Log.d(TAG, "epg eit, program updated for current");
                                checkCurrentContentBlockNeeded();
                            }
                            //break;
                        }
                    }
                }
                synchronized (this) {
                    if (mMonitorHandler != null) {
                         mMonitorHandler.removeMessages(MSG_MONITOR_FLUSH_PROGRAMS);
                         mMonitorHandler.sendMessageDelayed(mMonitorHandler.obtainMessage(MSG_MONITOR_FLUSH_PROGRAMS, null), 200);
                    }
                }
            }

            private void updateAtscChannelInfo(DTVEpgScanner.Event event) {
                synchronized (this) {
                    if (mTvDataBaseManager == null)
                        return;
                    ArrayList<ChannelInfo> channelList =
                        mTvDataBaseManager.getChannelList(mInputId, ChannelInfo.COMMON_PROJECTION,
                            TvContract.Channels.COLUMN_SERVICE_ID+"=? and "+
                            TvContract.Channels.COLUMN_DISPLAY_NUMBER+"=?",
                            new String[]{
                                "" + event.channel.getServiceId(),
                                "" + event.channel.getMajorChannelNumber() +
                                "-" + event.channel.getMinorChannelNumber()
                            });
                    for (ChannelInfo co : channelList) {
                        co.setDisplayName(TvMultilingualText.getText(event.channel.getDisplayName()));
                        co.setDisplayNameMulti(event.channel.getDisplayName());
                        mTvDataBaseManager.updateChannelInfo(co);
                    }
                }
            }

            private void updateDvbChannelInfo(DTVEpgScanner.Event event) {
                Log.d(TAG, "[NAME Update]: current: ONID:"+event.channel.getOriginalNetworkId()
                    +" Version:"+event.channel.getSdtVersion());
                Log.d(TAG, "\t[Service]: id:"+event.channel.getServiceId() + " name:"+event.channel.getDisplayName());
                Log.d(TAG, "[NAME Update]: ONID:"+event.services.mNetworkId
                    +" TSID:"+event.services.mTSId
                    +" Version:"+event.services.mVersion);
                for (DTVEpgScanner.Event.ServiceInfosFromSDT.ServiceInfoFromSDT s : event.services.mServices) {
                    Log.d(TAG, "\t[Service]: id:" + s.mId + " type:"+s.mType + " name:"+s.mName);
                    Log.d(TAG, "\t           running:"+s.mRunning + " freeCA:"+s.mFreeCA);
                }
                synchronized (this) {
                    if (mTvDataBaseManager == null)
                        return;
                    ArrayList<ChannelInfo> channelList =
                        mTvDataBaseManager.getChannelList(mInputId, ChannelInfo.COMMON_PROJECTION,
                            TvContract.Channels.COLUMN_ORIGINAL_NETWORK_ID+"=? and "+
                            TvContract.Channels.COLUMN_TRANSPORT_STREAM_ID+"=?",
                            new String[]{
                                /*
                                 * If sdt timeout in scanning, onid should be -1.
                                 * Risk exsits if sdt timeout more then once in autoscan,
                                 * TSId+SId may not identify a service without ONId.
                                 * */
                                "-1",
                                /*String.valueOf(event.services.mNetworkId),*/
                                String.valueOf(event.services.mTSId)
                            });
                    int count = 0;
                    for (ChannelInfo co : channelList) {
                        for (DTVEpgScanner.Event.ServiceInfosFromSDT.ServiceInfoFromSDT sn : event.services.mServices) {
                            if (co.getServiceId() == sn.mId) {
                                co.setOriginalNetworkId(event.services.mNetworkId);
                                co.setDisplayName(TvMultilingualText.getText(sn.mName));
                                co.setDisplayNameMulti(sn.mName);
                                co.setSdtVersion(event.services.mVersion);
                                mTvDataBaseManager.updateChannelInfo(co);
                                count = count + 1;
                            }
                        }
                    }
                    if (DEBUG) Log.d(TAG, "found ["+event.services.mServices.size()+"] services in SDT.");
                    if (DEBUG) Log.d(TAG, "update ["+count+"] services in DB.");
                }
            }

            private void updateEpgevents(List<DTVEpgScanner.Event> epglist) {
                if (epglist == null || epglist.size() == 0)
                    return;

                DTVEpgScanner.Event firstevent = epglist.get(0);
                if (isAtscEvent(firstevent.evts[0])) {
                    updateAtscPrograms(epglist);
                } else {
                    for (int i = 0; (channelMap != null && i < channelMap.size()); ++i) {
                        if (!isAlive) {
                            Log.e(TAG, "DTVMonitor is destroyed, exit EVENT_PROGRAM_EVENTS_UPDATE");
                            return;
                        }
                        ChannelInfo channel = (ChannelInfo)channelMap.get(i);
                        Uri channelUri = TvContract.buildChannelUri(channel.getId());

                        List<Program> channel_programs = new ArrayList<Program>();
                        for (DTVEpgScanner.Event event : epglist) {
                            List<Program> programs = getChannelPrograms(channelUri, channel, event);
                            channel_programs.addAll(programs);
                        }
                        synchronized(mEpgUpdateLock) {
                            if (mTvDataBaseManager != null && channel_programs.size() != 0)
                                mTvDataBaseManager.updatePrograms(channelUri, channel.getId(), channel_programs, null, false);
                        }
                    }
                    if (mMonitorHandler != null) {
                        mMonitorHandler.sendMessageDelayed(mMonitorHandler.obtainMessage(MSG_MONITOR_FLUSH_PROGRAMS, null), 200);
                    }
                }
            }

            private void resolveMonitorEvent(DTVEpgScanner.Event event) {
                //if (DEBUG) Log.d(TAG, "Monitor event: " + event.type + " this:" +this);
                switch (event.type) {
                    case DTVEpgScanner.Event.EVENT_EIT_CHANGED: {
                        int[] mEitVersions = mCurrentChannel != null? mCurrentChannel.getEitVersions() : null;
                        int oldVersion = 0;
                        if (mEitVersions == null) {
                            mEitVersions = new int[128];
                            for (int i = 0; i < 128; i++)
                                mEitVersions[i] = -1;
                            if (DEBUG) Log.d(TAG, "epg eit, create eit versions for channel");
                        }

                        if (mVctMap == null) {
                            buildVct();
                            if (mVctMap == null) {
                                if (DEBUG) Log.d(TAG, "epg eit, build VCT map mVctMap is null");
                                return;
                            }
                        }

                        int mVersionCount = mEitVersions.length < event.dvbVersion.length ? mEitVersions.length : event.dvbVersion.length;
                        for (int i = 0; i < mVersionCount; i++) {
                            if (mEitVersions[i] != event.dvbVersion[i] && mEitVersions[i] == -1) {
                                oldVersion = -1;
                            }
                            if (DEBUG) Log.d(TAG, "-epg eit["+ i +"] version [" + mEitVersions[i] + "]" + "new ver["+event.dvbVersion[i]+"]");
                            mEitVersions[i] = event.dvbVersion[i];
                        }
                        /*pause and remove all epgs in msg queue*/
                        synchronized(mEpgQueueLock) {
                            if (mEpgeventQueue != null) {
                                mEpgeventQueue.clear();
                            }
                        }
                        if (mMonitorHandler != null) {
                            mMonitorHandler.removeMessages(MSG_MONITOR_UPDATE_EPG);
                        }
                        synchronized (this) {
                            if (epgScanner == null)
                                break;
                            if (oldVersion != -1 && epgScanner != null) {
                                epgScanner.startScan(DTVEpgScanner.SCAN_PSIP_EIT_VERSION_CHANGE);
                                epgScanner.stopScan(MODE_Epg);
                            }
                            /*if (mMonitorHandler != null)
                                mMonitorHandler.removeMessages(MSG_MONITOR_EVENT);*/
                            if (mTvDataBaseManager != null && oldVersion != -1)
                                mTvDataBaseManager.resetPrograms();

                        /*clear all epgs with old version, and update channel's eit versions*/
                        /*without Epglock, there may be still chance that programs updating in the EPG handler thread runs after codes below*/
                        if (mTvDataBaseManager != null) {
                            for (Integer c : mVctMap.keySet()) {
                                /* Move the logic of deleting the EPG information of the old version of EIT to the place where the program is updated */
                                if (DEBUG) Log.d(TAG, "epg eit, clear old programs for channel(id:"+c+") with version: "
                                                +oldVersion+":"+event.eitNumber);
                                if (oldVersion != -1) {
                                    mTvDataBaseManager.deletePrograms(mVctMap.get(c),
                                            String.valueOf(mEitVersions[event.eitNumber]));
                                            //String.valueOf(event.eitNumber));
                                }
                                refreshChannelMap();
                                for (ChannelInfo ch : channelMap) {
                                    if (!isAlive) {
                                        Log.e(TAG, "DTVMonitor is destroyed, exit EVENT_EIT_CHANGED");
                                        return;
                                    }
                                    if (ch.getId() == mVctMap.get(c)) {
                                        if (DEBUG) Log.d(TAG, "epg eit, update channel(id:"+ch.getId()+" name:"+ch.getDisplayName()+") with new version");
                                        ch.setEitVersions(mEitVersions);
                                        if (mCurrentChannel != null && ch.getId() == mCurrentChannel.getId())
                                            mCurrentChannel.setEitVersions(mEitVersions);
                                        mTvDataBaseManager.updateChannelInfo(ch);
                                    }
                                }
                                }
                            }

                            /*restart all epg*/
                            if (oldVersion != -1 && epgScanner != null) {
                                epgScanner.startScan(MODE_Epg);
                                epgScanner.stopScan(DTVEpgScanner.SCAN_PSIP_EIT_VERSION_CHANGE);
                            }
                        }
                        if (mMonitorHandler != null) {
                            mMonitorHandler.sendMessageDelayed(mMonitorHandler.obtainMessage(MSG_MONITOR_UPDATE_EPG, null), 200);
                        }
                    }
                    break;
                    case DTVEpgScanner.Event.EVENT_TDT_END:
                        /* do nothing, Easy to be blocked, so deal with the msg outside.*/
                        break;
                    case DTVEpgScanner.Event.EVENT_PROGRAM_AV_UPDATE: {
                        if (mCurrentChannel != null && mCurrentChannel.isAtscChannel()) {
                            break; //Is ATSC, so will should break to prevent audio from refreshing the error
                        }
                        Log.d(TAG, "[AV Update]: ServiceId:" + event.channel.getServiceId()
                              + " Vid:" + event.channel.getVideoPid()
                              + " Pcr:" + event.channel.getPcrPid()
                              + " Aids:" + Arrays.toString(event.channel.getAudioPids())
                              + " Afmts:" + Arrays.toString(event.channel.getAudioFormats())
                              + " Alangs:" + Arrays.toString(event.channel.getAudioLangs())
                              + " Aexts:" + Arrays.toString(event.channel.getAudioExts())
                              + " Stypes:" + Arrays.toString(event.channel.getSubtitleTypes())
                              + " Sids:" + Arrays.toString(event.channel.getSubtitlePids())
                              + " Sstypes:" + Arrays.toString(event.channel.getSubtitleStypes())
                              + " Sid1s:" + Arrays.toString(event.channel.getSubtitleId1s())
                              + " Sid2s:" + Arrays.toString(event.channel.getSubtitleId2s())
                              + " Slangs:" + Arrays.toString(event.channel.getSubtitleLangs())
                             );
                        boolean updated = false;
                        synchronized (this) {
                            if (tvservice != null
                                    && tvservice.getServiceId() == event.channel.getServiceId()) {
                                    tvservice.setVideoPid(event.channel.getVideoPid());
                                    tvservice.setVfmt(event.channel.getVfmt());
                                    tvservice.setPcrPid(event.channel.getPcrPid());
                                    tvservice.setAudioPids(event.channel.getAudioPids());
                                    tvservice.setAudioFormats(event.channel.getAudioFormats());
                                    tvservice.setAudioExts(event.channel.getAudioExts());
                                    tvservice.setAudioLangs(event.channel.getAudioLangs());
                                    tvservice.setSubtitlePids(event.channel.getSubtitlePids());
                                    tvservice.setSubtitleTypes(event.channel.getSubtitleTypes());
                                    tvservice.setSubtitleStypes(event.channel.getSubtitleStypes());
                                    tvservice.setSubtitleId1s(event.channel.getSubtitleId1s());
                                    tvservice.setSubtitleId2s(event.channel.getSubtitleId2s());
                                    tvservice.setSubtitleLangs(event.channel.getSubtitleLangs());
                                if (mTvDataBaseManager != null)
                                    mTvDataBaseManager.updateChannelInfo(tvservice);
                                updated = true;
                            }
                        }
                        if (updated) {
                            if (auto_retune_service) {
                                if (DEBUG) Log.d(TAG, "Retune Current Uri");
                                if (mCurrentUri != null)
                                    switchToSourceInput(mCurrentUri);
                            }
                        }
                    }
                    break;
                    case DTVEpgScanner.Event.EVENT_PROGRAM_NAME_UPDATE: {
                        if ((event.channel.getOriginalNetworkId() == -1)
                            && (event.channel.getMajorChannelNumber() != 0)) {
                            updateAtscChannelInfo(event);
                        } else {
                            updateDvbChannelInfo(event);
                        }
                    }
                    break;
                    case DTVEpgScanner.Event.EVENT_CHANNEL_UPDATE:
                        if (DEBUG) Log.d(TAG, "[TS Update]: TS changed, need autoscan.");
                        synchronized (this) {
                            if (tvservice != null) {
                                if (DEBUG) Log.d(TAG, "[TS Update] Freq:" + tvservice.getFrequency());
                                int mode = new TvControlManager.TvMode(tvservice.getType()).getMode();
                                mUpdateFrequency = tvservice.getFrequency();
                                mEasText = null;
                                enterServiceLocked(null);
                                enterChannelLocked(null, true);
                                easHandler.sendEmptyMessage(MSG_STOP_EAS_TEXT);
                                stopSubtitle();
                                releasePlayer();
                                if (DEBUG) Log.d(TAG, "EVENT_CHANNEL_UPDATE mUpdateFrequency:" + mUpdateFrequency);
                                mTvControlManager.setStorDBListener(mMonitorStoreManager);
                                setEpgAutoReset(false);
                                mTvControlManager.DtvSetTextCoding("GB2312");
                                mTvControlManager.DtvManualScan(mode, mUpdateFrequency);
                            }
                        }
                        break;
                    case DTVEpgScanner.Event.EVENT_PMT_RATING:
                        if (mCurrentChannel != null && mCurrentChannel.isAtscChannel()) {
                            //Only atsc has rrt
                            if (DEBUG) Log.d(TAG, "[PMT dvbServiceID]:" + event.dvbServiceID + " RATING:" + new String(event.pmt_rrt_ratings));
                            check_program_pmt_rating_block(event.dvbServiceID, new String(event.pmt_rrt_ratings));
                        }
                        break;
                    default:
                        break;
                }
            }

            private void deleteChannelInfoByFreq(int Frequency) {
                ArrayList<ChannelInfo> channelMap = mTvDataBaseManager.getChannelList(mInputId, ChannelInfo.COMMON_PROJECTION, null, null);
                if (channelMap != null) {
                    Iterator<ChannelInfo> iter = channelMap.iterator();
                    while (iter.hasNext()) {
                        ChannelInfo c = iter.next();
                        if (c.getFrequency() == Frequency) {
                            if (DEBUG) Log.d(TAG, "delete Frequency c.getDisplayNumber()" + c.getDisplayNumber() + "getId:" + c.getId() + "ChannelInfo.getFrequency()" + c.getFrequency());
                            mTvDataBaseManager.deleteChannel(c);

                        }
                    }
                }
            }

            private boolean epg_auto_reset = false;
            private void setEpgAutoReset(boolean enable) {
                epg_auto_reset = enable;
            }

            private final class ChannelObserver extends ContentObserver {
                public ChannelObserver() {
                    super(new Handler());
                }

                @Override
                public void onChange(boolean selfChange, Uri uri) {
                    if (DEBUG) Log.d(TAG, "channel changed: selfchange:" + selfChange + " uri:" + uri);
                    if ((DroidLogicTvUtils.matchsWhich(uri) == DroidLogicTvUtils.MATCH_CHANNEL)//delete
                        || ((DroidLogicTvUtils.matchsWhich(uri) == DroidLogicTvUtils.MATCH_CHANNEL_ID)
                            && (DroidLogicTvUtils.getChannelId(uri) > maxChannel_ID))) { //add
                        if (DroidLogicTvUtils.matchsWhich(uri) == DroidLogicTvUtils.MATCH_CHANNEL) {
                            if (DEBUG) Log.d(TAG, "channel deleted");
                        } else if ((DroidLogicTvUtils.matchsWhich(uri) == DroidLogicTvUtils.MATCH_CHANNEL_ID)
                                   && (DroidLogicTvUtils.getChannelId(uri) > maxChannel_ID)) {
                            if (DEBUG) Log.d(TAG, "channel added: " + DroidLogicTvUtils.getChannelId(uri) + ">" + maxChannel_ID);
                        }
                        if (epg_auto_reset)
                            reset();
                    }
                }
                /*
                @Override
                public IContentObserver releaseContentObserver() {
                    // TODO Auto-generated method stub
                    return super.releaseContentObserver();
                }*/
            }

            private final class CCStyleObserver extends CaptioningChangeListener {
                public CCStyleObserver() {

                }

                private void updateCCStyleParams() {
                    /*
                       Log.d(TAG, "CCStyleObserver updateCCStyleParams.");
                       CCStyleParams ccParam = getCaptionStyle();
                       DTVSubtitleView.DTVCCParams params = new DTVSubtitleView.DTVCCParams(
                       0,
                       ccParam.fg_color,
                       ccParam.fg_opacity,
                       ccParam.bg_color,
                       ccParam.bg_opacity,
                       ccParam.font_style,
                       ccParam.font_size);
                       if (mSubtitleView != null)
                       mSubtitleView.setSubParams(params);
                       */
                }

                @Override
                public void onEnabledChanged(boolean enabled) {
                    //updateCCStyleParams();
                    mCurrentCCEnabled = enabled;
                    if (DEBUG) Log.d(TAG, "CCStyleObserver onEnabledChanged: " + enabled);
                    if (!enabled)
                        notifyTrackSelected(TvTrackInfo.TYPE_SUBTITLE, null);
                }

                @Override
                public void onUserStyleChanged(CaptionStyle userStyle) {
                    //updateCCStyleParams();
                }

                @Override
                public void onLocaleChanged(Locale locale) {
                    //updateCCStyleParams();
                }

                @Override
                public void onFontScaleChanged(float fontScale) {
                    //updateCCStyleParams();
                }
            }

            private final class MonitorStoreManager extends TvStoreManager implements TvControlManager.StorDBEventListener {
                public MonitorStoreManager(String inputId, int initialDisplayNumber) {
                    super(mContext, inputId, initialDisplayNumber);
                }
                public void onScanEnd() {
                    mTvControlManager.DtvStopScan();
                }

                public void onScanEndBeforeStore(int freg) {
                    //delete channel before store
                    if (mUpdateFrequency == 0 || mUpdateFrequency != freg) {
                        if (DEBUG) Log.d(TAG, "[onScanEndBeforeStore] mUpdateFrequency:" + mUpdateFrequency + ", freg:" + freg);
                        return;
                    }
                    deleteChannelInfoByFreq(mUpdateFrequency);
                }

                public void onStoreEnd(int freg) {
                    if (DEBUG) Log.d(TAG, "onStoreEnd");
                    setEpgAutoReset(true);
                    if (mUpdateFrequency == 0 || mUpdateFrequency != freg) {
                        if (DEBUG) Log.d(TAG, "[onScanExit]  mUpdateFrequency:" + mUpdateFrequency + ", freg:" + freg);
                        return;
                    }
                    ArrayList<ChannelInfo> channelMap = mTvDataBaseManager.getChannelList(mInputId, ChannelInfo.COMMON_PROJECTION, null, null);
                    if (channelMap != null) {
                        for (ChannelInfo c : channelMap) {
                             if (DEBUG) Log.d(TAG, "onScanExit mUpdateFrequency:" + mUpdateFrequency + "ChannelInfo.getFrequency()" + c.getFrequency());
                             if (mUpdateFrequency == c.getFrequency()) {
                                 if (DEBUG) Log.d(TAG, "Will send msg to Session handler");
                                 synchronized (this) {
                                     if (mCurrentSession != null)
                                         mCurrentSession.onUpdateTsPlay(c.getId());
                                     notifyChannelRetuned(c.getUri());
                                     if (DEBUG) Log.d(TAG, "TS changed,  notifyChannelRetuned: " + c.getUri());
                                 }
                                 break;
                             }
                        }
                        mUpdateFrequency = 0;
                    }
                }

                @Override
                public void StorDBonEvent(TvControlManager.ScannerEvent ev) {
                    onStoreEvent(ev);
                }
            }

            public String getStandard() {
                return mStandard;
            }
        }


        protected volatile long mRecordStartTimeMs_0 = 0;
        protected volatile long mRecordStartTimeMs = 0;
        protected volatile long mCurrentTimeMs = 0;

        protected void onTimeShiftEvent(int msg, int param) {
            switch (msg) {
                case TvControlManager.EVENT_AV_TIMESHIFT_PLAY_FAIL:
                    break;
                case TvControlManager.EVENT_AV_TIMESHIFT_REC_FAIL:
                    if (DEBUG) Log.d(TAG, "[timeshift] rec false");
                    notifyTimeShiftStatusChanged(TvInputManager.TIME_SHIFT_STATUS_UNAVAILABLE);
                    break;
                case TvControlManager.EVENT_AV_TIMESHIFT_START_TIME_CHANGED:
                    if (param == 0) {
                        if (DEBUG) Log.d(TAG, "[timeshift] init record time");
                        mRecordStartTimeMs_0 = mCurrentTimeMs = (mRecordingId == null)? System.currentTimeMillis() : 0;
                        mHandler.sendEmptyMessageDelayed(MSG_TIMESHIFT_AVAILABLE, 3000);
                    }
                    mRecordStartTimeMs = mRecordStartTimeMs_0 + param;

                    if (DEBUG) Log.d(TAG, "[timeshift] start:"+new Date(mRecordStartTimeMs + mTvTime.getDiffTime()).toString());
                    break;
                case TvControlManager.EVENT_AV_TIMESHIFT_CURRENT_TIME_CHANGED:
                    mCurrentTimeMs = mRecordStartTimeMs_0 + param;
                    if (mRecordingId == null) {
                        Log.d(TAG, "[timeshift] current:"+new Date(mCurrentTimeMs + mTvTime.getDiffTime()).toString());
                        Log.d(TAG, "[timeshift] end    :"+new Date(System.currentTimeMillis() + mTvTime.getDiffTime()).toString());
                    } else {
                        Log.d(TAG, "[timeshift] current:"+(mCurrentTimeMs + mTvTime.getDiffTime()));
                    }
                    break;
            }
        }

        protected void doTimeShiftPause() {
            if (DEBUG) Log.d(TAG, "[timeshift] doTimeShiftPause:");
            mTvControlManager.pausePlay("atsc");
        }

        protected void doTimeShiftResume() {
            if (DEBUG) Log.d(TAG, "[timeshift] doTimeShiftResume:");
            //mTvControlManager.setPlayParam("atsc", "{\"speed\":1}");
            mTvControlManager.resumePlay("atsc");
        }

        protected void doTimeShiftSeekTo(long timeMs) {
            String para = "{\"offset\":"+ (timeMs - (mRecordStartTimeMs + mTvTime.getDiffTime())) + "}";
            if (DEBUG) Log.d(TAG, "[timeshift] doTimeShiftSeekTo:"+"offset:"+(timeMs - (mRecordStartTimeMs + mTvTime.getDiffTime())));
            mTvControlManager.seekPlay("atsc", para);
        }

        protected void doTimeShiftSetPlaybackParams(PlaybackParams params) {
            int speed = (int)params.getSpeed();
            if (DEBUG) Log.d(TAG, "[timeshift] doTimeShiftSetPlaybackParams:"+speed);
            String param = "{\"speed\":"+speed+"}";
            mTvControlManager.setPlayParam("atsc", param);
        }

        private MiniRecordedProgram getMiniRecordedProgram(Uri recordedUri) {
            ContentResolver resolver = mContext.getContentResolver();
            try(Cursor c = resolver.query(recordedUri, MiniRecordedProgram.PROJECTION, null, null, null)) {
                if (c != null) {
                     MiniRecordedProgram result = MiniRecordedProgram.onQuery(c);
                    if (DEBUG) {
                        Log.d(TAG, "Finished query for " + this);
                    }
                    return result;
                } else {
                    if (c == null) {
                        Log.e(TAG, "Unknown query error for " + this);
                    } else {
                        if (DEBUG) {
                            Log.d(TAG, "Canceled query for " + this);
                        }
                    }
                    return null;
                }
            }
        }

        private String parseRecording(Uri uri) {
            MiniRecordedProgram recording = getMiniRecordedProgram(uri);
            if (recording != null) {
                return recording.getDataUri();
            }
            return null;
        }

        private String getRecordingPath(String id) {
            return Uri.parse(id).getPath();
        }

        protected void doRecPlay(Uri recordUri) {
            if (DEBUG) Log.d(TAG, "[timeshift] doRecPlay:"+recordUri);
            mRecordingId = parseRecording(recordUri);
            if (mRecordingId == null) {
                Log.w(TAG, "play failed. Can't find channel for " + recordUri);
                notifyVideoUnavailable(
                        TvInputManager.VIDEO_UNAVAILABLE_REASON_UNKNOWN);
                return;
            }
            if (DEBUG) Log.d(TAG, "[timeshift] mRecordingId:"+mRecordingId);

            mTvControlManager.SetAVPlaybackListener(this);
            mTvControlManager.SetPlayerInstanceNoListener(this);
            openTvAudio(DroidLogicTvUtils.SOURCE_TYPE_DTV);
            StringBuilder param =
                new StringBuilder("{")
                    .append("\"type\":\"dtv\"")
                    //.append(",\"fe\":" + info.getFEParas())
                    //.append(",\"v\":{\"pid\":"+info.getVideoPid()+",\"fmt\":"+info.getVfmt()+"}")
                    //.append(",\"a\":{\"pid\":"+(audio != null ? audio.mPid : -1)+",\"fmt\":"+(audio != null ? audio.mFormat : -1)+",\"AudComp\":"+info.getAudioCompensation()+"}")
                    //.append(",\"p\":{\"pid\":"+info.getPcrPid()+"}")
                    .append(",\"para\":{")
                        .append("\"mode\":1")
                        .append(",\"file\":"+"\""+getRecordingPath(mRecordingId)+"\"")
                    .append("}")
                .append("}");
            mTvControlManager.startPlay("atsc", param.toString());

            //tmp trigger manually
            notifyContentAllowed();
            notifyVideoAvailable();

            mPlayPaused = false;
        }

        public long getStartPosition() {
            if (DEBUG) Log.d("ttt", "start:"+new Date(mRecordStartTimeMs + mTvTime.getDiffTime()).toString());
            return mRecordStartTimeMs + mTvTime.getDiffTime();
        }

        public long getCurrentPosition() {
            if (DEBUG) Log.d("ttt", "current:"+new Date(mCurrentTimeMs + mTvTime.getDiffTime()).toString());
            return mCurrentTimeMs + mTvTime.getDiffTime();
        }

        protected boolean mPlayPaused;

        @Override
        public void onTimeShiftPause() {
            mHandler.sendEmptyMessage(MSG_TIMESHIFT_PAUSE);
            mPlayPaused = true;
        }

        @Override
        public void onTimeShiftResume() {
            mHandler.sendEmptyMessage(MSG_TIMESHIFT_RESUME);
            mPlayPaused = false;
        }

        @Override
        public void onTimeShiftSeekTo(long timeMs) {
            mHandler.obtainMessage(MSG_TIMESHIFT_SEEK_TO,
                    mPlayPaused ? 1 : 0, 0, timeMs).sendToTarget();
        }

        @Override
        public void onTimeShiftSetPlaybackParams(PlaybackParams params) {
            mHandler.obtainMessage(MSG_TIMESHIFT_SET_PLAYBACKPARAMS, params).sendToTarget();
        }

        @Override
        public long onTimeShiftGetStartPosition() {
            return getStartPosition();
        }

        @Override
        public long onTimeShiftGetCurrentPosition() {
            return getCurrentPosition();
        }

        @Override
        public void onTimeShiftPlay(Uri recordUri) {
            if (recordUri == null) {
                Log.w(TAG, "onTimeShiftPlay() is failed due to null channelUri.");
                //stopTune();
                return;
            }
            mHandler.obtainMessage(MSG_REC_PLAY, recordUri).sendToTarget();
        }

         public void showEasText(boolean isShowNow) {
            if (DEBUG) Log.d(TAG,"showEasText:"+mEasText);
            if (mOverlayView == null) {
                if (DEBUG) Log.d(TAG,"showEasText fail,because overlayview is NULL");
                return;
            }

            if (!isEasTextChannged && mOverlayView.isEasTextShown())
                return;
            if (mEasText != null) {
                isEasTextChannged = false;
                mOverlayView.setImageVisibility(false);
                if (mOverlayView.isEasTextShown()) {
                    Log.i("dtv","eas is shown, show immediately");
                    mOverlayView.setEasTextVisibility(true);
                    mOverlayView.setTextForEas(mEasText);
                }else if (isShowNow){
                    Log.i("dtv","eas is not shown ,show after 2s");
                    mOverlayView.setEasTextVisibility(true);
                    mOverlayView.setTextForEas(mEasText);
                }
            } else {
                mOverlayView.setEasTextVisibility(false);
            }
        }

        public boolean isEasTextShown() {
            if (mOverlayView != null) {
                return false;
            }
            return mOverlayView.isEasTextShown();
        }


        public void tuneToEasChannel(Uri uri) {
            doTuneInService(uri, getSessionId());
        }

        private void doChannelBlockToServer(boolean lock, long channelId) {
            if (!enableChannelBlockInServer()) {
                mTvControlManager.request("ADTV.BlockCurrentChannel", "");
                return;
            }
            if (DEBUG) Log.d(TAG, "do private cmd: block_channel: "+ lock);
            if (mCurrentChannel != null) {
                if (DEBUG) Log.d(TAG, "block_channel: "+ channelId + "-" + lock + ", current: " + mCurrentChannel.getId() + "-" + mCurrentChannel.isLocked());
                if (mCurrentChannel.getId() == channelId && mCurrentChannel.isLocked() != lock) {
                    mCurrentChannel.setLocked(lock);
                    if (DEBUG) Log.d(TAG, "block_channel, call to tvserver");
                    mTvControlManager.request("ADTV.block", "{\"isblocked\":" + lock + ",\"tunning\":" + false + "}");
                    if (lock) {
                        mIsChannelBlocked = true;
                    } else {
                        mIsChannelBlocked = false;
                    }
                }
            }
        }
    }


    public class DTVRecordingSession extends TvInputService.RecordingSession implements
             TvControlManager.RecorderEventListener, Handler.Callback {
        private String TAG = "DTVRecordingSession";
        //private boolean DEBUG = true;

        private final String mInputId;
        private final Context mContext;

        private final Handler mRecordingHandler;
        private final Random mRandom = new Random();

        private static final int MSG_RECORD_TUNE = 1;
        private static final int MSG_RECORD_DISCONNECT = 2;
        private static final int MSG_RECORD_START_RECORDING = 3;
        private static final int MSG_RECORD_STOP_RECORDING = 4;
        private static final int MSG_RECORD_RECORDING_RESULT = 5;
        private static final int MSG_RECORD_DELETE_RECORDING = 6;
        private static final int MSG_RECORD_RELEASE = 7;

        private static final int STATE_IDLE = 1;
        private static final int STATE_CONNECTED = 2;
        private static final int STATE_RECORDING = 3;
        private int mSessionState = STATE_IDLE;

        protected ChannelInfo mChannel;
        protected TvDataBaseManager mTvDataBaseManager;
        protected SystemControlManager mSystemControlManager;
        protected List<ChannelInfo.Subtitle> mCurrentSubtitles;
        protected List<ChannelInfo.Audio> mCurrentAudios;

        private File mStorageDir;
        private String mStorageFilePrefix;
        private long mRecordStartTime;
        private long mRecordEndTime;

        public DTVRecordingSession(Context context, String inputId) {
            super(context);
            mContext = context;
            mInputId = inputId;
            mRandom.setSeed(System.nanoTime());

            mTvDataBaseManager = new TvDataBaseManager(mContext);
            mSystemControlManager = SystemControlManager.getInstance();

            HandlerThread handlerThread = new HandlerThread(TAG);
            handlerThread.start();
            mRecordingHandler = new Handler(handlerThread.getLooper(), this);

            String RecordingPath = mSystemControlManager.getPropertyString("tv.dtv.rec.path", getCacheStoragePath());
            mStorageDir = new File(RecordingPath);

            if (DEBUG) Log.d(TAG, "DTVRecordingSession:"+inputId);
        }

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_RECORD_TUNE: {
                    Uri channelUri = (Uri) msg.obj;
                    if (extTune(channelUri)) {
                        onTuned(channelUri);
                    } else {
                        if (DEBUG) Log.w(TAG, "Recording session connect failed");
                        onTuneFailed();
                    }
                    return true;
                }
                case MSG_RECORD_START_RECORDING: {
                    if (exeStartRecording()) {
                        Toast.makeText(mContext, "Recording started",
                                Toast.LENGTH_SHORT).show();
                    }
                    else {
                        onRecordUnexpectedlyStopped(TvInputManager.RECORDING_ERROR_UNKNOWN);
                    }
                    return true;
                }
                case MSG_RECORD_STOP_RECORDING: {
                    exeStopRecording();
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mContext, "Recording stopped",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                    return true;
                }
                case MSG_RECORD_RECORDING_RESULT: {
                    exeRecordingResult((Boolean) msg.obj);
                    return true;
                }
                case MSG_RECORD_DELETE_RECORDING: {
                    Uri toDelete = (Uri) msg.obj;
                    //onDeleteRecording(toDelete);
                    return true;
                }
                case MSG_RECORD_RELEASE: {
                    exeRelease();
                    return true;
                }
            }
            return false;
        }


        private boolean extTune(Uri channelUri) {
            if (mSessionState == STATE_RECORDING) {
                return false;
            }
            mChannel = mTvDataBaseManager.getChannelInfo(channelUri);
            if (mChannel == null) {
                Log.w(TAG, "Failed to start recording. Couldn't find the channel for " + mChannel.getDisplayName());
                return false;
            }
            if (mChannel.isAnalogChannel()) {
                Log.w(TAG, "Failed to start recording. Do not support Analog channnel for " + mChannel.getDisplayName());
                return false;
            }
            if (mSessionState == STATE_CONNECTED) {
                return true;
            }

            mSessionState = STATE_CONNECTED;
            return true;
        }

        private String getStorageKey() {
            long prefix = System.currentTimeMillis();
            int suffix = mRandom.nextInt();
            return String.format(Locale.ENGLISH, "%016x_%016x", prefix, suffix);
        }

        private boolean exeStartRecording() {
            if (mSessionState != STATE_CONNECTED) {
                return false;
            }
            mRecordStartTime = System.currentTimeMillis();
            mTvControlManager.SetRecorderEventListener(this);

            mStorageFilePrefix = getStorageKey();

            //"{\"dvr\":2,\"path\":\"%s\",\"prefix\":\"program0-rec\",\"suffix\":\"ts\"\"v\":{\"pid\":%d,\"fmt\":%d},\"a\":{\"pid\":%d,\"fmt\":%d},\"max\":{\"time\":%d,\"size\":%d}}"
            StringBuilder param = new StringBuilder("{")
                //.append(",\"fe\":" + mChannel.getFEParas())
                .append("\"dvr\":2")
                .append(",\"fifo\":1")
                .append(",\"path\":\""+mStorageDir.getAbsolutePath()+"\"")
                .append(",\"prefix\":\""+mStorageFilePrefix+"\"")
                .append(",\"suffix\":\""+"ts"+"\"")
                .append(",\"v\":{\"pid\":"+mChannel.getVideoPid()+",\"fmt\":"+mChannel.getVfmt()+"}")
                .append(",\"a\":{\"pid\":"+mChannel.getAudioPids()[0]+",\"fmt\":"+mChannel.getAudioFormats()[0]+"}")
                .append("}");
            if (mTvControlManager.startRecording("atsc-rec", param.toString()) != 0) {
                Log.w(TAG, "Failed to start recording. ");
                return false;
            }

            mSessionState = STATE_RECORDING;
            return true;
        }

        private void exeStopRecording() {
            if (mSessionState != STATE_RECORDING) {
                return;
            }
            // Do not change session status.
            mTvControlManager.stopRecording("atsc-rec", "{}");
            mRecordEndTime = System.currentTimeMillis();
        }

        private boolean exeRecordingResult(boolean success) {
            if (mSessionState == STATE_RECORDING && success) {
                Uri uri = insertRecordedProgram(null, mChannel.getId(),
                        mStorageDir.toURI().toString()+mStorageFilePrefix+".ts", 1024 * 1024,
                        mRecordStartTime, mRecordEndTime);
                if (uri != null) {
                    onRecordFinished(uri);
                }
                mTvControlManager.stopRecording("atsc-rec", null);
                return true;
            }

            if (mSessionState == STATE_RECORDING) {
                onRecordUnexpectedlyStopped(TvInputManager.RECORDING_ERROR_UNKNOWN);
                if (DEBUG) Log.w(TAG, "Recording failed: " + mChannel == null ? "" : mChannel.getDisplayName());
                mTvControlManager.stopRecording("atsc-rec", null);
            } else {
                if (DEBUG) Log.e(TAG, "Recording session status abnormal");
                mTvControlManager.stopRecording("atsc-rec", null);
                mSessionState = STATE_IDLE;
            }
            return false;
        }

        private void exeRelease() {
            // Current recording will be canceled.
            mTvControlManager.stopRecording("atsc-rec", null);
            mRecordingHandler.getLooper().quitSafely();
        }

        private final String SORT_BY_TIME = TvContract.Programs.COLUMN_START_TIME_UTC_MILLIS
                + ", " + TvContract.Programs.COLUMN_CHANNEL_ID + ", "
                + TvContract.Programs.COLUMN_END_TIME_UTC_MILLIS;

        private final String[] PROJECTION = {
                TvContract.Programs.COLUMN_CHANNEL_ID,
                TvContract.Programs.COLUMN_TITLE,
                TvContract.Programs.COLUMN_EPISODE_TITLE,
                TvContract.Programs.COLUMN_SEASON_NUMBER,
                TvContract.Programs.COLUMN_EPISODE_NUMBER,
                TvContract.Programs.COLUMN_SHORT_DESCRIPTION,
                TvContract.Programs.COLUMN_POSTER_ART_URI,
                TvContract.Programs.COLUMN_THUMBNAIL_URI,
                TvContract.Programs.COLUMN_CANONICAL_GENRE,
                TvContract.Programs.COLUMN_CONTENT_RATING,
                TvContract.Programs.COLUMN_START_TIME_UTC_MILLIS,
                TvContract.Programs.COLUMN_END_TIME_UTC_MILLIS,
                TvContract.Programs.COLUMN_VIDEO_WIDTH,
                TvContract.Programs.COLUMN_VIDEO_HEIGHT
        };
/*
        private Program getRecordedProgram() {
            ContentResolver resolver = mContext.getContentResolver();
            long avg = mRecordStartTime / 2 + mRecordEndTime / 2;
            Uri programUri = TvContract.buildProgramsUriForChannel(mChannel.getId(), avg, avg);
            try (Cursor c = resolver.query(programUri, PROJECTION, null, null, SORT_BY_TIME)) {
                if (c != null) {
                    Program result = Program.onQuery(c);
                    if (DEBUG) {
                        Log.v(TAG, "Finished query for " + this);
                    }
                    return result;
                } else {
                    if (c == null) {
                        Log.e(TAG, "Unknown query error for " + this);
                    } else {
                        if (DEBUG) {
                            Log.d(TAG, "Canceled query for " + this);
                        }
                    }
                    return null;
                }
            }
        }
*/
        private Uri insertRecordedProgram(Program program, long channelId, String storageUri,
                long totalBytes, long startTime, long endTime) {
            RecordedProgram recordedProgram = RecordedProgram.builder()
                    .setInputId(mInputId)
                    .setChannelId(channelId)
                    .setDataUri(storageUri)
                    .setDurationMillis(endTime - startTime)
                    .setDataBytes(totalBytes)
                    .build();
            Uri uri = mContext.getContentResolver().insert(TvContract.RecordedPrograms.CONTENT_URI,
                    RecordedProgram.toValues(recordedProgram));
            return uri;
        }


        // RecordingSession
        @Override
        public void onTune(Uri channelUri) {
            if (DEBUG) {
                Log.d(TAG, "Requesting recording session tune: " + channelUri);
            }
            mRecordingHandler.removeCallbacksAndMessages(null);
            mRecordingHandler.obtainMessage(MSG_RECORD_TUNE, channelUri).sendToTarget();
        }

        @Override
        public void onRelease() {
            if (DEBUG) {
                Log.d(TAG, "Requesting recording session release.");
            }
            mRecordingHandler.removeCallbacksAndMessages(null);
            mRecordingHandler.sendEmptyMessage(MSG_RECORD_RELEASE);

        }

        @Override
        public void onStartRecording(Uri programHint) {
            if (DEBUG) {
                Log.d(TAG, "Requesting start recording.");
            }
            mRecordingHandler.sendEmptyMessage(MSG_RECORD_START_RECORDING);
        }

        @Override
        public void onStopRecording() {
            if (DEBUG) {
                Log.d(TAG, "Requesting stop recording.");
            }
            mRecordingHandler.sendEmptyMessage(MSG_RECORD_STOP_RECORDING);
        }


        // Called from TunerRecordingSessionImpl in a worker thread.
        protected void onTuned(Uri channelUri) {
            if (DEBUG) {
                Log.d(TAG, "Notifying recording session tuned.");
            }
            notifyTuned(channelUri);
        }

        public void onTuneFailed() {
            if (DEBUG) {
                Log.d(TAG, "Notifying recording session tune failed.");
            }
            notifyError(TvInputManager.RECORDING_ERROR_UNKNOWN);
        }

        public void onRecordFinished(final Uri recordedProgramUri) {
            if (DEBUG) {
                Log.d(TAG, "Notifying record successfully finished.");
            }
            notifyRecordingStopped(recordedProgramUri);
        }

        public void onRecordUnexpectedlyStopped(int reason) {
            Log.w(TAG, "Notifying record failed: " + reason);
            notifyError(reason);
        }

        @Override
        public void onRecoderEvent(TvControlManager.RecorderEvent ev){
            if (!TextUtils.equals(ev.Id, "atsc-rec"))
                return;

            if (DEBUG) Log.d("rrr", "rec evt status:"+ev.Status+" err:"+ev.Error);
            switch (ev.Status) {
                case TvControlManager.RecorderEvent.EVENT_RECORDER_START:
                    break;
                case TvControlManager.RecorderEvent.EVENT_RECORDER_STOP:
                    mRecordingHandler.obtainMessage(MSG_RECORD_RECORDING_RESULT, ev.Error == 0).sendToTarget();
                    break;
            }
        }

    }

    public static final class TvInput {
        public final String displayName;
        public final String name;
        public final String description;
        public final String logoThumbUrl;
        public final String logoBackgroundUrl;

        public TvInput(String displayName, String name, String description,
                       String logoThumbUrl, String logoBackgroundUrl) {
            this.displayName = displayName;
            this.name = name;
            this.description = description;
            this.logoThumbUrl = logoThumbUrl;
            this.logoBackgroundUrl = logoBackgroundUrl;
        }
    }

    public String getDeviceClassName() {
        return DTVInputService.class.getName();
    }

    public int getDeviceSourceType() {
        return DroidLogicTvUtils.DEVICE_ID_DTV;
    }

    public void handleAdtvAudioEvent(int cmd, int param1, int param2){
        if (mAudioSystemCmdManager == null) {
            Log.e(TAG, "handleAdtvAudioEvent mAudioSystemCmdManager is null, return");
            return;
        }
        mAudioSystemCmdManager.handleAdtvAudioEvent(cmd, param1, param2);
    }

    public TvContentRating[] parseParentalRatings(int parentalRating, String title)
    {
        String ratingSystemDefinition = "DVB";
        String ratingDomain = "com.android.tv";
        String DVB_ContentRating[] = {"DVB_4", "DVB_5", "DVB_6", "DVB_7", "DVB_8", "DVB_9", "DVB_10", "DVB_11",
                                      "DVB_12", "DVB_13", "DVB_14", "DVB_15", "DVB_16", "DVB_17", "DVB_18"};
        TvContentRating ratings_arry[];
        ratings_arry = new TvContentRating[1];
        parentalRating += 3; //minimum age = rating + 3 years
        Log.d(TAG, "parseParentalRatings parentalRating:"+ parentalRating + ", title = " + title);
        if (parentalRating >=4 && parentalRating <= 18) {
            TvContentRating r = TvContentRating.createRating(ratingDomain, ratingSystemDefinition, DVB_ContentRating[parentalRating-4], "");
            if (r != null) {
                ratings_arry[0] = r;
                Log.d(TAG, "parse ratings add rating:"+r.flattenToString()  + ", title = " + title);
            }
        }else {
            ratings_arry = null;
        }

        return ratings_arry;
    }

    public TvContentRating[] parseDRatingsT(String jsonString, TvDataBaseManager tvdatamanager, String title, Uri channeluri, long programid, long starttime) {
        return null;
    }

    @Override
    public void processDetailsChannelAlert(EasEvent easEvent){
          if (DEBUG) Log.d(TAG,"processDetailsChannelAlert");
          if (mCurrentSession != null && mCurrentSession.mCurrentChannel != null) {
              mEASProcessManager.SetCurDisplayNum(mCurrentSession.mCurrentChannel.getDisplayNumber());
              mEASProcessManager.SetCurInputId(mCurrentSession.getInputId());
              mEASProcessManager.SetCurUri(mCurrentSession.mCurrentUri);
              mEASProcessManager.setCallback(mCallback);
              mEASProcessManager.processDetailsChannelAlert(easEvent);
          }
    }

    private final EASProcessManager.EasProcessCallback mCallback =
            new EASProcessManager.EasProcessCallback() {
                @Override
                public void onEasStart() {
                    if (DEBUG) Log.d(TAG,"onEasStart:");
                    notifyAppEasStatus(true);
                }
                @Override
                public void onEasEnd() {
                    if (DEBUG) Log.d(TAG,"onEasEnd:");
                   notifyAppEasStatus(false);
                }
                @Override
                public void onUpdateEasText(String text) {
                    if (DEBUG) Log.d(TAG,"onUpdateEasText:"+text);
                    if ((mEasText == null && text == null) || (mEasText != null && mEasText.equals(text)))
                        isEasTextChannged = false;
                    else
                        isEasTextChannged = true;
                    mEasText = text;
                    if (mCurrentSession != null) {
                        mCurrentSession.showEasText(false);
                    }
                }
                @Override
                public void tuneToEasChannel(Uri uri) {
                    if (DEBUG) Log.d(TAG,"tuneToEasChannel:"+uri);
                    if (mCurrentSession != null)
                        mCurrentSession.tuneToEasChannel(uri);
                }
    };
}
