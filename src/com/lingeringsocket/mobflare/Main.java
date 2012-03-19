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

import java.util.*;
import java.io.*;

import android.app.*;
import android.content.*;
import android.location.*;
import android.os.*;
import android.text.*;
import android.text.method.*;
import android.util.*;
import android.view.*;
import android.widget.*;

import org.json.*;

/**
 * Entry point in mobflare UI.
 */
public class Main extends Activity
{
    private static String LOGTAG = "Main";
    
    private List<String> flareList;
    private ArrayAdapter<String> arrayAdapter;
    private RpcCoordinator rpcCoordinator;
    private String emptyMessage;
    private RefreshLocationTask locationTask;
    private Location newFlareLocation;
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        emptyMessage = getString(R.string.empty_message);
        ListView flareListView = (ListView) findViewById(R.id.flare_list);
        flareList = new ArrayList<String>();
        arrayAdapter = new ArrayAdapter<String>(
            this, android.R.layout.simple_list_item_1,
            android.R.id.text1, flareList);
        flareListView.setAdapter(arrayAdapter);
        flareListView.setOnItemClickListener(new ListView.OnItemClickListener()
            {
                public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) 
                {
                    String flareName = flareList.get((int) id);
                    if (flareName.equals(emptyMessage)) {
                        onCreateClicked(view);
                        return;
                    }
                    Intent i = new Intent(Main.this, WaitFlare.class);
                    i.putExtra(FlareTimer.FLARE_NAME, flareName);
                    startActivity(i);
                }
            }
            );

        rpcCoordinator = new RpcCoordinator(this);
        refresh();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case R.id.refresh:
            refresh();
            return true;
        case R.id.about:
            displayAboutBox();
            return true;
        case R.id.help:
            displayHelpDialog();
            return true;
        case R.id.settings:
            displaySettings();
            return true;
        default:
            return false;
        }
    }

    private void refresh()
    {
        if (locationTask != null) {
            refreshFlares(locationTask.getLastLocation());
            return;
        }
        locationTask = new RefreshLocationTask();        
        Location location = locationTask.getLastLocation();
        if (location == null) {
            locationTask.setBlocking(true);
        } else {
            // kick off location update in background
            locationTask.setBlocking(false);
            refreshFlares(location);
        }
        locationTask.execute();
    }

    private void refreshFlares(Location location) 
    {
        if (location == null) {
            locationError();
            return;
        }
        newFlareLocation = location;
        RefreshTask refreshTask = new RefreshTask(location);
        refreshTask.setCoordinator(rpcCoordinator);
        refreshTask.execute();
    }

    private void locationError()
    {
        Toast toast = Toast.makeText(
            this,
            R.string.no_location,
            Toast.LENGTH_LONG);
        toast.show();
    }
    
    protected void displaySettings()
    {
        startActivity(new Intent(this, Prefs.class));
    }

    protected void displayAboutBox()
    {
        startActivity(new Intent(this, About.class));
    }

    protected void displayHelpDialog()
    {
        startActivity(new Intent(this, Help.class));
    }

    public void onCreateClicked(View v)
    {
        if (newFlareLocation == null) {
            locationError();
            return;
        }
        Intent i = new Intent(this, FlareType.class);
        i.putExtra(FlareTimer.LATITUDE, newFlareLocation.getLatitude());
        i.putExtra(FlareTimer.LONGITUDE, newFlareLocation.getLongitude());
        startActivity(i);
    }

    private class RefreshTask extends RpcTask<Void, Void, List<String>> 
    {
        private Location location;

        RefreshTask(Location location)
        {
            this.location = location;
        }
        
        @Override
        protected void onPreExecute()
        {
            progress = new ProgressDialog(Main.this);
            progress.setMessage(getString(R.string.flare_refresh));
            super.onPreExecute();
        }
        
        @Override
        protected List<String> executeCall(Void... v) throws Exception
        {
            return rpcCoordinator.listFlares(location);
        }

        @Override
        protected void onPostExecute(List<String> flares)
        {
            flareList.clear();
            if (flares.isEmpty()) {
                flareList.add(emptyMessage);
            } else {
                flareList.addAll(flares);
            }
            arrayAdapter.notifyDataSetChanged();
            progress.dismiss();
        }
    }

    private class RefreshLocationTask extends LocationTask 
    {
        RefreshLocationTask() 
        {
            super(Main.this);
        }

        protected void onPostExecute(Location result)
        {
            super.onPostExecute(result);
            if (isBlocking()) {
                refreshFlares(result);
            } else {
                newFlareLocation = result;
            }
            locationTask = null;
        }
    }
}
