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

import android.app.*;
import android.content.*;
import android.net.*;
import android.os.*;
import android.view.*;

public class About extends Activity implements View.OnClickListener
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);

        View okButton = findViewById(R.id.about_ok);
        okButton.setOnClickListener(this);
        View projectUrl = findViewById(R.id.project_url);
        projectUrl.setOnClickListener(this);
    }

    public void onClick(View v)
    {
        switch (v.getId()) {
        case R.id.about_ok:
            finish();
            break;
        case R.id.project_url:
            openProjectUrlInBrowser();
            break;
        }
    }

    private void openProjectUrlInBrowser()
    {
        Uri uri = Uri.parse(getString(R.string.about_project_url));
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }
}
