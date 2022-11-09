/*
 * Copyright (c) 2014 Amlogic, Inc. All rights reserved.
 *
 * This source code is subject to the terms and conditions defined in the
 * file 'LICENSE' which is part of this source code package.
 *
 * Description: JAVA file
 */

package com.android.tv.droidlogic.tvtest;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Binder;
import android.os.Message;
import android.util.Log;
import android.content.Context;
import android.widget.Toast;
import android.text.TextUtils;
import android.content.ComponentName;
import android.app.ActivityManager;
import org.json.JSONObject;
import org.json.JSONException;
import com.droidlogic.app.tv.TvControlManager;
import com.droidlogic.app.tv.TvScanConfig;
import com.droidlogic.app.tv.DroidLogicTvUtils;
import com.droidlogic.app.tv.TvInSignalInfo;
import com.droidlogic.app.SystemControlManager;
import android.provider.Settings;
import android.os.PowerManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;
import java.net.InetAddress;
import java.util.concurrent.Callable;
import android.os.AsyncTask;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import com.droidlogic.tvinput.settings.OptionUiManagerT;
import com.droidlogic.tvinput.services.TvInputBaseSession;
import vendor.amlogic.hardware.tvserver.V1_0.FreqList;
import android.media.tv.TvContract;
import java.util.Iterator;

public class TvTestService extends Service {
    private static final String TAG = "TvTestService";
    private static final String INPUT_ID_ADTV = "com.droidlogic.tvinput/.services.ADTVInputService/HW16";
    private final String INPUT_ID_DTVKITSOURCE = "com.droidlogic.dtvkit.inputsource/.DtvkitTvInput/HW19";
    private static final String LIVETV_PACKAGENAME = "com.droidlogic.android.tv";
    private static final String LIVETV_CLASSNAME = "com.android.tv.MainActivity";
    private static final String ACTION_TV_TEST_INFO = "com.droidlogic.tv_test.action";
    private static final String TV_TEST_COMMAND = "tv_test_command";
    private static final String VALUE_TV_TEST_COMMAND_SEARCH_ATSC_NUMBER= "atsc-search-bynumber";
    private static final String VALUE_TV_TEST_COMMAND_SELECTED_ATSC_NUMBER= "atsc-selected-channel-bynumber";
    private static final String VALUE_TV_TEST_COMMAND_SEARCH_ATSC_FREQUENCY= "atsc-search-byfrequency";
    private static final String VALUE_TV_TEST_COMMAND_SELECTED_ATSC_FREQUENCY= "atsc-selected-channel-byfrequency";
    private static final String VALUE_TV_TEST_COMMAND_SEARCH_DVB_T_FREQUENCY = "dvb-t-search-byfrequency";
    private static final String VALUE_TV_TEST_COMMAND_SEARCH_DVB_C_FREQUENCY = "dvb-c-search-byfrequency";
    private static final String VALUE_TV_TEST_COMMAND_SEARCH_DTMB_FREQUENCY = "dtmb-search-byfrequency";
    private static final String VALUE_TV_TEST_COMMAND_SEARCH_ISDB_FREQUENCY = "isdb-search-byfrequency";
    private static final String VALUE_TV_TEST_COMMAND_SEARCH_DVB_S_FREQUENCY = "dvb-s-search-byfrequency";
    private static final String VALUE_TV_TEST_COMMAND_SEARCH_ISDB_CLEAR = "clear-isdb-search";
    private static final String VALUE_TV_TEST_COMMAND_SEARCH_DVBC_CLEAR = "clear-dvb-c-search";
    private static final String VALUE_TV_TEST_COMMAND_ATSC_FREQUENCY = "channelFreqency";
    private static final String ATSC_COUNTRY= "US";
    private static final String DTMB_COUNTRY= "CN";
    private static final int DTV_TO_ATV = 5;
    private static final String ATSC_MODE_AIR = "ATSC-T";
    private static final String ATSC_MODE_CABLE_AUTO = "ATSC-C-AUTO";
    private static final String DROP_FRAME_COUNT_NODE = "/sys/class/vdec/vdec_status";
    private static final String REMOTE_PROTOCOL = "/sys/devices/virtual/remote/amremote/protocol";

    private static final int ATSC_DTV_DEFAULT_CHANNELNUM = 1;
    private static final int ATSC_NOT_DTV_DEFAULT_FREQUENCY = 57000000;
    private static final int DEAL_TEST_SEARCH_CONTENT_NUMBER = 0x1001;
    private static final int DEAL_TEST_SELECTED_CONTENT_NUMBER = 0x1002;
    private static final int DEAL_TEST_SEARCH_CONTENT_FREQUENCY = 0x1003;
    private static final int DEAL_TEST_SELECTED_CONTENT_FREQUENCY = 0x1004;
    private static final int DEAL_TEST_DVBT_SEARCH_CONTENT_FREQUENCY = 0x1006;
    private static final int DEAL_TEST_CONTENT_CALLBACK = 0x1005;
    private static final int DEAL_TEST_DVBC_SEARCH_CONTENT_FREQUENCY = 0x1007;
    private static final int DEAL_TEST_DTMB_SEARCH_CONTENT_FREQUENCY = 0x1008;
    private static final int DEAL_TEST_ISDB_SEARCH_CONTENT_FREQUENCY = 0x1009;
    private static final int DEAL_TEST_DVBS_SEARCH_CONTENT_FREQUENCY = 0x1010;
    private static final int DEAL_TEST_ISDB_SEARCH_CONTENT_CLEAR = 0x1011;
    private static final int DEAL_TEST_DVBC_SEARCH_CONTENT_CLEAR = 0x1012;
    private static final int SOCKET_COMMUNICATION_THREAD = 2;

    private static final int DELAY_4_ACTIVITY_START = 2800;

