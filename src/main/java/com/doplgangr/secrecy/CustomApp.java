/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.doplgangr.secrecy;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.doplgangr.secrecy.FileSystem.storage;

import org.androidannotations.annotations.EApplication;

/**
 * Created by Matthew on 4/5/2014.
 */
@EApplication
public class CustomApp extends Application {
    public static Context context;
    public static String VERSIONNAME = "";
    private static int VERSIONCODE = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            VERSIONNAME = pInfo.versionName;
            VERSIONCODE = pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        storage.deleteTemp(); //Start clean every time!!
    }

}
