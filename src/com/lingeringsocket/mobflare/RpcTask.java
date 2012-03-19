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
import android.widget.*;

/**
 * Extends AsyncTask with progress and exception handling specific
 * to REST server calls.
 */
abstract class RpcTask<Params, Progress, Result>
    extends AsyncTask<Params, Progress, Result>
{
    private RpcCoordinator rpcCoordinator;
    protected ProgressDialog progress;
    private Exception ex;

    void setCoordinator(RpcCoordinator rpcCoordinator)
    {
        this.rpcCoordinator = rpcCoordinator;
    }
    
    @Override
    protected void onPreExecute()
    {
        if (progress != null) {
            progress.setIndeterminate(true);
            progress.setCancelable(false);
            progress.show();
        }
    }

    @Override
    protected Result doInBackground(Params... v)
    {
        try {
            return executeCall(v);
        } catch (Exception ex) {
            this.ex = ex;
            cancel(false);
            return null;
        }
    }

    Exception getException() 
    {
        return ex;
    }
    
    protected abstract Result executeCall(Params... v) throws Exception;

    @Override
    protected void onCancelled() 
    {
        super.onCancelled();
        if (progress != null) {
            int errId = R.string.server_error;
            if (rpcCoordinator != null) {
                errId = rpcCoordinator.getErrorId();
            }
            Toast toast = Toast.makeText(
                progress.getContext(), errId,
                Toast.LENGTH_LONG);
            progress.dismiss();
            toast.show();
        }
    }
}
