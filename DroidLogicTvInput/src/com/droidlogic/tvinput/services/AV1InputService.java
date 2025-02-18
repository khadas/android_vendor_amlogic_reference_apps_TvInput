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

import com.droidlogic.app.tv.TvInSignalInfo;
import com.droidlogic.tvinput.Utils;
import com.droidlogic.tvinput.customer.CustomerOps;

import com.droidlogic.app.tv.DroidLogicTvUtils;
import com.droidlogic.tvinput.R;

import java.lang.reflect.Method;
import java.lang.reflect.Field;

import android.content.Context;
import android.content.pm.ResolveInfo;
import android.graphics.Rect;
import android.media.tv.TvInputHardwareInfo;
import android.media.tv.TvInputInfo;
import android.media.tv.TvStreamConfig;
import android.media.tv.TvInputManager.Hardware;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Surface;
import android.widget.FrameLayout;

import java.lang.reflect.Method;
import java.lang.reflect.Field;

import java.util.*;

import android.net.Uri;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.media.tv.TvInputManager;
import android.media.tv.TvContentRating;
import com.droidlogic.app.tv.ChannelInfo;
import com.droidlogic.app.tv.TvDataBaseManager;
import com.droidlogic.tvinput.widget.DTVSubtitleView;
import android.os.Handler;
import android.os.HandlerThread;
import com.droidlogic.app.tv.Program;
import android.media.tv.TvContract;
import android.os.Message;
import android.view.accessibility.CaptioningManager;
import android.view.accessibility.CaptioningManager.CaptionStyle;
import android.view.accessibility.CaptioningManager.CaptioningChangeListener;
import android.graphics.Color;
import com.droidlogic.app.DataProviderManager;
import com.droidlogic.app.SystemControlManager;
import android.media.tv.TvTrackInfo;
import android.widget.Toast;

public class AV1InputService extends DroidLogicTvInputService {
    private static final String TAG = AV1InputService.class.getSimpleName();
    private AV1InputSession mCurrentSession;
    private int id = 0;
    private Map<Integer, AV1InputSession> sessionMap = new HashMap<>();
    private ChannelInfo mCurrentChannel = null;
    private TvDataBaseManager mTvDataBaseManager;
    protected List<ChannelInfo.Subtitle> mCurrentSubtitles;
    protected ChannelInfo.Subtitle mCurrentSubtitle;
    ChannelInfo.Subtitle pal_teletext_subtitle = null;
    protected final Object mLock = new Object();
    protected static int signalFmt = 0;

    protected static final int SIGNAL_PAL_FMT = 0;
    protected static final int SIGNAL_NTSC_FMT = 1;
    protected static final int SIGNAL_NOT_KNOWN = 2;

    protected static final int DTV_CC_STYLE_WHITE_ON_BLACK = 0;
    protected static final int DTV_CC_STYLE_BLACK_ON_WHITE = 1;
    protected static final int DTV_CC_STYLE_YELLOW_ON_BLACK = 2;
    protected static final int DTV_CC_STYLE_YELLOW_ON_BLUE = 3;
    protected static final int DTV_CC_STYLE_USE_DEFAULT = 4;
    protected static final int DTV_CC_STYLE_USE_CUSTOM = -1;

    protected static final int KEY_UP = 19;
    protected static final int KEY_DOWN = 20;
    protected static final int KEY_LEFT = 21;
    protected static final int KEY_RIGHT = 22;
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
    protected static final int KEY_REVEAL = 246;
    protected static final int KEY_CANCEL = 247;
    protected static final int KEY_SUBTITLE = 175;
    protected static final int KEY_SUB_PAGE = 206;

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

    private boolean is_subtitle_enable;

    protected static final String DTV_SUBTITLE_CC_PREFER = "persist.sys.cc.prefer";
    protected static final String DTV_SUBTITLE_CAPTION_EXIST = "tv.dtv.caption.exist";
    protected final Object mSubtitleLock = new Object();

