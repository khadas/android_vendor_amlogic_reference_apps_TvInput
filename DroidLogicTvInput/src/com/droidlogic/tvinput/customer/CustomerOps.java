/*
 * Copyright (c) 2014 Amlogic, Inc. All rights reserved.
 *
 * This source code is subject to the terms and conditions defined in the
 * file 'LICENSE' which is part of this source code package.
 *
 * Description: JAVA file
 */

/*class CustomerOps used to handle differ of customers*/
package com.droidlogic.tvinput.customer;

import android.content.Context;
import android.media.tv.TvContentRating;
import android.media.tv.TvInputManager;
import android.util.Log;

import com.droidlogic.app.DataProviderManager;
import com.droidlogic.app.SystemControlManager;
import com.droidlogic.app.tv.ChannelInfo;

public class CustomerOps {
    private static final String TAG = CustomerOps.class.getSimpleName();

    private static final String DTV_CC_TRACK_INDEX = "dtv_cc_index";
    private static final String ATV_CC_TRACK_INDEX = "atv_cc_index";
    public static final int INVALID_INDEX = 0xff;

    private static CustomerOps mInstance;
    private Context mContext;
    private TvInputManager mTvInputManager;
    private SystemControlManager mSystemControlManager;

    private CustomerOps(Context context) {
        mContext = context;
        mSystemControlManager = SystemControlManager.getInstance();
    }

    public static CustomerOps getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new CustomerOps(context);
        }
        return mInstance;
    }

    private TvInputManager getTvInputManager() {
        if (mTvInputManager == null)
            mTvInputManager = (TvInputManager)mContext.getSystemService(Context.TV_INPUT_SERVICE);
        return mTvInputManager;
    }

    public TvContentRating[] getCustomerNoneRatings() {
        TvContentRating ratings[] = new TvContentRating[1];

        TvContentRating tvRating = TvContentRating.createRating("com.android.tv", "US_TV", "US_TV_NR");
        TvContentRating mvRating = TvContentRating.createRating("com.android.tv", "US_MV", "US_MV_NR");

        if (getTvInputManager().isRatingBlocked(tvRating)) {
            ratings[0] = tvRating;
            Log.d(TAG, "add tv nr rating");
        } else if (mTvInputManager.isRatingBlocked(mvRating)) {
            ratings[0] = mvRating;
            Log.d(TAG, "add mv nr rating");
        } else {
            ratings = null;
        }
        return ratings;
    }

    public boolean shouldSendTimeShiftStatusToAtv() {
        return true;
    }

    public void saveTvClosedCaptionIndex(ChannelInfo info, int index) {
        if (info == null) {
            return;
        }
        if (info.isAtscChannel()) {
            DataProviderManager.putIntValue(mContext, DTV_CC_TRACK_INDEX, index);
        } else if (info.isNtscChannel()) {
            DataProviderManager.putIntValue(mContext, ATV_CC_TRACK_INDEX, index);
        }
    }

    public void saveAvClosedCaptionIndex(int index) {
        DataProviderManager.putIntValue(mContext, ATV_CC_TRACK_INDEX, index);
    }

    public int getTvClosedCaptionIndex(ChannelInfo info) {
        int index = -1;

        if (info == null) {
            return index;
        }
        if (info.isAtscChannel()) {
            index = DataProviderManager.getIntValue(mContext, DTV_CC_TRACK_INDEX, -1);
        } else if (info.isNtscChannel()) {
            index = DataProviderManager.getIntValue(mContext, ATV_CC_TRACK_INDEX, -1);
        } else {
            index = INVALID_INDEX;
        }
        return index;
    }

    public int getAvClosedCaptionIndex() {
        return DataProviderManager.getIntValue(mContext, ATV_CC_TRACK_INDEX, -1);
    }
}
