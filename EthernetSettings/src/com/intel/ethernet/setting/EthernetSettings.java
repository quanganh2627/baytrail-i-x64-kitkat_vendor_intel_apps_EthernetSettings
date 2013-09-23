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

package com.intel.ethernet.setting;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.*;
import android.net.LinkAddress;
import android.net.NetworkInfo.DetailedState;
import android.net.ProxyProperties;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.TextView;

import com.intel.internal.ethernet.EthernetInfo;
import com.intel.internal.ethernet.EthernetManager;

import java.net.InetAddress;

public class EthernetSettings extends PreferenceActivity
        implements Preference.OnPreferenceClickListener,
            DialogInterface.OnClickListener {
    private static final String TAG = "EthernetSettings";
    private static final boolean DEBUG = false;
    private static final int ETHERNET_DIALOG_ID = 1;

    private IntentFilter mFilter;
    private BroadcastReceiver mReceiver;
    private EthernetManager mEthernetManager;
    private EthernetDialog mDialog;
    private TextView mEmptyView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFilter = new IntentFilter();
        mFilter.addAction(EthernetManager.INTERFACE_STATE_CHANGED_ACTION);
        mFilter.addAction(EthernetManager.INTERFACE_REMOVED_ACTION);
        if (DEBUG) Log.d(TAG, "onCreate ");

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                EthernetInfo ei = null;
                if (EthernetManager.INTERFACE_STATE_CHANGED_ACTION.equals(action)) {
                    ei = intent.getParcelableExtra( EthernetManager.EXTRA_ETHERNET_INFO);
                    if (DEBUG) Log.d(TAG, "INTERFACE_STATE_CHANGED_ACTION e " + ei);
                } else if (EthernetManager.INTERFACE_REMOVED_ACTION.equals(action)) {
                    if (DEBUG) Log.d(TAG, "INTERFACE_REMOVED_ACTION");
                    ei = null;
                } else {
                    if (DEBUG) Log.w(TAG, "Error type, do nothing");
                    return ;
                }
                updatePreferences(ei);
            }
        };
        mEthernetManager = (EthernetManager) getSystemService(Context.ETHERNET_SERVICE);

        mEmptyView = (TextView)findViewById(android.R.id.empty);
        getListView().setEmptyView(mEmptyView);
    }

    public boolean onPreferenceClick(Preference preference) {
        if (preference.getKey().equals("ethernet_change_settings")) {
            showDialog(ETHERNET_DIALOG_ID);
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mFilter);
        updatePreferences(mEthernetManager.getCurrentInterface());
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        if (dialogId == ETHERNET_DIALOG_ID) {
            mDialog = new EthernetDialog(this,
                    (DialogInterface.OnClickListener)this,
                    mEthernetManager);
            return mDialog;
        }
        return super.onCreateDialog(dialogId);
    }

    private void updatePreferencesScanning(EthernetInfo info) {
        findPreference("ethernet_ip_settings").setSummary("Waiting for connection");

        findPreference("ethernet_ip_address").setEnabled(false);
        findPreference("ethernet_netmask").setEnabled(false);
        findPreference("ethernet_default_gateway").setEnabled(false);
        findPreference("ethernet_dns").setEnabled(false);
        findPreference("ethernet_proxy_server").setEnabled(false);
        findPreference("ethernet_proxy_port").setEnabled(false);
        findPreference("ethernet_proxy_exclusion").setEnabled(false);
    }

    private void updatePreferences(EthernetInfo info) {
        // Safeguard from some delayed event handling
        PreferenceScreen ps = getPreferenceScreen();
        if (ps != null) {
            ps.removeAll();
        }

        if (!mEthernetManager.isEnabled()) {
            if (DEBUG) Log.d(TAG, "updatePreferences mEthernetManager.isEnabled() "
                    + mEthernetManager.isEnabled());
            return;
        }

        if (DEBUG) Log.d(TAG, "updatePreferences info " + info);

        if (info != null) {
            addPreferencesFromResource(R.xml.ethernet_settings);
            Preference changeSettings = findPreference("ethernet_change_settings");
            changeSettings.setOnPreferenceClickListener(this);

            findPreference("ethernet_interface_name").setSummary(info.getName());
            findPreference("ethernet_mac_address").setSummary(info.getHwAddress());
            if (DEBUG) Log.d(TAG, "ethernet_interface_name" + info.getName());
            if (DEBUG) Log.d(TAG, "ethernet_mac_address" + info.getHwAddress());
            if (DEBUG) Log.d(TAG, "info.getDetailedState()" + info.getDetailedState());

            if (info.getDetailedState() != DetailedState.CONNECTED) {
                updatePreferencesScanning(info);
                return;
            }

            String ipAssignment = "Static";
            if (info.getIpAssignment() == EthernetInfo.IpAssignment.DHCP) {
                ipAssignment = "DHCP";
            }

            String ipAddress = "";
            String netmask = "";
            LinkAddress la = info.getLinkAddress();
            if (la != null) {
                ipAddress = la.getAddress().getHostAddress();
                netmask = Integer.toString(la.getNetworkPrefixLength());
            }

            String defaultGateway = "";
            InetAddress dgw = info.getDefaultGateway();
            if (dgw != null) {
                defaultGateway = dgw.getHostAddress();
            }
            String dns = "";
            InetAddress dnsIa = info.getDNS1();
            if (dnsIa != null) {
                dns = dnsIa.getHostAddress();
                dnsIa = info.getDNS2();
                if (dnsIa != null) {
                    dns += ", " + dnsIa.getHostAddress();
                }
            }

            String proxyHost = "";
            String proxyPort = "";
            String proxyExclusion = "";

            if (info.getProxySettings() == EthernetInfo.ProxySettings.STATIC) {
                ProxyProperties pp = info.getLinkProperties().getHttpProxy();
                proxyHost = pp.getHost();
                proxyPort = Integer.toString(pp.getPort());
                proxyExclusion = pp.getExclusionList();
            }
            if (DEBUG) Log.d(TAG, "ipAssignment" + ipAssignment);
            if (DEBUG) Log.d(TAG, "ipAddress" + ipAddress);
            if (DEBUG) Log.d(TAG, "netmask" + netmask);
            if (DEBUG) Log.d(TAG, "defaultGateway" + defaultGateway);
            if (DEBUG) Log.d(TAG, "dns" + dns);
            if (DEBUG) Log.d(TAG, "proxyHost" + proxyHost);
            if (DEBUG) Log.d(TAG, "proxyPort" + proxyPort);
            if (DEBUG) Log.d(TAG, "proxyExclusion" + proxyExclusion);

            findPreference("ethernet_ip_settings").setSummary(ipAssignment);
            findPreference("ethernet_ip_address").setSummary(ipAddress);
            findPreference("ethernet_netmask").setSummary(netmask);
            findPreference("ethernet_default_gateway").setSummary(defaultGateway);
            findPreference("ethernet_dns").setSummary(dns);
            findPreference("ethernet_proxy_server").setSummary(proxyHost);
            findPreference("ethernet_proxy_port").setSummary(proxyPort);
            findPreference("ethernet_proxy_exclusion").setSummary(proxyExclusion);
        }
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int button) {
        if (button == AlertDialog.BUTTON_POSITIVE) {
            if (mDialog == null) {
                if (DEBUG) Log.d(TAG, "mDialog == null");
            } else if (mDialog.getController() == null) {
                if (DEBUG) Log.d(TAG, "mDialog.getController() == null");
                return;
            } else if (mDialog.getController().getInfo() == null) {
                if (DEBUG) Log.d(TAG, "mDialog.getController().getInfo()");
            }

            if (DEBUG) Log.d(TAG, "mDialog.getController().getInfo()"
                    + mDialog.getController().getInfo());
            mEthernetManager.updateInterface(mDialog.getController().getInfo());
        }
    }
}
