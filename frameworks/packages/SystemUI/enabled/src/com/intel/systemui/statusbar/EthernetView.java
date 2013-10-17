/*
 * Copyright (C) 2013 Intel Corporation, All Rights Reserved
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

package com.intel.systemui.statusbar;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Slog;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;

import android.widget.Toast;
import com.android.systemui.statusbar.policy.NetworkController;

import com.android.systemui.R;


// Intimately tied to the design of res/layout/signal_cluster_view.xml
final public class EthernetView
        extends LinearLayout{
    static final boolean DEBUG = false;
    static final String TAG = "EthernetView";

    private static boolean sEthernetVisible = false;
    private static int sEthernetStatusId;
    private static int sEthernetActivityId = 0;
    private static String sEthernetDescription;

    private ViewGroup mEthernetGroup;
    private ImageView mEthernet;
    private ImageView mEthernetActivity;
    private View mSpacer;

    public EthernetView(Context context) {
        this(context, null);
    }

    public EthernetView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EthernetView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void onAttachedToWindow() {
        mEthernetGroup  = (ViewGroup) findViewById(R.id.ethernet_combo);
        mEthernet       = (ImageView) findViewById(R.id.ethernet_status);
        mEthernetActivity = (ImageView) findViewById(R.id.ethernet_inout);
        mSpacer        =  findViewById(R.id.spacer);
    }

    @Override
    public void onDetachedFromWindow() {
        mEthernetGroup  = null;
        mEthernet       = null;
        mEthernetActivity = null;
        mSpacer        = null;
    }

    public static void setEthernetIndicators(boolean visible, int statusIcon,
            int activityIcon, String contentDescription) {
        sEthernetVisible = visible;
        sEthernetStatusId = statusIcon;
        sEthernetActivityId = activityIcon;
        sEthernetDescription = contentDescription;
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        if (sEthernetVisible && mEthernetGroup.getContentDescription() != null) {
            event.getText().add(mEthernetGroup.getContentDescription());
        }
        return true;
    }

    public void apply(boolean mMobileVisible, boolean mWifiVisible,
            boolean mIsAirplaneMode, ImageView mMobileType, View mSpacer) {
        if(sEthernetVisible) {
            mEthernetGroup.setVisibility(View.VISIBLE);
            mEthernet.setImageResource(sEthernetStatusId);
            mEthernetActivity.setImageResource(sEthernetActivityId);
            mEthernetGroup.setContentDescription(sEthernetDescription);
        } else {
            mEthernetGroup.setVisibility(View.GONE);
        }

        if (DEBUG) Slog.d(TAG,
                String.format("ethernet: %s sig=%d act=%d",
                        (sEthernetVisible ? "VISIBLE" : "GONE"),
                        sEthernetStatusId, sEthernetActivityId));

        if(sEthernetVisible) {
            mSpacer.setVisibility(View.INVISIBLE);
        } else {
            mSpacer.setVisibility(View.GONE);
        }

        if (mMobileVisible && (mWifiVisible || sEthernetVisible) && mIsAirplaneMode) {
            mSpacer.setVisibility(View.INVISIBLE);
        } else {
            mSpacer.setVisibility(View.GONE);
        }

        mMobileType.setVisibility( !(mWifiVisible || sEthernetVisible) ? View.VISIBLE : View.GONE);
    }
}
