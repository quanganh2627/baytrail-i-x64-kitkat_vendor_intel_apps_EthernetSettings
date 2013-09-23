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
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;

// Intimately tied to the design of res/layout/signal_cluster_view.xml
final public class EthernetView
        extends LinearLayout{

    public EthernetView(Context context) {
        this(context, null);
    }

    public EthernetView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EthernetView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void onAttachedToWindow() {
    }

    public void onDetachedFromWindow() {
    }

    public static void setEthernetIndicators(boolean visible, int statusIcon,
            int activityIcon, String contentDescription) {
    }

    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        return false;
    }

    public void apply(boolean mMobileVisible, boolean mWifiVisible, boolean mIsAirplaneMode,
            ImageView mMobileType, View mSpacer) {
    }
}
