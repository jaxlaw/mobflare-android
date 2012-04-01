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

import java.io.*;

import android.app.*;
import android.hardware.*;
import android.view.*;

/**
 * Implements FlareOutput by blinking the camera's flash (without
 * taking any picture).
 */
class PureFlashOutput implements FlareOutput, SurfaceHolder.Callback
{
    private Activity activity;
    private Camera camera;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;

    @Override
    public void initialize(Activity activity)
    {
        this.activity = activity;

        surfaceView = (SurfaceView) activity.findViewById(R.id.surfaceview);
        SurfaceHolder holder = surfaceView.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        if (holder.getSurface() != null) {
            // If the surface was already created, the callback
            // won't be automatically invoked, so we have to do it
            // ourselves.
            surfaceCreated(holder);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        surfaceHolder = holder;
        accessCamera();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int i, int j, int k) {
        releaseCamera();
        surfaceHolder = holder;
        accessCamera();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        releaseCamera();
        surfaceHolder = null;
    }

    @Override
    public void beginOutput() 
    {
        setCameraFlash(Camera.Parameters.FLASH_MODE_TORCH);
    }
    
    @Override
    public void endOutput() 
    {
        setCameraFlash(Camera.Parameters.FLASH_MODE_OFF);
    }

    @Override
    public void release() 
    {
        releaseCamera();
    }

    private void setCameraFlash(String flashMode) 
    {
        accessCamera();
        if (camera == null) {
            return;
        }
        Camera.Parameters p = camera.getParameters();
        p.setFlashMode(flashMode);
        camera.setParameters(p);
    }
    
    private void accessCamera() {
        if (camera != null) {
            return;
        }
        if (surfaceHolder == null) {
            return;
        }
        camera = Camera.open();
        Camera.Parameters p = camera.getParameters();
        p.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        camera.setParameters(p);
        try {
            camera.setPreviewDisplay(surfaceHolder);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        camera.startPreview();
    }

    private void releaseCamera() 
    {
        if (camera != null) {
            camera.stopPreview();
            camera.setPreviewCallback(null);
            camera.release();
            camera = null;
        }
    }
}
