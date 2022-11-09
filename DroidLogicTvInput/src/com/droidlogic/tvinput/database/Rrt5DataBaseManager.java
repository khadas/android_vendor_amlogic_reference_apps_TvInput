package com.droidlogic.tvinput.database;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
import com.droidlogic.app.tv.RrtEvent;

import java.util.*;

public class Rrt5DataBaseManager {
    public static final String TAG = "Rrt5DataBaseManager";
    public static final boolean DEBUG = false;

    //rrt5 provider key define
    public static final String _ID = "_id";
    public static final String COLUMN_ORIGINAL_NETWORK_ID = "original_network_id";
    public static final String VERSION_NUM = "version_num";
    public static final String DIMENSION_NUM = "dimension_num";
    public static final String REGION5_NAME = "region5_name";
    public static final String DIMENSION_NAME = "dimension_name";
    public static final String VALUES_DEFINED = "values_defined";
    public static final String LEVEL_RATING_TEXT = "level_rating_text";
    public final String[] PROJECTION = {_ID,
            COLUMN_ORIGINAL_NETWORK_ID,
            VERSION_NUM,
            DIMENSION_NUM,
            REGION5_NAME,
            DIMENSION_NAME,
            VALUES_DEFINED,
            LEVEL_RATING_TEXT};

    public static final String[] ALL_DIMENSIONS = {"Entire Audience", "Dialogue", "Language", "Sex", "Violence", "Children", "Fantasy Violence", "MPAA"};
    public static final String[] ENTIRE_AUDIENCE = {"None", "TV-G", "TV-PG", "TV-14", "TV-MA"};
    public static final String[] DIALOG = {"D"};
    public static final String[] LANGUAGE = {"L"};
    public static final String[] SEX = {"S"};
    public static final String[] VIOLENCE = {"V"};
    public static final String[] CHILDREN = {"TV-Y", "TV-Y7"};
    public static final String[] FANTASY_VIOLENCE = {"FV"};
    public static final String[] MPAA = {"N/A", "G", "PG", "PG-13", "R", "NC-17", "X", "NR"};
    public static final LinkedHashMap<Integer, String> DEFAULT_DIMENSIONS = new LinkedHashMap<Integer, String>();
    public static final Map<String, Integer> DEFAULT_DIMENSIONS_GRADUATED_SCALE = new HashMap<String, Integer>();
    public static final Map<String, String[]> DEFAULT_DIMENSION_VALUES = new HashMap<String, String[]>();
    public static final ArrayList<RrtEvent> DEFAULT_RRT5_EVENTS = new ArrayList<RrtEvent>();

    static {
        for (int i = 0; i < ALL_DIMENSIONS.length; i++) {
            DEFAULT_DIMENSIONS.put(i, ALL_DIMENSIONS[i]);
            DEFAULT_DIMENSIONS_GRADUATED_SCALE.put(ALL_DIMENSIONS[i], 1);
        }
        DEFAULT_DIMENSION_VALUES.put(ALL_DIMENSIONS[0], ENTIRE_AUDIENCE);
        DEFAULT_DIMENSION_VALUES.put(ALL_DIMENSIONS[1], DIALOG);
        DEFAULT_DIMENSION_VALUES.put(ALL_DIMENSIONS[2], LANGUAGE);
        DEFAULT_DIMENSION_VALUES.put(ALL_DIMENSIONS[3], SEX);
        DEFAULT_DIMENSION_VALUES.put(ALL_DIMENSIONS[4], VIOLENCE);
        DEFAULT_DIMENSION_VALUES.put(ALL_DIMENSIONS[5], CHILDREN);
        DEFAULT_DIMENSION_VALUES.put(ALL_DIMENSIONS[6], FANTASY_VIOLENCE);
        DEFAULT_DIMENSION_VALUES.put(ALL_DIMENSIONS[7], MPAA);

        for (String dimension : ALL_DIMENSIONS) {
            String[] dimensionvalues = DEFAULT_DIMENSION_VALUES.get(dimension);
            for (String temp : dimensionvalues) {
                RrtEvent event = new RrtEvent();
                event.tableId = 0;
                event.ratingRegion = 5;
                event.versionNumber = 0;
                event.ratingRegionName = "U.S. (50 states + possessions)";
                event.dimensionDefined = ALL_DIMENSIONS.length;
                event.dimensionName = dimension;
                event.valuesDefined = dimensionvalues.length;
                event.abbrevRatingValue = temp;
                event.ratingValue = temp;
                DEFAULT_RRT5_EVENTS.add(event);
                if (DEBUG) Log.d(TAG, "static add default  dimension = " + dimension + ", values = " + temp);
            }
        }
    }

