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

package com.intel.internal.ethernet;


public class EthernetManager {
    private static final String PACKAGE = "com.android.internal.ethernet";

    /**
     * Broadcast when the NetworkInfo representing Ethernet as a whole changes.
     */
    public static final String NETWORK_STATE_CHANGED_ACTION =
                   PACKAGE + ".NETWORK_STATE_CHANGED_ACTION";

    /**
     * Broadcast when the NetworkInfo or EthernetInfo representing an
     * individual interface changes.
     */
    public static final String INTERFACE_STATE_CHANGED_ACTION =
                   PACKAGE + ".INTERFACE_STATE_CHANGED_ACTION";

    public static EthernetManager getEthernetManagerServiceInstance() {
        return null;
    }
}