    private TVTestServiceCallback mTVTestServiceCallback = null;
    private TvInputManager mTvInputManager = null;
    private TvControlManager mTvControlManager = null ;
    private PowerManager mPowerManager = null;
    private SystemControlManager mSystemControlManager = null;
    private boolean isConnectRemote = false;
    private int mDVBSigStatus = 0;
    private TvControlManager.SourceInput_Type mTvSource = TvControlManager.SourceInput_Type.SOURCE_TYPE_ADTV;
    private RemoteCallbackList<ITvTestCallbackListener> mListenerList = new RemoteCallbackList<>();
    private final int port = 9527;
    private ServerSocket server = null;
    private Socket socket = null;
    private ExecutorService pool = Executors.newFixedThreadPool(8);
    private Context mContext = null;
    private SocketCommunicationTask mSocketCommunicationTask = new SocketCommunicationTask();
    private volatile boolean signalState = false;
    private volatile int lastDropCount = 0;
    private volatile int lastFrameCount = 0;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg){
            super.handleMessage(msg);
            switch (msg.what) {
                case DEAL_TEST_SEARCH_CONTENT_NUMBER:
                    dealATSCSearchParamsByJsonWithNumber((String)msg.obj);
                    break;
                case DEAL_TEST_SELECTED_CONTENT_NUMBER:
                    dealATSCSelectedParamsByJsonWithNumber((String)msg.obj);
                    break;
                case DEAL_TEST_SEARCH_CONTENT_FREQUENCY:
                    dealATSCSearchParamsByJsonWithFreqency((String)msg.obj);
                    break;
                case DEAL_TEST_SELECTED_CONTENT_FREQUENCY:
                    dealATSCSelectedParamsByJsonWithFreqency((String)msg.obj);
                    break;
                case SOCKET_COMMUNICATION_THREAD:
                    startSocketThreadPool();
                    break;
                case DEAL_TEST_DVBT_SEARCH_CONTENT_FREQUENCY:
                    dealDVBTSearchParamsByJsonWithNumber((String)msg.obj);
                    break;
                case DEAL_TEST_DVBC_SEARCH_CONTENT_FREQUENCY:
                    dealDVBCSearchParamsByJsonWithFrequency((String)msg.obj);
                    break;
                case DEAL_TEST_DTMB_SEARCH_CONTENT_FREQUENCY:
                    dealDTMBSearchParamsByJsonWithFrequency((String)msg.obj);
                    break;
                case DEAL_TEST_ISDB_SEARCH_CONTENT_FREQUENCY:
                    dealISDBSearchParamsByJsonWithFrequency((String)msg.obj);
                    break;
                case DEAL_TEST_ISDB_SEARCH_CONTENT_CLEAR:
                    dealISDBClearSearchParamsByJson((String)msg.obj);
                    break;
                case DEAL_TEST_DVBC_SEARCH_CONTENT_CLEAR:
                    dealDVBCClearSearchParamsByJson((String)msg.obj);
                    break;
                case DEAL_TEST_DVBS_SEARCH_CONTENT_FREQUENCY:
                    dealDVBSSearchParamsByJsonWithFrequency((String)msg.obj);
                    break;
                default:
                    String message = (String)(msg.obj);
                    try{
                        int count = mListenerList.beginBroadcast();
                        for (int i = 0; i < count; i++) {
                            mListenerList.getBroadcastItem(i).onRespond(message);
                        }
                        mListenerList.finishBroadcast();
                    } catch (RemoteException e){
                        e.printStackTrace();
                    }
                    break;
            }
        }
    };

    private final ITvTestService.Stub mBinder = new ITvTestService.Stub(){

        public void init(String args){
            initT(args);
        }

        public void getDVBSignalStatusMsg(int sigStatus){
            getDVBSignalStatusMsgT(sigStatus);
        }

        public void registerListener(ITvTestCallbackListener listener) throws RemoteException {
            mListenerList.register(listener);
        }

        public void unregisterListener(ITvTestCallbackListener listener) throws RemoteException {
            mListenerList.unregister(listener);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "=====onBind");
        return mBinder;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "=====onCreate");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_TV_TEST_INFO);
        registerReceiver(mTvTestBroadcastReceiver, intentFilter);
        mTvControlManager = TvControlManager.getInstance();
        mTvInputManager = (TvInputManager)getSystemService(Context.TV_INPUT_SERVICE);
        mSystemControlManager = SystemControlManager.getInstance();
        mPowerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
        mContext = (Context)this;
        mSocketCommunicationTask.execute();
    }

    public void startSocketThreadPool() {

          Log.d(TAG, " ## server created ##");

			try {
				server = new ServerSocket(port);
				while (true) {
					Log.d(TAG, "----------- server wait client -----------");
					socket = server.accept();//阻塞方法，一直等到有个socket客户端连过来
					Log.d(TAG, "----------- server start thread -----------");
					Callable<String> callable = new socketCallable(socket);
					pool.submit(callable);
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
						Log.d(TAG, "---- close server ----");
						server.close();
					} catch (IOException e) {
						Log.d(TAG, "---- server error ----");
						e.printStackTrace();
					}
			}

	}

    public void initT(String args){
        Log.d(TAG, "=====init args="+args);
    }

    public void registerListener(ITvTestCallbackListener listener) throws RemoteException {
        mListenerList.register(listener);
    }

    public void unregisterListener(ITvTestCallbackListener listener) throws RemoteException {
        mListenerList.unregister(listener);
    }

    private final BroadcastReceiver mTvTestBroadcastReceiver = new BroadcastReceiver(){
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "TV_TEST_CASE intent = " + (intent != null ? intent.getExtras() : null));
            if (intent != null) {
                try {
                    String action = intent.getAction();
                    if (ACTION_TV_TEST_INFO.equals(action)) {
                        String tv_test_content = intent.getStringExtra(TV_TEST_COMMAND);
                        String command = getCommandByJson(tv_test_content);
                        boolean isActivityTop = isActivityTop(LIVETV_CLASSNAME, context);
                        switch (command) {
                            case VALUE_TV_TEST_COMMAND_SEARCH_ATSC_NUMBER:
                                //am broadcast -a com.droidlogic.tv_test.action --es tv_test_command "{"command":"atsc-search-bynumber","channelNum":11,"type":"ATSC-T"}"
                                Log.d(TAG, VALUE_TV_TEST_COMMAND_SEARCH_ATSC_NUMBER+", isActivityTop="+isActivityTop);
                                if (!isActivityTop) {
                                    startLiveTvMainAty();
                                    sendHandlerMSG(DEAL_TEST_SEARCH_CONTENT_NUMBER, 0, 0, tv_test_content, DELAY_4_ACTIVITY_START);
                                    return;
                                }
                                sendHandlerMSG(DEAL_TEST_SEARCH_CONTENT_NUMBER, 0, 0, tv_test_content, 200);
                                break;
                            case VALUE_TV_TEST_COMMAND_SELECTED_ATSC_NUMBER:
                                //am broadcast -a com.droidlogic.tv_test.action --es tv_test_command "{"command":"atsc-selected-channel-bynumber","channelNum":11}"
                                Log.d(TAG, VALUE_TV_TEST_COMMAND_SELECTED_ATSC_NUMBER+", isActivityTop="+isActivityTop);
                                sendHandlerMSG(DEAL_TEST_SELECTED_CONTENT_NUMBER, 0, 0, tv_test_content, 200);
                                break;
                            case VALUE_TV_TEST_COMMAND_SEARCH_ATSC_FREQUENCY:
                                //am broadcast -a com.droidlogic.tv_test.action --es tv_test_command "{"command":"atsc-search-byfrequency","channelFreqency":85000000,"type":"ATSC-T"}"
                                Log.d(TAG, VALUE_TV_TEST_COMMAND_SEARCH_ATSC_FREQUENCY+", isActivityTop="+isActivityTop);
                                if (!isActivityTop) {
                                    startLiveTvMainAty();
                                    sendHandlerMSG(DEAL_TEST_SEARCH_CONTENT_FREQUENCY, 0, 0, tv_test_content, DELAY_4_ACTIVITY_START);
                                    return;
                                }
                                sendHandlerMSG(DEAL_TEST_SEARCH_CONTENT_FREQUENCY, 0, 0, tv_test_content, 200);
                                break;
                            case VALUE_TV_TEST_COMMAND_SELECTED_ATSC_FREQUENCY:
                                //am broadcast -a com.droidlogic.tv_test.action --es tv_test_command "{"command":"atsc-selected-channel-byfrequency","channelFreqency":85000000}"
                                Log.d(TAG, VALUE_TV_TEST_COMMAND_SELECTED_ATSC_FREQUENCY);
                                sendHandlerMSG(DEAL_TEST_SELECTED_CONTENT_FREQUENCY, 0, 0, tv_test_content, 200);
                                break;
                            case VALUE_TV_TEST_COMMAND_SEARCH_DVB_T_FREQUENCY:
                                //am broadcast -a com.droidlogic.tv_test.action --es tv_test_command "{"command":"dvb-t-search-byfrequency","channelFreqency":85000000,"dvbt_bandWidth":"5MHZ","dvbt_transMode":"1K","dvbt_type":"DVB-T"}"
                                Log.d(TAG, VALUE_TV_TEST_COMMAND_SEARCH_DVB_T_FREQUENCY+", isActivityTop="+isActivityTop);
                                if (!isActivityTop) {
                                    startLiveTvMainAty();
                                    //sendHandlerMSG(DEAL_TEST_SEARCH_CONTENT_NUMBER, 0, 0, tv_test_content, DELAY_4_ACTIVITY_START);
                                    sendHandlerMSG(DEAL_TEST_DVBT_SEARCH_CONTENT_FREQUENCY, 0, 0, tv_test_content, DELAY_4_ACTIVITY_START);
                                    return;
                                }
                                //sendHandlerMSG(DEAL_TEST_SEARCH_CONTENT_NUMBER, 0, 0, tv_test_content, 200);
                                sendHandlerMSG(DEAL_TEST_DVBT_SEARCH_CONTENT_FREQUENCY, 0, 0, tv_test_content, 200);
                                break;
                            case VALUE_TV_TEST_COMMAND_SEARCH_DVB_C_FREQUENCY:
                                //am broadcast -a com.droidlogic.tv_test.action --es tv_test_command "{"command":"dvb-c-search-byfrequency","channelFreqency":154000000,"dvbc_modulation":"AUTO","dvbc_symbolrate":6900}"
                                Log.d(TAG, VALUE_TV_TEST_COMMAND_SEARCH_DVB_C_FREQUENCY+", isActivityTop="+isActivityTop);
                                if (!isActivityTop) {
                                    startLiveTvMainAty();
                                    sendHandlerMSG(DEAL_TEST_DVBC_SEARCH_CONTENT_FREQUENCY, 0, 0, tv_test_content, DELAY_4_ACTIVITY_START);
                                    return;
                                }
                                sendHandlerMSG(DEAL_TEST_DVBC_SEARCH_CONTENT_FREQUENCY, 0, 0, tv_test_content, 200);
                                break;
                            case VALUE_TV_TEST_COMMAND_SEARCH_DTMB_FREQUENCY:
                                //am broadcast -a com.droidlogic.tv_test.action --es tv_test_command "{"command":"dtmb-search-byfrequency","channelFreqency":60500000}"
                                Log.d(TAG, VALUE_TV_TEST_COMMAND_SEARCH_DTMB_FREQUENCY+", isActivityTop="+isActivityTop);
                                if (!isActivityTop) {
                                    startLiveTvMainAty();
                                    sendHandlerMSG(DEAL_TEST_DTMB_SEARCH_CONTENT_FREQUENCY, 0, 0, tv_test_content, DELAY_4_ACTIVITY_START);
                                    return;
                                }
                                sendHandlerMSG(DEAL_TEST_DTMB_SEARCH_CONTENT_FREQUENCY, 0, 0, tv_test_content, 200);
                                break;
                            case VALUE_TV_TEST_COMMAND_SEARCH_ISDB_FREQUENCY:
                                //am broadcast -a com.droidlogic.tv_test.action --es tv_test_command "{"command":"isdb-search-byfrequency","channelFreqency":85000000,"isdb_bandWidth":"5MHZ","isdb_transMode":"1K"}"
                                Log.d(TAG, VALUE_TV_TEST_COMMAND_SEARCH_ISDB_FREQUENCY+", isActivityTop="+isActivityTop);
                                if (!isActivityTop) {
                                    startLiveTvMainAty();
                                    sendHandlerMSG(DEAL_TEST_ISDB_SEARCH_CONTENT_FREQUENCY, 0, 0, tv_test_content, DELAY_4_ACTIVITY_START);
                                    return;
                                }
                                sendHandlerMSG(DEAL_TEST_ISDB_SEARCH_CONTENT_FREQUENCY, 0, 0, tv_test_content, 200);
                                break;
                            case VALUE_TV_TEST_COMMAND_SEARCH_ISDB_CLEAR:
                                //am broadcast -a com.droidlogic.tv_test.action --es tv_test_command "{"command":"clear-isdb-search"}"
                                Log.d(TAG, VALUE_TV_TEST_COMMAND_SEARCH_ISDB_CLEAR+", isActivityTop="+isActivityTop);
                                if (!isActivityTop) {
                                    startLiveTvMainAty();
                                    sendHandlerMSG(DEAL_TEST_ISDB_SEARCH_CONTENT_CLEAR, 0, 0, tv_test_content, DELAY_4_ACTIVITY_START);
                                    return;
                                }
                                sendHandlerMSG(DEAL_TEST_ISDB_SEARCH_CONTENT_CLEAR, 0, 0, tv_test_content, 200);
                                break;
                            case VALUE_TV_TEST_COMMAND_SEARCH_DVBC_CLEAR:
                                //am broadcast -a com.droidlogic.tv_test.action --es tv_test_command "{"command":"clear-dvb-c-search"}"
                                Log.d(TAG, VALUE_TV_TEST_COMMAND_SEARCH_DVBC_CLEAR+", isActivityTop="+isActivityTop);
                                if (!isActivityTop) {
                                        startLiveTvMainAty();
                                        sendHandlerMSG(DEAL_TEST_DVBC_SEARCH_CONTENT_CLEAR, 0, 0, tv_test_content, 2800);
                                        return;
                                }
                                sendHandlerMSG(DEAL_TEST_DVBC_SEARCH_CONTENT_CLEAR, 0, 0, tv_test_content, 200);
                                break;
                            case VALUE_TV_TEST_COMMAND_SEARCH_DVB_S_FREQUENCY:
                                //am broadcast -a com.droidlogic.tv_test.action --es tv_test_command "{"command":"dvb-s-search-byfrequency","channelFreqency":1000,"dvbs_symbolrate":27500,"dvbs_type":"DVB-S"}"
                                Log.d(TAG, VALUE_TV_TEST_COMMAND_SEARCH_DVB_S_FREQUENCY+", isActivityTop="+isActivityTop);
                                if (!isActivityTop) {
                                    startLiveTvMainAty();
                                    sendHandlerMSG(DEAL_TEST_DVBS_SEARCH_CONTENT_FREQUENCY, 0, 0, tv_test_content, DELAY_4_ACTIVITY_START);
                                    return;
                                }
                                sendHandlerMSG(DEAL_TEST_DVBS_SEARCH_CONTENT_FREQUENCY, 0, 0, tv_test_content, 200);
                                break;
                            default:
                                break;
                        }
                    }
                } catch (Exception e) {
                    Log.d(TAG, "mTvTestBroadcastReceiver Exception = " + e.getMessage());
                }
            }
        }
    };

    public void showToast(String str) {
        Toast.makeText(TvTestService.this, str, Toast.LENGTH_SHORT).show();
    }

    public interface TVTestServiceCallback {
        void doBack(int channelNum);
    }

    public void setTVTestServiceCallback(TVTestServiceCallback tvTestServiceCallback){
        this.mTVTestServiceCallback = tvTestServiceCallback;
    }

    private void startLiveTvMainAty(){
        Log.d(TAG, "### start LiveTv ###");
        ComponentName comp = new ComponentName(LIVETV_PACKAGENAME, LIVETV_CLASSNAME);
        Intent intent=new Intent();
        intent.setComponent(comp);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private String getCommandByJson(String msg){
        String command = "default";
        try{
            JSONObject obj = new JSONObject(msg);
            command = obj.optString("command");
            Log.d(TAG, "parseJsonMessage>>command="+command);
        }catch(JSONException e){
            e.printStackTrace();
        }
        return command;
    }

    private void dealATSCSearchParamsByJsonWithNumber(String msg){
        try{
            JSONObject obj = new JSONObject(msg);
            String command = obj.optString("command");
            String type = obj.optString("type");
            int channelNum = obj.optInt("channelNum");
            setSourceInput(TvControlManager.SourceInput_Type.SOURCE_TYPE_ADTV, INPUT_ID_ADTV);
            boolean setCountrySuss = setSearchCountry(ATSC_COUNTRY);
            boolean setTypeSuss = setSearchType(type, ATSC_COUNTRY);
            Log.d(TAG, "dealATSCSearchParamsByJsonWithNumber>>type="+type+",channelNum="+channelNum+
                ",setCountrySuss="+setCountrySuss+",setTypeSuss="+setTypeSuss);
            if (setCountrySuss && setTypeSuss)
                sendHandlerMSG(DEAL_TEST_CONTENT_CALLBACK, 0, 0, genATSCResponseMessage(command, channelNum), 200);
        }catch(JSONException e){
            e.printStackTrace();
        }
    }

    private void dealATSCSelectedParamsByJsonWithNumber(String msg){
        try{
            JSONObject obj = new JSONObject(msg);
            String command = obj.optString("command");
            int channelNum = obj.optInt("channelNum");
            Log.d(TAG, "dealATSCSelectedParamsByJsonWithNumber>>channelNum="+channelNum);
            sendHandlerMSG(DEAL_TEST_CONTENT_CALLBACK, 0, 0, genATSCResponseMessage(command, channelNum), 200);
        }catch(JSONException e){
            e.printStackTrace();
        }
    }

    private void dealATSCSearchParamsByJsonWithFreqency(String msg){
        try{
            JSONObject obj = new JSONObject(msg);
            String command = obj.optString("command");
            String type = obj.optString("type");
            int channelFreqency = obj.optInt("channelFreqency");
            setSourceInput(TvControlManager.SourceInput_Type.SOURCE_TYPE_ADTV, INPUT_ID_ADTV);
            boolean setCountrySuss = setSearchCountry(ATSC_COUNTRY);
            boolean setTypeSuss = setSearchType(type, ATSC_COUNTRY);
            Intent intent = new Intent();
            intent.putExtra(TvInputInfo.EXTRA_INPUT_ID, INPUT_ID_ADTV);
            OptionUiManagerT mOptionUiManagerT = new OptionUiManagerT(this, intent, true);
            int default_freq_dtv = mOptionUiManagerT.getDvbFrequencyByPd(ATSC_DTV_DEFAULT_CHANNELNUM);
            int channelNum_atv =  mOptionUiManagerT.getAtvPdByFrequency(channelFreqency);
            int channelNum_dtv = mOptionUiManagerT.getDtvDvbPdByFrequency(channelFreqency);
            int channelNum = Math.max(channelNum_atv, channelNum_dtv);
            if (channelFreqency == default_freq_dtv && default_freq_dtv != ATSC_NOT_DTV_DEFAULT_FREQUENCY) {
                channelNum = ATSC_DTV_DEFAULT_CHANNELNUM;
            }

            Log.d(TAG, "dealATSCSearchParamsByJsonWithFreqency>>type="+type+",channelFreqency="+channelFreqency+
            ",setCountrySuss="+setCountrySuss+",setTypeSuss="+setTypeSuss+",default_freq_dtv="+default_freq_dtv+
            ",channelNum_atv="+channelNum_atv+",channelNum_dtv="+channelNum_dtv+",channelNum="+channelNum);

            if (setCountrySuss && setTypeSuss)
                sendHandlerMSG(DEAL_TEST_CONTENT_CALLBACK, 0, 0, genATSCResponseMessage(command, channelNum), 200);
        }catch(JSONException e){
            e.printStackTrace();
        }
    }

    private void dealATSCSelectedParamsByJsonWithFreqency(String msg){
        try{
            JSONObject obj = new JSONObject(msg);
            String command = obj.optString("command");
            int channelFreqency = obj.optInt("channelFreqency");
            Intent intent = new Intent();
            intent.putExtra(TvInputInfo.EXTRA_INPUT_ID, INPUT_ID_ADTV);
            OptionUiManagerT mOptionUiManagerT = new OptionUiManagerT(this, intent, true);
            int default_freq_dtv = mOptionUiManagerT.getDvbFrequencyByPd(ATSC_DTV_DEFAULT_CHANNELNUM);
            int channelNum_atv =  mOptionUiManagerT.getAtvPdByFrequency(channelFreqency);
            int channelNum_dtv = mOptionUiManagerT.getDtvDvbPdByFrequency(channelFreqency);
            int channelNum = Math.max(channelNum_atv, channelNum_dtv);
            if (channelFreqency == default_freq_dtv && default_freq_dtv != ATSC_NOT_DTV_DEFAULT_FREQUENCY) {
                channelNum = ATSC_DTV_DEFAULT_CHANNELNUM;
            }

            Log.d(TAG, "dealATSCSelectedParamsByJsonWithFreqency>>channelFreqency="+channelFreqency+",default_freq_dtv="+
            default_freq_dtv+",channelNum_atv="+channelNum_atv+",channelNum_dtv="+channelNum_dtv+",channelNum="+channelNum);

            sendHandlerMSG(DEAL_TEST_CONTENT_CALLBACK, 0, 0, genATSCResponseMessage(command, channelNum), 200);
        }catch(JSONException e){
            e.printStackTrace();
        }
    }

	private void dealATSCSearchParamsStringWithFrequency(String message) {

			String [] cmd_result = message.split(",");
			String mode = null;
			String freq = null;
			String type = null;
			int channelFreqency = 0;
			if (cmd_result.length > 2) {
				Log.d(TAG, "### mode is " + cmd_result[1] );
				if (cmd_result[1].length() > 5) {
					mode = cmd_result[1].substring(5);
					Log.d(TAG, " ## signal modulation mode is " + mode);
					if (mode != null ) { /* cmd:SearchChannel_manual,mode:8VSB,frequency:63MHZ */
					if (mode.contains("VSB")) {
						type = ATSC_MODE_AIR;
					} else if (mode.contains("J83B")) {
						type = ATSC_MODE_CABLE_AUTO;
					} else {
						type = ATSC_MODE_AIR;
					}
					int freqStringLength = cmd_result[2].length();
					if (freqStringLength > 10) {
						freq = cmd_result[2].substring(10, freqStringLength-3);
						if (freq.indexOf(".") > -1) { //float data
							channelFreqency = (int) (Double.parseDouble(freq) * 1000000);
						} else {
							channelFreqency = Integer.parseInt(freq) * 1000000;
						}
						setSourceInput(TvControlManager.SourceInput_Type.SOURCE_TYPE_ADTV, INPUT_ID_ADTV);
						boolean setCountrySuss = setSearchCountry(ATSC_COUNTRY);
						boolean setTypeSuss = setSearchType(type, ATSC_COUNTRY);
						Intent intent = new Intent();
						intent.putExtra(TvInputInfo.EXTRA_INPUT_ID, INPUT_ID_ADTV);
						OptionUiManagerT mOptionUiManagerT = new OptionUiManagerT(this, intent, true);
						int default_freq_dtv = mOptionUiManagerT.getDvbFrequencyByPd(ATSC_DTV_DEFAULT_CHANNELNUM);
						int channelNum_atv =  mOptionUiManagerT.getAtvPdByFrequency(channelFreqency);
						int channelNum_dtv = mOptionUiManagerT.getDtvDvbPdByFrequency(channelFreqency);
						int channelNum = Math.max(channelNum_atv, channelNum_dtv);
						if (channelFreqency == default_freq_dtv && default_freq_dtv != ATSC_NOT_DTV_DEFAULT_FREQUENCY) {
							channelNum = ATSC_DTV_DEFAULT_CHANNELNUM;
						}

						Log.d(TAG, "dealATSCSearchParamsByJsonWithFreqency>>type="+type+",channelFreqency="+channelFreqency+
						",setCountrySuss="+setCountrySuss+",setTypeSuss="+setTypeSuss+",default_freq_dtv="+default_freq_dtv+
						",channelNum_atv="+channelNum_atv+",channelNum_dtv="+channelNum_dtv+",channelNum="+channelNum);

						setSourceInput(TvControlManager.SourceInput_Type.SOURCE_TYPE_ADTV, INPUT_ID_ADTV);
						if (setCountrySuss && setTypeSuss) {
							sendHandlerMSG(DEAL_TEST_CONTENT_CALLBACK, 0, 0, genATSCResponseMessage(VALUE_TV_TEST_COMMAND_SEARCH_ATSC_FREQUENCY, channelNum), 200);
						}
					}
				}
			}
			Log.d(TAG, " message not right: " + message);
		}
	}

	private void dealDVBTSearchParamsStringWithFrequency(String message) {

				String [] cmd_result = message.split(",");
				String mode = null;
				String freq = null;
				int transMode = 0;
				int bandwidth = 0;
				int channelFreqency = 0;
				int dvbt_type = 0;
				if (cmd_result.length > 4) {
					Log.d(TAG, "### mode is " + cmd_result[1] );
					if (cmd_result[1].length() > 5) {
						mode = cmd_result[1].substring(5);
						Log.d(TAG, " ## signal modulation mode is " + mode);
						if (mode != null ) { /* cmd:SearchChannel_manual,mode:DVBT2,frequency:474MHZ,dvbt_bandWidth:8MHZ,dvbt_transMode:8K */
							if (mode.contains("DVBT2")) { /*DVBT2 mode means DVB-T2*/
								dvbt_type = 1;
							} else {
								dvbt_type = 0;
							}
							int freqStringLength = cmd_result[2].length();
							Log.d(TAG, "--- freqStringLength:" + freqStringLength + " frequency is :" + cmd_result[2]);

							if (freqStringLength > 10) {
								freq = cmd_result[2].substring(10, freqStringLength-3);
								Log.d(TAG, "--- freq:" + freq);
								if (freq.indexOf(".") > -1) { //float data
									channelFreqency = (int) (Double.parseDouble(freq) * 1000000);
								} else {
									channelFreqency = Integer.parseInt(freq) * 1000000;
								}
								setSourceInput(TvControlManager.SourceInput_Type.SOURCE_TYPE_DTV, INPUT_ID_DTVKITSOURCE);
								if (cmd_result[3] != null) { /* get bandwidth */
									bandwidth = getDVBTBandwidthToInt(cmd_result[3]);
									if (cmd_result[4] != null) {
										transMode = getDVBTTransModeToInt(cmd_result[4]);
									}
								}
								Log.d(TAG, "dealDVBTSearchParamsStringWithFreqency>>dvbt_type="+dvbt_type+",channelFreqency="+channelFreqency+
								",transMode="+transMode+",bandwidth="+bandwidth);
								sendHandlerMSG(DEAL_TEST_CONTENT_CALLBACK, 0, 0, genDVBTSearchResponseMessage(VALUE_TV_TEST_COMMAND_SEARCH_DVB_T_FREQUENCY, channelFreqency, bandwidth, transMode, dvbt_type), 200);
							}
						}
					}
					Log.d(TAG, " message not right: " + message);
				}
		}

	private void dealDVBCSearchParamsStringWithFrequency(String message) {

					String [] cmd_result = message.split(",");
					String mode = null;
					String freq = null;
					String symbolRate = null;
					int symbolRateValue = 0;
					int modulation = 6900;
					int channelFreqency = 0;
					if (cmd_result.length > 2) {
						Log.d(TAG, "### mode is " + cmd_result[1] );
						if (cmd_result[1].length() > 5) {
							mode = cmd_result[1].substring(5);
							if (mode != null ) { /* cmd:SearchChannel_manual,mode:DVBC,frequency:113MHZ,dvbc_symbolRate:6.952,dvbc_modulation:64QAM */
								int freqStringLength = cmd_result[2].length();
								Log.d(TAG, "--- freqStringLength:" + freqStringLength + " frequency is :" + cmd_result[2]);

								if (freqStringLength > 10) {
									freq = cmd_result[2].substring(10, freqStringLength-3);
									Log.d(TAG, "--- freq:" + freq);
									if (freq.indexOf(".") > -1) { //float data
										channelFreqency = (int) (Double.parseDouble(freq) * 1000000);
									} else {
										channelFreqency = Integer.parseInt(freq) * 1000000;
									}
									if (cmd_result[3] != null) {
										symbolRate = cmd_result[3].substring(16, cmd_result[3].length());
										Log.d(TAG, "--- symbolrate: " + symbolRate);
										symbolRateValue = (int) (Double.parseDouble(symbolRate) * 1000);
									}
									if (cmd_result[4] != null) {
										Log.d(TAG, "modulation is:"+cmd_result[4]);
										modulation = getDVBCModulationToInt(cmd_result[4]);
									}

									setSourceInput(TvControlManager.SourceInput_Type.SOURCE_TYPE_MAX, INPUT_ID_DTVKITSOURCE);
									Log.d(TAG, "dealDVBCSearchParamsByJsonWithFrequency>>frequency="+channelFreqency+", symbolrate="+symbolRateValue+", modulation="+modulation);
									sendHandlerMSG(DEAL_TEST_CONTENT_CALLBACK, 0, 0, genDVBCSearchResponseMessage(VALUE_TV_TEST_COMMAND_SEARCH_DVB_C_FREQUENCY, channelFreqency, modulation, symbolRateValue), 200);
								}
							}
						}
						Log.d(TAG, " message not right: " + message);
					}

			}

	private void dealDTMBSearchParamsStringWithFrequency(String message) {
		String [] cmd_result = message.split(",");
		String mode = null;
		String freq = null;
		int channelFreqency = 0;
		if (cmd_result.length > 2) {
			Log.d(TAG, "### mode is " + cmd_result[1] );
			if (cmd_result[1].length() > 5) {
				mode = cmd_result[1].substring(5);
				if (mode != null ) { /* cmd:SearchChannel_manual,mode:DTMB,frequency:63MHZ */
					int freqStringLength = cmd_result[2].length();
					Log.d(TAG, "--- freqStringLength:" + freqStringLength + " frequency is :" + cmd_result[2]);

					if (freqStringLength > 10) {
						freq = cmd_result[2].substring(10, freqStringLength-3);
						Log.d(TAG, "--- freq:" + freq);
						if (freq.indexOf(".") > -1) { //float data
							channelFreqency = (int) (Double.parseDouble(freq) * 1000000);
						} else {
							channelFreqency = Integer.parseInt(freq) * 1000000;
						}
						setSourceInput(TvControlManager.SourceInput_Type.SOURCE_TYPE_DTV, INPUT_ID_ADTV);
						boolean setCountrySuss = setSearchCountry(DTMB_COUNTRY);
						boolean setModeSuss = setSearchMode("manual", DTMB_COUNTRY);
						boolean setTypeSuss = setSearchType("DTMB", DTMB_COUNTRY);
						boolean setDtmbPhysicalNumberSuss = setCurrentDtmbPhysicalNumber(channelFreqency);

						Log.d(TAG, "dealDTMBSearchParamsByJsonWithFrequency>>frequency="+channelFreqency+" ,setCountrySuss="+setCountrySuss+" ,setModeSuss="+
						setModeSuss+" ,setTypeSuss="+setTypeSuss+" ,setDtmbPhysicalNumberSuss="+setDtmbPhysicalNumberSuss);

						if (setCountrySuss && setModeSuss && setTypeSuss && setDtmbPhysicalNumberSuss) {
							sendHandlerMSG(DEAL_TEST_CONTENT_CALLBACK, 0, 0, genDTMBSearchResponseMessage(VALUE_TV_TEST_COMMAND_SEARCH_DTMB_FREQUENCY, channelFreqency), 200);
						}else{
							Log.e(TAG, "dealDTMBSearchParamsByJsonWithFrequency not success.");
						}
					}
				}
			}
			Log.d(TAG, " message not right: " + message);
		}
	}

	private void dealISDBSearchParamsStringWithFrequency(String message) {
		String [] cmd_result = message.split(",");
		String mode = null;
		String freq = null;
		int channelFreqency = 0;
		int transMode = 0;
		int bandWidth = 0;
		if (cmd_result.length > 2) {
			Log.d(TAG, "### mode is " + cmd_result[1] );
			if (cmd_result[1].length() > 5) {
				mode = cmd_result[1].substring(5);
				if (mode != null ) { /* cmd:SearchChannel_manual,mode:ISDB,frequency:63MHZ,isdb_bandWidth:5,isdb_transMode:1K */
					int freqStringLength = cmd_result[2].length();
					Log.d(TAG, "--- freqStringLength:" + freqStringLength + " frequency is :" + cmd_result[2]);

					if (freqStringLength > 10) {
						freq = cmd_result[2].substring(10, freqStringLength-3);
						Log.d(TAG, "--- freq:" + freq);
						if (freq.indexOf(".") > -1) { //float data
							channelFreqency = (int) (Double.parseDouble(freq) * 1000000);
						} else {
							channelFreqency = Integer.parseInt(freq) * 1000000;
						}
						setSourceInput(TvControlManager.SourceInput_Type.SOURCE_TYPE_MAX, INPUT_ID_DTVKITSOURCE);
						if (cmd_result[3] != null) { /* get bandwidth */
							bandWidth = getDVBTBandwidthToInt(cmd_result[3]);
							if (cmd_result[4] != null) {
								transMode = getDVBTTransModeToInt(cmd_result[4]);
							}
						}

						Log.d(TAG, "dealISDBSearchParamsByJsonWithFrequency>>frequency="+channelFreqency+",transMode="+transMode+",bandwidth="+bandWidth);

						sendHandlerMSG(DEAL_TEST_CONTENT_CALLBACK, 0, 0, genISDBSearchResponseMessage(VALUE_TV_TEST_COMMAND_SEARCH_ISDB_FREQUENCY, channelFreqency, bandWidth, transMode), 200);
					}
				}
			}
			Log.d(TAG, " message not right: " + message);
		}

	}

    private void dealISDBClearSearchParamsByJson(String msg) {
        try{
            JSONObject obj = new JSONObject(msg);
            String command = obj.optString("command");
            Log.d(TAG, "dealISDBClearSearchParamsByJson>>command="+command);
            setSourceInput(TvControlManager.SourceInput_Type.SOURCE_TYPE_MAX, INPUT_ID_DTVKITSOURCE);
            sendHandlerMSG(DEAL_TEST_CONTENT_CALLBACK, 0, 0, msg, 200);
        }catch(JSONException e){
            e.printStackTrace();
        }
    }

	private void dealISDBClearSearchParamsCmd(String message) {
		String [] cmd_result = message.split(",");
		if (cmd_result.length > 0) {
			Log.d(TAG, "### mode is " + cmd_result[1] );
			if (cmd_result[1].contains("ISDB")) { //clear ISDB program info
				try {
					JSONObject obj = new JSONObject();
					obj.put("command", VALUE_TV_TEST_COMMAND_SEARCH_ISDB_CLEAR);
					Log.d(TAG, "dealISDBClearSearchParamsByJson>>command="+VALUE_TV_TEST_COMMAND_SEARCH_ISDB_CLEAR);
					setSourceInput(TvControlManager.SourceInput_Type.SOURCE_TYPE_MAX, INPUT_ID_DTVKITSOURCE);
					sendHandlerMSG(DEAL_TEST_CONTENT_CALLBACK, 0, 0, obj.toString(), 200);
				} catch(JSONException e){
					e.printStackTrace();
				}
			} else if (cmd_result[1].contains("DVBC")) { //clear DVBC program info
				try {
					JSONObject obj = new JSONObject();
					obj.put("command", VALUE_TV_TEST_COMMAND_SEARCH_DVBC_CLEAR);
					Log.d(TAG, "dealDVBCClearSearchParamsByJson>>command="+VALUE_TV_TEST_COMMAND_SEARCH_DVBC_CLEAR);
					setSourceInput(TvControlManager.SourceInput_Type.SOURCE_TYPE_MAX, INPUT_ID_DTVKITSOURCE);
					sendHandlerMSG(DEAL_TEST_CONTENT_CALLBACK, 0, 0, obj.toString(), 200);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			} else {

			}
		}
	 }

	private void dealDVBSSearchParamsStringWithFrequency(String message){
		String [] cmd_result = message.split(",");
		String mode = null;
		String freq = null;
		String symbolRate = null;
		int dvbsType = 0;
		int symbolRateValue = 0;
		int channelFreqency = 0;
		if (cmd_result.length > 3) {
			Log.d(TAG, "### mode is " + cmd_result[1] );
			if (cmd_result[1].length() > 3) {
				mode = cmd_result[1].substring(5);
					if (mode != null ) { /* cmd:SearchChannel_manual,mode:DVB-S,frequency:1000MHZ,dvbs_symbolRate:24500000 */
						dvbsType = getDVBSTypeToInt(mode);
						int freqStringLength = cmd_result[2].length();
						Log.d(TAG, "--- freqStringLength:" + freqStringLength + " frequency  is :" + cmd_result[2]);

						if (freqStringLength > 10) {
							freq = cmd_result[2].substring(10, freqStringLength-3);
							Log.d(TAG, "--- freq:" + freq);
							if (freq.indexOf(".") > -1) { //float data
								channelFreqency = (int) (Double.parseDouble(freq));
							} else {
								channelFreqency = Integer.parseInt(freq);
							}
							if (cmd_result[3] != null) {
								symbolRate = cmd_result[3].substring(16, cmd_result[3].length());
								Log.d(TAG, "--- symbolrate: " + symbolRate);
								symbolRateValue = Integer.parseInt(symbolRate) / 1000;
							}

							setSourceInput(TvControlManager.SourceInput_Type.SOURCE_TYPE_MAX, INPUT_ID_DTVKITSOURCE);
							Log.d(TAG, "dealDVBSSearchParamsByJsonWithFrequency>>frequency="+channelFreqency+", symbolRate="+symbolRateValue+",dvbsType="+dvbsType);
							sendHandlerMSG(DEAL_TEST_CONTENT_CALLBACK, 0, 0, genDVBSSearchResponseMessage(VALUE_TV_TEST_COMMAND_SEARCH_DVB_S_FREQUENCY, channelFreqency, symbolRateValue, dvbsType), 200);
						}
				}
			} else {
				Log.d(TAG, " message not right: " + message);
			}
		}
    }

    private void dealDVBSSearchParamsByJsonWithFrequency(String msg){
        try{
            JSONObject obj = new JSONObject(msg);
            String command = obj.optString("command");
            int frequency = obj.optInt("channelFreqency");
            int symbolRate = obj.optInt("dvbs_symbolrate");
            setSourceInput(TvControlManager.SourceInput_Type.SOURCE_TYPE_MAX, INPUT_ID_DTVKITSOURCE);
            int dvbsType = getDVBSTypeToInt(obj.optString("dvbs_type"));
            Log.d(TAG, "dealDVBSSearchParamsByJsonWithFrequency>>frequency="+frequency+", symbolRate="+symbolRate+",dvbsType="+dvbsType);
            sendHandlerMSG(DEAL_TEST_CONTENT_CALLBACK, 0, 0, genDVBSSearchResponseMessage(command, frequency, symbolRate, dvbsType), 200);
        }catch(JSONException e){
            e.printStackTrace();
        }
    }

    private void dealDVBCClearSearchParamsByJson(String msg){
    try {
                JSONObject obj = new JSONObject(msg);
                String command = obj.optString("command");
                Log.d(TAG, "dealDVBCClearSearchParamsByJson>>command="+command);
                setSourceInput(TvControlManager.SourceInput_Type.SOURCE_TYPE_MAX, INPUT_ID_DTVKITSOURCE);
                sendHandlerMSG(DEAL_TEST_CONTENT_CALLBACK, 0, 0, msg, 200);
        } catch (JSONException e) {
                e.printStackTrace();
        }
   }

    private String genDVBSSearchResponseMessage(String command, int frequency, int symbolRate, int dvbsType){
        //{"command":"dvb-s-search-byfrequency","channelFreqency":1000,"dvbs_symbolrate":27500,"dvbs_type":"DVB-S"}
        String ret = "def_json";
        try{
            JSONObject obj = new JSONObject();
            obj.put("command",command);
            obj.put("channelFreqency",frequency);
            obj.put("dvbs_symbolrate", symbolRate);
            obj.put("dvbs_type", dvbsType);
            ret = obj.toString();
        }catch(JSONException e){
            e.printStackTrace();
        }
        return ret;
    }

    private int getDVBSTypeToInt(String dvbsType){
        int ret = 0;
        if (dvbsType.equals("DVB-S2") || dvbsType.equals("DVBS2")) {
            ret = 1;
        }else{
            ret = 0;
        }
        return ret;
    }

    private boolean isActivityTop(String cls,Context context){
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        String name = manager.getRunningTasks(1).get(0).topActivity.getClassName();
        return name.equals(cls);
    }

    private void sendHandlerMSG(int what, int arg1, int arg2, Object message, int delayMilis) {
        if (mHandler != null) {
            mHandler.removeMessages(what);
            Message mess = mHandler.obtainMessage(what, arg1, arg2, message);
            boolean info = mHandler.sendMessageDelayed(mess, delayMilis);
            Log.d(TAG, "sendHandlerMSG info= " + info+" ,msg="+mess.toString());
        }
    }

    private String genATSCResponseMessage(String command, int channelNumber){
        String ret = "def_json";
        try{
            JSONObject obj = new JSONObject();
            obj.put("command",command);
            obj.put("channelNum",channelNumber);
            ret = obj.toString();
        }catch(JSONException e){
            e.printStackTrace();
        }
        return ret;
    }

    public void setSourceInput(TvControlManager.SourceInput_Type sourceType, String inputId){
        String currentInputId = DroidLogicTvUtils.getCurrentInputId(this);
        Log.d(TAG, "setSourceInput:  currentInputId=" + currentInputId + ", inputId=" + inputId);
        if (!TextUtils.isEmpty(currentInputId) && currentInputId.equals(inputId))
            return;
        List<TvInputInfo> inputList = mTvInputManager.getTvInputList();
        for (TvInputInfo input : inputList) {
            if (inputId.equals(input.getId())) {
                Log.d(TAG, "setSourceInput:  info=" + input);
                DroidLogicTvUtils.setCurrentInputId(this, input.getId());
                if (!input.isPassthroughInput()) {
                    DroidLogicTvUtils.setSearchInputId(this, input.getId(), false);
                    if (sourceType == TvControlManager.SourceInput_Type.SOURCE_TYPE_TV) {
                        DroidLogicTvUtils.setSearchType(this, TvScanConfig.TV_SEARCH_TYPE.get(TvScanConfig.TV_SEARCH_TYPE_ATV_INDEX));
                    } else if (sourceType == TvControlManager.SourceInput_Type.SOURCE_TYPE_DTV) {
                        String country = DroidLogicTvUtils.getCountry(this);
                        ArrayList<String> dtvList = TvScanConfig.GetTvDtvSystemList(country);
                        DroidLogicTvUtils.setSearchType(this, dtvList.get(0));
                    }
                }

                Settings.System.putInt(getContentResolver(), DroidLogicTvUtils.TV_CURRENT_DEVICE_ID,
                        DroidLogicTvUtils.getHardwareDeviceId(input));
                if (INPUT_ID_DTVKITSOURCE.equals(input.getId())) {//DTVKIT SOURCE
                    Log.d(TAG, "DtvKit source");
                    mSystemControlManager.SetDtvKitSourceEnable(1);
                } else {
                    Log.d(TAG, "Not DtvKit source");
                    mSystemControlManager.SetDtvKitSourceEnable(0);
                }
                boolean isActivityTop = isActivityTop(LIVETV_CLASSNAME, this);
                if (isActivityTop) {
                    Intent intent = new Intent();
                    intent.setAction("action.startlivetv.settingui");
                    intent.putExtra("from_tv_source", true);
                    intent.putExtra(TvInputInfo.EXTRA_INPUT_ID, input.getId());
                    sendBroadcast(intent);
                } else {
                    Intent intent = new Intent(TvInputManager.ACTION_SETUP_INPUTS);
                    intent.putExtra("from_tv_source", true);
                    intent.putExtra(TvInputInfo.EXTRA_INPUT_ID, input.getId());
                    startActivity(intent);
                }
            }
        }
    }

    public boolean setSearchCountry(String curCountry) {
        Log.d(TAG, "setSearchCountry = " + curCountry);
        boolean ret = false;
        ArrayList<String> supportCountryShortNameList = TvScanConfig.GetTVSupportCountries();
        if (supportCountryShortNameList.contains(curCountry)) {
            DroidLogicTvUtils.setCountry(this, curCountry);
            mTvControlManager.SetTvCountry(curCountry);
            if (DroidLogicTvUtils.isDtvContainsAtscByCountry(curCountry)) {
                DroidLogicTvUtils.setAtvDtvModeFlag(this, DroidLogicTvUtils.TV_SEARCH_ATV_DTV);
            } else if (TvScanConfig.GetTvAtvSupport(curCountry)) {
                DroidLogicTvUtils.setAtvDtvModeFlag(this, DroidLogicTvUtils.TV_SEARCH_ATV);
            } else if (TvScanConfig.GetTvDtvSupport(curCountry)) {
                DroidLogicTvUtils.setAtvDtvModeFlag(this, DroidLogicTvUtils.TV_SEARCH_DTV);
            } else {
                Log.e(TAG, "current country:" + curCountry + " not support DTV and ATV");
            }
            DroidLogicTvUtils.setSearchOrder(this, DroidLogicTvUtils.TV_SEARCH_ORDER_LOW);
            DroidLogicTvUtils.setTvSearchTypeSys(this, TvScanConfig.TV_COLOR_SYS.get(TvScanConfig.TV_COLOR_SYS_AUTO_INDEX));
            DroidLogicTvUtils.setTvSearchSoundSys(this, TvScanConfig.TV_SOUND_SYS.get(TvScanConfig.TV_SOUND_SYS_AUTO_INDEX));
            ret = true;
        }
        return ret;
    }

    public boolean setSearchMode(String mode,String currentCountry){
        Log.d(TAG, "setSearchMode = " + mode);
        boolean ret = false;
        ArrayList<String> supportCountrySearchModeList = TvScanConfig.GetTvSearchModeList(currentCountry);
        if (supportCountrySearchModeList.contains(mode)) {
            DroidLogicTvUtils.setSearchMode(this, mode);
            ret = true;
        }
        return ret;
    }

    public boolean setSearchType(String searchType,String currentCountry){
        Log.d(TAG, "setSearchType = " + searchType);
        boolean ret = false;
        ArrayList<String> supportCountrySearchTypeList = TvScanConfig.GetTvDtvSystemList(currentCountry);
        if (supportCountrySearchTypeList.contains(searchType)) {
            DroidLogicTvUtils.setSearchType(this, searchType);
            int atvDtvMode = DroidLogicTvUtils.TV_SEARCH_ATV_DTV;
            if (!TvScanConfig.GetTvAtvSupport(DroidLogicTvUtils.getCountry(this))) {
                atvDtvMode = DroidLogicTvUtils.TV_SEARCH_DTV;
            }
            if (searchType.equals(TvScanConfig.TV_SEARCH_TYPE.get(TvScanConfig.TV_SEARCH_TYPE_ATV_INDEX))) {
                atvDtvMode = DroidLogicTvUtils.TV_SEARCH_ATV;
            } else if (searchType.equals(TvScanConfig.TV_SEARCH_TYPE.get(TvScanConfig.TV_SEARCH_TYPE_ATSC_C_AUTO_INDEX))) {
                DroidLogicTvUtils.setAtsccListMode(this, DroidLogicTvUtils.TV_ATSC_MODE_AUTO);
            } else if (searchType.equals(TvScanConfig.TV_SEARCH_TYPE.get(TvScanConfig.TV_SEARCH_TYPE_ATSC_C_HRC_INDEX))) {
                DroidLogicTvUtils.setAtsccListMode(this, DroidLogicTvUtils.TV_ATSC_MODE_HRC);
            } else if (searchType.equals(TvScanConfig.TV_SEARCH_TYPE.get(TvScanConfig.TV_SEARCH_TYPE_ATSC_C_LRC_INDEX))) {
                DroidLogicTvUtils.setAtsccListMode(this, DroidLogicTvUtils.TV_ATSC_MODE_LRC);
            } else if (searchType.equals(TvScanConfig.TV_SEARCH_TYPE.get(TvScanConfig.TV_SEARCH_TYPE_ATSC_C_STD_INDEX))) {
                DroidLogicTvUtils.setAtsccListMode(this, DroidLogicTvUtils.TV_ATSC_MODE_STANDARD);
            } else if (searchType.equals(TvScanConfig.TV_SEARCH_TYPE.get(TvScanConfig.TV_SEARCH_TYPE_ATSC_T_INDEX))){

            } else{
                atvDtvMode = DroidLogicTvUtils.TV_SEARCH_DTV;
            }

            Log.d(TAG, "setSearchType = " + searchType +", atvDtvMode="+atvDtvMode);
            DroidLogicTvUtils.setAtvDtvModeFlag(this, atvDtvMode);

            if (DroidLogicTvUtils.DTV_SEARCH_TYPE_LIST.containsKey(searchType)) {
                String dtvType = DroidLogicTvUtils.DTV_SEARCH_TYPE_LIST.get(searchType);
                DroidLogicTvUtils.setDtvType(this, dtvType);
            }else{
                Log.e(TAG, "not find search type:" + searchType + " in support list");
                ret = false;
                return ret;
            }
            ret = true;
        }
        return ret;
    }

    private boolean getCurrentSignalInfo(){
        boolean ret = false;
        TvInSignalInfo info = mTvControlManager.GetCurrentSignalInfo();
        TvInSignalInfo.SignalStatus sigStatus= info.sigStatus;
        if (info.sigStatus == TvInSignalInfo.SignalStatus.TVIN_SIG_STATUS_STABLE) {
            ret = true;
        }
        Log.d(TAG, "getCurrentSignalInfo = " + sigStatus +", ret="+ret);
        return ret;
    }

	private void rebootPlatform() {
		if (mPowerManager != null)
			mPowerManager.reboot("");
	}

	private boolean muteRemote() {
		if (null != mSystemControlManager) {
			return mSystemControlManager.writeSysFs(REMOTE_PROTOCOL, "5");
		} else {
			return false;
		}
	}

	private boolean unmuteRemote() {
		if (null != mSystemControlManager) {
			return mSystemControlManager.writeSysFs(REMOTE_PROTOCOL, "1");
		} else {
			return false;
		}
	}

	private boolean isSignalSmooth() {

		//String value = null;
		//int curDropCount = 0;
		if (!isSignalSatableJustifiedByVdecStatus())
			return false;
		TvControlManager.BasicVdecStatusInfo info = null;
		if (null != mTvControlManager) {
			info = mTvControlManager.getBasicVdecSTatusInfo(0);
			Log.d(TAG, "decode_time_cost:" +info.decode_time_cost);
			Log.d(TAG, "error_count:" +info.error_count);
			Log.d(TAG, "frame_count:" +info.frame_count);
			Log.d(TAG, "error_frame_count:" +info.error_frame_count);
			Log.d(TAG, "drop_frame_count:" +info.drop_frame_count);
			if (info.frame_count == 0 )
				return false;
			Log.d(TAG,"lastDropCount is " +lastDropCount );
			if ((info.drop_frame_count > lastDropCount && info.frame_rate <= 30) ||
				(info.drop_frame_count > lastDropCount+1 && info.frame_rate > 30)) {
				lastDropCount = info.drop_frame_count;
				return false;
			}
			lastDropCount = info.drop_frame_count;

		/*
		if (null != mSystemControlManager) {
			value = mSystemControlManager.readSysFs(DROP_FRAME_COUNT_NODE);
			Log.d(TAG, "### vdec status is " + value);
			if (null != value) {
				int start = value.indexOf("drop count");
				int end = value.indexOf("fra err count");
				String number =  value.substring(start+13, end);
				Log.d(TAG, "number is " + number);
				if (null != number) {
					curDropCount = Integer.parseInt(number);
					if (curDropCount > lastDropCount) {
						lastDropCount = curDropCount;
						return false;
					} else {
						lastDropCount = curDropCount;
					}
				}

			}
		}
		*/
		return true;

		}
		return false;
}

/*
	private boolean isSignalSatableJustifiedByBitrate() {
		String value = null;
		int bitrate = 0;
		if (null != mSystemControlManager) {
			value = mSystemControlManager.readSysFs(DROP_FRAME_COUNT_NODE);
			Log.d(TAG, "### vdec status is " + value);
			if (null != value) {
				int start = value.indexOf("bit rate");
				int end = value.indexOf("status");
				if (start > 0 && end > 0) {
					String bitString =  value.substring(start+11, end);
					int pbsIndex = bitString.indexOf("pbs");
					Log.d(TAG, "bitrate is" + bitString);
					String number = bitString.substring(0, pbsIndex-1);
					if (null != number) {
						bitrate = Integer.parseInt(number);
						Log.d(TAG, "bitrate is#:" + bitrate);
						if (bitrate > 1) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}
*/

	private boolean isSignalSatableJustifiedByVdecStatus() {
		TvControlManager.BasicVdecStatusInfo info = null;
		if (null != mTvControlManager) {
			info = mTvControlManager.getBasicVdecSTatusInfo(0);
			Log.d(TAG, "frame_count:" +info.frame_count);
			if (info.frame_count > lastFrameCount) {
				lastFrameCount = info.frame_count;
				return true;
			}
			lastFrameCount = info.frame_count;
		}
		return false;
	}

    private int getDVBTBandwidthToInt(String bandWidth){
        int ret = 0;
        if (bandWidth.contains(":6")) { // 6 MHz
            ret = 1;
        } else if (bandWidth.contains(":7")) { // 7 MHz
            ret = 2;
        } else if (bandWidth.contains(":8")) { // 8 MHz
            ret = 3;
        } else if (bandWidth.contains(":10")) { // 10 MHz
            ret = 4;
        } else {
            ret = 0;
        }
        return ret;
    }

    private int getDVBTTransModeToInt(String transMode){// Now our solution can get FFT mode automatically
        int ret = 0;
        if (transMode.contains("32K") || transMode.contains("32E")) { // 32KE support
            ret = 5;
            return ret;
        } else if (transMode.contains("16K") || transMode.contains("16E")) { // 16KE support
            ret = 4;
            return ret;
        } else if (transMode.contains("8K") || transMode.contains("8E")) { // 8KE support
            ret = 3;
            return ret;
        } else if (transMode.contains("4K")) {
            ret = 2;
            return ret;
        } else if (transMode.contains("2K")) {
            ret = 1;
            return ret;
        } else {
            ret = 0;
            return ret;
        }
    }

    private int getDVBTTypeToInt(String dvbtType){
        int ret = 0;
        if (dvbtType.contains("DVB-T2")) {
            ret = 1;
        }else{
            ret = 0;
        }
        return ret;
    }

    private String genDVBTSearchResponseMessage(String command, int frequency, int bandWidth, int transMode, int dvbt_type){
        //{"command":"dvb-t-search-byfrequency","channelFreqency":85000000,"dvbt_bandWidth":0,"dvbt_transMode":0,"dvbt_type":0}
        String ret = "def_json";
        try{
            JSONObject obj = new JSONObject();
            obj.put("command",command);
            obj.put("channelFreqency",frequency);
            obj.put("dvbt_bandWidth", bandWidth);
            obj.put("dvbt_transMode", transMode);
            obj.put("dvbt_type", dvbt_type);
            ret = obj.toString();
        }catch(JSONException e){
            e.printStackTrace();
        }
        return ret;
    }

    private void dealDVBTSearchParamsByJsonWithNumber(String msg){
        try{
            JSONObject obj = new JSONObject(msg);
            String command = obj.optString("command");
            int frequency = obj.optInt("channelFreqency");
            setSourceInput(TvControlManager.SourceInput_Type.SOURCE_TYPE_DTV, INPUT_ID_DTVKITSOURCE);
            int bandWidth = getDVBTBandwidthToInt(obj.optString("dvbt_bandWidth"));
            int transMode = getDVBTTransModeToInt(obj.optString("dvbt_transMode"));
            int dvbt_type = getDVBTTypeToInt(obj.optString("dvbt_type"));
            Log.d(TAG, "dealDVBTSearchParamsByJsonWithNumber>>frequency="+frequency+", bandWidth="+bandWidth+",transMode="+transMode+",dvbt_type="+dvbt_type);
            sendHandlerMSG(DEAL_TEST_CONTENT_CALLBACK, 0, 0, genDVBTSearchResponseMessage(command, frequency, bandWidth, transMode, dvbt_type), 200);
        }catch(JSONException e){
            e.printStackTrace();
        }
    }

	private void dealDVBCSearchParamsByJsonWithFrequency(String msg){
			try{
				JSONObject obj = new JSONObject(msg);
				String command = obj.optString("command");
				int frequency = obj.optInt("channelFreqency");
				setSourceInput(TvControlManager.SourceInput_Type.SOURCE_TYPE_MAX, INPUT_ID_DTVKITSOURCE);
				int modulation = getDVBCModulationToInt(obj.optString("dvbc_modulation"));
				int symbolRate = obj.optInt("dvbc_symbolrate");
				Log.d(TAG, "dealDVBCSearchParamsByJsonWithFrequency>>frequency="+frequency+",modulation="+modulation+",symbolRate="+symbolRate);
				if (symbolRate>0 && symbolRate <= 10000)
					sendHandlerMSG(DEAL_TEST_CONTENT_CALLBACK, 0, 0, genDVBCSearchResponseMessage(command, frequency, modulation, symbolRate), 200);
			}catch(JSONException e){
				e.printStackTrace();
			}
	}

	private int getDVBCModulationToInt(String modulation) {
			int ret = 0;
			if (modulation.contains("16QAM")) {
				ret = 0;
			} else if (modulation.contains("32QAM")) {
				ret = 1;
			} else if (modulation.contains("64QAM")) {
				ret = 2;
			} else if (modulation.contains("128QAM")) {
				ret = 3;
			}  else if (modulation.contains("256QAM")) {
				ret =4;
			} else {
				ret = 5;
			}
			return ret;
	}

	private String genDVBCSearchResponseMessage(String command, int frequency, int modulation, int symbolRate) {
			String ret = "def_json";
			try{
				 JSONObject obj = new JSONObject();
				 obj.put("command",command);
				 obj.put("channelFreqency",frequency);
				 obj.put("dvbc_modulation",modulation);
				 obj.put("dvbc_symbolrate",symbolRate);
				 ret = obj.toString();
			}catch(JSONException e){
				 e.printStackTrace();
			}
			return ret;
	}

    private void dealDTMBSearchParamsByJsonWithFrequency(String msg){
        try{
            JSONObject obj = new JSONObject(msg);
            String command = obj.optString("command");
            int frequency = obj.optInt("channelFreqency");
            setSourceInput(TvControlManager.SourceInput_Type.SOURCE_TYPE_DTV, INPUT_ID_ADTV);
            boolean setCountrySuss = setSearchCountry(DTMB_COUNTRY);
            boolean setModeSuss = setSearchMode("manual", DTMB_COUNTRY);
            boolean setTypeSuss = setSearchType("DTMB", DTMB_COUNTRY);
            boolean setDtmbPhysicalNumberSuss = setCurrentDtmbPhysicalNumber(frequency);

            Log.d(TAG, "dealDTMBSearchParamsByJsonWithFrequency>>frequency="+frequency+" ,setCountrySuss="+setCountrySuss+" ,setModeSuss="+
                setModeSuss+" ,setTypeSuss="+setTypeSuss+" ,setDtmbPhysicalNumberSuss="+setDtmbPhysicalNumberSuss);

            if (setCountrySuss && setModeSuss && setTypeSuss && setDtmbPhysicalNumberSuss) {
                sendHandlerMSG(DEAL_TEST_CONTENT_CALLBACK, 0, 0, msg, 200);
            }else{
                Log.e(TAG, "dealDTMBSearchParamsByJsonWithFrequency not success.");
            }
        }catch(JSONException e){
            e.printStackTrace();
        }
    }

	 private String genDTMBSearchResponseMessage(String command, int frequency) {
			String ret = "def_json";
			try{
				JSONObject obj = new JSONObject();
				obj.put("command",command);
				obj.put("channelFreqency",frequency);
				ret = obj.toString();
			}catch(JSONException e){
				e.printStackTrace();
			}
			return ret;
	 }

    private boolean setCurrentDtmbPhysicalNumber(int frequency) {
        boolean ret = false;
        TvControlManager.TvMode mode = new TvControlManager.TvMode(TvContract.Channels.TYPE_DTMB);
        mode.setList(0 + DTV_TO_ATV);
        ArrayList<FreqList> m_fList = mTvControlManager.DTVGetScanFreqList(mode.getMode());
        if (m_fList != null && m_fList.size() > 0) {
            Iterator it = m_fList.iterator();
            while (it.hasNext()) {
                FreqList tempFreqList = (FreqList)(it.next());
                String displayName = tempFreqList.physicalNumDisplayName;
                int channelNum = tempFreqList.channelNum;
                int channelFreqency = tempFreqList.freq;
                Log.d(TAG, "setCurrentDtmbPhysicalNumber preFrequency= "+frequency +" ,displayName="+displayName+" ,channelNum="+channelNum+" ,channelFreqency="+channelFreqency);
                if (frequency == channelFreqency) {
                    DroidLogicTvUtils.setCurrentDtmbPhysicalNumber(this, channelNum-1);
                    ret = true;
                    break;
                }
            }
        }
        return ret;
    }


	private void dealISDBSearchParamsByJsonWithFrequency(String msg){
		try{
			JSONObject obj = new JSONObject(msg);
			String command = obj.optString("command");
			int frequency = obj.optInt("channelFreqency");
			setSourceInput(TvControlManager.SourceInput_Type.SOURCE_TYPE_MAX, INPUT_ID_DTVKITSOURCE);
			int bandWidth = getDVBTBandwidthToInt(obj.optString("isdb_bandWidth"));
			int transMode = getDVBTTransModeToInt(obj.optString("isdb_transMode"));
			Log.d(TAG, "dealISDBSearchParamsByJsonWithFrequency>>frequency="+frequency+", bandWidth="+bandWidth+",transMode="+transMode);
			sendHandlerMSG(DEAL_TEST_CONTENT_CALLBACK, 0, 0, genISDBSearchResponseMessage(command, frequency, bandWidth, transMode), 200);
		}catch(JSONException e){
				 e.printStackTrace();
		}
	}

	private String genISDBSearchResponseMessage(String command, int frequency, int bandWidth, int transMode){
		//{"command":"isdb-search-byfrequency","channelFreqency":85000000,"isdb_bandWidth":0,"isdb_transMode":0}
		String ret = "def_json";
		try{
			JSONObject obj = new JSONObject();
			obj.put("command",command);
			obj.put("channelFreqency",frequency);
			obj.put("isdb_bandWidth", bandWidth);
			obj.put("isdb_transMode", transMode);
			ret = obj.toString();
		}catch(JSONException e){
			e.printStackTrace();
		}
		return ret;
	 }


    /**
    *SIG_NOTIFY_MSG_SIG_LOST = 0, SIG_NOTIFY_MSG_SIG_NORMAL = 1, SIG_NOTIFY_MSG_PARENT_LOCKED = 2
    * SIG_NOTIFY_MSG_PARENT_UNLOCKED = 3, SIG_NOTIFY_MSG_SCRAMBLE = 4, SIG_NOTIFY_MSG_REC_CHANNEL_LOCKED = 5, SIG_NOTIFY_MSG_UNSUPPORT = 6
    */
    public void getDVBSignalStatusMsgT(int sigStatus) {
        Log.i(TAG, "=====getDVBSignalStatusMsgT sigStatus="+sigStatus);
        mDVBSigStatus = sigStatus;
    }

    public int getCurrentDVBSignalStatusMsg(){
        return mDVBSigStatus;
    }


    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        Log.i(TAG, "=====onDestroy");
        pool.shutdownNow();
        unregisterReceiver(mTvTestBroadcastReceiver);
    }

	class socketCallable implements Callable<String> {

		static final String ack = "ACK: ";
		static final String result = "RET:";
		String host = null;
		static final int port = 9527;
		Socket sendbackSocket = null;
		private Socket mSocket = null;
		public socketCallable(Socket s) {
			this.mSocket = s;
		}

		/*
		private void sendbackMessage(Socket socket, String msg, boolean isCloseWriter) {

			Log.d(TAG, "-- do send back msg --");
			try {
				 Writer writer = new OutputStreamWriter(socket.getOutputStream());
				 writer.write(msg);
				 writer.flush();
				 if (isCloseWriter) {
					writer.close();
				 }
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println("---------- write error2 -----------");
			}
		} */


		private void sendbackMessage(Writer     writer, String msg, boolean isCloseWriter) {

			Log.d(TAG, "-- do send back msg --");
			try {
				 writer.write(msg);
				 //writer.flush();
				 if (isCloseWriter) {
					writer.flush();
					writer.close();
				 }
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.d(TAG, "---------- write error2 -----------");
			}
		}


		@Override
		public String call() throws Exception {
			// TODO Auto-generated method stub

			BufferedReader bf = null;
			InputStream in = null;
			StringBuilder builder = new StringBuilder();
			Writer writer = null;
			InetAddress addr =  mSocket.getInetAddress();
			if (addr != null) {
				host = addr.toString();
				Log.d(TAG, "### client ip is  "+host);
			}
			else {
				Log.d(TAG, "### client ip null, set default one "+host);
				host = "10.28.6.152";
			}

			//sendbackSocket = new Socket(host, port);

			try {
				writer = new OutputStreamWriter(mSocket.getOutputStream());
				Log.d(TAG,"#### reading data ....");
				in = mSocket.getInputStream();
				bf = new BufferedReader(new InputStreamReader(in,"utf-8"));
				String line="";
				while (!mSocket.isClosed() && (line = bf.readLine()) != null) {// 读取数据
					builder.append(line);
					line=null;
					Log.d(TAG,"* waiting news *");
					Log.d(TAG, "get msg：" + builder);
					String message = builder.toString();
					System.out.println("-- send back msg0 --");
					sendbackMessage(writer, ack+message, false);
					if (message.startsWith("cmd:SearchChannel_manual"))
					{
						boolean isActivityTop = isActivityTop(LIVETV_CLASSNAME, mContext);
						if (!isActivityTop) {
							Log.d(TAG, "### star Live TvMainActivity first ###");
							startLiveTvMainAty();
							Thread.sleep(DELAY_4_ACTIVITY_START); /*  Wait the starting up of LiveTV */
						}
						Log.d(TAG," ### Send cmd to LiveTV ###");
						if (message.contains("VSB") || message.contains("J83B")) { /*ATSC scan*/
							dealATSCSearchParamsStringWithFrequency(message);
						} else if (message.contains("DVBT")) { /*DVB-T scan*/
							dealDVBTSearchParamsStringWithFrequency(message);
						} else if (message.contains("DVBS")) {
							dealDVBSSearchParamsStringWithFrequency(message);
						} else if (message.contains("ISDBT")) {
							dealISDBSearchParamsStringWithFrequency(message);
						} else if (message.contains("DVBC")) {
							dealDVBCSearchParamsStringWithFrequency(message);
						} else if (message.contains("DTMB")) {
							dealDTMBSearchParamsStringWithFrequency(message);
						}
						lastDropCount = 0;
						lastFrameCount = 0;
						Log.d(TAG, "-- send back msg1 --");
						sendbackMessage(writer, result + "true", true);
						break;
					} else if (message.startsWith("cmd:isSignalStable")) {
						//signalState = isSignalSatableJustifiedByBitrate();
						signalState =  isSignalSatableJustifiedByVdecStatus();
						Log.d(TAG, " signal state from vdec status is " + signalState);
						Log.d(TAG, "-- send back msg2 --");
						sendbackMessage(writer, result + signalState, true);
						break;
					} else if (message.startsWith("cmd:isSignalSmooth")) {
						boolean smoothFlag = isSignalSmooth();
						Log.d(TAG, " smooth state is " + smoothFlag);
						Log.d(TAG, "-- send back msg3 --");
						sendbackMessage(writer, result + smoothFlag, true);
					} else if (message.startsWith("cmd:BoardReset")) {
						Log.d(TAG, "-- rebooting --");
						sendbackMessage(writer, result + "true", true);
						rebootPlatform();
					}  else if (message.startsWith("cmd:ClearAllChannel")) {
						dealISDBClearSearchParamsCmd(message);
						sendbackMessage(writer, result + "true", true);
					}  else if (message.startsWith("cmd:MuteRemote")) {
						boolean ret = muteRemote();
						sendbackMessage(writer, result + ret, true);
					}  else {
						Log.d(TAG, " unprocessed info ");
					}
				 }
			}catch (IOException e) {
			   System.out.println("* IO ERROR *");
			   e.printStackTrace();
			}  finally {
				try {
					System.out.println("* close socket in server end *");
					mSocket.close();
					in.close();
					bf.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return builder.toString();
		}
	}


	private class SocketCommunicationTask extends AsyncTask<String, String, String> {
        //onPreExecute方法用于在执行后台任务前做一些UI操作
        @Override
        protected void onPreExecute() {
            Log.d(TAG, "onPreExecute() called ");
        }

        //doInBackground方法内部执行后台任务,不可在此方法内修改UI
        @Override
        protected String doInBackground(String... params) {
		Log.i(TAG, "doInBackground(Params... params) called");
		//startSocketThreadPool();
		Log.d(TAG, " ## server created ##");

		try {
			server = new ServerSocket(port);
			while (true) {
				Log.d(TAG, "----------- server wait client -----------");
				socket = server.accept();//阻塞方法，一直等到有个socket客户端连过来
				Log.d(TAG, "----------- server start thread -----------");
				Callable<String> callable = new socketCallable(socket);
				pool.submit(callable);
				publishProgress("### submit the message read thread ###");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				Log.d(TAG, "---- close server ----");
				server.close();
			} catch (IOException e) {
				Log.d(TAG, "---- server error ----");
				e.printStackTrace();
			}
		}
		return null;
        }

        //onProgressUpdate方法用于更新进度信息
        @Override
        protected void onProgressUpdate(String... progresses) {
            Log.i(TAG, "onProgressUpdate(Progress... progresses) called");
			Log.d(TAG, "submit thread end");
        }

        //onPostExecute方法用于在执行完后台任务后更新UI,显示结果
        @Override
        protected void onPostExecute(String result) {
            Log.i(TAG, "onPostExecute(Result result) called");

        }

        //onCancelled方法用于在取消执行中的任务时更改UI
        @Override
        protected void onCancelled() {
		Log.i(TAG, "onCancelled() called");
		pool.shutdownNow();
        }
    }

}
