/*
 * Copyright (c) 2014 Amlogic, Inc. All rights reserved.
 *
 * This source code is subject to the terms and conditions defined in the
 * file 'LICENSE' which is part of this source code package.
 *
 * Description: JAVA file
 */

package com.droidlogic.tvinput;

import android.os.SystemProperties;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.droidlogic.app.tv.InputChangeAdapter;


public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    private static final String PROP_IS_TV = "ro.vendor.platform.has.tvuimode";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (SystemProperties.getBoolean(PROP_IS_TV, false)) {
            Log.d(TAG,"BootCompelte start otp if possbile");
            InputChangeAdapter.getInstance(context).sendBootOtpIntent();
        }
        Log.d(TAG, "onReceive boot complete broadcast");
    }
}

