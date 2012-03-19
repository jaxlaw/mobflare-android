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
import android.widget.*;

/**
 * Implements FlareOutput in an incredibly boring way by displaying a toast.
 * Useful for testing when you don't want to annoy people around you with
 * a real flash, or on emulators that crash when trying to load the camera.
 */
class ToastOutput implements FlareOutput
{
    private Activity activity;
    private Toast toast;
    
    @Override
    public void initialize(Activity activity)
    {
        this.activity = activity;
    }
    
    @Override
    public void beginOutput()
    {
        release();
        toast = Toast.makeText(activity, R.string.flash, Toast.LENGTH_SHORT);
        toast.show();
        
    }
    
    @Override
    public void endOutput()
    {
        release();
    }
    
    @Override
    public void release()
    {
        if (toast == null) {
            return;
        }
        toast.cancel();
        toast = null;
    }
}
