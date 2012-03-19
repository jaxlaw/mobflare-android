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
import android.view.inputmethod.*;
import android.widget.*;

/**
 * Assists user in choosing a flare name.
 */
public class FlareName extends Activity
{
    private static String LOGTAG = "FlareName";
    private EditText editText;
    private RpcCoordinator rpcCoordinator;

    private String [] alphabet = {
        "Alpha",
        "Bravo",
        "Charlie",
        "Delta",
        "Echo",
        "Foxtrot",
        "Golf",
        "Hotel",
        "India",
        "Juliet",
        "Kilo",
        "Lima",
        "Mike",
        "November",
        "Oscar",
        "Papa",
        "Quebec",
        "Romeo",
        "Sierra",
        "Tango",
        "Uniform",
        "Victor",
        "Whiskey",
        "X-ray",
        "Yankee",
        "Zulu"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.flare_name);
        editText = (EditText) findViewById(R.id.flare_name_entry);
        StringBuilder sb = new StringBuilder();
        Random r = new Random();
        int prev = -1;
        for (int i = 0; i < 3; ++i) {
            if (i > 0) {
                sb.append(" ");
            }
            int j = r.nextInt(26);
            if (j == prev) {
                // avoid repeats
                j += r.nextInt(25) + 1;
                j %= 26;
            }
            sb.append(alphabet[j]);
            prev = j;
        }
        editText.setText(sb.toString());
        rpcCoordinator = new RpcCoordinator(this);
    }

    public void onOkClicked(View v) 
    {
        CreateTask createTask = new CreateTask();
        createTask.setCoordinator(rpcCoordinator);
        createTask.execute();
    }

    private class CreateTask extends RpcTask<Void, Void, String>
    {
        @Override
        protected void onPreExecute()
        {
            progress = new ProgressDialog(FlareName.this);
            progress.setMessage(getString(R.string.creating_flare));
            super.onPreExecute();
        }
        
        @Override
        protected String executeCall(Void... v) throws Exception
        {
            String flareName = editText.getText().toString();
            rpcCoordinator.createFlare(
                flareName, getIntent().getExtras());
            return flareName;
        }

        @Override
        protected void onPostExecute(String flareName)
        {
            Intent i = new Intent(FlareName.this, WaitFlare.class);
            i.putExtra(FlareTimer.FLARE_NAME, flareName);
            startActivity(i);
            progress.dismiss();
            finish();
        }
    }
}
