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
import android.graphics.*;
import android.hardware.*;
import android.media.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.widget.*;

/**
 * Displays a countdown timer and invokes the flare output when the
 * timer expires.
 */
public class FlareTimer extends Activity
{
    private static String LOGTAG = "FlareTimer";
    
    protected static final String QUORUM_SIZE = "quorumSize";
    protected static final String JOIN_COUNT = "joinCount";
    protected static final String COUNTDOWN_SECONDS = "countdownSeconds";
    protected static final String REPEAT_DECI_SECONDS = "repeatDeciSeconds";
    protected static final String STAGGER_DECI_SECONDS = "staggerDeciSeconds";
    protected static final String COUNTDOWN_START_TIME = "countdownStartTime";
    protected static final String FLARE_NAME = "flareName";
    protected static final String FLARE_TYPE = "flareType";
    protected static final String PARTICIPANT_NUMBER = "participantNumber";
    protected static final String LATITUDE = "latitude";
    protected static final String LONGITUDE = "longitude";

    private int countdownSecondsRemaining;
    private int repeatDeciSeconds;
    private int staggerDeciSeconds;
    private int participantNumber;

    private PowerManager.WakeLock wakeLock;

    private long countdownStartTime;
    private long nextOutputTime;

    private boolean flashed;
    private String flareName;
    private FlareOutput output;
    private boolean suspended;

    private TextView countdownView;
    
    private Handler tickHandler = new Handler() 
        {
            @Override
            public void handleMessage(Message msg) {
                removeMessages(0);
                onTimerTick();
            }
        };
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.timer);

        countdownView =
            (TextView) findViewById(R.id.total_time_remaining);
        
        readIntent();
        updateDisplay();

        output = new PureFlashOutput();
        output.initialize(this);
        flashed = false;
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        acquireWakeLock();
        suspended = false;
        scheduleNextTick();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        releaseWakeLock();
        output.release();
        suspended = true;
    }

    public void onQuitClicked(View v)
    {
        finish();
    }

    private void readIntent()
    {
        int countdownSeconds = getIntent().getIntExtra(COUNTDOWN_SECONDS, 0);
        countdownStartTime =
            getIntent().getLongExtra(COUNTDOWN_START_TIME,
                System.currentTimeMillis());
        repeatDeciSeconds = getIntent().getIntExtra(REPEAT_DECI_SECONDS, 0);
        staggerDeciSeconds = getIntent().getIntExtra(STAGGER_DECI_SECONDS, 0);
        participantNumber = getIntent().getIntExtra(PARTICIPANT_NUMBER, 0);
        nextOutputTime = countdownStartTime + countdownSeconds * 1000L
            + participantNumber * staggerDeciSeconds * 100L;
        flareName = getIntent().getStringExtra(FLARE_NAME);
        TextView flareNameView = (TextView) findViewById(R.id.flare_name);
        flareNameView.setText(flareName);
    }

    public static String formatTime(int seconds)
    {
        return Integer.toString(seconds / 60) + ":" + padWithZeros(seconds % 60);
    }

    private static String padWithZeros(int seconds)
    {
        return seconds < 10 ? "0" + seconds : Integer.toString(seconds);
    }

    protected void updateDisplay()
    {
        countdownView.setText(formatTime(countdownSecondsRemaining));
    }

    private void scheduleNextTick()
    {
        // fiddle with the delay to try to get everyone synchronized
        // on tenths of a second
        long diff = nextOutputTime - System.currentTimeMillis();
        if (diff < 0) {
            diff = 0;
        }
        diff %= 100;
        long delay;
        if (diff < 50) {
            delay = 100 - diff;
        } else {
            delay = diff;
        }
        
        tickHandler.sendEmptyMessageDelayed(0, delay);
    }

    private void onTimerTick()
    {
        if (suspended) {
            return;
        }
        boolean cancelTimer = false;
        long remainingMillis =
            nextOutputTime - System.currentTimeMillis();
        if (remainingMillis < 0) {
            remainingMillis = 0;
        }
        countdownSecondsRemaining = (int) (remainingMillis / 1000L);
        if (countdownSecondsRemaining == 0) {
            if (flashed) {
                output.endOutput();
                if (repeatDeciSeconds == 0) {
                    cancelTimer = true;
                }
            } else {
                output.beginOutput();
                flashed = true;
            }
        }
        if ((remainingMillis == 0) && (repeatDeciSeconds != 0)) {
            nextOutputTime += repeatDeciSeconds * 100;
            flashed = false;
        }
        updateDisplay();
        if (!cancelTimer) {
            scheduleNextTick();
        }
    }

    private void acquireWakeLock()
    {
        if (wakeLock == null) {
            PowerManager pm =
                (PowerManager) getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(
                PowerManager.SCREEN_DIM_WAKE_LOCK,
                getClass().getCanonicalName());
            wakeLock.acquire();
        }
    }

    private void releaseWakeLock()
    {
        if ((wakeLock != null) && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }
    }
}
