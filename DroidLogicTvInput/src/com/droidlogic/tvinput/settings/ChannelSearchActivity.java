/*
 * Copyright (c) 2014 Amlogic, Inc. All rights reserved.
 *
 * This source code is subject to the terms and conditions defined in the
 * file 'LICENSE' which is part of this source code package.
 *
 * Description: JAVA file
 */

package com.droidlogic.tvinput.settings;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.text.TextUtils;
import android.text.InputType;
import android.text.InputFilter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.droidlogic.app.FileListManager;
import com.droidlogic.app.tv.PrebuiltChannelsManager;
import com.droidlogic.tvinput.KeyCodeHandler;
import com.droidlogic.tvinput.R;
import com.droidlogic.app.tv.ChannelInfo;
import com.droidlogic.app.tv.DroidLogicTvUtils;
import com.droidlogic.app.tv.EventParams;
import com.droidlogic.app.tv.TvControlManager;
import com.droidlogic.app.tv.TvDataBaseManager;
import com.droidlogic.app.tv.TvScanManager;
import com.droidlogic.app.tv.TvScanConfig;
import com.droidlogic.tvinput.services.TvMessage;
import vendor.amlogic.hardware.tvserver.V1_0.FreqList;
import com.droidlogic.app.DataProviderManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.Comparator;
import java.util.Collections;

public class ChannelSearchActivity extends Activity implements OnClickListener, TvScanManager.ScannerMessageListener {
    public static final String TAG = "ChannelSearchActivity";

    private static final String ACTION_SET_SEARCHED_CHANNEL_DATA = "droidlogic.tv.action.SET_SEARCHED_CHANNEL_DATA";

    private PowerManager mPowerManager;

    private TvScanManager mTvScanManager;
    private TvControlManager mTvControlManager;
    private TvControlManager.SourceInput_Type mTvSource;
    private TvControlManager.SourceInput_Type mVirtualTvSource;
    private TvControlManager.SourceInput mTvSourceInput;
    private TvDataBaseManager mTvDataBaseManager;
    private boolean isFinished = false;

    private ProgressBar mProgressBar;
    private TextView mScanningMessage;
    private TextView tvScanInfo;
    private View mChannelHolder;
    private volatile boolean mChannelListVisible;
    private Button mScanButton;
    private Spinner mCountrySetting;
    private Spinner mSearchModeSetting;
    private Spinner mSearchTypeSetting;
    private Spinner mOrderSetting;
    private Spinner mAtvColorSystem;
    private Spinner mAtvSoundSystem;
    private Spinner mDvbcQamModeSetting;
    private Spinner mSearchChannelTypeSetting;
    private Spinner mSubtitleLanguageSetting;
    private Spinner mSubtitle2ndLanguageSetting;
    private Spinner mAudioLanguageSetting;
    private Spinner mAudio2ndLanguageSetting;
    private EditText mInputChannelFrom;
    private EditText mInputChannelTo;
    private TextView mInputChannelFromText;
    private TextView mInputChannelToText;
    private EditText mDvbcSymbolRateValue;
    private TextView mDvbcSymbolRateText;

    private TextView mSearchOptionText;
    private ViewGroup mAllInputLayout;
    private ViewGroup mFromInputLayout;
    private ViewGroup mToInputLayout;
    private TextView mAtvSearchOrderText;
    private TextView mAtvColorSystemText;
    private TextView mAtvSoundSystemText;
    private TextView mDvbcQamModeText;
    private ViewGroup mDtmbFrequencySelect;
    private Button mDtmbSelectFrequencyLeft;
    private TextView mDtmbFrequency;
    private Button mDtmbSelectFrequencyRight;
    private LinearLayout mChannelTypeLayout;
    private Button mExportChannel;
    private Button mImportChannel;
    private LinearLayout mLanguageSettingsLayout;

    private String[] mDtmbFrequencyName = null;

    private String mInputId;
    private Intent in;
    private boolean isConnected = false;

    public static final int MANUAL_START = 0;
    public static final int AUTO_START = 1;
    public static final int PROCESS = 2;
    public static final int CHANNEL = 3;
    public static final int STATUS = 4;
    public static final int NUMBER_SEARCH_START = 5;
    public static final int FREQUENCY = 6;
    public static final int CHANNELNUMBER = 7;
    public static final int EXIT_SCAN = 8;
    public static final int SAVING = 9;
    public static final int ATSCC_OPTION_DEFAULT = 0;

    public static final int SET_DTMB = 0;
    public static final int SET_DVB_C = 1;
    public static final int SET_DVB_T = 2;
    public static final int SET_DVB_T2 = 3;
    public static final int SET_ATSC_T = 4;
    public static final int SET_ATSC_C= 5;
    public static final int SET_ISDB_T = 6;

    private static final int DTV_TO_ATV = 5;
    private List<String> mDtmbDisplayNameWithExtra = new ArrayList<String>();

    public static final String STRING_NAME = "name";
    public static final String STRING_STATUS = "status";

    public static final int START_TIMER= 0;
    public static final int START_INIT = 1;

    //number search  key "numbersearch" true or false, "number" 1~135 or 2~69
    public static final String NUMBERSEARCH = DroidLogicTvUtils.TV_NUMBER_SEARCH_MODE;
    public static final String NUMBER = DroidLogicTvUtils.TV_NUMBER_SEARCH_NUMBER;
    public static final boolean NUMBERSEARCHDEFAULT = false;
    public static final int NUMBERDEFAULT = -1;
    private ProgressDialog mProDia;
    private boolean isNumberSearchMode = false;
    private String mNumber = null;
    private boolean hasFoundChannel = false;
    private boolean mNumberSearchDtv = true;
    private boolean  mNumberSearchAtv = true;
    private int hasFoundChannelNumber = 0;
    private Map hasFoundInfo =  new HashMap<String, Integer>();

    public static final String HINT_CHANNEL_NUMBER = "number";
    public static final String HINT_CHANNEL_FREQUENCY = "freq MHz";

    /* config in file 'tvconfig.cfg' [atv.auto.scan.mode] */
    /* 0: freq table list sacn mode */
    /* 1: all band sacn mode */
    private int mATvAutoScanMode = TvScanConfig.TV_ATV_AUTO_FREQ_LIST;
    private int channelCounts = 0;

    public static final String KEY_TVTEST_START_DTMB_SEARCH = "isTvTestDTMBSearch";
    private static final int TVTEST_DTMB_SEARCH_START = 0x1001;
    private boolean isTvTestDTMBSearchMode = false;
    private boolean mHasPreferLanguageFeature = false;

    private final KeyCodeHandler mKeyCodeHandler = new KeyCodeHandler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        mTvDataBaseManager = new TvDataBaseManager(this);
        mTvControlManager = TvControlManager.getInstance();
        mPowerManager = getSystemService(PowerManager.class);

        //language settings
        String value =  mTvControlManager.TvMiscConfigGet("dtv.prefer.language.en", "0");
        int ret = 0;
        try {
            ret = Integer.valueOf(value);
        } catch (Exception ignore) {
        }
        mHasPreferLanguageFeature = (ret == 1);

        in = getIntent();
        if (in != null) {
            mTvScanManager = TvScanManager.getInstance(getApplicationContext(), in);
            mTvScanManager.setMessageListener(this);

            if (in.getBooleanExtra(NUMBERSEARCH, NUMBERSEARCHDEFAULT)) {
                mNumber = in.getStringExtra(NUMBER);
                if (null == mNumber) {
                    finish();
                    return;
                }
                //number search channel define
                //atv: 3-0\4-0  dtv: 3-\4- atv&dtv: 3\4
                isNumberSearchMode = true;
                String[] numberarray = mNumber.split("-");
                if (numberarray.length == 1) {
                    mNumberSearchDtv = true;
                    mNumberSearchAtv = true;
                } else if (numberarray.length == 2) {
                    if (Integer.parseInt(numberarray[1]) == 0) {
                        mNumberSearchDtv = false;
                        mNumberSearchAtv = true;
                    } else {
                        mNumberSearchDtv = true;
                        mNumberSearchAtv = false;
                    }
                }
                setContentView(R.layout.tv_number_channel_scan);
            } else {
                setContentView(R.layout.tv_channel_scan);
            }

            isTvTestDTMBSearchMode = in.getBooleanExtra(KEY_TVTEST_START_DTMB_SEARCH, false);
            mInputId = in.getStringExtra(TvInputInfo.EXTRA_INPUT_ID);
            DroidLogicTvUtils.saveInputId(this, mInputId);
            int deviceId = in.getIntExtra(DroidLogicTvUtils.EXTRA_CHANNEL_DEVICE_ID, -1);
            if (deviceId == -1)//for TIF compatible
                deviceId = DroidLogicTvUtils.getHardwareDeviceId(mInputId);
            mTvSource = DroidLogicTvUtils.parseTvSourceTypeFromDeviceId(deviceId);
            mTvSourceInput = DroidLogicTvUtils.parseTvSourceInputFromDeviceId(deviceId);
            mVirtualTvSource = mTvSource;
            if (mTvSource == TvControlManager.SourceInput_Type.SOURCE_TYPE_ADTV) {
                long channelId = in.getLongExtra(DroidLogicTvUtils.EXTRA_CHANNEL_NUMBER, -1);
                ChannelInfo currentChannel = mTvDataBaseManager.getChannelInfo(TvContract.buildChannelUri(channelId));
                if (currentChannel != null) {
                    mTvSource = DroidLogicTvUtils.parseTvSourceTypeFromSigType(DroidLogicTvUtils.getSigType(currentChannel));
                    mTvSourceInput = DroidLogicTvUtils.parseTvSourceInputFromSigType(DroidLogicTvUtils.getSigType(currentChannel));
                }
                if (mVirtualTvSource == mTvSource) {//no channels in adtv input, DTV for default.
                    mTvSource = TvControlManager.SourceInput_Type.SOURCE_TYPE_DTV;
                    mTvSourceInput = TvControlManager.SourceInput.DTV;
                }
            }
        }
        isFinished = false;

        String country = DroidLogicTvUtils.getCountry(this);
        setSelectCountry(country);

        mATvAutoScanMode = TvScanConfig.GetTvAtvStepScan(country);
        mDtmbDisplayNameWithExtra = getDtmbFrequencyInfoAddExtra();
        handler.sendEmptyMessage(START_INIT);

        //start number search when service connected
        if (isNumberSearchMode) {
            startShowActivityTimer();
            return;
        }

        mProgressBar = (ProgressBar) findViewById(R.id.tune_progress);
        mScanningMessage = (TextView) findViewById(R.id.tune_description);
        tvScanInfo = findViewById(R.id.tv_scan_info);
        ViewGroup progressHolder = (ViewGroup) findViewById(R.id.progress_holder);
        if (isTvTestDTMBSearchMode)
            progressHolder.setVisibility(View.INVISIBLE);
        mChannelHolder = findViewById(R.id.channel_holder);
        mSearchOptionText = findViewById(R.id.search_option);
        mAllInputLayout = findViewById(R.id.channel_input_type_container);
        mFromInputLayout = findViewById(R.id.channel_input_from_container);
        mToInputLayout = findViewById(R.id.channel_input_to_container);
        mAtvSearchOrderText = findViewById(R.id.order);
        mAtvColorSystemText = findViewById(R.id.atv_color);
        mAtvSoundSystemText = findViewById(R.id.atv_sound);
        mDvbcQamModeText = findViewById(R.id.dvbc_qam_mode);

