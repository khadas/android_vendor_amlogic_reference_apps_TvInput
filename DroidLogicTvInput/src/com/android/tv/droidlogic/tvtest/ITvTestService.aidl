/*
 * Copyright (C) 2017 Amlogic Corporation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.android.tv.droidlogic.tvtest;

import com.android.tv.droidlogic.tvtest.ITvTestCallbackListener;

interface ITvTestService{
    void init(String args);

    void getDVBSignalStatusMsg(int sigStatus);

    void registerListener(ITvTestCallbackListener listener);

    void unregisterListener(ITvTestCallbackListener listener);
}
