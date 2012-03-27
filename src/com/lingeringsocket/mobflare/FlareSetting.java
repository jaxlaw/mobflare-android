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
import android.os.*;
import android.text.*;
import android.text.method.*;
import android.util.*;
import android.view.*;
import android.widget.*;

import com.michaelnovakjr.numberpicker.*;

/**
 * Interacts with the user to define properties of a new flare.
 */
public class FlareSetting extends Activity
{
    private TextView descText;
    private TextView hintText;
    private NumberPicker numberPicker;
    private List<FlareProperty> props;
    private int iProp = 0;
    private int flareType;

    private FlareProperty addProperty(
        int descResource, int hintResource, String intentKey, int defaultVal)
    {
        FlareProperty prop = new FlareProperty();
        prop.descResource = descResource;
        prop.hintResource = hintResource;
        prop.intentKey = intentKey;
        prop.defaultVal = defaultVal;
        props.add(prop);
        return prop;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.flare_setting);
        numberPicker = (NumberPicker)
            findViewById(R.id.num_participants);
        descText = (TextView)
            findViewById(R.id.flare_prop_text);
        hintText = (TextView)
            findViewById(R.id.flare_prop_hint);

        flareType = getIntent().getIntExtra(FlareTimer.FLARE_TYPE, 0);
        
        props = new ArrayList<FlareProperty>();
        addProperty(
            R.string.number_of_participants,
            R.string.number_of_participants_hint,
            FlareTimer.QUORUM_SIZE,
            1);
        addProperty(
            R.string.countdown_seconds,
            R.string.countdown_seconds_hint,
            FlareTimer.COUNTDOWN_SECONDS,
            10);
        if (flareType == R.id.flare_type_repeat) {
            addProperty(
                R.string.repeat_seconds,
                R.string.repeat_seconds_hint,
                FlareTimer.REPEAT_DECI_SECONDS,
                2);
        }
        if ((flareType == R.id.flare_type_wave)
            || (flareType == R.id.flare_type_wave_repeat))
        {
            addProperty(
                R.string.stagger,
                R.string.stagger_hint,
                FlareTimer.STAGGER_DECI_SECONDS,
                5);
        }

        initializePropView();
    }

    public void onNextClicked(View v)
    {
        FlareProperty modifiedProp = props.get(iProp);
        modifiedProp.val = numberPicker.getCurrent();
        ++iProp;
        if (iProp < props.size()) {
            initializePropView();
            return;
        } else {
            --iProp;
        }
        if (flareType == R.id.flare_type_wave_repeat) {
            FlareProperty prop = addProperty(
                R.string.repeat_seconds,
                R.string.repeat_seconds_hint,
                FlareTimer.REPEAT_DECI_SECONDS,
                0);

            // Note that we can arbitrarily set any non-zero value here;
            // the coordinator will determine the correct value based on the
            // number of participants and the stagger
            prop.val = 1;
        }
        Intent i = new Intent(this, FlareName.class);
        i.putExtras(getIntent());
        for (FlareProperty prop : props) {
            if (prop.intentKey.equals(FlareTimer.REPEAT_DECI_SECONDS)) {
                // user inputs seconds, but we want deciseconds
                prop.val *= 10;
            }
            i.putExtra(prop.intentKey, prop.val);
        }
        startActivity(i);
        finish();
    }

    private void initializePropView()
    {
        FlareProperty prop = props.get(iProp);

        numberPicker.setRange(1, 1000000, null);
        numberPicker.setWrap(false);
        numberPicker.setCurrent(prop.defaultVal);

        descText.setText(prop.descResource);
        hintText.setText(prop.hintResource);
    }
}