    protected final BroadcastReceiver mParentalControlsBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mCurrentSession != null) {
                String action = intent.getAction();
                Log.d(TAG, "BLOCKED_RATINGS_CHANGED");
                mCurrentSession.checkIsNeedClearUnblockRating();
                mCurrentSession.checkCurrentContentBlockNeeded();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        initInputService(DroidLogicTvUtils.DEVICE_ID_AV1, AV1InputService.class.getName());

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TvInputManager.ACTION_BLOCKED_RATINGS_CHANGED);
        intentFilter.addAction(TvInputManager.ACTION_PARENTAL_CONTROLS_ENABLED_CHANGED);
        intentFilter.addAction(Intent.ACTION_TIME_CHANGED);
        registerReceiver(mParentalControlsBroadcastReceiver, intentFilter,
            2/*Context.RECEIVER_EXPORTED*/ | Context.RECEIVER_VISIBLE_TO_INSTANT_APPS);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mParentalControlsBroadcastReceiver);
    }

    @Override
    public Session onCreateSession(String inputId) {
        super.onCreateSession(inputId);

        mCurrentSession = new AV1InputSession(this, inputId, getHardwareDeviceId(inputId));
        mCurrentSession.setSessionId(id);
        registerInputSession(mCurrentSession);
        sessionMap.put(id, mCurrentSession);
        id++;

        return mCurrentSession;
    }

    @Override
    public void setCurrentSessionById(int sessionId) {
        Utils.logd(TAG, "setCurrentSessionById:"+sessionId);
        AV1InputSession session = sessionMap.get(sessionId);
        if (session != null) {
            mCurrentSession = session;
        }
    }

    @Override
    public void doTuneFinish(int result, Uri uri, int sessionId) {
        Log.d(TAG, "doTuneFinish,result:"+result+"sessionId:"+sessionId);
        if (result == ACTION_SUCCESS) {
            AV1InputSession session = sessionMap.get(sessionId);
            if (session != null) {
                mCurrentChannel = mTvDataBaseManager.getChannelInfo(uri);
                Log.d(TAG, "mCurrentChannel:"+mCurrentChannel);
                session.checkContentBlockNeeded(mCurrentChannel);
            }
        }
    }



    private int signal_is_pal(TvInSignalInfo signal_info)
    {
        if (signal_info.sigFmt == TvInSignalInfo.SignalFmt.TVIN_SIG_FMT_CVBS_PAL_CN ||
            signal_info.sigFmt == TvInSignalInfo.SignalFmt.TVIN_SIG_FMT_CVBS_PAL_I) {
            return SIGNAL_PAL_FMT;
        }
        else if (signal_info.sigFmt == TvInSignalInfo.SignalFmt.TVIN_SIG_FMT_CVBS_NTSC_443 ||
            signal_info.sigFmt == TvInSignalInfo.SignalFmt.TVIN_SIG_FMT_CVBS_NTSC_M ||
            signal_info.sigFmt == TvInSignalInfo.SignalFmt.TVIN_SIG_FMT_CVBS_PAL_60 ||
            signal_info.sigFmt == TvInSignalInfo.SignalFmt.TVIN_SIG_FMT_CVBS_PAL_M) {
            return SIGNAL_NTSC_FMT;
        }
        else {
            return SIGNAL_NOT_KNOWN;
        }
    }

    @Override
    public void onSigChange(TvInSignalInfo signal_info) {
        TvInSignalInfo.SignalStatus status = signal_info.sigStatus;

        if (status == TvInSignalInfo.SignalStatus.TVIN_SIG_STATUS_STABLE) {
            Log.d(TAG, "currSession " + mCurrentSession + " tmpInfo.fmt.toString() for av=" + signal_info.sigFmt.toString());
            signalFmt = signal_is_pal(signal_info);
        }
        else
            signalFmt = SIGNAL_NOT_KNOWN;

        super.onSigChange(signal_info);
    }

    public class AV1InputSession extends TvInputBaseSession  implements DTVSubtitleView.SubtitleDataListener{
        private TvInputManager mTvInputManager;
        private final Context mContext;
        //private TvControlManager mTvControlManager;
        private TvContentRating mLastBlockedRating;
        private int mChannelBlocked = -1;
        //private TvContentRating mCurrentContentRating;
        private final Set<TvContentRating> mUnblockedRatingSet = new HashSet<>();
        protected DTVSubtitleView mSubtitleView = null;
        private TvContentRating[] mATVContentRatings = null;
        protected HandlerThread mHandlerThread = null;
        protected Handler mMainHandler = null;
        protected Handler mHandler = null;
        protected CaptioningManager mCaptioningManager = null;
        protected SystemControlManager mSystemControlManager;
        private static final int DELAY_TRY_PREFER_CC = 2000;
        // void receiving vbi too late when switching to this source
        private boolean needRestartCC = false;
        private int mTeletextPageNumber = -1;
        private int mTeletextSubPageNumber = 0;
        private char[] mTeletextSubPageByteArr = new char[4];
        private int mTeletextSubPageNumber_count = 0;

        private final static String ACTION_TTX_KEYEVENT = "TTX_KEYEVENT";
        private final static String KEY_TTX_KEYEVENT = "TTX_KEYEVENT";
        private final static String VALUE_TTX_KEY_0 = "TTX_KEY_0";
        private final static String VALUE_TTX_KEY_1 = "TTX_KEY_1";
        private final static String VALUE_TTX_KEY_2 = "TTX_KEY_2";
        private final static String VALUE_TTX_KEY_3 = "TTX_KEY_3";
        private final static String VALUE_TTX_KEY_4 = "TTX_KEY_4";
        private final static String VALUE_TTX_KEY_5 = "TTX_KEY_5";
        private final static String VALUE_TTX_KEY_6 = "TTX_KEY_6";
        private final static String VALUE_TTX_KEY_7 = "TTX_KEY_7";
        private final static String VALUE_TTX_KEY_8 = "TTX_KEY_8";
        private final static String VALUE_TTX_KEY_9 = "TTX_KEY_9";
        private final static int TYPE_NUMBER_TIMEOUT = 2000;//2000MS
        private final static int TYPE_NUMBER_WAIT = 500;//500MS

        private final Map<String, Integer> VALUE_MAP = new HashMap<String, Integer>();
        private final String[] VALUE_TTX_KEY = {VALUE_TTX_KEY_0, VALUE_TTX_KEY_1, VALUE_TTX_KEY_2,
                VALUE_TTX_KEY_3, VALUE_TTX_KEY_4, VALUE_TTX_KEY_5,
                VALUE_TTX_KEY_6, VALUE_TTX_KEY_7, VALUE_TTX_KEY_8,
                VALUE_TTX_KEY_9};

        private void updateMap() {
            if (VALUE_MAP.size() == 0) {
                VALUE_MAP.put(VALUE_TTX_KEY_0, 0);
                VALUE_MAP.put(VALUE_TTX_KEY_1, 1);
                VALUE_MAP.put(VALUE_TTX_KEY_2, 2);
                VALUE_MAP.put(VALUE_TTX_KEY_3, 3);
                VALUE_MAP.put(VALUE_TTX_KEY_4, 4);
                VALUE_MAP.put(VALUE_TTX_KEY_5, 5);
                VALUE_MAP.put(VALUE_TTX_KEY_6, 6);
                VALUE_MAP.put(VALUE_TTX_KEY_7, 7);
                VALUE_MAP.put(VALUE_TTX_KEY_8, 8);
                VALUE_MAP.put(VALUE_TTX_KEY_9, 9);
            }
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

        protected int getTeletextRegionID(String ttxRegionName) {
            final String[] supportedRegions = {"English", "Deutsch", "Svenska/Suomi/Magyar",
                    "Italiano", "Fran?ais", "Português/Espa?ol",
                    "Cesky/Slovencina", "Türk?e", "Ellinika", "Alarabia / English" ,
                    "Russian", "Cyrillic"
            };
            final int[] regionIDMaps = {16, 17, 18, 19, 20, 21, 14, 22, 55 , 64, 36, 32};

            int i;
            for (i = 0; i < supportedRegions.length; i++) {
                if (supportedRegions[i].equals(ttxRegionName))
                    break;
            }

            if (i >= supportedRegions.length) {
                Log.d(TAG, "Teletext default region " + ttxRegionName +
                        " not found, using 'English' as default!");
                i = 0;
            }

            Log.d(TAG, "Teletext default region id: " + regionIDMaps[i]);
            return regionIDMaps[i];
        }

        protected void setSubtitleParam(int type, int pid, int stype, int id1, int id2, String lang) {
            if (type == ChannelInfo.Subtitle.TYPE_ATV_CC) {
                //CCStyleParams ccParam = getCaptionStyle();
                CCStyleParams ccParam = getCaptionStyle();//new CCStyleParams(1,3,2,3,0,2);
                DTVSubtitleView.AVCCParams params =
                    new DTVSubtitleView.AVCCParams(pid, id1, lang,
                        ccParam.fg_color,
                        ccParam.fg_opacity,
                        ccParam.bg_color,
                        ccParam.bg_opacity,
                        ccParam.font_style,
                        ccParam.font_size);

                mSubtitleView.setSubParams(params);
                mSubtitleView.setMargin(225, 128, 225, 128);
                Log.d(TAG, "ATV CC pid="+pid+",fg_color="+ccParam.fg_color+", fg_op="+ccParam.fg_opacity+", bg_color="+ccParam.bg_color+", bg_op="+ccParam.bg_opacity);
                Log.d(TAG,"font_style:"+ccParam.font_style+"font_size"+ccParam.font_size);
            } else if (type == ChannelInfo.Subtitle.TYPE_ATV_TELETEXT) {
                int pgno;
                pgno = (id1 == 0) ? 800 : id1 * 100;
                pgno += (id2 & 15) + ((id2 >> 4) & 15) * 10 + ((id2 >> 8) & 15) * 100;
                DTVSubtitleView.AtvTeleTextParams params =
                        new DTVSubtitleView.AtvTeleTextParams(pgno, 0x3F7F, getTeletextRegionID("English"), getSessionId(), getDeviceId());
                mSubtitleView.setSubParams(params);
            }
        }
        public AV1InputSession(Context context, String inputId, int deviceId) {
            super(context, inputId, deviceId);
            mContext = context;
            Utils.logd(TAG, "=====new AVInputSession=====");
            if (mTvInputManager == null)
                mTvInputManager = (TvInputManager)getSystemService(Context.TV_INPUT_SERVICE);
            mCurrentChannel = null;
            needRestartCC = true;
            mTvDataBaseManager = new TvDataBaseManager(mContext);

            initWorkThread();
            initOverlayView(R.layout.layout_overlay);
            if (mOverlayView != null) {
                mOverlayView.setImage(R.drawable.bg_no_signal);
                mOverlayView.setEasTextVisibility(false);
                mSubtitleView = (DTVSubtitleView)mOverlayView.getSubtitleView();
                mSubtitleView.setSubtitleDataListener(this);
            }

            if (getBlockNoRatingEnable()) {
                isBlockNoRatingEnable = true;
            } else {
                isBlockNoRatingEnable = false;
                isUnlockCurrent_NR = false;
            }
            Log.d(TAG,"isBlockNoRatingEnable:"+isBlockNoRatingEnable+",isUnlockCurrent_NR:"+isUnlockCurrent_NR);
            mCaptioningManager = (CaptioningManager) mContext.getSystemService(Context.CAPTIONING_SERVICE);
            mSystemControlManager = SystemControlManager.getInstance();

        }

        private boolean getBlockNoRatingEnable() {
            int status = DataProviderManager.getIntValue(mContext, DroidLogicTvUtils.BLOCK_NORATING, 0) ;
            Log.d(TAG,"getBlockNoRatingEnable:"+status);
            return (status == 1) ? true : false;
        }

        @Override
        public void notifyVideoAvailable() {
            super.notifyVideoAvailable();
            if (needRestartCC) {
                notifyTracks(mCurrentChannel);
                stopSubtitle();
                startSubtitleAutoAnalog();
            }
            needRestartCC = true;

            if (mSubtitleView != null) {
                mSubtitleView.setVisible(is_subtitle_enable);
            }
        }

        @Override
        public void notifyVideoUnavailable(int reason) {
            super.notifyVideoUnavailable(reason);
            if (mOverlayView != null) {
                mOverlayView.setTextVisibility(true);
                mSubtitleView.setVisible(false);
            }
            if (signalFmt == SIGNAL_PAL_FMT) {
                stop_teletext();
            }
        }

        @Override
        public boolean onSetSurface(Surface surface) {
            super.onSetSurface(surface);
            return setSurfaceInService(surface,this);
        }

        @Override
        public void onOverlayViewSizeChanged(int width, int height) {
            Log.d(TAG, "onOverlayViewSizeChanged: "+ width + "," + height);
            super.onOverlayViewSizeChanged(width, height);
            if (mIsPip) {
                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(width, height);
                mOverlayView.setLayoutParams(layoutParams);
            }
        }

        @Override
        public boolean onTune(Uri channelUri) {
            isUnlockCurrent_NR = false;
            mUnblockedRatingSet.clear();
            return doTuneInService(channelUri, getSessionId());
        }

        protected void checkIsNeedClearUnblockRating()
        {
            boolean isParentControlEnabled = mTvInputManager.isParentalControlsEnabled();
            Log.d(TAG, "checkIsNeedClearUnblockRating  into");
            if (isParentControlEnabled)
            {
              Iterator<TvContentRating> rateIter = mUnblockedRatingSet.iterator();
              while (rateIter.hasNext()) {
                TvContentRating rating = rateIter.next();
                if (mTvInputManager.isRatingBlocked(rating))
                {
                    mUnblockedRatingSet.remove(rating);
                }
              }
            }
        }

        public void doRelease() {
            mUnblockedRatingSet.clear();
            stopSubtitle();
            releaseWorkThread();
            synchronized(mLock) {
                mCurrentChannel = null;
            }
            if (mHandler != null) {
                mHandler.removeMessages(MSG_PARENTAL_CONTROL_AV);
                mHandler.removeCallbacksAndMessages(null);
            }
            if (sessionMap.containsKey(getSessionId())) {
                sessionMap.remove(getSessionId());
                if (mCurrentSession == this) {
                    mCurrentSession = null;
                    registerInputSession(null);
                }
            }
            mSubtitleView = null;
            super.doRelease();
        }

        protected void releaseWorkThread() {
            if (mHandler != null) {
                mHandler.removeCallbacksAndMessages(null);
            }
            if (mHandlerThread != null) {
                mHandlerThread.quit();
                mHandlerThread = null;
                mHandler = null;
            }
        }
        @Override
        public void doUnblockContent(TvContentRating rating) {
            super.doUnblockContent(rating);
            Log.d(TAG, "doUnblockContent");
            // TIS should unblock content only if unblock request is legitimate.
            if (rating == null
                    || mLastBlockedRating == null
                    || (mLastBlockedRating != null && rating.equals(mLastBlockedRating))) {
                mLastBlockedRating = null;
                isUnlockCurrent_NR = true;
                if (rating != null) {
                    mUnblockedRatingSet.add(rating);
                }
                playProgram(mCurrentChannel);

                Log.d(TAG, "notifyContentAllowed");
                notifyContentAllowed();
            }

        }
        @Override
        public void doAppPrivateCmd(String action, Bundle bundle) {
            //super.doAppPrivateCmd(action, bundle);
            if (TextUtils.equals(DroidLogicTvUtils.ACTION_STOP_TV, action)) {
                if (mHardware != null) {
                    mHardware.setSurface(null, null);
                }
            } else if (DroidLogicTvUtils.ACTION_BLOCK_NORATING.equals(action)) {
                Log.d(TAG, "do private cmd: ACTION_BLOCK_NORATING:"+ bundle.getInt(DroidLogicTvUtils.PARAM_NORATING_ENABLE));
                if (DroidLogicTvUtils.NORATING_OFF == bundle.getInt(DroidLogicTvUtils.PARAM_NORATING_ENABLE)) {
                    isBlockNoRatingEnable = false;
                    isUnlockCurrent_NR = false;
                } else if (DroidLogicTvUtils.NORATING_ON == bundle.getInt(DroidLogicTvUtils.PARAM_NORATING_ENABLE))
                    isBlockNoRatingEnable = true;
                else if (DroidLogicTvUtils.NORATING_UNLOCK_CURRENT == bundle.getInt(DroidLogicTvUtils.PARAM_NORATING_ENABLE))
                    isUnlockCurrent_NR = true;
                checkCurrentContentBlockNeeded();
            } else if (TextUtils.equals("action_enabled_hdmi_pip", action)) {
                Log.d(TAG,"doAppPrivateCmd action = " + action);
                mIsPip = true;
            }
        }
        public int mParentControlDelay = 3000;
        protected void doParentalControls(ChannelInfo channelInfo) {
            if (mHandler != null)
                mHandler.removeMessages(MSG_PARENTAL_CONTROL_AV);

            if (mTvInputManager == null)
                mTvInputManager = (TvInputManager)getSystemService(Context.TV_INPUT_SERVICE);

            //Log.d(TAG, "doPC:"+this);
            Log.d(TAG, "doParentalControls:"+channelInfo);
            boolean isParentalControlsEnabled = mTvInputManager.isParentalControlsEnabled();
            if (isParentalControlsEnabled) {
                TvContentRating blockContentRating = getContentRatingOfCurrentProgramBlocked(channelInfo);
                if (blockContentRating != null) {
                    Log.d(TAG, "Check parental controls: blocked by content rating - "
                            + blockContentRating.flattenToString());
                } else {
                    //Log.d(TAG, "Check parental controls: available");
                }
                updateChannelBlockStatus(blockContentRating != null, blockContentRating, channelInfo);
            } else {
                //Log.d(TAG, "Check parental controls: disabled");
                updateChannelBlockStatus(false, null, channelInfo);
            }

           if (mHandler != null) {
                if (false) {
                   /* TvTime TvTime = new TvTime(mContext);
                    Program mCurrentProgram = mTvDataBaseManager.getProgram(TvContract.buildChannelUri(channelInfo.getId()), TvTime.getTime());
                    Program mNextProgram = null;
                    if (mCurrentProgram != null)
                        mNextProgram = mTvDataBaseManager.getProgram(TvContract.buildChannelUri(channelInfo.getId()), mCurrentProgram.getEndTimeUtcMillis() + 1);
                    mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_PARENTAL_CONTROL_AV, this),
                        (mNextProgram == null ? mParentControlDelay : mNextProgram.getStartTimeUtcMillis() - TvTime.getTime()));*/
                    //Log.d(TAG, "doPC next:"+(mNextProgram == null ? mParentControlDelay : mNextProgram.getStartTimeUtcMillis() - TvTime.getTime())+"ms");
                    Log.d(TAG, "doPC next");
                } else {
                    mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_PARENTAL_CONTROL_AV, this), mParentControlDelay);
                    Log.d(TAG, "---doPC next:"+mParentControlDelay);
                }
            }
        }

        protected TvContentRating[] getContentRatingsOfCurrentProgram(ChannelInfo channelInfo) {
            Log.d(TAG, "getContentRatingsOfCurrentProgram:"+channelInfo);
            TvContentRating[] ratings = mATVContentRatings;
            if (ratings == null || ratings.length == 0)
                ratings = CustomerOps.getInstance(mContext).getCustomerNoneRatings();
            return ratings;
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

        @Override
        public boolean onSelectTrack(int type, String trackId) {
            int index = -1;
            notifyTrackSelected(type, trackId);
            if (type == TvTrackInfo.TYPE_SUBTITLE) {
                stopSubtitle();
                ChannelInfo.Subtitle subtitle = parseSubtitleIdString(trackId);
                if (subtitle == null) {
                    index = -2;
                    mCurrentSubtitle = null;
                } else {
                    index = subtitle.id;
                    mCurrentSubtitle = subtitle;
                    startSubtitle();
                }
                CustomerOps.getInstance(mContext).saveAvClosedCaptionIndex(index);
                Log.d(TAG, "onSelectTrack: [type:" + type + "] [id:" + trackId + "] " + "index" + index);
            }
            return true;
        }

        @Override
        public void onSubtitleData(String json) {
            Log.d(TAG, "onSubtitleData curchannel:"+(mCurrentChannel!=null?mCurrentChannel.toString():"null"));
            Log.d(TAG, "onSubtitleData json:"+json);

            int mask = DroidLogicTvUtils.getObjectValueInt(json, "cc", "data", -1);
            if (mask != -1) {
                sendCCDataInfoByTif(mask);
                if (mHandler != null) {
                    Message msg = mHandler.obtainMessage(MSG_CC_DATA, this);
                    msg.arg1 = mask;
                    msg.sendToTarget();
                }
                Log.d(TAG, "ccc send data");
                return;
            }

            mATVContentRatings = DroidLogicTvUtils.parseARatings(json);
            sendAvRatingByTif();
            if (mHandler != null)
                mHandler.sendMessage(mHandler.obtainMessage(MSG_PARENTAL_CONTROL_AV, this));
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
            } else if (status == DTVSubtitleView.TT_NOTIFY_NOSIG) {
                stop_teletext();
                Toast.makeText(mContext, "No teletext", Toast.LENGTH_SHORT).show();
            }
        }

        private void sendAvRatingByTif() {
            Bundle ratingbundle = new Bundle();
            ratingbundle.putString(DroidLogicTvUtils.SIG_INFO_AV_VCHIP_KEY, Program.contentRatingsToString(mATVContentRatings));
            notifySessionEvent(DroidLogicTvUtils.SIG_INFO_AV_VCHIP, ratingbundle);
        }

        private void sendCCDataInfoByTif(final int mask) {
            Bundle ratingbundle = new Bundle();
            ratingbundle.putInt(DroidLogicTvUtils.SIG_INFO_CC_DATA_INFO_KEY, mask);
            notifySessionEvent(DroidLogicTvUtils.SIG_INFO_CC_DATA_INFO, ratingbundle);
        }

        private int mCurrentCCExist = 0;
        private int mCurrentCCStyle = -1;
        private boolean mCurrentCCEnabled = false;
        public void doCCData(int mask) {
            Log.d(TAG, "cc data: " + mask);

            if ((mask != (1 << 16)) && (mask & (1 << 15)) == 0) {
                if (mATVContentRatings != null) {
                    mATVContentRatings = null;
                    sendAvRatingByTif();
                    checkContentBlockNeeded(mCurrentChannel);
                }
            }
        }

        protected void tryPreferredSubtitleContinue(int exist) {
            synchronized (mSubtitleLock) {
                if (tryPreferredSubtitle(exist) == -1) {
                    Log.d(TAG,"tryPreferredSubtitleContinue,mCurrentCCStyle:"+mCurrentCCStyle);
                    startSubtitleCCBackground();
                    notifyTrackSelected(TvTrackInfo.TYPE_SUBTITLE, null);
                }
                mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_CC_TRY_PREFERRED, mCurrentCCExist, 0, this),
                                DELAY_TRY_PREFER_CC);
            }
        }
        protected int tryPreferredSubtitle(int exist) {
            if (mSystemControlManager != null) {
                int to =  mSystemControlManager.getPropertyInt(DTV_SUBTITLE_CC_PREFER, -1);

                Log.d(TAG, "ccc tryPrefer, exist["+exist+"] to["+to+"] Enable["+mCurrentCCEnabled+"]");

                if (mCurrentCCStyle == to)//already show
                    return 0;

                if (to != -1 && (mCurrentCCStyle != to)) {
                    mCurrentCCStyle = to;
                    startSubtitle();//startSubtitle(s);
                    notifyTrackSelected(TvTrackInfo.TYPE_SUBTITLE, null);
                }
                Log.d(TAG,"tryPreferredSubtitle,return:"+to);
                return to;
            }
            return 0;
        }

        protected void startSubtitleCCBackground() {
            Log.d(TAG, "start bg cc for xds");
            startSubtitle();
            enableSubtitleShow(false);
            //mCurrentCCStyle = -1;
            //mSystemControlManager.setProperty(DTV_SUBTITLE_TRACK_IDX, "-3");
        }

        protected void checkCurrentContentBlockNeeded() {
            Log.d(TAG, "checkCurrentContentBlockNeeded");
            checkContentBlockNeeded(mCurrentChannel);
        }

        protected void checkContentBlockNeeded(ChannelInfo channelInfo) {
            //doParentalControls(channelInfo);
            Log.d(TAG, "checkContentBlockNeeded:"+channelInfo);
            doParentalControls(channelInfo);
        }

        private void updateChannelBlockStatus(boolean channelBlocked,
                TvContentRating contentRating, ChannelInfo channelInfo) {
            if (channelInfo == null) {
                Log.d(TAG,"channelInfo is null ,exit updateChannelBlockStatus");
               // return;
            }
            Log.d(TAG, "updateBlock:"+channelBlocked + " curBlock:"+mChannelBlocked + " channel:"+channelInfo);

            //only for block norationg function
            TvContentRating tcr = TvContentRating.createRating("com.android.tv", "block_norating", "block_norating", "");

            boolean needChannelBlock = channelBlocked;
            Log.d(TAG, "isBlockNoRatingEnable:"+isBlockNoRatingEnable+",isUnlockCurrent_NR:"+isUnlockCurrent_NR);
            //add for no-rating block
            boolean isParentControlEnabled = mTvInputManager.isParentalControlsEnabled();
            TvContentRating currentBlockRatting = getCurrentRating();
            if ((mATVContentRatings == null || (currentBlockRatting != null && currentBlockRatting.getMainRating().equals("None")))
                    && isBlockNoRatingEnable && !isUnlockCurrent_NR) {
                needChannelBlock = true;
            }

            Log.d(TAG, "needChannelBlock:"+needChannelBlock);
            needChannelBlock = isParentControlEnabled & needChannelBlock;
            Log.d(TAG, "updated needChannelBlock:"+needChannelBlock);

            if ((mChannelBlocked != -1) && (mChannelBlocked == 1) == needChannelBlock
                    && (!needChannelBlock || (needChannelBlock && contentRating != null && contentRating.equals(mLastBlockedRating))))
                return;

            mChannelBlocked = (needChannelBlock ? 1 : 0);
            if (needChannelBlock) {
                if (contentRating != null) {
                    Log.d(TAG, "notifyBlock:"+contentRating.flattenToString());
                    notifyContentBlocked(contentRating);
                } else if (isBlockNoRatingEnable) {
                    Log.d(TAG, "notifyBlock because of block_norating:"+tcr.flattenToString());
                    notifyContentBlocked(tcr);
                }
                mLastBlockedRating = contentRating;
            } else {
                synchronized(mLock) {
                   // if (mCurrentChannel != null) {
                        playProgram(mCurrentChannel);
                        Log.d(TAG, "notifyAllowed");
                        notifyContentAllowed();
                   // }
                }
            }
        }

        private boolean playProgram(ChannelInfo info) {
            Log.d(TAG,"playProgram");

            notifyTracks(info);
            startSubtitle();

            return true;
        }

        private void start_teletext()
        {
            if (pal_teletext_subtitle != null) {
//                            Toast.makeText(mContext, "Searching teletext", Toast.LENGTH_SHORT).show();
                setSubtitleParam(ChannelInfo.Subtitle.TYPE_ATV_TELETEXT,
                        0,
                        0,
                        1,
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

        protected void startSubtitle() {
            Log.d(TAG, "start Subtitle:");
            startSubtitleAutoAnalog();
        }

        public void reset_atv_status()
        {
            tt_subpg_walk_mode = false;
            if (tt_display_mode == DTVSubtitleView.TTX_MIX_MODE_LEFT_RIGHT) {
                Rect rect = new Rect();
                mSubtitleView.getGlobalVisibleRect(rect);
                layoutSurface(rect.left, rect.top, rect.right, rect.bottom);
            }
            mSubtitleView.reset_atv_status();
            Log.e(TAG, "reset_atv_status done");
        }

        boolean teletext_switch = false;
        int tt_display_mode = DTVSubtitleView.TTX_MIX_MODE_NORAML;
        boolean tt_subpg_walk_mode = false;

        @Override
        public boolean onKeyUp(int keyCode, KeyEvent event) {
            if (signalFmt != SIGNAL_PAL_FMT)
                return super.onKeyUp(keyCode, event);
            else {
                switch (keyCode) {
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

        private int reg_id = 0;
        @Override
        public boolean onKeyDown(int keyCode, KeyEvent event) {
            Log.e(TAG, "keycode down: " + keyCode + " tt_switch " + teletext_switch);
            //Teletext is not opened.
            if ((!teletext_switch && keyCode != KEY_TELETEXT_SWITCH))
                return super.onKeyDown(keyCode, event);

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

        private void stop_teletext()
        {
            enableSubtitleShow(false);
            teletext_switch = false;
            reset_atv_status();
            mSubtitleView.stop();
            notifyTrackSelected(TvTrackInfo.TYPE_SUBTITLE, null);
        }

        protected void prepareTeletext(List<ChannelInfo.Subtitle> subtitles)
        {
            pal_teletext_subtitle
                    = new ChannelInfo.Subtitle(ChannelInfo.Subtitle.TYPE_ATV_TELETEXT,
                    1,
                    ChannelInfo.Subtitle.TYPE_ATV_TELETEXT,
                    0,
                    0,
                    "TELETEXT",
                    0);
            subtitles.add(pal_teletext_subtitle);
        }


        protected void startSubtitleAutoAnalog() {
            Log.d(TAG, "start Subtitle AutoAnalog");

            if (mSubtitleView == null) {
                Log.d(TAG, "subtitle view current channel is null");
                return;
            }
            if (signalFmt == SIGNAL_NTSC_FMT) {
                Log.d(TAG, "mCurrentCCStyle:" + mCurrentCCStyle);
                int trackPid = -1;
                int temp = mCurrentCCStyle;
                if (temp == -1) {
                    int ccPrefer = mSystemControlManager.getPropertyInt(DTV_SUBTITLE_CC_PREFER, -1);
                    temp = ccPrefer > 0 ? ccPrefer : ChannelInfo.Subtitle.CC_CAPTION_VCHIP_ONLY;//parse xds vchip only
                }
                mSubtitleView.stop();
                if (mCurrentSubtitle != null) {
                    Log.d(TAG, "subtitle pid = " + mCurrentSubtitle.mPid);
                    trackPid = mCurrentSubtitle.mPid;
                } else {
                    int trackIndex = CustomerOps.getInstance(mContext).getAvClosedCaptionIndex();
                    if (trackIndex >= 0
                        && mCurrentSubtitles != null
                        && (mCurrentSubtitles.size() > trackIndex)
                        && mCurrentSubtitles.get(trackIndex) != null) {
                        mCurrentSubtitle = mCurrentSubtitles.get(trackIndex);
                        trackPid = mCurrentSubtitle.mPid;
                    }
                }
                if (trackPid != -1) {
                    setSubtitleParam(ChannelInfo.Subtitle.TYPE_ATV_CC, trackPid,
                            mCurrentCCStyle == -1 ? temp : mCurrentCCStyle, 0, 0, "");
                } else {
                    setSubtitleParam(ChannelInfo.Subtitle.TYPE_ATV_CC, 15,
                            mCurrentCCStyle == -1 ? temp : mCurrentCCStyle, 0, 0, "");
                }
                mSubtitleView.setActive(true);
                mSubtitleView.startSub();
                enableSubtitleShow(true);

            } else if (signalFmt == SIGNAL_PAL_FMT) {
                if (teletext_switch) {
                    Log.d(TAG, "SIGNAL_PAL_FMT startSubtitleAutoAnalog");
                    start_teletext();
                }
            }

        }

        protected void enableSubtitleShow(boolean enable) {
            is_subtitle_enable = enable;
            if (mSubtitleView != null) {
                mSubtitleView.setVisible(enable);
                if (enable)
                    mSubtitleView.show();
                else
                    mSubtitleView.hide();
            }
            sendSessionMessage(enable ? MSG_SUBTITLE_SHOW : MSG_SUBTITLE_HIDE);
        }

        private TvContentRating getCurrentRating() {
            if (mATVContentRatings != null) {
                for (TvContentRating rating : mATVContentRatings) {
                    return rating;
                }
            }

            return null;
        }

        protected TvContentRating getContentRatingOfCurrentProgramBlocked(ChannelInfo channelInfo) {
            TvContentRating ratings[] = getContentRatingsOfCurrentProgram(channelInfo);
            if (ratings == null)
                return null;

            Log.d(TAG, "current Ratings:");
            for (TvContentRating rating : ratings) {
                Log.d(TAG, "\t" + rating.flattenToString());   //com.android.tv/US_MV/US_MV_G
            }

            for (TvContentRating rating : ratings) {
               if (!mUnblockedRatingSet.contains(rating) && mTvInputManager
                        .isRatingBlocked(rating)) {
                    return rating;
                }
            }
            return null;
        }

        public static final int MSG_PARENTAL_CONTROL_AV = 2;
        public static final int MSG_CC_DATA = 100;
        public static final int MSG_CC_TRY_PREFERRED = 101;

        protected void initWorkThread() {
            if (mHandlerThread == null) {
                mHandlerThread = new HandlerThread("DtvInputWorker");
                mHandlerThread.start();
                mMainHandler = new Handler();
                mHandler = new Handler(mHandlerThread.getLooper(), new Handler.Callback() {
                    @Override
                    public boolean handleMessage(Message msg) {
                        if (mCurrentSession == msg.obj) {
                            switch (msg.what) {
                                case MSG_PARENTAL_CONTROL_AV:
                                    Log.d(TAG,"MSG_PARENTAL_CONTROL_AV,checkContentBlockNeeded:"+mCurrentChannel);
                                    checkContentBlockNeeded(mCurrentChannel);
                                    break;
                                case MSG_CC_DATA:
                                    doCCData(msg.arg1);
                                    break;
                                case MSG_CC_TRY_PREFERRED:
                                    tryPreferredSubtitleContinue(msg.arg1);
                                    break;
                                default:
                                    break;
                            }
                        }
                        return false;
                    }
                });
            }
        }

        protected void stopSubtitle() {
            Log.d(TAG, "stop Subtitle");
            synchronized (mSubtitleLock) {
                if (mSubtitleView != null) {
                    mSubtitleView.stop();
                    enableSubtitleShow(false);
                }
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
            Log.d(TAG, ">> opacity:"+Integer.toHexString(opacity));
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

        protected String addSubtitleTracks(List <TvTrackInfo> tracks, ChannelInfo ch) {
            if (mCurrentSubtitles == null || mCurrentSubtitles.size() == 0)
                return null;

            Log.d(TAG, "add subtitle tracks[" + mCurrentSubtitles.size() + "]");

            int auto = (mCurrentSubtitle == null) ? -1 : mCurrentSubtitle.id;
            if (auto != -1) {
                mCurrentSubtitle.id = CustomerOps.getInstance(mContext).getAvClosedCaptionIndex();
                auto = mCurrentSubtitle.id;
            } else if (mCurrentSubtitle == null) {
                int trackIndex = CustomerOps.getInstance(mContext).getAvClosedCaptionIndex();
                if (trackIndex >= 0
                        && mCurrentSubtitles != null
                        && (mCurrentSubtitles.size() > trackIndex)
                        && mCurrentSubtitles.get(trackIndex) != null) {
                    mCurrentSubtitle = mCurrentSubtitles.get(trackIndex);
                    auto = mCurrentSubtitle.id;
                }
            }
            Iterator<ChannelInfo.Subtitle> iter = mCurrentSubtitles.iterator();
            while (iter.hasNext()) {
                ChannelInfo.Subtitle s = iter.next();
                String Id = generateSubtitleIdString(s);
                TvTrackInfo SubtitleTrack =
                        new TvTrackInfo.Builder(TvTrackInfo.TYPE_SUBTITLE, Id)
                                .setLanguage(s.mLang)
                                .build();
                tracks.add(SubtitleTrack);

                Log.d(TAG, "\t" + (((auto==s.id))? ("*"+s.id+":[") : (""+s.id+": [")) + s.mLang + "]"
                        + " [pid:" + s.mPid + "] [type:" + s.mType + "]");
                Log.d(TAG, "\t" + "   [id1:" + s.mId1 + "] [id2:" + s.mId2 + "] [stype:" + s.mStype + "]");
            }

            if (auto >= 0 && mCurrentSubtitles.size() > auto)
                return generateSubtitleIdString(mCurrentSubtitles.get(auto));

            return null;
        }

        protected void notifyTracks(ChannelInfo ch) {
            List < TvTrackInfo > tracks = new ArrayList<>();;
            String AudioSelectedId = null;
            String SubSelectedId = null;
            mCurrentSubtitles = new ArrayList<ChannelInfo.Subtitle>();
            if (signalFmt == SIGNAL_NTSC_FMT) {
                Log.e(TAG, "SIGNAL_NTSC_FMT prepare cc");
                int count = 0;
                for (int i=0; i<4; i++)
                {
                    ChannelInfo.Subtitle sub = new ChannelInfo.Subtitle(
                            ChannelInfo.Subtitle.TYPE_ATV_CC,
                            ChannelInfo.Subtitle.CC_CAPTION_CC1 + i,
                            ChannelInfo.Subtitle.TYPE_ATV_CC,
                            0,
                            0,
                            "CC"+(i+1),
                            count++);
                    mCurrentSubtitles.add(sub);
                }
                for (int i=0; i<4; i++) {
                    ChannelInfo.Subtitle s = new ChannelInfo.Subtitle(
                            ChannelInfo.Subtitle.TYPE_ATV_CC,
                            ChannelInfo.Subtitle.CC_CAPTION_TEXT1 + i,
                            ChannelInfo.Subtitle.TYPE_ATV_CC,
                            0,
                            0,
                            "TX"+(i+1),
                            count++);
                    mCurrentSubtitles.add(s);
                }
            } else if (signalFmt == SIGNAL_PAL_FMT){
                Log.e(TAG, "SIGNAL_PAL_FMT prepare teletext");
                prepareTeletext(mCurrentSubtitles);
            } else if (signalFmt == SIGNAL_NOT_KNOWN)
                return;

            SubSelectedId = addSubtitleTracks(tracks, ch);

            if (tracks != null) {
                Log.d(TAG, "notify Tracks["+tracks.size()+"]");
                notifyTracksChanged(tracks);
            }

            Log.d(TAG, "\tAuto Aud: [" + AudioSelectedId + "]");
            notifyTrackSelected(TvTrackInfo.TYPE_AUDIO, AudioSelectedId);

            Log.d(TAG, "\tAuto Sub: [" + SubSelectedId + "]");
            notifyTrackSelected(TvTrackInfo.TYPE_SUBTITLE, SubSelectedId);

            //update mCurrentSubtitle as selected subtitle
            ChannelInfo.Subtitle subtitle = parseSubtitleIdString(SubSelectedId);
            if (subtitle != null) {
                Log.d(TAG, "update mCurrentSubtitle as selected subtitle");
                mCurrentSubtitle = subtitle;
            }
        }

        private int getRawUserStyle(){
           //TODO
           /* try {
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

        protected CCStyleParams getCaptionStyle() {
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
            Log.d(TAG, "get style: " + style + ", fontStyle" + fontStyle + ", typeface: " + getRawTypeface(userStyle));

            int fg = userStyle.foregroundColor;
            int bg = userStyle.backgroundColor;

            int convert_fg_color = USE_NEW_CCVIEW? fg_color : getColor(fg_color);
            int convert_fg_opacity = USE_NEW_CCVIEW? fg_opacity : getOpacity(fg_opacity);
            int convert_bg_color = USE_NEW_CCVIEW? bg_color : getColor(bg_color);
            int convert_bg_opacity = USE_NEW_CCVIEW? bg_opacity : getOpacity(bg_opacity);
            float convert_font_size = USE_NEW_CCVIEW? textSize: getFontSize(textSize);
            Log.d(TAG, "Caption font size:"+convert_font_size+" ,fg_color:"+Integer.toHexString(fg)+
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
    }

    public String getDeviceClassName() {
        return AV1InputService.class.getName();
    }

    public int getDeviceSourceType() {
        return DroidLogicTvUtils.DEVICE_ID_AV1;
    }
}
