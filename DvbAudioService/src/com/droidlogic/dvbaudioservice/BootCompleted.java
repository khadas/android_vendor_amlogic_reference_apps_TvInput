/*
 * Copyright (c) 2014 Amlogic, Inc. All rights reserved.
 *
 * This source code is subject to the terms and conditions defined in the
 * file 'LICENSE' which is part of this source code package.
 *
 * Description:
 *     AMLOGIC BootCompleted
 */

package com.droidlogic.dvbaudioservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.droidlogic.app.SystemControlManager;

public class BootCompleted extends BroadcastReceiver {
    private boolean mAudioSystemCmd;
    private SystemControlManager mSystemControlManager;

    @Override
    public void onReceive(Context context, Intent intent) {
        mSystemControlManager = SystemControlManager.getInstance();
        mAudioSystemCmd = mSystemControlManager.getPropertyBoolean("ro.vendor.platform.support.audiosystemcmd", true);
        if (mAudioSystemCmd) {
            context.startService(new Intent(context, AudioSystemCmdService.class));
        }
    }
}