        mScanButton = (Button) findViewById(R.id.search_channel);
        mScanButton.setOnClickListener(this);
        mScanButton.requestFocus();
        mProgressBar.setIndeterminate(false);
        mInputChannelFrom = (EditText) findViewById(R.id.input_channel_from);
        mInputChannelTo= (EditText) findViewById(R.id.input_channel_to);
        mInputChannelFromText = (TextView) findViewById(R.id.channel_from);
        mInputChannelToText = (TextView) findViewById(R.id.channel_to);
        mDvbcSymbolRateText = (TextView) findViewById(R.id.dvbc_symbol);
        mDvbcSymbolRateValue = (EditText) findViewById(R.id.dvbc_symbol_in);
        mDvbcSymbolRateValue.setHint(DroidLogicTvUtils.getDvbcSymbolRate(this) + "");
        mDvbcSymbolRateValue.setInputType(InputType.TYPE_CLASS_NUMBER);
        mDtmbFrequencySelect = (ViewGroup) findViewById(R.id.dtmb_select_frequency);
        mDtmbSelectFrequencyLeft = (Button) findViewById(R.id.button_left);
        mDtmbSelectFrequencyRight = (Button) findViewById(R.id.button_right);
        mDtmbFrequency = (TextView) findViewById(R.id.textview_display);
        mDtmbFrequency.setText((mDtmbDisplayNameWithExtra.size() > 0 && getCurrentDtmbPhysicalNumber() < mDtmbDisplayNameWithExtra.size()) ? mDtmbDisplayNameWithExtra.get(getCurrentDtmbPhysicalNumber()) : "------");
        mDtmbSelectFrequencyLeft.setOnClickListener(this);
        mDtmbSelectFrequencyRight.setOnClickListener(this);

        mChannelTypeLayout = (LinearLayout) findViewById(R.id.search_channel_type_ll);
        if (!DroidLogicTvUtils.isDtvContainsAtscByCountry(country)) {
            mChannelTypeLayout.setVisibility(View.GONE);
        }

        mExportChannel = (Button) findViewById(R.id.export_channel);
        mImportChannel = (Button) findViewById(R.id.import_channel);
        mExportChannel.setOnClickListener(v -> {
            PrebuiltChannelsManager prebuiltChannelsManager = new PrebuiltChannelsManager(ChannelSearchActivity.this, getChannelFilePath());
            int result = prebuiltChannelsManager.dbChannelInfoWriteToConfigFile();
            if (result == PrebuiltChannelsManager.RETURN_VALUE_SUCCESS) {
                ShowToastTint(getString(R.string.channel_export_successful));
                mScanningMessage.setText("file path:" + prebuiltChannelsManager.getChannelFilePath());
            } else {
                ShowToastTint(getString(R.string.channel_export_failed));
            }
        });
        mImportChannel.setOnClickListener(v -> {
            int result = new PrebuiltChannelsManager(ChannelSearchActivity.this, getChannelFilePath()).configFileReadToDbChannelInfo();
            if (result == PrebuiltChannelsManager.RETURN_VALUE_SUCCESS) {
                ShowToastTint(getString(R.string.channel_import_successful));
                finish();
            } else {
                ShowToastTint(getString(R.string.channel_import_failed));
            }
        });

