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
abstract class LocationTask extends AsyncTask<Void, Void, Location>
{
    private static String LOGTAG = "LocationTask";
    private static long TIMEOUT_MILLIS = 120000;

    private LocationManager locationManager;
    private String provider;
    private ProgressDialog progress;
    private Activity activity;
    private Object monitor;
    private Location location;
    private boolean done;
    private boolean blocking;
    private ListenerCallback listener;

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
        listener = new ListenerCallback();
    }

    Location getLastLocation()
    {
        return location;
    }
    
    @Override
    protected void onPreExecute()
    {
        if (blocking) {
            progress = new ProgressDialog(activity);
            progress.setMessage(
                activity.getString(R.string.obtaining_location));
            progress.setIndeterminate(true);
            progress.setCancelable(true);
            progress.setOnCancelListener(
                new DialogInterface.OnCancelListener()
                {
                    public void onCancel(DialogInterface dialog)
                    {
                        cancel(true);
                    }
                });
            progress.show();
        }

        monitor = new Object();
        locationManager.requestLocationUpdates(provider, 0, 0, listener);
    }

    @Override
    protected Location doInBackground(Void... params)
    {
        long expiry = System.currentTimeMillis() + TIMEOUT_MILLIS;
        synchronized (monitor) {
            while (!done) {
                try {
                    long delta = expiry - System.currentTimeMillis();
                    if (delta < 0) {
                        // give up
                        break;
                    }
                    monitor.wait(delta);
                } catch (InterruptedException ex) {
                    // someone requested cancel
                    break;
                }
            }
        }
        return location;
    }

    protected abstract void onLocationObtained(Location result, boolean isBogus);

    private void locationObtained(Location result)
    {
        boolean isBogus = false;
        if (result == null) {
            result = new Location(LocationManager.GPS_PROVIDER);
            isBogus = true;
        }
        onLocationObtained(result, isBogus);
    }
    
    protected void onPostExecute(Location result)
    {
        super.onPostExecute(result);
        if (progress != null) {
            progress.dismiss();
            progress = null;
        }
        locationManager.removeUpdates(listener);
        locationObtained(result);
    }

    protected void onCancelled()
    {
        locationManager.removeUpdates(listener);
        locationObtained(null);
    }
    
    void notifyUnavailable()
    {
        locationManager.removeUpdates(listener);
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
                locationManager.removeUpdates(this);
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
