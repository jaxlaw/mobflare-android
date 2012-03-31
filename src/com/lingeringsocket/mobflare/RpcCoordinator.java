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

import org.apache.http.*;
import org.apache.http.client.*;
import org.apache.http.client.entity.*;
import org.apache.http.client.methods.*;
import org.apache.http.entity.*;
import org.apache.http.impl.client.*;
import org.apache.http.message.*;

import org.json.*;

import android.content.*;
import android.location.*;
import android.net.http.*;
import android.os.*;
import android.util.*;

import java.util.*;
import java.io.*;
import java.net.*;

/**
 * Makes RPC calls (currently via HTTP+JSON) to the mobflare coordinator.
 */
class RpcCoordinator
{
    private static String LOGTAG = "RpcCoordinator";

    private Context context;
    private int errId;
    private static HttpClient httpClient;
    private HttpUriRequest abortableRequest;

    static 
    {
        // TODO:  client version info
        httpClient = AndroidHttpClient.newInstance("mobflare-android");
    }

    RpcCoordinator(Context context) 
    {
        this.context = context;
    }

    private String getServerUri()
    {
        return Prefs.getServerUri(context);
    }

    List<String> listFlares(Location location, float radius, int clientVersion)
        throws Exception
    {
        String uri = getServerUri() + "/list?latitude="
            + location.getLatitude() + "&longitude="
            + location.getLongitude() + "&radius="
            + radius + "&clientVersion="
            + clientVersion;
        HttpGet httpGet = new HttpGet(uri);
        try {
            HttpResponse httpResponse = execute(httpGet);
            if (httpResponse.getStatusLine().getStatusCode() == 403) {
                errId = R.string.need_upgrade;
                throw new RuntimeException("Obsolete client version");
            }
            String json =
                readInputStream(
                    httpResponse.getEntity().getContent());
            JSONTokener tokener = new JSONTokener(json);
            JSONArray jsonArray = new JSONArray(tokener);
            // sort flares by distance; fake a multimap just to deal
            // with the pathological case of equal distances,
            // which actually comes up during testing on
            // an emulator
            SortedMap<Double, List<String>> sortedMap =
                new TreeMap<Double, List<String>>();
            for (int i = 0; i < jsonArray.length(); ++i) {
                JSONObject jsonObj = jsonArray.getJSONObject(i);
                String name = jsonObj.getString("name");
                double distance = jsonObj.getDouble("km");
                List<String> list = sortedMap.get(distance);
                if (list == null) {
                    list = new ArrayList<String>();
                    sortedMap.put(distance, list);
                }
                list.add(name);
            }
            List<String> flareList = new ArrayList<String>();
            for (List<String> list : sortedMap.values()) {
                flareList.addAll(list);
            }
            return flareList;
        } catch (Exception ex) {
            Log.e(LOGTAG, "HTTP GET failed", ex);
            throw ex;
        }
    }

    private URI generateFlareUri(String flareName) throws Exception
    {
        return new URI(
            getServerUri() + "/flare/" + URLEncoder.encode(flareName, "UTF-8"));
    }

    private HttpResponse execute(HttpUriRequest request) throws Exception
    {
        errId = R.string.server_error;
        httpClient.getConnectionManager().closeExpiredConnections();
        try {
            synchronized(this) {
                abortableRequest = request;
            }
            return httpClient.execute(request);
        } finally {
            synchronized(this) {
                abortableRequest = null;
            }
        }
    }
    
    JSONObject getFlare(String flareName) throws Exception
    {
        HttpGet httpGet =
            new HttpGet(generateFlareUri(flareName));
        try {
            HttpResponse httpResponse = execute(httpGet);
            if (httpResponse.getStatusLine().getStatusCode() == 404) {
                return null;
            }
            String json =
                readInputStream(
                    httpResponse.getEntity().getContent());
            JSONTokener tokener = new JSONTokener(json);
            return new JSONObject(tokener);
        } catch (Exception ex) {
            Log.e(LOGTAG, "HTTP GET failed", ex);
            throw ex;
        }
    }

    int joinFlare(String flareName) throws Exception
    {
        HttpPost httpPost =
            new HttpPost(generateFlareUri(flareName));
        try {
            HttpResponse httpResponse = execute(httpPost);
            if (httpResponse.getStatusLine().getStatusCode() == 404) {
                errId = R.string.invalid_flare;
                throw new RuntimeException("Flare expired");
            }
            String json =
                readInputStream(
                    httpResponse.getEntity().getContent());
            JSONTokener tokener = new JSONTokener(json);
            JSONObject jsonObj = new JSONObject(tokener);
            return jsonObj.getInt(FlareTimer.PARTICIPANT_NUMBER);
        } catch (Exception ex) {
            Log.e(LOGTAG, "HTTP POST failed", ex);
            throw ex;
        }
    }

    String createFlare(String name, Bundle props)
        throws Exception
    {
        HttpPut httpPut =
            new HttpPut(generateFlareUri(name));
        try {
            JSONObject jsonObj = new JSONObject();
            for (String key : props.keySet()) {
                jsonObj.put(key, props.get(key));
            }
            httpPut.setEntity(new StringEntity(jsonObj.toString()));
            HttpResponse httpResponse = execute(httpPut);
            if (httpResponse.getStatusLine().getStatusCode() == 409) {
                errId = R.string.duplicate_flare_name;
                throw new RuntimeException("Flare name already in use");
            }
            String json =
                readInputStream(httpResponse.getEntity().getContent());
            JSONTokener tokener = new JSONTokener(json);
            jsonObj = new JSONObject(tokener);
            return jsonObj.getString("name");
        } catch (Exception ex) {
            Log.e(LOGTAG, "HTTP PUT failed", ex);
            throw ex;
        }
    }

    synchronized void cancel()
    {
        if (abortableRequest != null) {
            abortableRequest.abort();
        }
    }
    
    private static String readInputStream(InputStream is) throws IOException
    {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[16384];
        
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        is.close();
        return new String(buffer.toByteArray());
    }

    int getErrorId() 
    {
        return errId;
    }
}