    private Context mContext;
    private ContentResolver mContentResolver;
    private boolean mUpgrading = false;
    protected final Object mLock = new Object();
    protected final Object mProcessLock = new Object();
    private int mOriginalNetworkId = -1;
    private int mVersionNumber = -1;
    private int mDimensionNumber = -1;
    private String mRegionName = null;
    private LinkedHashMap<Integer, String> mDimensionNameValues = new LinkedHashMap<Integer, String>();
    // 1 means level become more and more higher, 0 means the same level
    private Map<String, Integer> mDimensionGraduatedScale = new HashMap<String, Integer>();
    private Map<String, String[]> mDimensionValues = new HashMap<String, String[]>();

    //receive rrtEvent
    private ArrayList<RrtEvent> mRrtEvent = new ArrayList<RrtEvent>();
    private String mReceiveDimensionName = null;
    private int mReceiveDimensionTotal = -1;
    private int mReceiveDimensionIndex = -1;
    private int mReceiveDimensionValueIndex = -1;
    private int mReceiveDimensionValueNum = -1;

    private String mAuthority = "com.droidlogic.database";
    private String mTableName = "tv_rrt";
    private String mDomain = "com.android.tv";
    private Uri mUri = null;

    //handle meassage
    public final static int RESET_UPDATE = 0;

    Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case RESET_UPDATE:
                    resetUpdateRrtWhenTimeout();
                 default:
                    break;
            }
        }
    };

    public Rrt5DataBaseManager(Context context) {
        mContext = context;
        mUri = Uri.parse("content://" + mAuthority + "/" + mTableName);
        mContentResolver = mContext.getContentResolver();
        if (SystemProperties.getBoolean("sys.debug.rrt5default", false)) {
            setDefaultRrt();
        }
        updateDimensionValues();
    }

    public Rrt5DataBaseManager(Context context, String domain, String authority, String table) {
        if (!TextUtils.isEmpty(authority))
            mAuthority = authority;
        if (!TextUtils.isEmpty(table))
            mTableName = table;
        if (!TextUtils.isEmpty(domain))
            mDomain = domain;
        mContext = context;
        mUri = Uri.parse("content://" + mAuthority + "/" + mTableName);
        mContentResolver = mContext.getContentResolver();
        if (SystemProperties.getBoolean("sys.debug.rrt5default", false)) {
            setDefaultRrt();
        }
        updateDimensionValues();
    }

    public String getRrt5Domain() {
        return mDomain;
    }

    public void releasResource() {
        preventUpdateTimeout(false);
        resetUpdateRrtWhenTimeout();
    }

    public static boolean isTestingRrt() {
        return SystemProperties.getBoolean("sys.debug.rrt5test", false);
    }

    //return the first is dimensionname, the second is rating text
    public String[] getRrt5Rating(int dimension, int value) {
        //Log.d(TAG, "getRrt5Rating");
        final String[] result = new String[3];
        synchronized (mLock) {
            if (DEBUG) Log.d(TAG, "getRrt5Rating dimension = " + dimension + ", value = " + value + ", dimension = " + mDimensionNameValues.size() + ", value = " + (mDimensionNameValues.get(dimension) != null ? mDimensionValues.get(mDimensionNameValues.get(dimension)).length : 0));
            if (mDimensionNameValues.size() == 0 || mDimensionValues.size() == 0 || dimension >= mDimensionNameValues.size() ||
                    mDimensionNameValues.get(dimension) == null || (value > mDimensionValues.get(mDimensionNameValues.get(dimension)).length)) {
                result[0] = null;
                result[1] = null;
                result[2] = null;
                if (DEBUG) Log.e(TAG, "getRrt5Rating null");
                return result;
            }
            result[0] = mRegionName;
            result[1] = mDimensionNameValues.get(dimension);
            result[2] = (mDimensionValues.get(result[1]))[value - 1];//value minimum is 1
        }
        if (DEBUG) Log.d(TAG, "getRrt5Rating regionname = " + result[0] + ", dimension = " + result[1] + ", value = " + result[2]);
        return result;
    }

    private void updateDimensionValues() {
        if (DEBUG)  Log.d(TAG, "updateDimensionValues");
        LinkedHashMap<Integer, String> allTempDimensionNameValues = new LinkedHashMap<Integer, String>();
        Map<String, String[]> allTempDimensionValue = new HashMap<String, String[]>();
        int originalNetworkId = -1;
        int versionnumber = -1;
        int dimensionnumber = -1;
        String region5name = null;
        String singleDimensionName = null;
        String[] singleDimensionValues = null;
        int singleDimensionNameKey = -1;
        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(mUri, PROJECTION, null, null, null);
            while (cursor != null && cursor.moveToNext()) {
                int id = getIntFromCur(cursor, _ID);
                if (id < 1) {
                    Log.e(TAG, "[updateDimensionValues] no data");
                    continue;
                }
                if (originalNetworkId == -1)
                    originalNetworkId = getIntFromCur(cursor, COLUMN_ORIGINAL_NETWORK_ID);
                if (versionnumber == -1)
                    versionnumber = getIntFromCur(cursor, VERSION_NUM);
                if (dimensionnumber == -1)
                    dimensionnumber = getIntFromCur(cursor, DIMENSION_NUM);
                if (region5name == null)
                    region5name = getStringFromCur(cursor, REGION5_NAME);

                final int initValueDefined = getIntFromCur(cursor, VALUES_DEFINED);
                final String initDimensionName = getStringFromCur(cursor, DIMENSION_NAME);
                String[] initDimensionValues = null;
                if (initValueDefined > 0) {
                    singleDimensionNameKey++;
                    initDimensionValues = new String[initValueDefined];
                    allTempDimensionNameValues.put(singleDimensionNameKey, initDimensionName);
                    initDimensionValues[0] = getStringFromCur(cursor, LEVEL_RATING_TEXT);
                    //Log.d(TAG, "updateDimensionValues add key [" + singleDimensionNameKey + "]");
                } else {
                    Log.e(TAG, "[updateDimensionValues] valuedefined erro");
                    break;
                }
                for (int temp = 1; temp < initValueDefined; temp++) {
                    if (cursor != null && cursor.moveToNext()) {
                        initDimensionValues[temp] = getStringFromCur(cursor, LEVEL_RATING_TEXT);
                    } else {
                        Log.e(TAG, "[updateDimensionValues] get level text erro temp = " + temp);
                        break;
                    }
                }
                allTempDimensionValue.put(initDimensionName, initDimensionValues);
                Log.d(TAG, "[updateDimensionValues] dimension = " + initDimensionName + Arrays.toString(initDimensionValues));
            }
        } catch (Exception e) {
            Log.e(TAG, "updateDimensionValues Exception: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        synchronized(mLock) {
            mOriginalNetworkId = originalNetworkId;
            mVersionNumber = versionnumber;
            mDimensionNumber = dimensionnumber;
            mRegionName = region5name;
            mDimensionNameValues = allTempDimensionNameValues;
            mDimensionValues = allTempDimensionValue;
            printDimensionValues();
        }
    }

    private int getIntFromCur(Cursor cursor, String key) {
        int value = -1;
        int index = cursor.getColumnIndex(key);
        if (index > -1) {
            value = cursor.getInt(index);
        }
        return value;
    }

    private String getStringFromCur(Cursor cursor, String key) {
        String value = null;
        int index = cursor.getColumnIndex(key);
        if (index > -1) {
            value = cursor.getString(index);
        }
        return value;
    }

    public void SynchronizedUpdateRrt(RrtEvent rrtEvent) {
        synchronized (mProcessLock) {
            updateRrt5Event(rrtEvent);
            preventUpdateTimeout(true);
        }
    }

    public boolean isUpdateFinished() {
        return !mUpgrading;
    }

    private void updateRrt5Event(RrtEvent rrtEvent) {
        //Log.d(TAG, "updateRrt5Event " + (rrtEvent != null ?  (rrtEvent.dimensionName + ":" + rrtEvent.abbrevRatingValue) : null));
        if (!mUpgrading) {
            startUpdateRrt();
        }
        if (mReceiveDimensionTotal == -1) {
            mReceiveDimensionTotal = rrtEvent.dimensionDefined;
        }
        if (!TextUtils.equals(mReceiveDimensionName, rrtEvent.dimensionName)) {
            mReceiveDimensionName = rrtEvent.dimensionName;
            mReceiveDimensionIndex++;
            mReceiveDimensionValueIndex = -1;
            mReceiveDimensionValueNum = rrtEvent.valuesDefined;
        }
        if (mReceiveDimensionValueIndex < (rrtEvent.valuesDefined)) {
            mReceiveDimensionValueIndex++;
        }
        Log.d(TAG, "mReceiveDimensionTotal = " + mReceiveDimensionTotal + ", mReceiveDimensionIndex = " + mReceiveDimensionIndex + ", mReceiveDimensionValueNum = " + mReceiveDimensionValueNum + ", mReceiveDimensionValueIndex = " + mReceiveDimensionValueIndex);
        mRrtEvent.add(rrtEvent);
        //judge finished
        if ((mReceiveDimensionIndex == mReceiveDimensionTotal - 1) && (mReceiveDimensionValueIndex == mReceiveDimensionValueNum - 1)) {
            finishUpdateRrt();
            updateDimensionValues();
        }
    }

    private void startUpdateRrt() {
        if (DEBUG) Log.d(TAG, "startUpdateRrt");
        mUpgrading = true;
        mRrtEvent.clear();
        mReceiveDimensionTotal = -1;
        mReceiveDimensionIndex = -1;
        mReceiveDimensionValueIndex = -1;
        mReceiveDimensionName = null;
    }

    private void preventUpdateTimeout (boolean value) {
        if (value) {
            handler.removeMessages(RESET_UPDATE);
            handler.sendEmptyMessageDelayed(RESET_UPDATE, 5 * 1000);
        } else {
            handler.removeMessages(RESET_UPDATE);
        }
    }

    private void resetUpdateRrtWhenTimeout() {
        if (DEBUG) Log.d(TAG, "resetUpdateRrtWhenTimeout");
        mUpgrading = false;
        mRrtEvent.clear();
        mReceiveDimensionTotal = -1;
        mReceiveDimensionIndex = -1;
        mReceiveDimensionValueIndex = -1;
        mReceiveDimensionName = null;
    }

    private void finishUpdateRrt() {
        if (DEBUG) Log.d(TAG, "finishUpdateRrt");
        mUpgrading = false;
        mContentResolver.delete(mUri, _ID + "!=-1", null);
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        Iterator iterator = mRrtEvent.iterator();
        while (iterator.hasNext()) {
            RrtEvent event = (RrtEvent)iterator.next();
            ContentValues values = new ContentValues();
            values.put(COLUMN_ORIGINAL_NETWORK_ID, 0);
            values.put(VERSION_NUM, event.versionNumber);
            values.put(DIMENSION_NUM, event.dimensionDefined);
            values.put(REGION5_NAME, event.ratingRegionName);
            values.put(DIMENSION_NAME, event.dimensionName);
            values.put(VALUES_DEFINED, event.valuesDefined);
            values.put(LEVEL_RATING_TEXT, event.abbrevRatingValue);
            ops.add(ContentProviderOperation.newInsert(mUri)
                    .withValues(values)
                    .build());
        }
        try {
            mContentResolver.applyBatch(mAuthority, ops);
        } catch (RemoteException | OperationApplicationException e) {
            e.printStackTrace();
        }
        ops.clear();
    }

    private void setDefaultRrt() {
        if (DEBUG) Log.d(TAG, "setDefaultRrt");
        Cursor cursor = null;
        boolean needInit = false;
        try {
            cursor = mContentResolver.query(mUri, PROJECTION, null, null, null);
            while (cursor != null && cursor.moveToNext()) {
                int id = getIntFromCur(cursor, _ID);
                if (id > 0) {
                    break;
                } else if (id <= 0) {
                    Log.e(TAG, "data is empty");
                    needInit = true;
                    break;
                }
            }
            if (!(cursor != null && cursor.moveToNext())) {
                needInit = true;
            }
            if (needInit) {
                saveDefaultRrt(cursor);
            }
        } catch (Exception e) {
            Log.e(TAG, "setDefaultRrt Exception: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void saveDefaultRrt(Cursor cursor) {
        if (DEBUG) Log.d(TAG, "saveDefaultRrt");
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        Iterator iterator = DEFAULT_RRT5_EVENTS.iterator();
        while (iterator.hasNext()) {
            RrtEvent event = (RrtEvent)iterator.next();
            ContentValues values = new ContentValues();
            values.put(COLUMN_ORIGINAL_NETWORK_ID, 0);
            values.put(VERSION_NUM, event.versionNumber);
            values.put(DIMENSION_NUM, event.dimensionDefined);
            values.put(REGION5_NAME, event.ratingRegionName);
            values.put(DIMENSION_NAME, event.dimensionName);
            values.put(VALUES_DEFINED, event.valuesDefined);
            values.put(LEVEL_RATING_TEXT, event.abbrevRatingValue);
            ops.add(ContentProviderOperation.newInsert(mUri)
                    .withValues(values)
                    .build());
            Log.d(TAG, "saveDefaultRrt dimensionname = " + event.dimensionName + ", value = " + event.abbrevRatingValue);
        }
        try {
            mContentResolver.applyBatch(mAuthority, ops);
        } catch (RemoteException | OperationApplicationException e) {
            Log.e(TAG, "insert default rrt data Failed = " + e.getMessage());
        }
        ops.clear();
    }

    private void printDimensionValues() {
        if (mDimensionNameValues == null || mDimensionNameValues.size() == 0) {
            Log.w(TAG, "print empty DimensionValues");
            return;
        }
        Iterator iterator = mDimensionNameValues.keySet().iterator();
        while (iterator.hasNext()) {
            int key = (int)iterator.next();
            Log.d(TAG, "print dimensionname = "+ mDimensionNameValues.get(key) + ": " + Arrays.toString(mDimensionValues.get(mDimensionNameValues.get(key))));
        }
    }
}
