package com.droidlogic.tvinput.database;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import com.droidlogic.app.tv.RrtEvent;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Rrt5DataBaseManager {
    public static final String TAG = "Rrt5DataBaseManager";
    public static final boolean DEBUG = false;
    private static final int DOWNLOADABLE_REGION_ID = 5;

    //rrt5 provider key define
    public static final String COLUMN_MAJOR_NUMBER = "major_number";
    public static final String COLUMN_ORIGINAL_NETWORK_ID = "original_network_id";
    public static final String COLUMN_REGION_ID = "region_id";
    public static final String COLUMN_VERSION_NUM = "version_num";
    public static final String COLUMN_DIMENSION_NUM = "dimension_num";
    public static final String COLUMN_REGION5_NAME = "region5_name";
    public static final String COLUMN_DIMENSION_NAME = "dimension_name";
    public static final String COLUMN_VALUES_DEFINED = "values_defined";
    public static final String COLUMN_LEVEL_RATING_TEXT = "level_rating_text";
    public static final String COLUMN_RATING_DESC = "rating_desc";
    public final String[] PROJECTION = {
            COLUMN_REGION_ID,
            COLUMN_VERSION_NUM,
            COLUMN_DIMENSION_NUM,
            COLUMN_REGION5_NAME,
            COLUMN_DIMENSION_NAME,
            COLUMN_VALUES_DEFINED,
            COLUMN_LEVEL_RATING_TEXT,
            COLUMN_RATING_DESC
    };

    private final Context mContext;
    private final ContentResolver mContentResolver;
    private boolean mUpgrading = false;
    protected final Object mProcessLock = new Object();

    //receive rrtEvent
    private final List<RrtEvent> mRrtEvent = new ArrayList<>();
    private String mReceiveDimensionName = null;
    private int mReceiveDimensionTotal = -1;
    private int mReceiveDimensionIndex = -1;
    private int mReceiveDimensionValueIndex = -1;
    private int mReceiveDimensionValueNum = -1;

    private String mAuthority = "com.droidlogic.database";
    private String mTableName = "tv_rrt";
    private String mDomain = "com.android.tv";
    private final Uri mUri;

    //handle messages
    public final static int RESET_UPDATE = 0;

    //for some customers need save separate rrt5 info with different channe
    //enable this to match the requirements
    private final boolean CONFIG_STORE_RRT5_WITH_MAJOR_NUMBER = false;

    Handler handler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            if (msg.what == RESET_UPDATE) {
                resetUpdateRrtWhenTimeout();
            }
        }
    };

    public Rrt5DataBaseManager(Context context) {
        mContext = context;
        mUri = Uri.parse("content://" + mAuthority + "/" + mTableName);
        mContentResolver = mContext.getContentResolver();
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
    }

    public String getRrt5Domain() {
        return mDomain;
    }

    public void releaseResource() {
        preventUpdateTimeout(false);
        resetUpdateRrtWhenTimeout();
    }

    private Rrt5RatingInfo getRrt5RatingInfo(int majorNumber) {
        Rrt5RatingInfo rrt = null;
        int selectNumbe = majorNumber;

        if (!CONFIG_STORE_RRT5_WITH_MAJOR_NUMBER)
            selectNumbe = 0;

        try (Cursor cursor = mContentResolver.query(
                mUri,
                PROJECTION,
                COLUMN_MAJOR_NUMBER + "=?1",
                new String[]{"" + selectNumbe}, null)) {
            if (cursor != null && cursor.getCount() > 0) {
                rrt = Rrt5RatingInfo.buildFromCursor(selectNumbe, cursor);
            }
        } catch (Exception ignore) {}
        return rrt;
    }

    public String[] getRrt5Rating(int majorNumber, int dimension, int value) {
        final String[] result = new String[]{null, null, null};
        Rrt5RatingInfo rrt = getRrt5RatingInfo(majorNumber);
        if (rrt != null && rrt.dimensions.size() > dimension && rrt.ratings.size() > value) {
            result[0] = rrt.regionName;
            result[1] = rrt.dimensions.get(dimension);
            result[2] = rrt.getRating(dimension, value - 1);//value minimum is 1
        }
        return result;
    }

    private static int getIntFromCur(@NonNull Cursor cursor, String key) {
        int value = -1;
        int index = cursor.getColumnIndex(key);
        if (index > -1) {
            value = cursor.getInt(index);
        }
        return value;
    }

    private static String getStringFromCur(@NonNull Cursor cursor, String key) {
        String value = null;
        int index = cursor.getColumnIndex(key);
        if (index > -1) {
            value = cursor.getString(index);
        }
        return value;
    }

    public void SynchronizedUpdateRrt(int majorNumber, RrtEvent rrtEvent) {
        synchronized (mProcessLock) {
            updateRrt5Event(majorNumber, rrtEvent);
            preventUpdateTimeout(true);
        }
    }

    public boolean isUpdateFinished() {
        return !mUpgrading;
    }

    private void updateRrt5Event(int majorNumber, RrtEvent rrtEvent) {
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
        Log.d(TAG, "mReceiveDimensionTotal = " + mReceiveDimensionTotal +
                ", mReceiveDimensionIndex = " + mReceiveDimensionIndex +
                ", mReceiveDimensionValueNum = " + mReceiveDimensionValueNum +
                ", mReceiveDimensionValueIndex = " + mReceiveDimensionValueIndex);
        mRrtEvent.add(rrtEvent);
        //judge finished
        if ((mReceiveDimensionIndex == mReceiveDimensionTotal - 1) &&
                (mReceiveDimensionValueIndex == mReceiveDimensionValueNum - 1)) {
            finishUpdateRrt(majorNumber);
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

    private void finishUpdateRrt(int majorNumber) {
        if (DEBUG) Log.d(TAG, "finishUpdateRrt");
        mUpgrading = false;
        int selectNumbe = majorNumber;

        if (!CONFIG_STORE_RRT5_WITH_MAJOR_NUMBER)
            selectNumbe = 0;

        Rrt5RatingInfo rrtDb = getRrt5RatingInfo(selectNumbe);
        Log.d(TAG, "Database rrt5 rating: (" + selectNumbe + ": " + rrtDb + ")");
        Rrt5RatingInfo rrtEvents = Rrt5RatingInfo.buildFromEvents(selectNumbe, mRrtEvent);
        Log.d(TAG, "New rrt5 rating: (" + selectNumbe + ": " + rrtEvents + ")");
        if (rrtDb != null && rrtEvents.equals(rrtDb)) {
            Log.d(TAG, "Same rrt table received for " + selectNumbe + ", skip");
            return;
        }

        if (rrtEvents.regionId < DOWNLOADABLE_REGION_ID) {
            return;
        }

        Log.d(TAG, "Add rrt rating: " + rrtEvents);
        mContentResolver.delete(mUri, COLUMN_MAJOR_NUMBER + "=?1", new String[]{"" + selectNumbe});
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        for (RrtEvent event : mRrtEvent) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_MAJOR_NUMBER, selectNumbe);
            values.put(COLUMN_ORIGINAL_NETWORK_ID, 0);
            values.put(COLUMN_REGION_ID, event.ratingRegion);
            values.put(COLUMN_VERSION_NUM, event.versionNumber);
            values.put(COLUMN_DIMENSION_NUM, event.dimensionDefined);
            values.put(COLUMN_REGION5_NAME, event.ratingRegionName);
            values.put(COLUMN_DIMENSION_NAME, event.dimensionName);
            values.put(COLUMN_VALUES_DEFINED, event.valuesDefined);
            values.put(COLUMN_LEVEL_RATING_TEXT, event.abbrevRatingValue);
            values.put(COLUMN_RATING_DESC, event.ratingValue);
            ops.add(ContentProviderOperation.newInsert(mUri)
                    .withValues(values)
                    .build());
        }

        try {
            mContentResolver.applyBatch(mAuthority, ops);
        } catch (Exception e) {
            e.printStackTrace();
        }
        ops.clear();
    }

    private static class Rrt5RatingInfo {
        private final int channelMajorNumber;
        private int version;
        private int regionId;
        private String regionName;
        private final List<String> dimensions;
        private final List<Rating> ratings;

        private static class Rating {
            int dimensionIndex;
            String rating;
            String desc;
            int valueIndex;

            @NonNull
            @Override
            public String toString() {
                return "(" +
                        dimensionIndex + ":" +
                        valueIndex + "," +
                        rating + "[" +
                        desc + "])";
            }
        }

        private Rrt5RatingInfo(int majorNumber) {
            channelMajorNumber = majorNumber;
            version = -1;
            regionId = -1;
            regionName = null;
            dimensions = new ArrayList<>();
            ratings = new ArrayList<>();
        }

        public static Rrt5RatingInfo buildFromCursor(int majorNumber, @NonNull Cursor cursor) {
            Rrt5RatingInfo rrt = new Rrt5RatingInfo(majorNumber);
            int dimensionIndex = -1;
            int valueIndex = -1;
            while (cursor.moveToNext()) {
                Rating r = new Rating();
                if (rrt.version == -1)
                    rrt.version = getIntFromCur(cursor, COLUMN_VERSION_NUM);
                if (rrt.regionId == -1)
                    rrt.regionId = getIntFromCur(cursor, COLUMN_REGION_ID);
                if (rrt.regionName == null)
                    rrt.regionName = getStringFromCur(cursor, COLUMN_REGION5_NAME);
                String dimension = getStringFromCur(cursor, COLUMN_DIMENSION_NAME);
                if (!rrt.dimensions.contains(dimension)) {
                    rrt.dimensions.add(dimension);
                }
                if (dimensionIndex < rrt.dimensions.indexOf(dimension)) {
                    dimensionIndex = rrt.dimensions.indexOf(dimension);
                    valueIndex = 0;
                }
                r.rating = getStringFromCur(cursor, COLUMN_LEVEL_RATING_TEXT);
                r.desc = getStringFromCur(cursor, COLUMN_RATING_DESC);
                r.dimensionIndex = dimensionIndex;
                r.valueIndex = valueIndex;
                valueIndex ++;

                rrt.ratings.add(r);
            }
            return rrt;
        }

        public static Rrt5RatingInfo buildFromEvents(int majorNumber, @NonNull List<RrtEvent> events) {
            Rrt5RatingInfo rrt = new Rrt5RatingInfo(majorNumber);
            int dimensionIndex = -1;
            int valueIndex = -1;
            for (RrtEvent event : events) {
                Rating r = new Rating();
                if (rrt.version == -1)
                    rrt.version = event.versionNumber;
                if (rrt.regionId == -1)
                    rrt.regionId = event.ratingRegion;
                if (rrt.regionName == null)
                    rrt.regionName = event.ratingRegionName;
                String dimension = event.dimensionName;
                if (!rrt.dimensions.contains(dimension)) {
                    rrt.dimensions.add(dimension);
                }
                if (dimensionIndex < rrt.dimensions.indexOf(dimension)) {
                    dimensionIndex = rrt.dimensions.indexOf(dimension);
                    valueIndex = 0;
                }
                r.rating = event.abbrevRatingValue;
                r.desc = event.ratingValue;
                r.dimensionIndex = dimensionIndex;
                r.valueIndex = valueIndex;
                valueIndex ++;

                rrt.ratings.add(r);
            }
            return rrt;
        }

        public boolean equals(@NonNull Rrt5RatingInfo rrt) {
            return (channelMajorNumber == rrt.channelMajorNumber &&
                    version == rrt.version &&
                    regionId == rrt.regionId &&
                    regionName.equals(rrt.regionName) &&
                    dimensions.equals(rrt.dimensions));
        }

        public String getRating(int dimension, int value) {
            for (Rating r : ratings) {
                if (r.dimensionIndex == dimension && r.valueIndex == value) {
                    return r.rating;
                }
            }
            return null;
        }

        @NonNull
        @Override
        public String toString() {
            return "Rrt5RatingInfo(major_number:" + channelMajorNumber +
                    ",version:" + version +
                    ",regionId:" + regionId +
                    ",region:" + regionName +
                    ",dimensions:" + dimensions +
                    ",ratings:" + ratings +
                    ")";
        }
    }
}
