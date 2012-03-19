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

import android.os.*;
import android.app.*;
import android.content.*;
import android.location.*;
import android.widget.*;
import android.util.*;

/**
 * Subclasses AsyncTask to get GPS location.
 */
class LocationTask extends AsyncTask<Void, Void, Location>
{
    private static String LOGTAG = "LocationTask";

    private LocationManager locationManager;
    private String provider;
    private ProgressDialog progress;
    private Activity activity;
    private Object monitor;
    private Location location;
    private boolean done;
    private boolean blocking;

    LocationTask(Activity activity)
    {
        this.activity = activity;
        locationManager = (LocationManager)
            activity.getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setSpeedRequired(false);
        criteria.setCostAllowed(true);
        provider = locationManager.getBestProvider(criteria, true);
        location = locationManager.getLastKnownLocation(provider);
    }

    Location getLastLocation()
    {
        return location;
    }
    
    @Override
    protected void onPreExecute()
    {
        ListenerCallback listenerCallback = new ListenerCallback();

        if (blocking) {
            progress = new ProgressDialog(activity);
            progress.setMessage(
                activity.getString(R.string.obtaining_location));
            progress.setIndeterminate(true);
            progress.setCancelable(false);
            progress.show();
        }

        monitor = new Object();
        locationManager.requestSingleUpdate(provider, listenerCallback, null);
    }

    @Override
    protected Location doInBackground(Void... params)
    {
        synchronized (monitor) {
            while (!done) {
                try {
                    monitor.wait();
                } catch (InterruptedException ex) {
                    break;
                }
            }
        }
        return location;
    }

    protected void onPostExecute(Location result)
    {
        if (progress != null) {
            progress.dismiss();
            progress = null;
        }
    }

    void notifyUnavailable()
    {
        synchronized (monitor) {
            done = true;
            monitor.notifyAll();
        }
    }

    void setBlocking(Boolean blocking) 
    {
        this.blocking = blocking;
    }
    
    protected boolean isBlocking() 
    {
        return blocking;
    }
    
    public class ListenerCallback implements LocationListener
    {
        @Override
        public void onLocationChanged(Location location)
        {
            synchronized (monitor) {
                LocationTask.this.location = location;
                done = true;
                monitor.notifyAll();
            }
        }

        @Override
        public void onProviderEnabled(String provider)
        {
        }

        @Override
        public void onProviderDisabled(String provider)
        {
            notifyUnavailable();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras)
        {
            if (status != LocationProvider.AVAILABLE) {
                notifyUnavailable();
            }
        }
    }
}
