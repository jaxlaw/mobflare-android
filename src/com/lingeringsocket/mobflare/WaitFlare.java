/**
 * Copyright 2012 The Mobflare Project
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
import android.os.*;
import android.text.*;
import android.text.method.*;
import android.util.*;
import android.view.*;
import android.widget.*;

import org.json.*;

/**
 * Displays status while waiting for participants to join.
 */
public class WaitFlare extends Activity
{
    private static String LOGTAG = "WaitFlare";

    private TextView quorumText;
    private TextView joinedText;
    private boolean suspended;
    private int participantNumber;
    private String flareName;
    private RpcCoordinator rpcCoordinator;
    private boolean needJoin;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wait_flare);
        quorumText = (TextView)
            findViewById(R.id.participants_required_num);
        joinedText = (TextView)
            findViewById(R.id.participants_joined_num);
        flareName = getIntent().getStringExtra(FlareTimer.FLARE_NAME);
        TextView flareNameView = (TextView) findViewById(R.id.flare_name);
        flareNameView.setText(flareName);
        rpcCoordinator = new RpcCoordinator(this);
        needJoin = true;
        onTimerTick();
    }

    private Handler tickHandler = new Handler() 
        {
            @Override
            public void handleMessage(Message msg) {
                removeMessages(0);
                onTimerTick();
            }
        };

    @Override
    protected void onResume()
    {
        super.onResume();
        // TODO:  reuse wakelock
        suspended = false;
        scheduleNextTick();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        suspended = true;
    }

    private void scheduleNextTick()
    {
        // poll every second
        tickHandler.sendEmptyMessageDelayed(0, 1000);
    }

    private void onTimerTick()
    {
        if (suspended) {
            return;
        }
        JoinTask joinTask = new JoinTask();
        joinTask.setCoordinator(rpcCoordinator);
        joinTask.execute(flareName);
    }

    public void onGiveUpClicked(View v)
    {
        finish();
    }

    private class JoinTask extends RpcTask<String, Void, JSONObject> 
    {
        @Override
        protected JSONObject executeCall(String ... flareNames) throws Exception
        {
            assert flareNames.length == 1;
            String flareName = flareNames[0];

            if (needJoin) {
                try {
                    participantNumber = rpcCoordinator.joinFlare(flareName);
                } catch (Exception ex) {
                    return null;
                }
                needJoin = false;
            }
            
            return rpcCoordinator.getFlare(flareName);
        }

        @Override
        protected void onPostExecute(JSONObject jsonObj)
        {
            try {
                handleResult(jsonObj);
            } catch (JSONException ex) {
                Log.wtf(LOGTAG, "JSON parse failed", ex);
            }
        }
        
        private void handleResult(JSONObject jsonObj) throws JSONException
        {
            if (jsonObj == null) {
                Toast toast = Toast.makeText(
                    WaitFlare.this, R.string.invalid_flare,
                    Toast.LENGTH_LONG);
                toast.show();
                finish();
                return;
            }

            int quorumSize =
                jsonObj.getInt(FlareTimer.QUORUM_SIZE);
            quorumText.setText(Integer.toString(quorumSize));
            int joinCount =
                jsonObj.getInt(FlareTimer.JOIN_COUNT);
            joinedText.setText(Integer.toString(joinCount));
            
            long countdownStartTime =
                jsonObj.getLong(FlareTimer.COUNTDOWN_START_TIME);
            if (countdownStartTime == 0) {
                scheduleNextTick();
                return;
            }

            Intent i = new Intent(WaitFlare.this, FlareTimer.class);
            i.putExtra(
                FlareTimer.REPEAT_DECI_SECONDS,
                jsonObj.getInt(FlareTimer.REPEAT_DECI_SECONDS));
            i.putExtra(
                FlareTimer.STAGGER_DECI_SECONDS,
                jsonObj.getInt(FlareTimer.STAGGER_DECI_SECONDS));
            i.putExtra(
                FlareTimer.COUNTDOWN_SECONDS,
                jsonObj.getInt(FlareTimer.COUNTDOWN_SECONDS));
            i.putExtra(
                FlareTimer.COUNTDOWN_START_TIME,
                countdownStartTime);
            i.putExtra(
                FlareTimer.PARTICIPANT_NUMBER,
                participantNumber);
            i.putExtra(
                FlareTimer.FLARE_NAME,
                flareName);
            startActivity(i);
            finish();
        }
    }
}
