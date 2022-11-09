/*
 * Copyright (c) 2014 Amlogic, Inc. All rights reserved.
 *
 * This source code is subject to the terms and conditions defined in the
 * file 'LICENSE' which is part of this source code package.
 *
 * Description: JAVA file
 */

package com.droidlogic.tvinput.services;
import com.droidlogic.app.SystemControlManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.android.tv.droidlogic.tvtest.TvTestService;


public class TvScanServiceReceiver extends BroadcastReceiver {
	static final String TAG = "TvScanServiceReceiver";
	private static final String ACTION_BOOT_COMPLETED ="android.intent.action.BOOT_COMPLETED";
	private static final String PROP_TV_TEST_MODE = "persist.vendor.sys.testmode.enable";
	private SystemControlManager mSystemControlManager;
	@Override
	public void onReceive(Context context, Intent intent) {
		if (ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
			Log.d(TAG,"TvScanService Start*******************************************");
			context.startService(new Intent(context, TvScanService.class));
			//start TvScanService
			mSystemControlManager = SystemControlManager.getInstance();
			if (mSystemControlManager.getPropertyBoolean(PROP_TV_TEST_MODE, false)) {
				Log.d(TAG,"TvTestService STart*************");
				context.startService(new Intent(context, TvTestService.class));
			}
		}
	}
}
