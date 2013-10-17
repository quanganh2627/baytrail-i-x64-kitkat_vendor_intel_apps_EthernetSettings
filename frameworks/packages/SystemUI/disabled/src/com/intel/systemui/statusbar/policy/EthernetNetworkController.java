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

package com.intel.systemui.statusbar.policy;

import android.content.Context;
import android.content.Intent;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.systemui.statusbar.policy.NetworkController;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final public class EthernetNetworkController {
    public  EthernetNetworkController(Context context, NetworkController networkController) {
    }

    public void addEthernetView(RelativeLayout notificationPanel) {
    }

    public void updateEthernetState(Intent intent, int inetCondition) {
    }

    public void updateEthernetIcons(int inetCondition) {
    }

    public boolean isEthernetConnected() {
        return false;
    }

    public boolean isEthernetEnabled() {
        return false;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
    }

    public void dump2(FileDescriptor fd, PrintWriter pw, String[] args) {
    }

    private String getResourceName(int resId) {
        return null;
    }

    public int getEthernetActivityIconId() {
        return 0;
    }

    public int getEthernetIconId() {
        return 0;
    }

    public int getLastEthernetIconId() {
        return 0;
    }

    public String getContentDescriptionEthernet() {
        return null;
    }

    public void updateEthernetIconId() {
    }

    public void updateEthernetlabelView(){
    }
}
