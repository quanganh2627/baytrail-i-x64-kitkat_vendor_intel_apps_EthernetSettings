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

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.net.*;
import android.os.Handler;
import android.text.TextUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.intel.internal.ethernet.EthernetInfo;
import com.intel.internal.ethernet.EthernetInfo.ProxySettings;
import com.intel.internal.ethernet.EthernetInfo.IpAssignment;
import com.intel.internal.ethernet.EthernetInfo.InterfaceStatus;
import com.intel.internal.ethernet.EthernetManager;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.Iterator;

public class EthernetConfigController implements TextWatcher,
        View.OnClickListener, AdapterView.OnItemSelectedListener {
    private static final String TAG = "EthernetConfigController";
    private static final boolean DBG = false;

    private final EthernetConfigUiBase mConfigUi;
    private final View mView;
    private final Handler mTextViewChangedHandler;
    private EthernetManager mEthernetManager;

    private Spinner mIpSettingsSpinner;
    private TextView mIpAddressView;
    private TextView mGatewayView;
    private TextView mNetworkPrefixLengthView;
    private TextView mDns1View;
    private TextView mDns2View;

    private Spinner mProxySettingsSpinner;
    private TextView mProxyHostView;
    private TextView mProxyPortView;
    private TextView mProxyExclusionListView;

    private static final int DHCP = 0;
    private static final int STATIC_IP = 1;

    public static final int PROXY_NONE = 0;
    public static final int PROXY_STATIC = 1;

    /* These next four items are where the validated user input gets stored.
     * getInfo then populates a new EthernetInfo with their contents and
     * returns it to the user. */
    private InterfaceStatus mInterfaceStatus = InterfaceStatus.DISABLED;
    private IpAssignment mIpAssignment = IpAssignment.DHCP;
    private ProxySettings mProxySettings = ProxySettings.UNASSIGNED;
    private LinkProperties mLinkProperties = new LinkProperties();

    EthernetConfigController(EthernetConfigUiBase parent, View view,
            EthernetManager manager) {
        mConfigUi = parent;
        mView = view;
        mTextViewChangedHandler = new Handler();
        mEthernetManager = manager;
        final Context context = mConfigUi.getContext();
        final Resources resources = context.getResources();
        mIpSettingsSpinner = (Spinner) mView.findViewById(R.id.ip_settings);
        mIpSettingsSpinner.setOnItemSelectedListener(this);
        mProxySettingsSpinner = (Spinner) mView.findViewById(R.id.proxy_settings);
        mProxySettingsSpinner.setOnItemSelectedListener(this);

        EthernetInfo info = mEthernetManager.getCurrentInterface();
        if (info != null) {
            mConfigUi.setTitle("Ethernet Settings");
            ViewGroup group = (ViewGroup) mView.findViewById(R.id.info);
            InterfaceStatus status = info.getInterfaceStatus();
            // If status is available, display it
            if (status != null) {
                String ethernetStatus = "";
                if(status == InterfaceStatus.DISABLED) {
                    ethernetStatus = context.getResources().getString(R.string.ethernet_disabled);
                } else {
                    ethernetStatus = context.getResources().getString(R.string.ethernet_enabled);
                }
                addRow(group, R.string.ethernet_status, ethernetStatus);
            }

            // Display IP addresses
            for(InetAddress a : info.getLinkProperties().getAddresses()) {
                addRow(group, R.string.ethernet_ip_address, a.getHostAddress());
            }

            boolean showAdvancedFields = false;

            if (info.getIpAssignment() == IpAssignment.STATIC) {
                mIpSettingsSpinner.setSelection(STATIC_IP);
                showAdvancedFields = true;
            } else {
                mIpSettingsSpinner.setSelection(DHCP);
            }

            if (info.getProxySettings() == ProxySettings.STATIC) {
                mProxySettingsSpinner.setSelection(PROXY_STATIC);
                showAdvancedFields = true;
            } else {
                mProxySettingsSpinner.setSelection(PROXY_NONE);
            }

            showIpConfigFields(info);
            showProxyFields(info);
            mView.findViewById(R.id.ethernet_advanced_toggle).setVisibility(View.VISIBLE);
            mView.findViewById(R.id.ethernet_advanced_togglebox).setOnClickListener(this);
            if (showAdvancedFields) {
                ((CheckBox) mView.findViewById(R.id.ethernet_advanced_togglebox)).setChecked(true);
                mView.findViewById(R.id.ethernet_advanced_fields).setVisibility(View.VISIBLE);
            }

            mConfigUi.setSubmitButton(context.getString(R.string.ethernet_save));
        }

        mConfigUi.setCancelButton(context.getString(R.string.ethernet_cancel));
        if (mConfigUi.getSubmitButton() != null) {
            enableSubmitIfAppropriate();
        }
    }

    EthernetInfo getInfo() {
        EthernetInfo result = new EthernetInfo();

        result.setName(mEthernetManager.getCurrentInterface().getName());
        result.setHwAddress(mEthernetManager.getCurrentInterface().getHwAddress());
        result.setInterfaceStatus(mInterfaceStatus);
        result.setProxySettings(mProxySettings);
        result.setLinkProperties(mLinkProperties);
        result.setIpAssignment(mIpAssignment);

        return result;
    }

    /* show submit button if ip and proxy settings are valid */
    void enableSubmitIfAppropriate() {
        Button submit = mConfigUi.getSubmitButton();
        if (submit == null) return;
        boolean enabled;

        if (ipAndProxyFieldsAreValid()) {
            mInterfaceStatus = InterfaceStatus.ENABLED;
            enabled = true;
        } else {
            mInterfaceStatus = InterfaceStatus.DISABLED;
            enabled = false;
        }
        submit.setEnabled(enabled);
    }

    private void addRow(ViewGroup group, int name, String value) {
        View row = mConfigUi.getLayoutInflater().inflate(R.layout.ethernet_dialog_row, group, false);
        ((TextView) row.findViewById(R.id.name)).setText(name);
        ((TextView) row.findViewById(R.id.value)).setText(value);
        group.addView(row);
    }

    private void showIpConfigFields(EthernetInfo info) {

        mView.findViewById(R.id.ip_fields).setVisibility(View.VISIBLE);

        if (mIpSettingsSpinner.getSelectedItemPosition() == STATIC_IP) {
            mView.findViewById(R.id.staticip).setVisibility(View.VISIBLE);
            if (mIpAddressView == null) {
                mIpAddressView = (TextView) mView.findViewById(R.id.ipaddress);
                mIpAddressView.addTextChangedListener(this);
                mGatewayView = (TextView) mView.findViewById(R.id.gateway);
                mGatewayView.addTextChangedListener(this);
                mNetworkPrefixLengthView = (TextView) mView.findViewById(
                        R.id.network_prefix_length);
                mNetworkPrefixLengthView.addTextChangedListener(this);
                mDns1View = (TextView) mView.findViewById(R.id.dns1);
                mDns1View.addTextChangedListener(this);
                mDns2View = (TextView) mView.findViewById(R.id.dns2);
                mDns2View.addTextChangedListener(this);
            }
            if (info != null) {
                LinkProperties linkProperties = info.getLinkProperties();

                Iterator<LinkAddress> iterator = linkProperties.getLinkAddresses().iterator();
                if (iterator.hasNext()) {
                    LinkAddress linkAddress = iterator.next();
                    mIpAddressView.setText(linkAddress.getAddress().getHostAddress());
                    mNetworkPrefixLengthView.setText(Integer.toString(linkAddress
                            .getNetworkPrefixLength()));
                }

                InetAddress gw = info.getDefaultGateway();
                if (gw != null) {
                    mGatewayView.setText(gw.getHostAddress());
                } else {
                    mGatewayView.setText("");
                }

                Iterator<InetAddress> dnsIterator = linkProperties.getDnses().iterator();
                if (dnsIterator.hasNext()) {
                    mDns1View.setText(dnsIterator.next().getHostAddress());
                }
                if (dnsIterator.hasNext()) {
                    mDns2View.setText(dnsIterator.next().getHostAddress());
                }
            }
        } else {
            mView.findViewById(R.id.staticip).setVisibility(View.GONE);
        }
    }

    private void showProxyFields(EthernetInfo info) {

        mView.findViewById(R.id.proxy_settings_fields).setVisibility(View.VISIBLE);

        if (mProxySettingsSpinner.getSelectedItemPosition() == PROXY_STATIC) {
            mView.findViewById(R.id.proxy_warning_limited_support).setVisibility(View.VISIBLE);
            mView.findViewById(R.id.proxy_fields).setVisibility(View.VISIBLE);
            if (mProxyHostView == null) {
                mProxyHostView = (TextView) mView.findViewById(R.id.proxy_hostname);
                mProxyHostView.addTextChangedListener(this);
                mProxyPortView = (TextView) mView.findViewById(R.id.proxy_port);
                mProxyPortView.addTextChangedListener(this);
                mProxyExclusionListView = (TextView) mView.findViewById(R.id.proxy_exclusionlist);
                mProxyExclusionListView.addTextChangedListener(this);
            }
            if (info != null) {
                ProxyProperties proxyProperties = info.getLinkProperties().getHttpProxy();
                if (proxyProperties != null) {
                    mProxyHostView.setText(proxyProperties.getHost());
                    mProxyPortView.setText(Integer.toString(proxyProperties.getPort()));
                    mProxyExclusionListView.setText(proxyProperties.getExclusionList());
                }
            }
        } else {
            mView.findViewById(R.id.proxy_warning_limited_support).setVisibility(View.GONE);
            mView.findViewById(R.id.proxy_fields).setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View view) {
        //TODO: Complete onClick method body
        if (view.getId() == R.id.ethernet_advanced_togglebox) {
            if (((CheckBox) view).isChecked()) {
                mView.findViewById(R.id.ethernet_advanced_fields).setVisibility(View.VISIBLE);
            } else {
                mView.findViewById(R.id.ethernet_advanced_fields).setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        //TODO: Complete onItemSelected method body
        EthernetInfo info = mEthernetManager.getCurrentInterface();
        if (parent == mProxySettingsSpinner) {
            showProxyFields(info);
        } else {
            showIpConfigFields(info);
        }
        enableSubmitIfAppropriate();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // work done in afterTextChanged
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // work done in afterTextChanged
    }

    @Override
    public void afterTextChanged(Editable s) {
        mTextViewChangedHandler.post(new Runnable() {
            public void run() {
                enableSubmitIfAppropriate();
            }
        });
    }

    private boolean ipAndProxyFieldsAreValid() {
        mLinkProperties.clear();

        mIpAssignment = (mIpSettingsSpinner != null &&
                mIpSettingsSpinner.getSelectedItemPosition() == STATIC_IP) ?
                IpAssignment.STATIC : IpAssignment.DHCP;

        EthernetInfo info = mEthernetManager.getCurrentInterface();
        if (info != null) {
            mLinkProperties.setInterfaceName(info.getName());
        }

        if (mIpAssignment == IpAssignment.STATIC) {
            int result = validateIpConfigFields(mLinkProperties);
            if (result != 0) {
                return false;
            }
        }

        mProxySettings = (mProxySettingsSpinner != null &&
                mProxySettingsSpinner.getSelectedItemPosition() == PROXY_STATIC) ?
                ProxySettings.STATIC : ProxySettings.NONE;

        if (mProxySettings == ProxySettings.STATIC && mProxyHostView != null) {
            String host = mProxyHostView.getText().toString();
            String portStr = mProxyPortView.getText().toString();
            String exclusionList = mProxyExclusionListView.getText().toString();
            int port = 0;
            int result = 0;
            try {
                port = Integer.parseInt(portStr);
                final Context context = mConfigUi.getContext();
                result = validate(context, host, portStr, exclusionList);
            } catch (NumberFormatException e) {
                result = R.string.proxy_error_invalid_port;
            }
            if (result == 0) {
                ProxyProperties proxyProperties= new ProxyProperties(host, port, exclusionList);
                mLinkProperties.setHttpProxy(proxyProperties);
            } else {
                return false;
            }
        }
        return true;
    }

    private int validateIpConfigFields(LinkProperties linkProperties) {
        if (mIpAddressView == null) return 0;

        String ipAddr = mIpAddressView.getText().toString();
        if (TextUtils.isEmpty(ipAddr)) return R.string.ethernet_ip_settings_invalid_ip_address;

        InetAddress inetAddr = null;
        try {
            inetAddr = NetworkUtils.numericToInetAddress(ipAddr);
        } catch (IllegalArgumentException e) {
            return R.string.ethernet_ip_settings_invalid_ip_address;
        }

        int networkPrefixLength = -1;
        try {
            networkPrefixLength = Integer.parseInt(mNetworkPrefixLengthView.getText().toString());
            if (networkPrefixLength < 0 || networkPrefixLength > 32) {
                return R.string.ethernet_ip_settings_invalid_network_prefix_length;
            }
            linkProperties.addLinkAddress(new LinkAddress(inetAddr, networkPrefixLength));
        } catch (NumberFormatException e) {
            // Set the hint as default after user types in ip address
            mNetworkPrefixLengthView.setText(mConfigUi.getContext().getString(
                    R.string.ethernet_network_prefix_length_hint));
        }

        String gateway = mGatewayView.getText().toString();
        if (TextUtils.isEmpty(gateway)) {
            try {
                // Extract a default gateway from IP address
                InetAddress netPart = NetworkUtils.getNetworkPart(inetAddr, networkPrefixLength);
                byte[] addr = netPart.getAddress();
                addr[addr.length-1] = 1;
                mGatewayView.setText(InetAddress.getByAddress(addr).getHostAddress());
            } catch (RuntimeException ee) {
            } catch (java.net.UnknownHostException u) {
            }
        } else {
            InetAddress gatewayAddr = null;
            try {
                gatewayAddr = NetworkUtils.numericToInetAddress(gateway);
            } catch (IllegalArgumentException e) {
                return R.string.ethernet_ip_settings_invalid_gateway;
            }
            linkProperties.addRoute(new RouteInfo(gatewayAddr));
        }

        String dns = mDns1View.getText().toString();
        InetAddress dnsAddr = null;

        if (TextUtils.isEmpty(dns)) {
            // If everything else is valid, provide hint as a default option
            mDns1View.setText(mConfigUi.getContext().getString(R.string.ethernet_dns1_hint));
        } else {
            try {
                dnsAddr = NetworkUtils.numericToInetAddress(dns);
            } catch (IllegalArgumentException e) {
                return R.string.ethernet_ip_settings_invalid_dns;
            }
            linkProperties.addDns(dnsAddr);
        }

        if (mDns2View.length() > 0) {
            dns = mDns2View.getText().toString();
            try {
                dnsAddr = NetworkUtils.numericToInetAddress(dns);
            } catch (IllegalArgumentException e) {
                Log.d(TAG, "Caught IllegalArgumentException during dns2 conversion to InetAddress");
                return R.string.ethernet_ip_settings_invalid_dns;
            }
            linkProperties.addDns(dnsAddr);
        }
        return 0;
    }

    private static int validate(final Context context, final String host,
            final String portStr, final String exclusionList) {
        String packageName = "com.android.settings";
        String className = "com.android.settings.ProxySelector";
        if (DBG) Log.d(TAG, "create package :" + packageName + " className :" + className);
        int result = -1;
        Class[] ptype = new Class[]{String.class, String.class, String.class};
        Object[] objArray = new Object[]{host, portStr, exclusionList};

        try {
            final Context ctx = getDynamicContext(context, packageName);
            Class cls = ctx.getClassLoader().loadClass(className);

            Method m = cls.getMethod("validate", ptype);
            Object object = m.invoke(null, objArray);
            result = ((Integer)object).intValue();
            if (DBG) Log.d(TAG, "validate result :" + result);

        } catch (NameNotFoundException e) {
            Log.w(TAG, "Got expected NameNotFoundException ", e);
        } catch (IllegalAccessException iae) {
            Log.w(TAG, "Got expected PackageAccess complaint", iae);
        } catch (InstantiationError ie) {
            Log.w(TAG, "Got Got expected InstantationError", ie);
        } catch (Exception ex) {
            Log.w(TAG, "Got unexpected MaybeAbstract failure", ex);
        }
        return result;
    }

    private static Context getDynamicContext(final Context context, final String packageName)
            throws NameNotFoundException {
        if (DBG) Log.d(TAG, "getDynamicContext packageName :" + packageName);
        return context.createPackageContext(packageName,
                Context.CONTEXT_IGNORE_SECURITY | Context.CONTEXT_INCLUDE_CODE);
    }

}