        if (mHasPreferLanguageFeature) {
            mLanguageSettingsLayout = (LinearLayout) findViewById(R.id.language_settings);
            if ("CN".equals(DroidLogicTvUtils.getCountry(this))) {
                mLanguageSettingsLayout.setVisibility(View.VISIBLE);
            }
        } else {
            mLanguageSettingsLayout = null;
        }
        initSpinner();
        startShowActivityTimer();
    }

    private String getChannelFilePath() {
        FileListManager fileListManager = new FileListManager(this);
        List<Map<String, Object>> devices = fileListManager.getDevices();
        String path = null;
        if (devices != null && devices.size() > 0) {
            for (Map<String, Object> map : devices) {
                if (map != null) {
                    path = (String) map.get("key_path");
                    Log.i(TAG, "ChannelFilePath map:" + map.toString());
                    if (path != null && path.startsWith("/storage/")) {
                        if (path.startsWith("/storage/emulated/0")) {
                            continue;
                        }
                        break;
                    } else {
                        path = null;
                    }
                }
            }
        }
        Log.i(TAG, "ChannelFilePath path:" + path);
        return path;
    }

    @Override
    public void onMessage(TvMessage msg) {
        Log.d(TAG, "=====receive message from MessageListener type = " + msg.getType());
        int arg1 = msg.getType();
        int what = msg.getMessage();
        String information = msg.getInformation();
        Message message = new Message();
        message.arg1 = arg1;
        message.what = what;
        if (arg1 == CHANNEL && what == 0) {
            message.obj = getEventParams(information);
        } else {
            message.obj = information;
        }
        mHandler.sendMessage(message);
    }

    private TvControlManager.SourceInput_Type getCurrentTvSource() {
        return mTvSource;
    }

    private ArrayList<HashMap<String, Object>> getSearchedInfo(EventParams event) {
        Log.d(TAG, "===== getSearchedInfo");
        ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();

        HashMap<String, Object> item = new HashMap<String, Object>();
        item.put(STRING_NAME, getResources().getString(R.string.frequency_l) + ":");
        item.put(STRING_STATUS, Double.toString(event.getFrequency() / (1000 * 1000)) +
                getResources().getString(R.string.mhz));
        Log.d(TAG, "***** frequency : " + Double.toString(event.getFrequency() / (1000 * 1000)) +
                getResources().getString(R.string.mhz));
        list.add(item);

        item = new HashMap<String, Object>();
        item.put(STRING_NAME, getResources().getString(R.string.quality) + ":");
        item.put(STRING_STATUS, event.getQuality() + getResources().getString(R.string.db));
        Log.d(TAG, "***** quality : " + event.getQuality() + getResources().getString(R.string.db));
        list.add(item);

        item = new HashMap<String, Object>();
        item.put(STRING_NAME, getResources().getString(R.string.strength) + ":");
        item.put(STRING_STATUS, event.getStrength() + "%");
        Log.d(TAG, "***** strength : " + event.getStrength() + "%");
        list.add(item);

        item = new HashMap<String, Object>();
        item.put(STRING_NAME, getResources().getString(R.string.tv_channel) + ":");
        item.put(STRING_STATUS, event.getChannelNumber());
        Log.d(TAG, "***** tv channel : " + event.getChannelNumber());
        list.add(item);

        item = new HashMap<String, Object>();
        item.put(STRING_NAME, getResources().getString(R.string.radio_channel) + ":");
        item.put(STRING_STATUS, event.getRadioNumber());
        Log.d(TAG, "***** radio channel : " + event.getRadioNumber());
        list.add(item);

        return list;
    }

    public boolean isSearching() {
        Log.d(TAG, "===== Search Status :" + DroidLogicTvUtils.getSearchStatus(this));
        return (DroidLogicTvUtils.getSearchStatus(this) != 0);
    }

    public EventParams getEventParams (String params) {
        EventParams eventParams;
        if (params != null) {
            String[] channelParams = params.split("-");
            channelCounts = Integer.valueOf(channelParams[3]).intValue() + Integer.valueOf(channelParams[4]).intValue();
            eventParams = new EventParams(
                Integer.valueOf(channelParams[0]).intValue(),
                Integer.valueOf(channelParams[1]).intValue(),
                Integer.valueOf(channelParams[2]).intValue(),
                Integer.valueOf(channelParams[3]).intValue(),
                Integer.valueOf(channelParams[4]).intValue());
        } else {
            int firstFreq = 0;
            String country = DroidLogicTvUtils.getCountry(this);
            switch (country) {
                case "CN":
                    if (DroidLogicTvUtils.isATV(this)) {
                        firstFreq = 49;
                    } else if (DroidLogicTvUtils.isDTV(this)) {
                        firstFreq = 52;
                    }
                    break;
                case "IN":
                    firstFreq = 48;
                    break;
                case "US":
                case "MX":
                    if (TextUtils.equals(DroidLogicTvUtils.getDtvType(this), TvContract.Channels.TYPE_ATSC_T)) {
                        firstFreq = 55;
                    } else if (TextUtils.equals(DroidLogicTvUtils.getDtvType(this), TvContract.Channels.TYPE_ATSC_C)) {
                        firstFreq = 73;
                    }
                    break;
                case "ID":
                    if (DroidLogicTvUtils.isATV(this)) {
                        firstFreq = 48;
                    } else if (DroidLogicTvUtils.isDTV(this)) {
                        firstFreq = 47;
                    }
                    break;
                case "DE":
                    if (DroidLogicTvUtils.isATV(this)) {
                            firstFreq = 48;
                    } else if (DroidLogicTvUtils.isDTV(this)) {
                        firstFreq = 47;
                    }
                case "PH":
                    if (DroidLogicTvUtils.isATV(this)) {
                        firstFreq = 48;
                    } else if (DroidLogicTvUtils.isDTV(this)) {
                        firstFreq = 47;
                    }
                    break;
            }
            eventParams = new EventParams(firstFreq, 0, 0, 0, 0);
        }
        return eventParams;
    }

    public void dtvStopScan() {
        mTvControlManager.DtvStopScan();
    }

    public void stopTv() {
        mTvControlManager.StopTv();
    }

    private void initNumberSearch(Context context) {
        mProDia= new ProgressDialog(this);
        mProDia.setTitle(R.string.number_search_title);
        mProDia.setMessage(getResources().getString(R.string.number_search_dtv));
        mProDia.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProDia.show();
        if (isNumberSearchMode)
            sendMessage(NUMBER_SEARCH_START, 0, null);
    }

    private void exitNumberSearch() {
        if (mProDia != null && mProDia.isShowing()) {
            mProDia.dismiss();
        }
    }
    final private int[] SEARCH_ORDER = {R.string.tv_search_order_low, R.string.tv_search_order_high};

    private List<String> mSupportCountryFullNameList = new ArrayList<String>();
    private HashMap<String, String> mCountryFullNameToShortName = new HashMap<String, String>();
    private List<String> mSupportSearchModeList = new ArrayList<String>();
    private List<String> mSupportSearchTypeList = new ArrayList<String>();
    private List<String> mSupportAtvColorSysList = new ArrayList<String>();
    private List<String> mSupportAtvAudioSysList = new ArrayList<String>();
    final private int[] QAM_FOR_DVB_C = {R.string.ut_tune_dvb_c_qam16, R.string.ut_tune_dvb_c_qam32, R.string.ut_tune_dvb_c_qam64, R.string.ut_tune_dvb_c_qam128, R.string.ut_tune_dvb_c_qam256, R.string.ut_tune_dvb_c_qamauto};

    private void initSpinner() {
        ArrayAdapter<String> country_arr_adapter;
        ArrayAdapter<String> search_mode_arr_adapter;;
        ArrayAdapter<String> search_type_arr_adapter;
        ArrayAdapter<String> search_order_arr_adapter;
        List<String> search_order_data_list = new ArrayList<String>();
        ArrayAdapter<String> search_atv_color_arr_adapter;
        ArrayAdapter<String> search_atv_sound_arr_adapter;
        ArrayAdapter<String> dvbc_qam_mode_arr_adapter;
        ArrayAdapter<String> search_channel_type_adapter;
        List<String> dvbc_qam_mode_data_list = new ArrayList<String>();
        Log.d(TAG, "init Spinner start");
        // config current support country list
        mSupportCountryFullNameList.clear();
        mCountryFullNameToShortName.clear();
        List<String> supportCountryShortNameList = TvScanConfig.GetTVSupportCountries();
        for (int i = 0; i < supportCountryShortNameList.size(); i++) {
            String shortCountryId = supportCountryShortNameList.get(i);
            String fullCountryName = TvScanConfig.GetTvCountryNameById(shortCountryId);
            mCountryFullNameToShortName.put(fullCountryName, shortCountryId);
            mSupportCountryFullNameList.add(fullCountryName);
        }
        if (0 == supportCountryShortNameList.size()) {
            Log.e(TAG, "support country list size is 0");
            return;
        }
        String currentCountry = DroidLogicTvUtils.getCountry(this);

        // config search mode (auto, manul, number)
        mSupportSearchModeList.clear();
        ArrayList<String> tempList = TvScanConfig.GetTvSearchModeList(currentCountry);
        for (int i = 0; i < tempList.size(); i++) {
            if (tempList.get(i).equals(TvScanConfig.TV_SEARCH_MODE.get(TvScanConfig.TV_SEARCH_NUMBER_INDEX))) {
                // UI not display number mode
                Log.i(TAG, "hide number mode display in UI");
                continue;
            }
            mSupportSearchModeList.add(tempList.get(i));
        }
        String searchType = DroidLogicTvUtils.getSearchType(this);
        // only DVBC support nit scan channel
        if (TvScanConfig.TV_SEARCH_TYPE.get(TvScanConfig.TV_SEARCH_TYPE_DVBC_INDEX).equals(searchType)) {
            mSupportSearchModeList.add(TvScanConfig.TV_SEARCH_MODE.get(TvScanConfig.TV_SEARCH_NIT_INDEX));
        }
        if (0 == mSupportSearchModeList.size()) {
            Log.e(TAG, "support search mode list size is 0");
            return;
        }
        for (int i = 0; i < SEARCH_ORDER.length; i++) {
            search_order_data_list.add(getString(SEARCH_ORDER[i]));
        }

        // config atv color and audio system list
        mSupportSearchTypeList.clear();
        mSupportAtvColorSysList.clear();
        mSupportAtvAudioSysList.clear();
        boolean isAtvSupport = TvScanConfig.GetTvAtvSupport(currentCountry);
        if (isAtvSupport) {
            mSupportAtvColorSysList = TvScanConfig.GetTvAtvColorSystemList(currentCountry);
            if (0 == mSupportAtvColorSysList.size()) {
                Log.e(TAG, "support color system list size is 0");
                return;
            }
            mSupportAtvAudioSysList = TvScanConfig.GetTvAtvSoundSystemList(currentCountry);
            if (0 == mSupportAtvAudioSysList.size()) {
                Log.e(TAG, "support audio system list size is 0");
                return;
            }
        }

        // config search type list
        boolean isDtvSupport = TvScanConfig.GetTvDtvSupport(currentCountry);
        if (!DroidLogicTvUtils.isDtvContainsAtscByCountry(currentCountry) && isAtvSupport) {
            mSupportSearchTypeList.add(TvScanConfig.TV_SEARCH_TYPE.get(TvScanConfig.TV_SEARCH_TYPE_ATV_INDEX));
        }
        if (isDtvSupport) {
            tempList = TvScanConfig.GetTvDtvSystemList(currentCountry);

            for (int i = 0; i < tempList.size(); i++) {
                mSupportSearchTypeList.add(tempList.get(i));
            }
            if (0 == tempList.size()) {
                Log.e(TAG, "support dtv system list size is 0");
                return;
            }
        }
        for (int i = 0; i < QAM_FOR_DVB_C.length; i++) {
            dvbc_qam_mode_data_list.add(getString(QAM_FOR_DVB_C[i]));
        }

        if ((isAtvSupport || isDtvSupport) && mSupportSearchTypeList.size() != 0) {
            if (DroidLogicTvUtils.DTV_SEARCH_TYPE_LIST.containsKey(mSupportSearchTypeList.get(0))) {
                DroidLogicTvUtils.setDtvType(this, DroidLogicTvUtils.DTV_SEARCH_TYPE_LIST.get(mSupportSearchTypeList.get(0)));
            } else {
                Log.w(TAG, "not find dtv search type:" + mSupportSearchTypeList.get(0));
            }
        }
        mCountrySetting = (Spinner) findViewById(R.id.country_spinner);
        mSearchModeSetting = (Spinner) findViewById(R.id.search_mode_spinner);
        mSearchTypeSetting = (Spinner) findViewById(R.id.search_type_spinner);
        mOrderSetting = (Spinner) findViewById(R.id.order_spinner);
        mAtvColorSystem = (Spinner) findViewById(R.id.atv_color_spinner);
        mAtvSoundSystem = (Spinner) findViewById(R.id.atv_sound_spinner);
        mDvbcQamModeSetting = (Spinner) findViewById(R.id.dvbc_qam_mode_spinner);
        mSearchChannelTypeSetting = (Spinner) findViewById(R.id.search_channel_type_spinner);
        mSubtitleLanguageSetting = (Spinner) findViewById(R.id.subtitle_language_spinner);
        mSubtitle2ndLanguageSetting = (Spinner) findViewById(R.id.subtitle_second_language_spinner);
        mAudioLanguageSetting = (Spinner) findViewById(R.id.audio_language_spinner);
        mAudio2ndLanguageSetting = (Spinner) findViewById(R.id.audio_second_language_spinner);
        country_arr_adapter = new ArrayAdapter<String>(ChannelSearchActivity.this, android.R.layout.simple_spinner_item, mSupportCountryFullNameList);
        country_arr_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item );
        mCountrySetting.setAdapter(country_arr_adapter);
        if (supportCountryShortNameList.contains(currentCountry)) {
            mCountrySetting.setSelection(supportCountryShortNameList.indexOf(currentCountry));
        } else {
            Log.w(TAG, "not find country:" + currentCountry + " in supportCountryShortNameList, set default position 0");
            mCountrySetting.setSelection(0);
            currentCountry = supportCountryShortNameList.get(0);
            DroidLogicTvUtils.setCountry(this, currentCountry);
        }

        search_mode_arr_adapter = new ArrayAdapter<String>(ChannelSearchActivity.this, android.R.layout.simple_spinner_item, mSupportSearchModeList);
        search_mode_arr_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item );
        mSearchModeSetting.setAdapter(search_mode_arr_adapter);
        String searchMode = DroidLogicTvUtils.getSearchMode(this);
        if (mSupportSearchModeList.contains(searchMode)) {
            mSearchModeSetting.setSelection(mSupportSearchModeList.indexOf(searchMode));
        } else {
            Log.i(TAG, "not find search mode:" + searchMode + " in list, set default position 0, mode:" + mSupportSearchModeList.get(0));
            mSearchModeSetting.setSelection(0);
            searchMode = mSupportSearchModeList.get(0);
            DroidLogicTvUtils.setSearchMode(this, searchMode);
        }

        search_type_arr_adapter = new ArrayAdapter<String>(ChannelSearchActivity.this, android.R.layout.simple_spinner_item, mSupportSearchTypeList);
        search_type_arr_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item );
        mSearchTypeSetting.setAdapter(search_type_arr_adapter);
        if (mSupportSearchTypeList.contains(searchType)) {
            mSearchTypeSetting.setSelection(mSupportSearchTypeList.indexOf(searchType));
        } else {
            Log.i(TAG, "not find search type:" + searchType + " in list, set default position 0, type:" + mSupportSearchTypeList.get(0));
            mSearchTypeSetting.setSelection(0);
            DroidLogicTvUtils.setSearchType(this, mSupportSearchTypeList.get(0));
        }

        search_order_arr_adapter = new ArrayAdapter<String>(ChannelSearchActivity.this, android.R.layout.simple_spinner_item, search_order_data_list);
        search_order_arr_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item );
        mOrderSetting.setAdapter(search_order_arr_adapter);
        mOrderSetting.setSelection(DroidLogicTvUtils.getSearchOrder(this));

        if (mLanguageSettingsLayout != null) {
            ArrayAdapter<String> mPreferredLanguageAdapter =
                new ArrayAdapter<String>(ChannelSearchActivity.this,
                    android.R.layout.simple_spinner_item, getPreferredLanguageList());
            mPreferredLanguageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mSubtitleLanguageSetting.setAdapter(mPreferredLanguageAdapter);
            mSubtitleLanguageSetting.setSelection(
                mTvControlManager.getCurrentPreferredSelection(this, DroidLogicTvUtils.PREFERRED_SUB_DEFAULT));

            mSubtitle2ndLanguageSetting.setAdapter(mPreferredLanguageAdapter);
            mSubtitle2ndLanguageSetting.setSelection(
                mTvControlManager.getCurrentPreferredSelection(this, DroidLogicTvUtils.PREFERRED_SUB_SECONDARY));

            mAudioLanguageSetting.setAdapter(mPreferredLanguageAdapter);
            mAudioLanguageSetting.setSelection(
                mTvControlManager.getCurrentPreferredSelection(this, DroidLogicTvUtils.PREFERRED_AUD_DEFAULT));

            mAudio2ndLanguageSetting.setAdapter(mPreferredLanguageAdapter);
            mAudio2ndLanguageSetting.setSelection(
                mTvControlManager.getCurrentPreferredSelection(this, DroidLogicTvUtils.PREFERRED_AUD_SECONDARY));
        }

        if (isAtvSupport) {
            search_atv_color_arr_adapter = new ArrayAdapter<String>(ChannelSearchActivity.this, android.R.layout.simple_spinner_item, mSupportAtvColorSysList);
            search_atv_color_arr_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mAtvColorSystem.setAdapter(search_atv_color_arr_adapter);
            String colorSys = DroidLogicTvUtils.getTvSearchTypeSys(this);
            if (mSupportAtvColorSysList.contains(colorSys)) {
                mAtvColorSystem.setSelection(mSupportAtvColorSysList.indexOf(colorSys));
            } else {
                Log.i(TAG, "not find color system:" + colorSys + " in list, set default position 0, colorSys:" +  mSupportAtvColorSysList.get(0));
                mAtvColorSystem.setSelection(0);
                DroidLogicTvUtils.setTvSearchTypeSys(this, mSupportAtvColorSysList.get(0));
            }

            search_atv_sound_arr_adapter = new ArrayAdapter<String>(ChannelSearchActivity.this, android.R.layout.simple_spinner_item, mSupportAtvAudioSysList);
            search_atv_sound_arr_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mAtvSoundSystem.setAdapter(search_atv_sound_arr_adapter);
            String soundSys = DroidLogicTvUtils.getTvSearchSoundSys(this);
            if (mSupportAtvAudioSysList.contains(soundSys)) {
                mAtvSoundSystem.setSelection(mSupportAtvAudioSysList.indexOf(soundSys));
            } else {
                Log.i(TAG, "not find audio system:" + soundSys + " in list, set default position 0, audioSys:" + mSupportAtvAudioSysList.get(0));
                mAtvSoundSystem.setSelection(0);
                DroidLogicTvUtils.setTvSearchSoundSys(this, mSupportAtvAudioSysList.get(0));
            }
        }
        dvbc_qam_mode_arr_adapter = new ArrayAdapter<String>(ChannelSearchActivity.this, android.R.layout.simple_spinner_item, dvbc_qam_mode_data_list);
        dvbc_qam_mode_arr_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item );
        mDvbcQamModeSetting.setAdapter(dvbc_qam_mode_arr_adapter);
        mDvbcQamModeSetting.setSelection(DroidLogicTvUtils.getDvbcQamMode(this) - 1);//mode change from 1~5

        int atvDtvFlag = DroidLogicTvUtils.getAtvDtvModeFlag(this);
        Log.i(TAG,"searchMode = " + searchMode);

        String[] channel_type = getResources().getStringArray(R.array.array_channel_type_values);
        if (DroidLogicTvUtils.TV_SEARCH_MODE_MANUAL.equals(searchMode)) {
            channel_type = Arrays.copyOfRange(channel_type, 0, 2);
        }
        search_channel_type_adapter = new ArrayAdapter<String>(ChannelSearchActivity.this, android.R.layout.simple_spinner_item, channel_type);
        search_channel_type_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSearchChannelTypeSetting.setAdapter(search_channel_type_adapter);
        mSearchChannelTypeSetting.setSelection(atvDtvFlag);

        // Forced hide Atv search settings
        hideAtvRelatedOption(true);
        if (DroidLogicTvUtils.TV_SEARCH_DTV == atvDtvFlag) {//atv type
            mAtvColorSystem.setEnabled(false);
            mAtvSoundSystem.setEnabled(false);
            //hideAtvRelatedOption(true);
        } else {
            mAtvColorSystem.setEnabled(true);
            mAtvSoundSystem.setEnabled(true);
            //hideAtvRelatedOption(false);
        }

        if (TvScanConfig.TV_SEARCH_MODE.get(TvScanConfig.TV_SEARCH_AUTO_INDEX).equals(searchMode)) {
            hideInputChannel(true);
            mScanButton.setText(R.string.ut_auto_scan);
        } else if (TvScanConfig.TV_SEARCH_MODE.get(TvScanConfig.TV_SEARCH_NIT_INDEX).equals(searchMode)) {
            hideInputChannel(false);
            mScanButton.setText(R.string.ut_nit_scan);
        } else {
            hideInputChannel(false);
            mScanButton.setText(R.string.ut_manual_scan);
        }
        if (!TvContract.Channels.TYPE_DVB_C.equals(DroidLogicTvUtils.getDtvType(this))) {
            mDvbcQamModeSetting.setVisibility(View.GONE);
            mDvbcQamModeText.setVisibility(View.GONE);
        } else {
            mDvbcQamModeSetting.setVisibility(View.VISIBLE);
            mDvbcQamModeText.setVisibility(View.VISIBLE);
        }
        //no effect at moment, and hide it
        mOrderSetting.setVisibility(View.GONE);
        mAtvSearchOrderText.setVisibility(View.GONE);
        mCountrySetting.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= mSupportCountryFullNameList.size()) {
                    Log.e(TAG, "country select position:" + position + " invalid, support size:" + mSupportCountryFullNameList.size());
                    return;
                }
                String curCountry = mCountryFullNameToShortName.get(mSupportCountryFullNameList.get(position));
                String preCountry = DroidLogicTvUtils.getCountry(ChannelSearchActivity.this);
                if (!TextUtils.equals(curCountry, "US")) {
                    mExportChannel.setVisibility(View.INVISIBLE);
                    mImportChannel.setVisibility(View.INVISIBLE);
                }
                if (!curCountry.equals(preCountry)) {
                    if (mLanguageSettingsLayout != null) {
                        if ("CN".equals(curCountry)) {
                            mLanguageSettingsLayout.setVisibility(View.VISIBLE);
                        } else {
                            mLanguageSettingsLayout.setVisibility(View.GONE);
                        }
                    }
                    DroidLogicTvUtils.setCountry(ChannelSearchActivity.this, curCountry);
                    if (DroidLogicTvUtils.isDtvContainsAtscByCountry(curCountry)) {
                        DroidLogicTvUtils.setAtvDtvModeFlag(ChannelSearchActivity.this, DroidLogicTvUtils.TV_SEARCH_ATV_DTV);
                        mChannelTypeLayout.setVisibility(View.VISIBLE);
                    } else if (TvScanConfig.GetTvAtvSupport(curCountry)) {
                        mChannelTypeLayout.setVisibility(View.GONE);
                        DroidLogicTvUtils.setAtvDtvModeFlag(ChannelSearchActivity.this, DroidLogicTvUtils.TV_SEARCH_ATV);
                    } else if (TvScanConfig.GetTvDtvSupport(curCountry)){
                        mChannelTypeLayout.setVisibility(View.GONE);
                        DroidLogicTvUtils.setAtvDtvModeFlag(ChannelSearchActivity.this, DroidLogicTvUtils.TV_SEARCH_DTV);
                    } else {
                        mChannelTypeLayout.setVisibility(View.GONE);
                        Log.e(TAG, "current country:" + curCountry + " not support DTV and ATV");
                    }

                    DroidLogicTvUtils.setSearchMode(ChannelSearchActivity.this, TvScanConfig.TV_SEARCH_MODE.get(TvScanConfig.TV_SEARCH_AUTO_INDEX));
                    DroidLogicTvUtils.setSearchType(ChannelSearchActivity.this, TvScanConfig.TV_SEARCH_TYPE.get(TvScanConfig.TV_SEARCH_TYPE_ATV_INDEX));
                    DroidLogicTvUtils.setAtsccListMode(ChannelSearchActivity.this, DroidLogicTvUtils.TV_ATSC_MODE_STANDARD);
                    DroidLogicTvUtils.setSearchOrder(ChannelSearchActivity.this, DroidLogicTvUtils.TV_SEARCH_ORDER_LOW);
                    DroidLogicTvUtils.setTvSearchTypeSys(ChannelSearchActivity.this, TvScanConfig.TV_COLOR_SYS.get(TvScanConfig.TV_COLOR_SYS_AUTO_INDEX));
                    DroidLogicTvUtils.setTvSearchSoundSys(ChannelSearchActivity.this, TvScanConfig.TV_SOUND_SYS.get(TvScanConfig.TV_SOUND_SYS_AUTO_INDEX));
                    DroidLogicTvUtils.setDvbcQamMode(ChannelSearchActivity.this, DroidLogicTvUtils.TV_SEARCH_DVBC_QAM16);
                    initSpinner();//init again
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });
        mSearchModeSetting.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= mSupportSearchModeList.size()) {
                    Log.e(TAG, "search mode select position:" + position + " invalid, support size:" + mSupportSearchModeList.size());
                    return;
                }
                boolean hasChanged = !DroidLogicTvUtils.getSearchMode(ChannelSearchActivity.this).equals(mSupportSearchModeList.get(position));
                DroidLogicTvUtils.setSearchMode(ChannelSearchActivity.this, mSupportSearchModeList.get(position));
                if (TvScanConfig.TV_SEARCH_MODE.get(TvScanConfig.TV_SEARCH_AUTO_INDEX).equals(mSupportSearchModeList.get(position))) {
                    hideInputChannel(true);
                    mScanButton.setText(R.string.ut_auto_scan);
                }  else if (TvScanConfig.TV_SEARCH_MODE.get(TvScanConfig.TV_SEARCH_NIT_INDEX).equals(DroidLogicTvUtils.getSearchMode(ChannelSearchActivity.this))) {
                    hideInputChannel(false);
                    mScanButton.setText(R.string.ut_nit_scan);
                } else {
                    hideInputChannel(false);
                    mScanButton.setText(R.string.ut_manual_scan);
                }
                if (hasChanged && DroidLogicTvUtils.isDtvContainsAtscByCountry(DroidLogicTvUtils.getCountry(ChannelSearchActivity.this))) {
                    if (DroidLogicTvUtils.TV_SEARCH_MODE_MANUAL.equals(mSupportSearchModeList.get(position))
                            && DroidLogicTvUtils.getAtvDtvModeFlag(ChannelSearchActivity.this) == DroidLogicTvUtils.TV_SEARCH_ATV_DTV) {
                        DroidLogicTvUtils.setAtvDtvModeFlag(ChannelSearchActivity.this,  DroidLogicTvUtils.TV_SEARCH_ATV);
                    }
                    initSpinner();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });
        mSearchTypeSetting.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String searchType = "";
                if (position >= mSupportSearchTypeList.size()) {
                    Log.e(TAG, "set search type position:" + position + " error, set auto");
                    searchType = TvScanConfig.TV_SEARCH_MODE.get(TvScanConfig.TV_SEARCH_AUTO_INDEX);
                } else {
                    searchType = mSupportSearchTypeList.get(position);
                }
                int atvDtvMode = DroidLogicTvUtils.TV_SEARCH_ATV_DTV;
                if (!TvScanConfig.GetTvAtvSupport(DroidLogicTvUtils.getCountry(ChannelSearchActivity.this))) {
                    atvDtvMode = DroidLogicTvUtils.TV_SEARCH_DTV;
                }
                if (searchType.equals(TvScanConfig.TV_SEARCH_TYPE.get(TvScanConfig.TV_SEARCH_TYPE_ATV_INDEX))) {
                    atvDtvMode = DroidLogicTvUtils.TV_SEARCH_ATV;
                } else if (searchType.equals(TvScanConfig.TV_SEARCH_TYPE.get(TvScanConfig.TV_SEARCH_TYPE_ATSC_C_AUTO_INDEX))) {
                    DroidLogicTvUtils.setAtsccListMode(ChannelSearchActivity.this, DroidLogicTvUtils.TV_ATSC_MODE_AUTO);
                } else if (searchType.equals(TvScanConfig.TV_SEARCH_TYPE.get(TvScanConfig.TV_SEARCH_TYPE_ATSC_C_HRC_INDEX))) {
                    DroidLogicTvUtils.setAtsccListMode(ChannelSearchActivity.this, DroidLogicTvUtils.TV_ATSC_MODE_HRC);
                } else if (searchType.equals(TvScanConfig.TV_SEARCH_TYPE.get(TvScanConfig.TV_SEARCH_TYPE_ATSC_C_LRC_INDEX))) {
                    DroidLogicTvUtils.setAtsccListMode(ChannelSearchActivity.this, DroidLogicTvUtils.TV_ATSC_MODE_LRC);
                } else if (searchType.equals(TvScanConfig.TV_SEARCH_TYPE.get(TvScanConfig.TV_SEARCH_TYPE_ATSC_C_STD_INDEX))) {
                    DroidLogicTvUtils.setAtsccListMode(ChannelSearchActivity.this, DroidLogicTvUtils.TV_ATSC_MODE_STANDARD);
                } else if (searchType.equals(TvScanConfig.TV_SEARCH_TYPE.get(TvScanConfig.TV_SEARCH_TYPE_ATSC_T_INDEX))){
                } else{
                    atvDtvMode = DroidLogicTvUtils.TV_SEARCH_DTV;
                }
                if (atvDtvMode != DroidLogicTvUtils.TV_SEARCH_ATV_DTV || !searchType.equals(DroidLogicTvUtils.getSearchType(ChannelSearchActivity.this))) {
                    Log.d(TAG, "tv_search_type / atvDtvMode was changed, update");
                    DroidLogicTvUtils.setSearchType(ChannelSearchActivity.this, searchType);
                    if (atvDtvMode != DroidLogicTvUtils.TV_SEARCH_ATV_DTV || !DroidLogicTvUtils.TV_SEARCH_MODE_MANUAL.equals(DroidLogicTvUtils.getSearchMode(ChannelSearchActivity.this))) {
                        DroidLogicTvUtils.setAtvDtvModeFlag(ChannelSearchActivity.this, atvDtvMode);
                        mSearchChannelTypeSetting.setSelection(atvDtvMode);
                    }
                }
                String searchMode = DroidLogicTvUtils.getSearchMode(ChannelSearchActivity.this);
                if (DroidLogicTvUtils.DTV_SEARCH_TYPE_LIST.containsKey(searchType)) {
                    String dtvType = DroidLogicTvUtils.DTV_SEARCH_TYPE_LIST.get(searchType);
                    DroidLogicTvUtils.setDtvType(ChannelSearchActivity.this, dtvType);
                    if (!TvContract.Channels.TYPE_DVB_C.equals(dtvType)) {
                        mDvbcQamModeSetting.setVisibility(View.GONE);
                        mDvbcQamModeText.setVisibility(View.GONE);
                        if (TvScanConfig.TV_SEARCH_MODE.get(TvScanConfig.TV_SEARCH_NIT_INDEX).equals(searchMode)) {
                            searchMode = TvScanConfig.TV_SEARCH_MODE.get(TvScanConfig.TV_SEARCH_AUTO_INDEX);
                            DroidLogicTvUtils.setSearchMode(ChannelSearchActivity.this, searchMode);
                            hideInputChannel(false);
                            mScanButton.setText(R.string.ut_nit_scan);
                            mSearchModeSetting.setSelection(0, true);
                        }
                        if (mSupportSearchModeList.contains(TvScanConfig.TV_SEARCH_MODE.get(TvScanConfig.TV_SEARCH_NIT_INDEX))) {
                            mSupportSearchModeList.remove(TvScanConfig.TV_SEARCH_MODE.get(TvScanConfig.TV_SEARCH_NIT_INDEX));
                        }
                    } else {
                        // DVBC need to support nit search channel
                        if (!mSupportSearchModeList.contains(TvScanConfig.TV_SEARCH_MODE.get(TvScanConfig.TV_SEARCH_NIT_INDEX))) {
                            mSupportSearchModeList.add(TvScanConfig.TV_SEARCH_MODE.get(TvScanConfig.TV_SEARCH_NIT_INDEX));
                        }
                        mDvbcQamModeSetting.setVisibility(View.VISIBLE);
                        mDvbcQamModeText.setVisibility(View.VISIBLE);
                    }
                } else {
                    Log.e(TAG, "not find search type:" + searchType + " in support list");
                }
                // Forced hide Atv search settings
                hideAtvRelatedOption(true);
                if (!(atvDtvMode == DroidLogicTvUtils.TV_SEARCH_DTV)) {//atv type
                    mAtvColorSystem.setEnabled(true);
                    mAtvSoundSystem.setEnabled(true);
                    //hideAtvRelatedOption(false);
                } else {
                    mAtvColorSystem.setEnabled(false);
                    mAtvSoundSystem.setEnabled(false);
                    //hideAtvRelatedOption(true);
                }

                if (TvScanConfig.TV_SEARCH_MODE.get(TvScanConfig.TV_SEARCH_AUTO_INDEX).equals(searchMode)) {
                    hideInputChannel(true);
                    mScanButton.setText(R.string.ut_auto_scan);
                } else if (TvScanConfig.TV_SEARCH_MODE.get(TvScanConfig.TV_SEARCH_NIT_INDEX).equals(searchMode)) {
                    hideInputChannel(false);
                    mScanButton.setText(R.string.ut_nit_scan);
                }  else {
                    hideInputChannel(false);
                    mScanButton.setText(R.string.ut_manual_scan);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });
        mOrderSetting.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                DroidLogicTvUtils.setSearchOrder(ChannelSearchActivity.this, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });
        mAtvColorSystem.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= mSupportAtvColorSysList.size()) {
                    Log.w(TAG, "set color system position:" + position + " error, set auto");
                    DroidLogicTvUtils.setTvSearchTypeSys(ChannelSearchActivity.this, TvScanConfig.TV_COLOR_SYS.get(TvScanConfig.TV_COLOR_SYS_AUTO_INDEX));
                } else {
                    DroidLogicTvUtils.setTvSearchTypeSys(ChannelSearchActivity.this, mSupportAtvColorSysList.get(position));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });
        mAtvSoundSystem.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= mSupportAtvAudioSysList.size()) {
                    Log.w(TAG, "set audio system position:" + position + " error, set auto");
                    DroidLogicTvUtils.setTvSearchSoundSys(ChannelSearchActivity.this, TvScanConfig.TV_SOUND_SYS.get(TvScanConfig.TV_SOUND_SYS_AUTO_INDEX));
                } else {
                    DroidLogicTvUtils.setTvSearchSoundSys(ChannelSearchActivity.this, mSupportAtvAudioSysList.get(position));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });
        mDvbcQamModeSetting.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= (DroidLogicTvUtils.TV_SEARCH_DVBC_QAM16 - 1) && position < DroidLogicTvUtils.TV_SEARCH_DVBC_QAMAUTO) {
                        DroidLogicTvUtils.setDvbcQamMode(ChannelSearchActivity.this, position + 1);
                    }
                }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });
        mSearchChannelTypeSetting.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                DroidLogicTvUtils.setAtvDtvModeFlag(ChannelSearchActivity.this, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });

        if (mLanguageSettingsLayout != null) {
            OnItemSelectedListener mPreferredListener = new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    int type = -1;
                    switch (parent.getId()) {
                        case R.id.subtitle_language_spinner:
                            type = DroidLogicTvUtils.PREFERRED_SUB_DEFAULT;
                            break;
                        case R.id.subtitle_second_language_spinner:
                            type = DroidLogicTvUtils.PREFERRED_SUB_SECONDARY;
                            break;
                        case R.id.audio_language_spinner:
                            type = DroidLogicTvUtils.PREFERRED_AUD_DEFAULT;
                            break;
                        case R.id.audio_second_language_spinner:
                            type = DroidLogicTvUtils.PREFERRED_AUD_SECONDARY;
                            break;
                    }
                    if (type != -1)
                        mTvControlManager.setPreferredLanguageWithSelection(ChannelSearchActivity.this, type, position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // TODO Auto-generated method stub
                }
            };
            mSubtitleLanguageSetting.setOnItemSelectedListener(mPreferredListener);
            mSubtitle2ndLanguageSetting.setOnItemSelectedListener(mPreferredListener);
            mAudioLanguageSetting.setOnItemSelectedListener(mPreferredListener);
            mAudio2ndLanguageSetting.setOnItemSelectedListener(mPreferredListener);
        }
    }


    private void hideInputChannel(boolean value) {
        if (value) {
            mAllInputLayout.setVisibility(View.GONE);
            mDvbcSymbolRateText.setVisibility(View.GONE);
            mDvbcSymbolRateValue.setVisibility(View.GONE);
        } else {
            mAllInputLayout.setVisibility(View.VISIBLE);
            //show start frequency and end frequency as need
            if (mATvAutoScanMode == TvScanConfig.TV_ATV_AUTO_ALL_BAND &&
                    DroidLogicTvUtils.getAtvDtvModeFlag(this) == DroidLogicTvUtils.TV_SEARCH_ATV) {
                //SpannableString hint = new SpannableString("");
                mInputChannelFrom.setHint(HINT_CHANNEL_FREQUENCY);
                mInputChannelTo.setHint(HINT_CHANNEL_FREQUENCY);
                mInputChannelFrom.setFilters(new InputFilter[]{new InputFilter.LengthFilter(10)});
                mInputChannelTo.setFilters(new InputFilter[]{new InputFilter.LengthFilter(10)});
                mInputChannelFromText.setText(R.string.tv_search_channel_from);
                mInputChannelToText.setText(R.string.tv_search_channel_to);
                mDvbcSymbolRateText.setVisibility(View.GONE);
                mDvbcSymbolRateValue.setVisibility(View.GONE);
            } else if (TvScanConfig.TV_SEARCH_MODE.get(TvScanConfig.TV_SEARCH_NIT_INDEX).equals(DroidLogicTvUtils.getSearchMode(this))) {
                mInputChannelFrom.setHint(HINT_CHANNEL_FREQUENCY);
                mInputChannelTo.setHint(HINT_CHANNEL_FREQUENCY);
                //mInputChannelFrom.setHint(hint_channel_number);
                //mInputChannelTo.setHint(hint_channel_number);
                mInputChannelFrom.setFilters(new InputFilter[]{new InputFilter.LengthFilter(10)});

                mInputChannelTo.setFilters(new InputFilter[]{new InputFilter.LengthFilter(10)});
                mInputChannelFromText.setText(R.string.tv_search_center_channel1);
                mInputChannelToText.setText(R.string.tv_search_center_channel2);
                mDvbcSymbolRateText.setVisibility(View.VISIBLE);
                mDvbcSymbolRateValue.setVisibility(View.VISIBLE);
                mDvbcSymbolRateValue.setHint(DroidLogicTvUtils.getDvbcSymbolRate(this) + "");
            } else if (TvScanConfig.TV_SEARCH_MODE.get(TvScanConfig.TV_SEARCH_MANUAL_INDEX).equals(DroidLogicTvUtils.getSearchMode(this)) && DroidLogicTvUtils.isAtscCountry(this)) {
                mInputChannelFrom.setHint(HINT_CHANNEL_FREQUENCY);
                mInputChannelTo.setFilters(new InputFilter[]{new InputFilter.LengthFilter(10)});
                mInputChannelFromText.setText(R.string.tv_search_channel_frequency);
                mDvbcSymbolRateText.setVisibility(View.GONE);
                mDvbcSymbolRateValue.setVisibility(View.GONE);
            } else {
                mInputChannelFrom.setHint(HINT_CHANNEL_NUMBER);
                mInputChannelTo.setHint(HINT_CHANNEL_NUMBER);
                mInputChannelFrom.setFilters(new InputFilter[]{new InputFilter.LengthFilter(3)});
                mInputChannelTo.setFilters(new InputFilter[]{new InputFilter.LengthFilter(3)});
                mInputChannelFromText.setText(R.string.tv_search_channel_from);
                mInputChannelToText.setText(R.string.tv_search_channel_to);
                mDvbcSymbolRateText.setVisibility(View.GONE);
                mDvbcSymbolRateValue.setVisibility(View.GONE);
            }

            mInputChannelFrom.setInputType(InputType.TYPE_CLASS_NUMBER);
            mInputChannelTo.setInputType(InputType.TYPE_CLASS_NUMBER);


            if (TvScanConfig.TV_SEARCH_MODE.get(TvScanConfig.TV_SEARCH_NIT_INDEX).equals(DroidLogicTvUtils.getSearchMode(this))) {
                mToInputLayout.setVisibility(View.VISIBLE);
            } else {
                mToInputLayout.setVisibility(View.GONE);
            }
            if (checkDtmbFrequencySelectDisplay()) {
                mFromInputLayout.setVisibility(View.GONE);
            } else {
                mFromInputLayout.setVisibility(View.VISIBLE);
            }
        }
        mInputChannelFrom.setText("");
        mInputChannelTo.setText("");
    }

    private boolean checkDtmbFrequencySelectDisplay() {
        if (TvContract.Channels.TYPE_DTMB.equals(DroidLogicTvUtils.getDtvType(this)) &&
                (DroidLogicTvUtils.getAtvDtvModeFlag(this) == DroidLogicTvUtils.TV_SEARCH_DTV) &&
                TvScanConfig.TV_SEARCH_MODE.get(TvScanConfig.TV_SEARCH_MANUAL_INDEX).equals(DroidLogicTvUtils.getSearchMode(this))) {
            hideDtmbFrequencySelect(false);
            return true;
        } else {
            hideDtmbFrequencySelect(true);
            return false;
        }
    }

    private void hideDtmbFrequencySelect(boolean value) {
        if (value) {
            mDtmbFrequencySelect.setVisibility(View.GONE);
        } else {
            mDtmbFrequencySelect.setVisibility(View.VISIBLE);
        }
    }

    private void hideSearchOption(boolean value) {
        if (value) {
            mSearchOptionText.setVisibility(View.GONE);
        } else {
            mSearchOptionText.setVisibility(View.VISIBLE);
        }
    }

    private void hideAtvRelatedOption(boolean value) {
        if (value) {
            Log.d(TAG, "hideAtvRelatedOption = " + value);
            mAtvSearchOrderText.setVisibility(View.GONE);
            mOrderSetting.setVisibility(View.GONE);
            mAtvColorSystemText.setVisibility(View.GONE);
            mAtvColorSystem.setVisibility(View.GONE);
            mAtvSoundSystemText.setVisibility(View.GONE);
            mAtvSoundSystem.setVisibility(View.GONE);
        } else {
            Log.d(TAG, "hideAtvRelatedOption = " + value);
            //no effect at monent, and hide it
            //mAtvSearchOrderText.setVisibility(View.VISIBLE);
            //mOrderSetting.setVisibility(View.VISIBLE);
            mAtvColorSystemText.setVisibility(View.VISIBLE);
            mAtvColorSystem.setVisibility(View.VISIBLE);
            mAtvSoundSystemText.setVisibility(View.VISIBLE);
            mAtvSoundSystem.setVisibility(View.VISIBLE);
        }
    }

    private void showStopScanDialog() {
        Log.d(TAG, "showStopScanDialog");
        AlertDialog.Builder stopScanDialog = new AlertDialog.Builder(this);
        String allBandScanMessage;
        if (channelCounts == 1) {
            allBandScanMessage = getString(R.string.tv_all_band_search_dialog_message1)
                + " " + channelCounts + " "
                + getString(R.string.tv_all_band_search_dialog_messages2)
                + " " + getString(R.string.tv_all_band_search_dialog_message2);
        } else {
            allBandScanMessage = getString(R.string.tv_all_band_search_dialog_message1)
                + " " + channelCounts + " "
                + getString(R.string.tv_all_band_search_dialog_messages1)
                + " " + getString(R.string.tv_all_band_search_dialog_message2);
        }
        stopScanDialog.setMessage(allBandScanMessage);
        stopScanDialog.setPositiveButton(getString(R.string.dialog_btn_confirm_text)
            , new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (TvScanConfig.TV_SEARCH_MODE.get(TvScanConfig.TV_SEARCH_MANUAL_INDEX).equals(DroidLogicTvUtils.getSearchMode(ChannelSearchActivity.this))) {
                        if (ShowNoAtvFrequencyOrChannel()) {
                            if (!isManualStarted) {
                                isManualStarted = true;
                                mScanButton.setText(R.string.ut_stop_channel_scan);
                                if (mChannelHolder.getVisibility() == View.GONE) {
                                    mChannelHolder.setVisibility(View.VISIBLE);
                                }
                            } else {
                                isManualStarted = false;
                                mScanButton.setText(R.string.ut_manual_scan);
                                if (mChannelHolder.getVisibility() == View.VISIBLE) {
                                    mChannelHolder.setVisibility(View.GONE);
                                }
                            }
                            sendMessage(MANUAL_START, 0, null);
                        }
                    }
                    dialog.dismiss();
                }
            });
        stopScanDialog.setNegativeButton(getString(R.string.dialog_btn_cancel_text)
            , new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                    dialog.dismiss();
                }
            });
        stopScanDialog.create().show();
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.arg1) {
                case MANUAL_START:
                    Log.d(TAG, "=====MANUAL_START");
                    if (mTvScanManager.isConnected()) {
                        initparametres(MANUAL_SEARCH);
                        mTvScanManager.startManualScan();
                    } else {
                        Log.d(TAG, "Service connecting, please wait a second!");
                        sendMessageDelayeds(MANUAL_START, 0, null, 200);
                    }
                    break;
                case AUTO_START:
                    Log.d(TAG, "=====AUTO_START");
                    if (mTvScanManager.isConnected()) {
                        initparametres(AUTO_SEARCH);
                        mTvScanManager.startAutoScan();
                    } else {
                        Log.d(TAG, "Service connecting, please wait a second!");
                        sendMessageDelayeds(AUTO_START, 0, null, 200);
                    }
                    break;
                case PROCESS:
                    Log.d(TAG, "=====PROCESS");
                    if (!isNumberSearchMode) {
                        mProgressBar.setProgress(msg.what);
                    } else {
                        mProDia.setProgress(msg.what);
                    }
                    break;
                case CHANNEL:
                    Log.d(TAG, "=====CHANNEL");
                    if (!isNumberSearchMode && (isAutoStarted || isManualStarted)) {
                        if (msg.obj instanceof EventParams) {
                            EventParams eventParams = (EventParams) msg.obj;
                            if (eventParams == null) {
                                eventParams = getEventParams(null);
                            }
                            ArrayList<HashMap<String, Object>> stringInfo = getSearchedInfo(eventParams);
                            if (mChannelHolder.getVisibility() == View.GONE) {
                                mChannelHolder.setVisibility(View.VISIBLE);
                            }
                            StringBuilder sbInfo = new StringBuilder();
                            for (HashMap<String, Object> infoMap : stringInfo) {
                                sbInfo.append(infoMap.get(STRING_NAME))
                                        .append(infoMap.get(STRING_STATUS)).append("\t");
                            }
                            tvScanInfo.setText(sbInfo.toString());
                        }
                    }
                    if (mATvAutoScanMode == TvScanConfig.TV_ATV_AUTO_ALL_BAND
                        && isManualStarted && isAllBandScanStopped) {
                        isManualStarted = false;
                        isAllBandScanStopped = false;
                        mScanButton.setText(R.string.ut_manual_scan);
                        showStopScanDialog();
                    }
                    break;
                case STATUS:
                    Log.d(TAG, "=====STATUS");
                    if (!isNumberSearchMode) {
                        if (mATvAutoScanMode == TvScanConfig.TV_ATV_AUTO_ALL_BAND) {
                            if ("pause".equals((String) msg.obj)) {//scan pause
                                Log.d(TAG, "set flag to stop scanning");
                                isAllBandScanStopped = true;
                            }
                        } else {
                            mScanningMessage.setText((String) msg.obj);
                        }
                    } else {
                        hasFoundChannelNumber = msg.what;
                        if (hasFoundChannelNumber > 0) {
                            hasFoundChannel = true;
                            Log.d(TAG, "find channel num = " + hasFoundChannelNumber);
                            if (isSearching()) {
                                dtvStopScan();
                            }
                        } else if (hasFoundChannelNumber == -1) {
                            Log.d(TAG, "=====exit");
                            exitNumberSearch();
                            finish();
                        }
                        /*if ("exit".equals((String) msg.obj)) {//scan exit
                            exitNumberSearch();
                            finish();
                        }*/
                    }
                    break;
                case NUMBER_SEARCH_START:
                    Log.d(TAG, "=====NUMBER_SEARCH_START");
                    handler.postDelayed(TimeOutStopScanRunnable, 30000);//timeout 30s
                    initparametres(NUMBER_SEARCH_START);
                    mTvScanManager.startManualScan();
                    break;
                case TVTEST_DTMB_SEARCH_START:
                    Log.d(TAG, "=====TVTEST_DTMB_SEARCH_START");
                    if (mTvScanManager.isConnected()) {
                        onClick(mScanButton);
                    } else {
                        Log.d(TAG, "Service connecting, please wait a second!");
                        sendMessageDelayeds(TVTEST_DTMB_SEARCH_START, 0, null, 200);
                    }
                    break;
                case FREQUENCY:
                    Log.d(TAG, "=====FREQUENCY");
                    if (msg != null && !hasFoundInfo.containsKey((String) msg.obj)) {
                        hasFoundInfo.put((String) msg.obj, msg.what);
                        if (!hasFoundInfo.containsKey(DroidLogicTvUtils.FIRSTAUTOFOUNDFREQUENCY)) {
                            hasFoundInfo.put(DroidLogicTvUtils.FIRSTAUTOFOUNDFREQUENCY, msg.what);
                        }
                        if (!hasFoundInfo.containsKey(DroidLogicTvUtils.AUTO_SEARCH_MODE)) {
                            if (!isNumberSearchMode && TvScanConfig.TV_SEARCH_MODE.get(TvScanConfig.TV_SEARCH_AUTO_INDEX).equals(DroidLogicTvUtils.getSearchMode(ChannelSearchActivity.this))) {
                                hasFoundInfo.put(DroidLogicTvUtils.AUTO_SEARCH_MODE, 1);
                            }
                        }
                    }
                    break;
                case CHANNELNUMBER:
                    Log.d(TAG, "=====CHANNELNUMBER");
                        hasFoundInfo.put((String) msg.obj, msg.what);
                    break;
                case EXIT_SCAN:
                    Log.d(TAG, "=====EXIT_SCAN");
                    finish();
                    break;
                case SAVING:
                    Log.d(TAG, "=====SAVING");
                    if (mScanningMessage != null) {
                        mScanningMessage.setText(getString(R.string.tv_search_channel_saving));
                    }
                    if (mProgressBar != null) {
                        mProgressBar.setIndeterminate(true);
                    }
                    //wait EXIT_SCAN from tvstoremanager
                    removeShowActivityTimer();
                    break;
                default :
                    break;
            }
        }
    };

    private static final int MANUAL_SEARCH = 0;
    private static final int AUTO_SEARCH = 1;
    private int mAutoSearchType = -1;

    private void initparametres(int type) {
        int flag = DroidLogicTvUtils.getAtvDtvModeFlag(this);
        switch (flag) {
            case DroidLogicTvUtils.TV_SEARCH_ATV:
                mTvScanManager.setSearchSys(false, true);
                break;
            case DroidLogicTvUtils.TV_SEARCH_DTV:
                mTvScanManager.setSearchSys(true, false);
                break;
            case DroidLogicTvUtils.TV_SEARCH_ATV_DTV:
                mTvScanManager.setSearchSys(true, true);
                break;
            default:
                break;
        }

        if (DroidLogicTvUtils.isDtvContainsAtscByCountry(DroidLogicTvUtils.getCountry(this))) {
            mTvScanManager.setAtsccSearchSys(DroidLogicTvUtils.getAtsccListMode(this));
        } else {
            mTvScanManager.setAtsccSearchSys(DroidLogicTvUtils.TV_ATSC_MODE_STANDARD);
        }
        setNumberSearchNeed(0);

        if (AUTO_SEARCH == type) {
            mAutoSearchType = flag;
        }
        if (NUMBER_SEARCH_START== type) {
            //mTvScanManager.setSearchSys(mNumberSearchDtv, mNumberSearchAtv);
            setNumberSearchNeed(1);
        }
        setFrequency();
        setSelectCountry(DroidLogicTvUtils.getCountry(this));
        /*if (!isNumberSearchMode && mChannelHolder.getVisibility() == View.VISIBLE) {
            mChannelHolder.setVisibility(View.GONE);
        }*/
   }

    private void setNumberSearchNeed (int isNumberSearch) {
        DataProviderManager.putIntValue(this, DroidLogicTvUtils.IS_NUMBER_SEARCH, isNumberSearch);
    }

    private void setFrequency() {
        String atvdefaultbegain = "42250";
        String atvdefaultend = "868250";
        String dtvdefaultstart = "0";
        String from = null;
        String to = null;

        int[] freqPair = new int[2];
        //extra frequency need to add in dtmb xml
        if (TvContract.Channels.TYPE_DTMB.equals(DroidLogicTvUtils.getDtvType(this)) &&
                (DroidLogicTvUtils.getAtvDtvModeFlag(this) == DroidLogicTvUtils.TV_SEARCH_DTV) &&
                TvScanConfig.TV_SEARCH_MODE.get(TvScanConfig.TV_SEARCH_MANUAL_INDEX).equals(DroidLogicTvUtils.getSearchMode(this))) {
            from = String.valueOf(getCurrentDtmbPhysicalNumber() + 1);
        } else if (!isNumberSearchMode && mInputChannelFrom.getText() != null && mInputChannelFrom.getText().length() > 0) {
            from = mInputChannelFrom.getText().toString();
        } else if (isNumberSearchMode) {
            from = String.valueOf(mNumber.split("-")[0]);
        }
        if (!isNumberSearchMode && mInputChannelTo.getText() != null && mInputChannelTo.getText().length() > 0) {
            to = mInputChannelTo.getText().toString();
        } else if (isNumberSearchMode) {
            to = String.valueOf(mNumber.split("-")[0]);
        }

        if (mATvAutoScanMode == TvScanConfig.TV_ATV_AUTO_ALL_BAND && DroidLogicTvUtils.getAtvDtvModeFlag(this) == DroidLogicTvUtils.TV_SEARCH_ATV) {
            TvScanConfig.GetTvAtvMinMaxFreq(DroidLogicTvUtils.getCountry(this), freqPair);

            atvdefaultbegain = String.valueOf(freqPair[0] / 1000000);
            atvdefaultend = String.valueOf(freqPair[1] / 1000000);
            mTvScanManager.setFrequency(from != null ? from : atvdefaultbegain, to != null ? to : atvdefaultend);
        } else {
            mTvScanManager.setFrequency(from != null ? from : dtvdefaultstart, to != null ? to : dtvdefaultstart);
        }
        if (TvContract.Channels.TYPE_DVB_C.equals(DroidLogicTvUtils.getDtvType(this))) {
            if (mDvbcSymbolRateValue != null && mDvbcSymbolRateValue.getText() != null) {
                String symbol = mDvbcSymbolRateValue.getText().toString();
                if (!TextUtils.isEmpty(symbol) && TextUtils.isDigitsOnly(symbol)) {
                    DroidLogicTvUtils.setDvbcSymbolRate(this, Integer.valueOf(symbol));
                }
            }
        }
    }

    private boolean ShowNoAtvFrequencyOrChannel() {
        boolean status = false;
        String searchMode = DroidLogicTvUtils.getSearchMode(this);
        boolean manualsearch = false;
        int[] freqPair = new int[2];
        int input = 0;

        if (TvScanConfig.TV_SEARCH_MODE.get(TvScanConfig.TV_SEARCH_MANUAL_INDEX).equals(searchMode) || TvScanConfig.TV_SEARCH_MODE.get(TvScanConfig.TV_SEARCH_NIT_INDEX).equals(searchMode)) {
            manualsearch = true;
        }
        if (manualsearch && ((mInputChannelFrom.getText() != null && mInputChannelFrom.getText().length() > 0) || checkDtmbFrequencySelectDisplay())) {
            if (mATvAutoScanMode == TvScanConfig.TV_ATV_AUTO_ALL_BAND && DroidLogicTvUtils.getAtvDtvModeFlag(this) == DroidLogicTvUtils.TV_SEARCH_ATV) {
                TvScanConfig.GetTvAtvMinMaxFreq(DroidLogicTvUtils.getCountry(this), freqPair);

                input = Integer.valueOf(mInputChannelFrom.getText().toString());

                if (input < (freqPair[0] / 1000000) || (freqPair[1] / 1000000) < input) {
                    status = false;
                    ShowToastTint("Please input from " + freqPair[0] / 1000000 + "MHz to " +  freqPair[1] / 1000000 + "MHz");
                } else {
                    status = true;
                }
            } else {
                status = true;
            }
        } else if (manualsearch && mInputChannelFrom.getText() == null || mInputChannelFrom.getText().length() <= 0) {
            status = false;
            ShowToastTint(getString(R.string.set_frquency_channel));
        } else if (!manualsearch) {
            status = true;
        }
        if (status &&
                TvScanConfig.TV_COUNTRY.get(TvScanConfig.TV_COUNTRY_CHINA_INDEX).equals(DroidLogicTvUtils.getCountry(this)) &&
                (mInputChannelFrom.getText() == null || mInputChannelFrom.getText().length() <= 0)) {
            //show it if need a frequency range
            //status = false;
            //ShowToastTint(getString(R.string.set_frquency_channel));
        }
        return status;
    }

    private void ShowToastTint(String text) {
        LayoutInflater inflater = (LayoutInflater)ChannelSearchActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.layout_toast, null);

        TextView propmt = (TextView)layout.findViewById(R.id.toast_content);
        propmt.setText(text);

        Toast toast = new Toast(ChannelSearchActivity.this);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }

    private void sendMessage(int type, int message, String information) {
        Message msg = new Message();
        msg.arg1 = type;
        msg.what = message;
        msg.obj = information;
        mHandler.sendMessage(msg);
    }

    private void sendMessageDelayeds(int type, int message, String information, long delayMillis) {
        Message msg = new Message();
        msg.arg1 = type;
        msg.what = message;
        msg.obj = information;
        mHandler.sendMessageDelayed(msg, delayMillis);
    }

    boolean isManualStarted = false;
    boolean isAutoStarted = false;
    boolean isAllBandScanStopped = false;

    @Override
    public void onClick(View v) {
        //At the moment, if search_type_changed is 1 in Settings, make it 0,
        //otherwise, when searching channel finished, retreat from LiveTv and lunch LiveTv again, or click Menu-->Channel,
        //it will execute resumeTvIfNeeded and send broadcast to switch channel automatically, so it should be avoid.
        DroidLogicTvUtils.resetSearchTypeChanged(this);

        switch (v.getId()) {
            case R.id.search_channel:
                String searchMode = DroidLogicTvUtils.getSearchMode(this);
                if (TvScanConfig.TV_SEARCH_MODE.get(TvScanConfig.TV_SEARCH_MANUAL_INDEX).equals(searchMode) || TvScanConfig.TV_SEARCH_MODE.get(TvScanConfig.TV_SEARCH_NIT_INDEX).equals(searchMode)) {
                    if (ShowNoAtvFrequencyOrChannel()) {
                        if (!isManualStarted) {
                            isManualStarted = true;
                            mScanButton.setText(R.string.ut_stop_channel_scan);
                            if (mChannelHolder.getVisibility() == View.GONE) {
                                mChannelHolder.setVisibility(View.VISIBLE);
                            }
                        } else {
                            isManualStarted = false;
                            if (TvScanConfig.TV_SEARCH_MODE.get(TvScanConfig.TV_SEARCH_NIT_INDEX).equals(searchMode)) {
                                mScanButton.setText(R.string.ut_nit_scan);;
                            } else {
                                mScanButton.setText(R.string.ut_manual_scan);
                            }
                            if (mChannelHolder.getVisibility() == View.VISIBLE) {
                                mChannelHolder.setVisibility(View.GONE);
                            }
                            if (isSearching()) {
                                stopScan();
                            }
                            finish();
                            return;
                        }
                        sendMessage(MANUAL_START, 0, null);
                    }
                } else {
                    if (!isAutoStarted) {
                        isAutoStarted = true;
                        mScanButton.setText(R.string.ut_stop_channel_scan);
                        sendMessage(CHANNEL, 0, null);
                        if (mChannelHolder.getVisibility() == View.GONE) {
                            mChannelHolder.setVisibility(View.VISIBLE);
                        }
                    } else {
                        isAutoStarted = false;
                        mScanButton.setText(R.string.ut_auto_scan);
                        if (mChannelHolder.getVisibility() == View.VISIBLE) {
                            mChannelHolder.setVisibility(View.GONE);
                        }
                        if (isSearching()) {
                            stopScan();
                        }
                        finish();
                        return;
                    }
                    sendMessage(AUTO_START, 0, null);
                }
                break;
            /*case R.id.tune_auto:
                if (!isAutoStarted) {
                    isAutoStarted = true;
                    mAutoScanButton.setText(R.string.ut_stop_channel_scan);
                } else {
                    isAutoStarted = false;
                    mAutoScanButton.setText(R.string.ut_auto_scan);
                }
                sendMessage(AUTO_START, 0, null);
                break;
            case R.id.manual_tune_atv:
            case R.id.auto_tune_atv:
                if (mManualATV.isChecked() || mAutoATV.isChecked()) {
                    mAtscSearchTypeOption.setEnabled(true);
                } else {
                    mAtscSearchTypeOption.setEnabled(false);
                }
                break;*/
            case R.id.button_left:
                int currentDtmbPhysicalNumber1 = getCurrentDtmbPhysicalNumber();
                currentDtmbPhysicalNumber1--;
                if (mDtmbDisplayNameWithExtra.size() == 0) {
                    currentDtmbPhysicalNumber1 = 0;
                } else if (currentDtmbPhysicalNumber1 < 0) {
                    currentDtmbPhysicalNumber1 = mDtmbDisplayNameWithExtra.size() - 1;
                }
                mDtmbFrequency.setText(mDtmbDisplayNameWithExtra.size() > 0 ? mDtmbDisplayNameWithExtra.get(currentDtmbPhysicalNumber1) : "------");
                DroidLogicTvUtils.setCurrentDtmbPhysicalNumber(this, currentDtmbPhysicalNumber1);
                break;
            case R.id.button_right:
                int currentDtmbPhysicalNumber2 = getCurrentDtmbPhysicalNumber();
                currentDtmbPhysicalNumber2++;
                if (mDtmbDisplayNameWithExtra.size() == 0) {
                    currentDtmbPhysicalNumber2 = 0;
                } else if (currentDtmbPhysicalNumber2 > mDtmbDisplayNameWithExtra.size() - 1) {
                    currentDtmbPhysicalNumber2 = 0;
                }
                mDtmbFrequency.setText(mDtmbDisplayNameWithExtra.size() > 0 ? mDtmbDisplayNameWithExtra.get(currentDtmbPhysicalNumber2) : "------");
                DroidLogicTvUtils.setCurrentDtmbPhysicalNumber(this, currentDtmbPhysicalNumber2);
                break;
            default:
                break;
        }
    }

    private void stopScan() {
        dtvStopScan();
        DroidLogicTvUtils.resetSearchStatus(this);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        String country = DroidLogicTvUtils.getCountry(this);
        if (mKeyCodeHandler.input(event) && TextUtils.equals("US", country) && !isSearching()) {
            mExportChannel.setVisibility(View.VISIBLE);
            mImportChannel.setVisibility(View.VISIBLE);
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "==== focus =" + getCurrentFocus() + ", keycode =" + keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (isSearching()) {
                    stopScan();
                } else {
                    finish();
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                startShowActivityTimer();
                break;
            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void finish() {
        //At the moment, if search_type_changed is 1 in Settings, make it 0,
        //otherwise, when change search_type in ChannelSearchActivity, but don't search channel and push the EXIT key to return to LiveTv,
        //it will do resumeTvIfNeeded and the current channel will be switched to next one, so this can't happen.
        DroidLogicTvUtils.resetSearchTypeChanged(this);

        isFinished = true;

        if (!mPowerManager.isScreenOn()) {
            Log.d(TAG, "TV is going to sleep, stop tv");
            return;
        }
        //send search info to livetv if found any
        Intent intent = getChannelDataIntent();
        if (intent != null) {
            if (mHomePressed) {
                intent.setAction(ACTION_SET_SEARCHED_CHANNEL_DATA);
                ChannelSearchActivity.this.sendBroadcast(intent);
                mHomePressed = false;
            } else {
                setResult(RESULT_OK, intent);
            }
        } else {
            // when auto search no channel, let LiveTv know
            if (mAutoSearchType >= DroidLogicTvUtils.TV_SEARCH_ATV) {
                Intent it = new Intent();
                it.putExtra("service_number", 0);
                it.putExtra("service_type", mAutoSearchType);
                setResult(RESULT_CANCELED, it);
            }
        }
        super.finish();
    }

    private Intent getChannelDataIntent() {
        if (hasFoundInfo.size() > 0) {
            Intent intent = new Intent();
            intent.putExtra("service_type", mAutoSearchType);
            for (Object key : hasFoundInfo.keySet()) {
                intent.putExtra((String)key, (int)hasFoundInfo.get(key));
                Log.d(TAG, "searched info key = " + ((String)key) + ", value = " + ((int)hasFoundInfo.get(key)));
            }
            return intent;
        } else {
            return null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");

        if (isFinished) {
            ShowToastTint(getString(R.string.tv_search_channel_stopped));
            finish();
            //resume();
            isFinished = false;
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(DroidLogicTvUtils.ACTION_CHANNEL_CHANGED);
        filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        registerReceiver(mReceiver, filter,
            2/*Context.RECEIVER_EXPORTED*/ | Context.RECEIVER_VISIBLE_TO_INSTANT_APPS);
        if (isTvTestDTMBSearchMode)
            sendMessageDelayeds(TVTEST_DTMB_SEARCH_START, 0, null, 1000);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        unregisterReceiver(mReceiver);

        if (isSearching()) {
            dtvStopScan();

            if (!mPowerManager.isScreenOn()) {
                stopTv();
            }
        }

        if (!isFinishing()) {
            finish();
        }
    }

    @Override
    public void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        mTvScanManager.unBindService();
        DroidLogicTvUtils.resetSearchStatus(this);
        release();
    }

    @Override
    protected void finalize() throws Throwable {
        Log.d(TAG, "finalized");
        super.finalize();
    }

    public void startShowActivityTimer () {
        Log.d(TAG, "===== startShowActivityTimer");
        handler.removeMessages(START_TIMER);
        handler.sendEmptyMessageDelayed(START_TIMER, 30 * 1000);
    }

    public void removeShowActivityTimer () {
        Log.d(TAG, "===== removeShowActivityTimer");
        handler.removeMessages(START_TIMER);
    }

    Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case START_TIMER:
                    Log.d(TAG, "===== START_TIMER");
                    InputMethodManager imm = getSystemService(InputMethodManager.class);
                    if ((!isNumberSearchMode && (!isSearching() && !imm.isAcceptingText())) || (isNumberSearchMode && hasFoundChannel)) {
                        finish();
                    } else {
                        sendEmptyMessageDelayed(START_TIMER, 30 * 1000);
                    }
                    break;
                case START_INIT:
                    Log.d(TAG, "===== START_INIT");
                    if (mTvScanManager.isConnected()) {
                        mTvScanManager.init();
                        if (isNumberSearchMode || isTvTestDTMBSearchMode) {
                            initNumberSearch(ChannelSearchActivity.this);
                            return;
                        }
                    } else {
                        sendEmptyMessageDelayed(START_INIT, 200);
                    }
                    break;
            }
        }
    };

    private boolean mHomePressed = false;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            /*if (action.equals(DroidLogicTvUtils.ACTION_CHANNEL_CHANGED)) {
                mSettingsManager.setCurrentChannelData(intent);
                    mOptionUiManagerT.init(mSettingsManager);
                currentFragment.refreshList();
            } else */if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
                String reason = intent.getStringExtra("reason");
                if (TextUtils.equals(reason, "homekey")) {
                    stopTv();
                    Log.d(TAG,"stop tv when exiting by home key");
                    mHomePressed = true;
                    finish();
                }
            }
        }
    };

    private void resume() {
        isManualStarted = false;
        isAutoStarted = false;
        isAllBandScanStopped = false;

        initSpinner();
        mTvScanManager.init();
        startShowActivityTimer();
    }

    private void release() {
        mTvScanManager.release();
        handler.removeCallbacksAndMessages(null);
        mHandler.removeCallbacksAndMessages(null);
        mHandler = null;
        handler = null;
        mTvDataBaseManager = null;
        mTvScanManager = null;
        exitNumberSearch();
    }

    final int TV_SEARCH_SYS_AUTO = 0;
    final int TV_SEARCH_SYS_PAL = 1;
    final int TV_SEARCH_SYS_NTSC = 2;
    final int TV_SEARCH_SYS_SECAM = 3;

    final int ATV_SEARCH_SOUND_MIN = 0;
    final int ATV_SEARCH_SOUND_DK = 0;
    final int ATV_SEARCH_SOUND_I = 1;
    final int ATV_SEARCH_SOUND_BG = 2;
    final int ATV_SEARCH_SOUND_M = 3;
    final int ATV_SEARCH_SOUND_L = 4;
    final int ATV_SEARCH_SOUND_AUTO = 5;
    final int ATV_SEARCH_SOUND_MAX = 6;

    public final int TV_SEARCH_ATV = 0;
    public final int TV_SEARCH_DVB_T = 1;
    public final int TV_SEARCH_ATSC_T = 2;
    public final int TV_SEARCH_ATSC_C = 3;
    public final int TV_SEARCH_ATSC_AUTO = 0;
    public final int TV_SEARCH_ATSC_LRC = 1;
    public final int TV_SEARCH_ATSC_HRC = 2;

    public void setSelectCountry(String country) {
        Log.d(TAG, "setCountrytoTvserver = " + country);
        mTvControlManager.SetTvCountry(country);
    }

    private int getIndex(String value, String[] list) {
        if (value != null && list != null) {
            for (int i = 0; i < list.length; i++) {
                if (value.equals(list[i])) {
                    return i;
                }
            }
        }
        return -1;
    }

    private int getCurrentDtmbPhysicalNumber() {
        int mode = DroidLogicTvUtils.getCurrentDtmbPhysicalNumber(this);
        Log.d(TAG, "getCurrentDtmbPhysicalNumber = " + mode);
        return mode;
    }

    private List<String> getDtmbFrequencyInfoAddExtra() {
        List<String> result = new ArrayList<String>();
        TvControlManager.TvMode mode = new TvControlManager.TvMode(TvContract.Channels.TYPE_DTMB);
        mode.setList(0 + DTV_TO_ATV);
        ArrayList<FreqList> m_fList = mTvControlManager.DTVGetScanFreqList(mode.getMode());
        if (m_fList != null && m_fList.size() > 0) {
            Iterator it = m_fList.iterator();
            while (it.hasNext()) {
                FreqList tempFreqList = (FreqList)(it.next());
                String display = tempFreqList.physicalNumDisplayName;
                //Log.d(TAG, "getDtmbFrequencyInfoAddExtra = " + display + ", num = " + tempFreqList.channelNum + ", freq = " + tempFreqList.freq);
                result.add(display);
            }
        }
        return result;
    }

     //30s timeout, stop scan
    Runnable TimeOutStopScanRunnable = new Runnable() {
        @Override
        public void run() {
            if (isSearching()) {
                dtvStopScan();
            }
            exitNumberSearch();
            finish();
        }
    };

    private boolean hasChannelBeenSaved() {
        boolean result = false;
        if (hasFoundInfo != null) {
            int searchedFirstFrequency = 0;
            if (hasFoundInfo.get(DroidLogicTvUtils.DTVPROGRAM) != null) {
                searchedFirstFrequency = (int)hasFoundInfo.get(DroidLogicTvUtils.DTVPROGRAM);
            } else if (hasFoundInfo.get(DroidLogicTvUtils.ATVPROGRAM) != null) {
                searchedFirstFrequency = (int)hasFoundInfo.get(DroidLogicTvUtils.ATVPROGRAM);
            } else if (hasFoundInfo.get(DroidLogicTvUtils.RADIOPROGRAM) != null) {
                searchedFirstFrequency = (int)hasFoundInfo.get(DroidLogicTvUtils.RADIOPROGRAM);
            }
            if (searchedFirstFrequency != 0) {
                final String ADTV_INPUT_ID = "com.droidlogic.tvinput/.services.ADTVInputService/HW16";
                ArrayList<ChannelInfo> channelInfoList = mTvDataBaseManager.getChannelList(ADTV_INPUT_ID, TvContract.Channels.SERVICE_TYPE_AUDIO_VIDEO, false, true);
                channelInfoList.addAll(mTvDataBaseManager.getChannelList(ADTV_INPUT_ID, TvContract.Channels.SERVICE_TYPE_AUDIO, false, true));
                if (channelInfoList != null && channelInfoList.size() > 0) {
                    Collections.sort(channelInfoList, new CompareDisplayNumber());
                    for (ChannelInfo channel : channelInfoList) {
                        if (channel.getFrequency() == searchedFirstFrequency) {
                            Log.d(TAG, "hasChannelBeenSaved searchedFirstFrequency = " + searchedFirstFrequency);
                            result = true;
                            break;
                        }
                    }
                }
            }
        }
        return result;
    }

    public static class CompareDisplayNumber implements Comparator<ChannelInfo> {

        @Override
        public int compare(ChannelInfo o1, ChannelInfo o2) {
            int result = compareString(o1.getDisplayNumber(), o2.getDisplayNumber());
            return result;
        }

        private int compareString(String a, String b) {
            if (a == null) {
                return b == null ? 0 : -1;
            }
            if (b == null) {
                return 1;
            }

            int[] disnumbera = getMajorAndMinor(a);
            int[] disnumberb = getMajorAndMinor(b);
            if (disnumbera[0] != disnumberb[0]) {
                return (disnumbera[0] - disnumberb[0]) > 0 ? 1 : -1;
            } else if (disnumbera[1] != disnumberb[1]) {
                return (disnumbera[1] - disnumberb[1]) > 0 ? 1 : -1;
            }
            return 0;
        }

        private int[] getMajorAndMinor(String disnumber) {
            int[] result = {-1, -1};//major, minor
            String[] splitone = (disnumber != null ? disnumber.split("-") : null);
            if (splitone != null && splitone.length > 0) {
                int length = 2;
                if (splitone.length <= 2) {
                    length = splitone.length;
                } else {
                    Log.d(TAG, "informal disnumber");
                    return result;
                }
                for (int i = 0; i < length; i++) {
                    try {
                       result[i] = Integer.valueOf(splitone[i]);
                    } catch (NumberFormatException e) {
                        Log.d(TAG, splitone[i] + " not integer:" + e.getMessage());
                    }
                }
            }
            return result;
        }
    }

    private List<String> getPreferredLanguageList() {
        List<String> languageList = new ArrayList<String>();
        languageList.add("Traditional Chinese");
        languageList.add("English");
        languageList.add("Simplified Chinese");
        languageList.add("None");
        return languageList;
    }
}
