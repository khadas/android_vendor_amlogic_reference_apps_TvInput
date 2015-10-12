package com.droidlogic.tv;


import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.droidlogic.app.DroidLogicKeyEvent;
import com.droidlogic.app.tv.ChannelDataManager;
import com.droidlogic.app.tv.DroidLogicTvUtils;
import com.droidlogic.ui.SourceButton;
import com.droidlogic.ui.SourceButton.OnSourceClickListener;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.media.tv.TvView;
import android.media.tv.TvView.TvInputCallback;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DroidLogicTv extends Activity implements Callback, OnSourceClickListener {
    private static final String TAG = "DroidLogicTv";
    private static final String SHARE_NAME = "tv_app";

    private Context mContext;
    private TvInputManager mTvInputManager;
    private ChannelDataManager mChannelDataManager;

    private TvView mSourceView;
    private SourceButton mSourceInput;

    private LinearLayout mSourceMenuLayout;
    private LinearLayout mSourceInfoLayout;

    //max index of all hardware devices in mSourceMenuLayout
    private int maxHardwareIndex = 0;

    private int mSigType;
    private String mSigLabel;

    private boolean isNoSignal;
    private boolean isNoSignalShowing;
    private boolean isSourceMenuShowing;
    private boolean isSourceInfoShowing;

    private Timer delayTimer = null;
    private int delayCounter = 0;

    private Handler mHandler;
    private static final int MSG_INFO_DELAY = 0;
    private static final int MSG_INFO_DELAY_TIME = 5;
    private static final int MSG_SOURCE_DELAY = 1;
    private static final int MSG_SOURCE_DELAY_TIME = 5;

    private static final int START_SETUP = 0;
    private boolean needUpdateSource = true;
    //if activity has been stopped, source input must be switched again.
    private boolean hasStopped = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.logd(TAG, "==== onCreate ====");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        initVideoView();
        init();
    }

    private void initVideoView() {
        ViewGroup root = (ViewGroup) getWindow().getDecorView().findViewById(android.R.id.content);
        SurfaceView surfaceView = new SurfaceView(this);
        root.addView(surfaceView, 0);
        if (surfaceView != null) {
            surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceDestroyed(SurfaceHolder arg0) {
                    // TODO Auto-generated method stub
                }

                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    MediaPlayer mediaPlayer = new MediaPlayer();
                    mediaPlayer.reset();
                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    try {
                        mediaPlayer.setDataSource("tvin:test");
                        mediaPlayer.setDisplay(holder);
                        mediaPlayer.prepare();
                    } catch (Exception e) {
                        Utils.loge(TAG, e.toString());
                    }
                    mediaPlayer.start();
                }

                @Override
                public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
                    // TODO Auto-generated method stub
                }
            });
        }
    }

    private void init() {
        mContext = getApplicationContext();
        mTvInputManager = (TvInputManager)getSystemService(Context.TV_INPUT_SERVICE);
        mChannelDataManager = new ChannelDataManager(getApplicationContext());

        mHandler = new Handler(this);

        mSourceView = (TvView) findViewById(R.id.source_view);
        mSourceView.setCallback(new DroidLogicInputCallback());

        mSourceMenuLayout = (LinearLayout)findViewById(R.id.menu_layout);
        mSourceInfoLayout = (LinearLayout)findViewById(R.id.info_layout);

        initSourceMenuLayout();
    }

    /**
     * Add given number views, the number depends on the input services have registered.
     * The total number must be one more than the views need add, because there is a {@link TextView}
     * have been added {@link main.xml}.
     */
    private void initSourceMenuLayout() {
        List<TvInputInfo> input_list = mTvInputManager.getTvInputList();
        if (mSourceMenuLayout.getChildCount() > 1) {
            mSourceMenuLayout.removeViews(1, mSourceMenuLayout.getChildCount()-1);
        }
        for (TvInputInfo info : input_list) {
            SourceButton sb = new SourceButton(mContext, info);
            if (sb.isHardware()) {
                setDefaultChannelInfo(sb);
                if (maxHardwareIndex == 0) {
                    mSourceMenuLayout.addView(sb, 1);
                    maxHardwareIndex++;
                } else {
                    int lo = 1;
                    int hi = maxHardwareIndex;

                    while (lo <= hi) {
                        final int mid = (lo + hi) >>> 1;
                        final SourceButton temp = (SourceButton) mSourceMenuLayout.getChildAt(mid);
                        final int temp_id = temp.getDeviceId();
                        if (temp_id < sb.getDeviceId()) {
                            lo = mid + 1;
                        } else if (temp_id > sb.getDeviceId()) {
                            hi = mid - 1;
                        }
                    }
                    mSourceMenuLayout.addView(sb, lo);
                    maxHardwareIndex++;
                }
            }else {
                mSourceMenuLayout.addView(sb);
            }
            sb.setOnSourceClickListener(this);
        }
    }

    @Override
    protected void onResume() {
        Utils.logd(TAG, "== onResume ====");

        if (hasStopped || needUpdateSource) {
            switchToSourceInput();
        }
        hasStopped = false;
        needUpdateSource = true;

        popupSourceInfo(Utils.SHOW_VIEW);
        super.onResume();
    }

    /**
     * get the default source input after {@code this.onCreate}.
     */
    private void setDefaultChannelInfo(SourceButton sb) {
        int device_id, atv_channel, dtv_channel;
        boolean is_radio;
        SharedPreferences sp = getSharedPreferences(SHARE_NAME, Context.MODE_PRIVATE);
        device_id = sp.getInt("device_id", 0);
        atv_channel = sp.getInt("atv_channel", -1);
        dtv_channel = sp.getInt("dtv_channel", -1);
        is_radio = sp.getBoolean("is_radio", false);
        if (sb.getSourceType() == DroidLogicTvUtils.SOURCE_TYPE_ATV && atv_channel >= 0) {
            sb.moveToChannel(atv_channel, is_radio);
        } else if (sb.getSourceType() == DroidLogicTvUtils.SOURCE_TYPE_DTV && dtv_channel >= 0) {
            sb.moveToChannel(dtv_channel, is_radio);
        }

        if (device_id == sb.getDeviceId()) {
            mSourceInput = sb;
            mSigType = getSigType(sb.getSourceType());
        }
    }

    /**
     * must be invoked after {@link SourceButton.moveToChannel}.
     * if there is nothing about channel switching in the tv.db, the {@code channel_uri}
     * used to tune must be wrong.
     */
    private void switchToSourceInput() {
        Uri channel_uri = mSourceInput.getUri();
        Utils.logd(TAG, "channelUri switching to is " + channel_uri);

        mSourceView.tune(mSourceInput.getInputId(), channel_uri);
        if (isSourceMenuShowing)
            popupSourceMenu(Utils.HIDE_VIEW);
    }

    private void startSetupActivity () {
        TvInputInfo info = mTvInputManager.getTvInputInfo(mSourceInput.getInputId());
        Intent intent = info.createSetupIntent();
        startActivityForResult(intent, START_SETUP);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == DroidLogicTvUtils.RESULT_OK) {
            needUpdateSource = false;
        } else if (resultCode == DroidLogicTvUtils.RESULT_UPDATE
                || resultCode == DroidLogicTvUtils.RESULT_FAILED) {
            needUpdateSource = true;
        }
        Utils.logd(TAG, "====onActivityResult, requestCode=" + requestCode + ", resultCode=" + resultCode);

    }

    private int getSigType(int source_type) {
        int ret = 0;
        switch (source_type) {
            case DroidLogicTvUtils.SOURCE_TYPE_ATV:
                ret = DroidLogicTvUtils.SIG_INFO_TYPE_ATV;
                break;
            case DroidLogicTvUtils.SOURCE_TYPE_DTV:
                ret = DroidLogicTvUtils.SIG_INFO_TYPE_DTV;
                break;
            case DroidLogicTvUtils.SOURCE_TYPE_AV1:
            case DroidLogicTvUtils.SOURCE_TYPE_AV2:
                ret = DroidLogicTvUtils.SIG_INFO_TYPE_AV;
                break;
            case DroidLogicTvUtils.SOURCE_TYPE_HDMI1:
            case DroidLogicTvUtils.SOURCE_TYPE_HDMI2:
            case DroidLogicTvUtils.SOURCE_TYPE_HDMI3:
                ret = DroidLogicTvUtils.SIG_INFO_TYPE_HDMI;
                break;
            default:
                ret = DroidLogicTvUtils.SIG_INFO_TYPE_OTHER;
                break;
        }
        return ret;
    }

    @Override
    public void onButtonClick(SourceButton sb) {
        Utils.logd(TAG, "==== onButtonClick ====" + sb);
        if (!TextUtils.isEmpty(mSigLabel) && mSigLabel.equals(sb.getLabel()))
            return;
        saveDefaultChannelId();
        mSourceInput = sb;
        mSigType = getSigType(sb.getSourceType());
        switchToSourceInput();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Utils.logd(TAG, "====keycode =" + keyCode);

        if (isSourceMenuShowing) {
            createDelayTimer(MSG_SOURCE_DELAY, MSG_SOURCE_DELAY_TIME);
        }
        switch (keyCode) {
            case DroidLogicKeyEvent.KEYCODE_TV_SHORTCUTKEY_SOURCE_LIST:
                popupSourceMenu(isSourceMenuShowing ? Utils.HIDE_VIEW : Utils.SHOW_VIEW);
                return true;
            case DroidLogicKeyEvent.KEYCODE_MENU:
                if (isSourceMenuShowing) {
                    popupSourceMenu(Utils.HIDE_VIEW);
                } else if (isSourceInfoShowing) {
                    popupSourceInfo(Utils.HIDE_VIEW);
                }
                startSetupActivity();
                break;
            case DroidLogicKeyEvent.KEYCODE_TV_SHORTCUTKEY_TVINFO:
                popupSourceInfo(Utils.SHOW_VIEW);
                return true;
            case DroidLogicKeyEvent.KEYCODE_BACK:
                if (isSourceMenuShowing) {
                    popupSourceMenu(Utils.HIDE_VIEW);
                    return true;
                }
                break;
            case DroidLogicKeyEvent.KEYCODE_CHANNEL_UP:
                mSourceInput.channelUp();
                switchToSourceInput();
                break;
            case DroidLogicKeyEvent.KEYCODE_CHANNEL_DOWN:
                mSourceInput.channelDown();
                switchToSourceInput();
                break;
            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void popupSourceMenu(boolean show_or_hide) {//ture:show
        if (!show_or_hide) {
            destroyDelayTimer();
            isSourceMenuShowing = false;
            mSourceMenuLayout.setVisibility(View.INVISIBLE);
            popupSourceInfo(Utils.SHOW_VIEW);
        } else {
            isSourceMenuShowing = true;
            mSourceMenuLayout.setVisibility(View.VISIBLE);
            mSourceMenuLayout.requestLayout();
            mSourceInput.requestFocus();
            if (isSourceInfoShowing)
                popupSourceInfo(Utils.HIDE_VIEW);
            if (isNoSignalShowing)
                popupNoSignal(Utils.HIDE_VIEW);
            createDelayTimer(MSG_SOURCE_DELAY, MSG_SOURCE_DELAY_TIME);
        }
    }

    private void popupNoSignal(boolean show_or_hide) {//true:show
        TextView no_signal = (TextView)findViewById(R.id.no_signal);
        if (!show_or_hide) {
            isNoSignalShowing = false;
            no_signal.setVisibility(View.INVISIBLE);
        } else {
            isNoSignalShowing = true;
            no_signal.setVisibility(View.VISIBLE);
            no_signal.requestLayout();
            if (isSourceInfoShowing)
                popupSourceInfo(Utils.HIDE_VIEW);
        }
    }

    private void popupSourceInfo(boolean show_or_hide) {//true:show
        if (!show_or_hide) {
            destroyDelayTimer();
            isSourceInfoShowing = false;
            mSourceInfoLayout.setVisibility(View.INVISIBLE);
        } else {
            mSourceInfoLayout.removeAllViews();
            switch (mSigType) {
                case DroidLogicTvUtils.SIG_INFO_TYPE_ATV:
                    initATVInfo();
                    break;
                case DroidLogicTvUtils.SIG_INFO_TYPE_DTV:
                    initDTVInfo();
                    break;
                case DroidLogicTvUtils.SIG_INFO_TYPE_AV:
                    initAVInfo();
                    break;
                case DroidLogicTvUtils.SIG_INFO_TYPE_HDMI:
                    initHmdiInfo();
                    break;
                default:
                    initOtherInfo();
                    break;
            }
            if (isNoSignalShowing) {
                popupNoSignal(Utils.HIDE_VIEW);
            }
            if (isSourceMenuShowing) {
                popupSourceMenu(Utils.HIDE_VIEW);
            }
            mSourceInfoLayout.setVisibility(View.VISIBLE);
            isSourceInfoShowing = true;
            createDelayTimer(MSG_INFO_DELAY, MSG_INFO_DELAY_TIME);
        }
    }

    private void initOtherInfo() {
        TextView tv_name;
        TextView tv_number;
        TextView tv_val;
        if (mSourceInfoLayout.getChildCount() == 0) {
            LayoutInflater inflate = LayoutInflater.from(mContext);
            mSourceInfoLayout.addView(inflate.inflate(R.layout.atv_dtv_info, mSourceInfoLayout, false));
        }
        tv_name = (TextView) findViewById(R.id.ad_info_name);
        tv_number = (TextView) findViewById(R.id.ad_info_number);
        tv_val = (TextView) findViewById(R.id.ad_info_value);
        tv_name.setText(mSourceInput.getLabel());
        tv_number.setText(mSourceInput.getChannelNumber());
        tv_val.setText(mSourceInput.getChannelName());
    }

    private void initATVInfo() {
        TextView tv_name;
        TextView tv_number;
        TextView tv_val;
        if (mSourceInfoLayout.getChildCount() == 0) {
            LayoutInflater inflate = LayoutInflater.from(mContext);
            mSourceInfoLayout.addView(inflate.inflate(R.layout.atv_dtv_info, mSourceInfoLayout, false));
        }
        tv_name = (TextView) findViewById(R.id.ad_info_name);
        tv_number = (TextView) findViewById(R.id.ad_info_number);
        tv_val = (TextView) findViewById(R.id.ad_info_value);
        tv_name.setText(mSourceInput.getLabel());
        int number = mSourceInput.getChannelId();
        if (number < 0) {
            tv_number.setText("");
        } else {
            tv_number.setText(Integer.toString(mSourceInput.getChannelId()));
        }
        tv_val.setText(mSourceInput.getChannelType());
    }

    private void initDTVInfo() {
        TextView tv_name;
        TextView tv_number;
        TextView tv_val;
        if (mSourceInfoLayout.getChildCount() == 0) {
            LayoutInflater inflate = LayoutInflater.from(mContext);
            mSourceInfoLayout.addView(inflate.inflate(R.layout.atv_dtv_info, mSourceInfoLayout, false));
        }
        tv_name = (TextView) findViewById(R.id.ad_info_name);
        tv_number = (TextView) findViewById(R.id.ad_info_number);
        tv_val = (TextView) findViewById(R.id.ad_info_value);
        if (mSourceInput.isRadioChannel()) {
            tv_name.setText(getResources().getString(R.string.radio_label));
        } else {
            tv_name.setText(mSourceInput.getLabel());
        }
        int number = mSourceInput.getChannelId();
        if (number < 0) {
            tv_number.setText("");
        } else {
            tv_number.setText(Integer.toString(mSourceInput.getChannelId()));
        }
        tv_val.setText(mSourceInput.getChannelName());
    }

    private void initAVInfo() {
        TextView tv_name;
        TextView tv_val;
        if (mSourceInfoLayout.getChildCount() == 0) {
            LayoutInflater inflate = LayoutInflater.from(mContext);
            mSourceInfoLayout.addView(inflate.inflate(R.layout.hdmi_av_info, mSourceInfoLayout, false));
        }
        tv_name = (TextView) findViewById(R.id.ha_info_name);
        tv_val = (TextView) findViewById(R.id.ha_info_value);
        tv_name.setText(mSourceInput.getLabel());
        tv_val.setText(mSourceInput.getChannelType());
    }

    private void initHmdiInfo() {
        TextView tv_type;
        TextView tv_val;
        if (mSourceInfoLayout.getChildCount() == 0) {
            LayoutInflater inflate = LayoutInflater.from(mContext);
            mSourceInfoLayout.addView(inflate.inflate(R.layout.hdmi_av_info, mSourceInfoLayout, false));
        }
        tv_type = (TextView) findViewById(R.id.ha_info_name);
        tv_val = (TextView) findViewById(R.id.ha_info_value);
        tv_type.setText(mSourceInput.getLabel());
        tv_val.setText(mSourceInput.getChannelVideoFormat());
    }

    private void processSessionEvent(String inputId, String eventType, Bundle eventArgs) {
        if (eventType.equals(DroidLogicTvUtils.SIG_INFO_EVENT)) {//sig_info
            mSigType = getSigType(mSourceInput.getSourceType());
            String args = "";

            switch (mSigType) {
                case DroidLogicTvUtils.SIG_INFO_TYPE_HDMI:
                    args = eventArgs.getString(DroidLogicTvUtils.SIG_INFO_ARGS);
                    String[] temp = args.split("_");
                    mSourceInput.setChannelVideoFormat(temp[0] + "_" + temp[1]);
                    break;
                case DroidLogicTvUtils.SIG_INFO_TYPE_AV:
                    args = eventArgs.getString(DroidLogicTvUtils.SIG_INFO_ARGS);
                    mSourceInput.setChannelType(args);
                    break;
                default:
                    break;
            }
            popupSourceInfo(Utils.SHOW_VIEW);
        }
    }

    @Override
    protected void onPause() {
        Utils.logd(TAG, "==== onPause ====");
        super.onPause();
    }

    @Override
    protected void onStop() {
        Utils.logd(TAG, "==== onStop ====");
        hasStopped = true;
        saveDefaultChannelInfo();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Utils.logd(TAG, "==== onDestroy ====");
        mChannelDataManager.release();
        mSourceView.reset();
        super.onDestroy();
    }

    private void saveDefaultChannelInfo() {
        if (mSourceInput.getSourceType() != DroidLogicTvUtils.SOURCE_TYPE_OTHER) {
            SharedPreferences sp = getSharedPreferences(SHARE_NAME, Context.MODE_PRIVATE);
            sp.edit().putInt("device_id", mSourceInput.getDeviceId()).commit();
            saveDefaultChannelId();
        }
    }

    private void saveDefaultChannelId() {
        int id = mSourceInput.getChannelId();
        int type = mSourceInput.getSourceType();
        boolean is_radio = mSourceInput.isRadioChannel();

        if (id < 0)
            return;

        SharedPreferences sp = getSharedPreferences(SHARE_NAME, Context.MODE_PRIVATE);
        Editor edit = sp.edit();
        if (type == DroidLogicTvUtils.SOURCE_TYPE_ATV) {
            edit.putInt("atv_channel", id).commit();
        } else if (type == DroidLogicTvUtils.SOURCE_TYPE_DTV) {
            edit.putInt("dtv_channel", id);
            edit.putBoolean("is_radio", is_radio);
            edit.commit();
        }
    }

    private void createDelayTimer(final int msg_event, final int time){
        destroyDelayTimer();
        delayTimer = new Timer();
        TimerTask task = new TimerTask(){
            @Override
            public void run() {
                mHandler.obtainMessage(msg_event, time).sendToTarget();
            }
        };
        delayTimer.schedule(task, 0, 1000);
    }

    private void destroyDelayTimer(){
        if (delayTimer != null) {
            delayTimer.cancel();
            delayTimer = null;
        }
        delayCounter = 0;
    }

    @Override
    public boolean handleMessage(Message msg) {
        int max_counter;
        switch (msg.what) {
            case MSG_INFO_DELAY:
                delayCounter++;
                max_counter = (int)msg.obj;
                if (delayCounter > max_counter) {
                    popupSourceInfo(Utils.HIDE_VIEW);
                    Utils.logd(TAG, "====isNoSignal =" + isNoSignal);
                    if (isNoSignal) {
                        popupNoSignal(Utils.SHOW_VIEW);
                    }
                }
                break;
            case MSG_SOURCE_DELAY:
                delayCounter++;
                max_counter = (int)msg.obj;
                if (delayCounter > max_counter) {
                    popupSourceMenu(Utils.HIDE_VIEW);
                }
                break;
            default:
                break;
        }
        return false;
    }

    public class DroidLogicInputCallback extends TvInputCallback {

        @Override
        public void onEvent(String inputId, String eventType, Bundle eventArgs) {
            Utils.logd(TAG, "====onEvent==inputId =" + inputId +", ===eventType ="+ eventType);
            processSessionEvent(inputId, eventType, eventArgs);
        }

        @Override
        public void onVideoAvailable(String inputId) {
            Utils.logd(TAG, "====onVideoAvailable==inputId =" + inputId);

            isNoSignal = false;
            popupNoSignal(Utils.HIDE_VIEW);
        }

        @Override
        public void onVideoUnavailable(String inputId, int reason) {
            Utils.logd(TAG, "====onVideoUnavailable==inputId =" + inputId +", ===reason ="+ reason);

            switch (reason) {
                case TvInputManager.VIDEO_UNAVAILABLE_REASON_UNKNOWN:
                case TvInputManager.VIDEO_UNAVAILABLE_REASON_TUNING:
                case TvInputManager.VIDEO_UNAVAILABLE_REASON_BUFFERING:
                    isNoSignal = true;
                    popupNoSignal(Utils.SHOW_VIEW);
                    break;
                default:
                    break;
            }
        }
    }

}
