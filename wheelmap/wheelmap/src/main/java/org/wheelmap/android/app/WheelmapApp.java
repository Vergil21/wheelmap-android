/*
 * #%L
 * Wheelmap - App
 * %%
 * Copyright (C) 2011 - 2012 Michal Harakal - Michael Kroez - Sozialhelden e.V.
 * %%
 * Wheelmap App based on the Wheelmap Service by Sozialhelden e.V.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS-IS" BASIS
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.wheelmap.android.app;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;
import org.holoeverywhere.HoloEverywhere;
import org.holoeverywhere.addon.AddonMyRoboguice;
import org.holoeverywhere.app.Application;
import org.wheelmap.android.manager.MyLocationManager;
import org.wheelmap.android.manager.SupportManager;
import org.wheelmap.android.model.Support;
import org.wheelmap.android.model.UserQueryHelper;
import org.wheelmap.android.model.Wheelmap;
import org.wheelmap.android.modules.MainModule;
import org.wheelmap.android.online.R;

import android.content.Context;

import de.akquinet.android.androlog.Log;
import roboguice.RoboGuice;

// Beta and PRE-RC key: "dGJWQW5PelRXWUFTbDh6VW5UYm94cXc6MQ"
// RC1 - key: @ReportsCrashes(formKey = "dC1VVDdKenJLRUpZTC1MZXBVR3p6ZlE6MQ" )
// RC2 - key: @ReportsCrashes(formKey = "dG1fUDltTlNiM3V4NmRvaVExT3dJclE6MQ" )
// Release v0.7 @ReportsCrashes( formKey = "dGMzcTRSZjRMRG14c0JmU25ET1JLQmc6MQ")
// the first market version. @ReportsCrashes( formKey = "dGMzcTRSZjRMRG14c0JmU25ET1JLQmc6MQ")
// the second market version @ReportsCrashes( formKey = "dEFLbUtHV1VlNEp2MHc0UXg3M0VyUnc6MQ")
// v0.8: @ReportsCrashes( formKey = "dGEyal90UGZ2Mk0tSmROYnBsVk02THc6MQ")
// v0.9 @ReportsCrashes(formKey = "dEl3ZHFJUkxYZnplcDRoN0RUZGNCUXc6MQ")
// v0.99 @ReportsCrashes(formKey = "dDdKSUloUldNQlBVR3dWbG1FVU1xbEE6MQ")

@ReportsCrashes(formUri = "http://www.bugsense.com/api/acra?api_key=56cc8f6f", formKey = "")
public class WheelmapApp extends Application {

    private final static String TAG = WheelmapApp.class.getSimpleName();

    private static WheelmapApp INSTANCE;

    private SupportManager mSupportManager;

    private static final long ACTIVE_FREQUENCY = 2 * 60 * 1000;

    private static final int ACTIVE_MAXIMUM_AGE = 10 * 60 * 1000;

    private boolean isAcraInitCalled;

    @Override
    public void onCreate() {
        RoboGuice.setModulesResourceId(R.array.roboguice_modules);
        AddonMyRoboguice.addModule(MainModule.class);
        super.onCreate();
        INSTANCE = this;
        Log.init(getApplicationContext(), getString(R.string.andrologproperties));
        Log.d(TAG, "onCreate: creating App");

        if (!getResources().getBoolean(R.bool.developbuild) && !isAcraInitCalled) {
            ACRA.init(this);
            isAcraInitCalled = true;
        }

        Support.init(getApplicationContext());
        Wheelmap.POIs.init(getApplicationContext());
        AppCapability.init(getApplicationContext());
        MyLocationManager.init(getApplicationContext());

        mSupportManager = new SupportManager(getApplicationContext());
        UserQueryHelper.init(getApplicationContext());
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        Log.d(TAG, "onTerminate");
        MyLocationManager.destroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.d("lowmemory", "wheelmap app - onLowMemory");
    }

    public static Context get() {
        return INSTANCE.getApplicationContext();
    }

    public static WheelmapApp getApp() {
        return INSTANCE;
    }

    public boolean isAcraInitCalled() {
        return isAcraInitCalled;
    }

    public static SupportManager getSupportManager() {
        if (INSTANCE == null || INSTANCE.mSupportManager == null) {
            throw new NullPointerException(

                    "instance is null or mSupportManager is null - need to terminated");
        }

        return INSTANCE.mSupportManager;
    }

    static {
        HoloEverywhere.DEBUG = true;
    }

}
