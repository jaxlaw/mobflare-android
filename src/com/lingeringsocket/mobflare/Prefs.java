/**
 * Copyright 2012 Lingering Socket Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lingeringsocket.mobflare;

import android.content.*;
import android.os.*;
import android.preference.*;

public class Prefs extends PreferenceActivity {
    private static final String SERVER_URI = "server_uri";
    private static final String SERVER_URI_DEFAULT =
        "http://rpc.mobflare.com:8080/mobflare";

    private static final String SEARCH_RADIUS = "search_radius";
    private static final String SEARCH_RADIUS_DEFAULT = "2.0";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
    }

    public static String getServerUri(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(
            SERVER_URI, SERVER_URI_DEFAULT);
    }

    public static void setServerUri(Context context, String serverUri) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(
            SERVER_URI, serverUri).commit();
    }

    public static float getSearchRadius(Context context) {
        return Float.valueOf(
            PreferenceManager.getDefaultSharedPreferences(context).getString(
                SEARCH_RADIUS, SEARCH_RADIUS_DEFAULT));
    }

    public static void setSearchRadius(Context context, float searchRadius) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putFloat(
            SEARCH_RADIUS, searchRadius).commit();
    }
}
