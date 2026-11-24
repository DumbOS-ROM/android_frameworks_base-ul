/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.systemui.recents;

import android.content.Context;
import android.os.UserHandle;
import android.content.Intent;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;

import com.android.systemui.dagger.SysUISingleton;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.shared.recents.IOverviewProxy;
import com.android.systemui.statusbar.policy.KeyguardStateController;

import javax.inject.Inject;

/**
 * An implementation of the Recents interface which proxies to the OverviewProxyService.
 */
@SysUISingleton
public class OverviewProxyRecentsImpl implements RecentsImplementation {

    private final static String TAG = "OverviewProxyRecentsImpl";
    private Context mContext;
    private Handler mHandler;
    private final OverviewProxyService mOverviewProxyService;
    private final ActivityStarter mActivityStarter;
    private final KeyguardStateController mKeyguardStateController;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @Inject
    public OverviewProxyRecentsImpl(
            OverviewProxyService overviewProxyService,
            ActivityStarter activityStarter,
            KeyguardStateController keyguardStateController) {
        mOverviewProxyService = overviewProxyService;
        mActivityStarter = activityStarter;
        mKeyguardStateController = keyguardStateController;
    }

    @Override
    public void onStart(Context context) {
        mContext = context;
        mHandler = new Handler();
    }

    @Override
    public void showRecentApps(boolean triggeredFromAltTab) {
        IOverviewProxy overviewProxy = mOverviewProxyService.getProxy();
        if (overviewProxy != null) {
            try {
                overviewProxy.onOverviewShown(triggeredFromAltTab);
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to send overview show event to launcher.", e);
            }
        } else {
            launchDumbOsRecents("com.dumbos.launcher.ACTION_SHOW_RECENTS");
        }
    }

    @Override
    public void hideRecentApps(boolean triggeredFromAltTab, boolean triggeredFromHomeKey) {
        IOverviewProxy overviewProxy = mOverviewProxyService.getProxy();
        if (overviewProxy != null) {
            try {
                overviewProxy.onOverviewHidden(triggeredFromAltTab, triggeredFromHomeKey);
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to send overview hide event to launcher.", e);
            }
        }
    }

    @Override
    public void toggleRecentApps() {
        // If connected to launcher service, let it handle the toggle logic;
        // otherwise fall back to DumbLauncher's single-process OLED RecentsActivity.
        final Runnable toggleRecents = () -> {
            IOverviewProxy overviewProxy = mOverviewProxyService.getProxy();
            if (overviewProxy != null) {
                try {
                    overviewProxy.onOverviewToggle();
                    mOverviewProxyService.notifyToggleRecentApps();
                } catch (RemoteException e) {
                    Log.e(TAG, "Cannot send toggle recents through proxy service.", e);
                }
            } else {
                launchDumbOsRecents("com.dumbos.launcher.ACTION_TOGGLE_RECENTS");
            }
        };
        if (mKeyguardStateController.isShowing()) {
            mActivityStarter.executeRunnableDismissingKeyguard(
                    () -> mHandler.post(toggleRecents), null, true /* dismissShade */,
                    false /* afterKeyguardGone */,
                    true /* deferred */);
        } else {
            toggleRecents.run();
        }
    }

    private void launchDumbOsRecents(String action) {
        if (mContext == null) {
            return;
        }
        try {
            Intent intent = new Intent(action);
            intent.setClassName("com.dumbos.launcher", "com.dumbos.launcher.RecentsActivity");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP
                    | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            mContext.startActivityAsUser(intent, UserHandle.CURRENT);
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch DumbOS RecentsActivity.", e);
        }
    }
}
