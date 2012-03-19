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
import android.graphics.drawable.*;
import android.os.*;
import android.text.*;
import android.text.method.*;
import android.util.*;
import android.view.*;
import android.widget.*;

/**
 * Asks user to select flare type.
 */
public class FlareType extends Activity
{
    private static String LOGTAG = "FlareType";
    private RadioGroup radioGroup;
    private GridView gridView;
    private ImageAdapter imageAdapter;
    private int currentId;
    private int iTick;
    private boolean suspended;
    private static Bitmap flareBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.flare_type);
        radioGroup = (RadioGroup) findViewById(R.id.flare_type_group);
        gridView = (GridView) findViewById(R.id.flaregrid);
        if (flareBitmap == null) {
            flareBitmap = BitmapFactory.decodeResource(
                getResources(), R.drawable.flare);
        }
        imageAdapter = new ImageAdapter(this);
        gridView.setAdapter(imageAdapter);
        currentId = radioGroup.getCheckedRadioButtonId();
        scheduleNextTick();
    }

    private Handler tickHandler = new Handler() 
        {
            @Override
            public void handleMessage(Message msg) {
                removeMessages(0);
                onTimerTick(msg.what);
            }
        };

    @Override
    protected void onResume()
    {
        super.onResume();
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
        // update every half second
        tickHandler.sendEmptyMessageDelayed(0, 500);
    }

    private void onTimerTick(int what)
    {
        if (suspended) {
            return;
        }
        ++iTick;
        imageAdapter.notifyDataSetChanged();
        scheduleNextTick();
    }

    public void onRadioButtonClicked(View v) 
    {
        currentId = v.getId();
        iTick = -1;
    }

    public void onNextClicked(View v) 
    {
        Intent i = new Intent(this, FlareSetting.class);
        i.putExtra(FlareTimer.FLARE_TYPE, currentId);
        i.putExtras(getIntent());
        startActivity(i);
        finish();
    }

    private class ImageAdapter extends BaseAdapter 
    {
        private Context context;

        public ImageAdapter(Context context) {
            this.context = context;
        }

        public int getCount() {
            return 5;
        }

        public Object getItem(int position) {
            return null;
        }

        public long getItemId(int position) {
            return 0;
        }

        // create a new ImageView for each item referenced by the Adapter
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            if (convertView == null) {
                imageView = new ImageView(context);
                imageView.setLayoutParams(new GridView.LayoutParams(50, 50));
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setPadding(8, 8, 8, 8);
            } else {
                imageView = (ImageView) convertView;
            }
            switch (currentId) {
            case R.id.flare_type_once:
                imageView.setImageBitmap(flareBitmap);
                break;
            case R.id.flare_type_repeat:
                if ((iTick % 2) == 0) {
                    imageView.setImageDrawable(new ColorDrawable());
                } else {
                    imageView.setImageBitmap(flareBitmap);
                }
                break;
            case R.id.flare_type_wave:
                if (iTick < getCount() && ((iTick % getCount()) == position)) {
                    imageView.setImageBitmap(flareBitmap);
                } else {
                    imageView.setImageDrawable(new ColorDrawable());
                }
                break;
            case R.id.flare_type_wave_repeat:
                if ((iTick % getCount()) == position) {
                    imageView.setImageBitmap(flareBitmap);
                } else {
                    imageView.setImageDrawable(new ColorDrawable());
                }
                break;
            }
            return imageView;
        }
    }
    
}
