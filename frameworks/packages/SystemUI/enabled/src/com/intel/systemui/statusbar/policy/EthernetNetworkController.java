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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wimax.WimaxManagerConstants;
import android.os.Binder;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Slog;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.internal.app.IBatteryStats;
import com.intel.internal.ethernet.EthernetInfo;
import com.intel.internal.ethernet.EthernetManager;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.cdma.EriInfo;
import com.android.internal.util.AsyncChannel;
import com.android.server.am.BatteryStatsService;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.NetworkController;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final public class EthernetNetworkController {
    static final String TAG = "StatusBar.EthernetNetworkController";
    static final boolean DEBUG = false;

    private String mContentDescriptionEthernet;

    private final EthernetManager mEthernetManager;
    private AsyncChannel mEthernetChannel;
    public boolean mEthernetEnabled;
    private boolean mEthernetConnected;
    private int mEthernetIconId = 0;
    private int mEthernetActivityIconId = 0;
    private int mEthernetActivity = EthernetManager.DATA_ACTIVITY_NONE;

    ArrayList<ImageView> mEthernetIconViews = new ArrayList<ImageView>();
    ArrayList<TextView> mEthernetLabelViews = new ArrayList<TextView>();
    int mLastEthernetIconId = -1;
    private final NetworkController mNetworkController;
    private final Context mContext ;

    public  EthernetNetworkController(Context context, NetworkController networkController) {
        mContext = context;

        mEthernetManager = (EthernetManager) context.getSystemService(Context.ETHERNET_SERVICE);
        Handler ethernetHandler = new EthernetHandler();
        mEthernetChannel = new AsyncChannel();
        Messenger ethernetMessenger = mEthernetManager.getEthernetServiceMessenger();

        if(ethernetMessenger != null) {
            mEthernetChannel.connect(mContext, ethernetHandler, ethernetMessenger);
        }
        mNetworkController = networkController;
    }

    public void addEthernetView(RelativeLayout notificationPanel) {
        final ImageView ethernetRSSI =
                (ImageView)notificationPanel.findViewById(R.id.ethernet_status);
        if (ethernetRSSI != null) {
            mEthernetIconViews.add(ethernetRSSI);
        }
        mEthernetLabelViews.add(
                (TextView)notificationPanel.findViewById(R.id.ethernet_text));
    }

    class EthernetHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case AsyncChannel.CMD_CHANNEL_HALF_CONNECTED:
                    if (msg.arg1 == AsyncChannel.STATUS_SUCCESSFUL) {
                        mEthernetChannel.sendMessage(Message.obtain(this,
                                AsyncChannel.CMD_CHANNEL_FULL_CONNECTION));
                    } else {
                        Slog.e(TAG, "Failed to connect to ethernet service");
                    }
                    break;
                case EthernetManager.DATA_ACTIVITY_NOTIFICATION:
                    if (msg.arg1 != mEthernetActivity) {
                        mEthernetActivity = msg.arg1;
                        mNetworkController.refreshViews();
                    }
                    break;
                default:
                    //Ignore
                    break;
            }
        }
    }

    public void updateEthernetState(Intent intent, int inetCondition) {
        final String action = intent.getAction();
        if (action == null) return;

        if( action.equals(EthernetManager.NETWORK_STATE_CHANGED_ACTION)) {
            mEthernetEnabled = true;
            final EthernetInfo ei = (EthernetInfo)
                    intent.getParcelableExtra(EthernetManager.EXTRA_ETHERNET_INFO);
            if (ei != null) {
                final NetworkInfo networkInfo = ei.getNetworkInfo();
                mEthernetConnected = networkInfo != null && networkInfo.isConnected();
            }
        }
        updateEthernetIcons(inetCondition);
    }

    public void updateEthernetIcons(int inetCondition) {
        if (mEthernetConnected) {
            Slog.d(TAG, "inetCondition is " + inetCondition);
            mEthernetIconId = EthernetIcons.ETHERNET_STATE[inetCondition + 1];
            //TODO: Check this after all the stuff is in place
            mContentDescriptionEthernet =
                                  mContext.getString(R.string.accessibility_ethernet_connected);
        } else {
            mEthernetIconId = 0;
            mContentDescriptionEthernet = mContext.getString(R.string.accessibility_no_ethernet);
        }
    }

    public boolean isEthernetConnected() {
        return mEthernetConnected;
    }

    public boolean isEthernetEnabled() {
        return mEthernetEnabled;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("  - ethernet ------");
        pw.print("  mEthernetEnabled=");
        pw.println(mEthernetEnabled);
        pw.print("  mEthernetConnected=");
        pw.println(mEthernetConnected);
        pw.print("  mEthernetActivity=");
        pw.println(mEthernetActivity);
    }

    public void dump2(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.print("  mLastEthernetIconId=0x");
        pw.print(Integer.toHexString(mLastEthernetIconId));
        pw.print("/");
        pw.println(getResourceName(mLastEthernetIconId));
    }

    private String getResourceName(int resId) {
        if (resId != 0) {
            final Resources res = mContext.getResources();
            try {
                return res.getResourceName(resId);
            } catch (android.content.res.Resources.NotFoundException ex) {
                return "(unknown)";
            }
        } else {
            return "(null)";
        }
    }

    public int getEthernetActivityIconId() {
            switch (mEthernetActivity) {
                case EthernetManager.DATA_ACTIVITY_IN:
                    mEthernetActivityIconId = R.drawable.stat_sys_wifi_in;
                    break;
                case EthernetManager.DATA_ACTIVITY_OUT:
                    mEthernetActivityIconId = R.drawable.stat_sys_wifi_out;
                    break;
                case EthernetManager.DATA_ACTIVITY_INOUT:
                    mEthernetActivityIconId = R.drawable.stat_sys_wifi_inout;
                    break;
                case EthernetManager.DATA_ACTIVITY_NONE:
                    mEthernetActivityIconId = 0;
                    break;
            }
        return mEthernetActivityIconId;
    }

    public int getEthernetIconId() {
        return mEthernetIconId;
    }

    public int getLastEthernetIconId() {
        return mLastEthernetIconId;
    }

    public String getContentDescriptionEthernet() {
        return mContentDescriptionEthernet;
    }

    public void updateEthernetIconId() {
        // ethernet icons on phones
        if (mLastEthernetIconId != mEthernetIconId) {
            mLastEthernetIconId = mEthernetIconId;
            int N = mEthernetIconViews.size();
            for (int i=0; i<N; i++) {
                final ImageView v = mEthernetIconViews.get(i);
                if (mEthernetIconId == 0) {
                    v.setVisibility(View.GONE);
                } else {
                    v.setVisibility(View.VISIBLE);
                    v.setImageResource(mEthernetIconId);
                    v.setContentDescription(mContentDescriptionEthernet);
                }
            }
        }
    }

    public void updateEthernetlabelView(){
        String ethernetLabel = "";
        // ethernet label
        int N = mEthernetLabelViews.size();
        for(int i=0; i<N; i++) {
            TextView v = mEthernetLabelViews.get(i);
            v.setText(ethernetLabel);
            if("".equals(ethernetLabel)) {
                v.setVisibility(View.GONE);
            } else {
                v.setVisibility(View.VISIBLE);
            }
        }
    }
}
