/*
 * Paperclickers - Affordable solution for classroom response system.
 * 
 * Copyright (C) 2015 Jomara Bindá <jbinda@dca.fee.unicamp.br>
 * Copyright (C) 2015-2016 Eduardo Valle Jr <dovalle@dca.fee.unicamp.br>
 * Copyright (C) 2015-2016 Eduardo Seiti de Oliveira <eduseiti@dca.fee.unicamp.br>
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *   
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *   
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 * 
 */

package com.paperclickers.camera;

import java.util.List;

import com.paperclickers.log;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;


@SuppressLint("WrongCall")
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

	private static String TAG = "paperclickers.CameraPreview";
	
	private Camera mCamera;
	private Context mContext;

	private SurfaceHolder mSurfaceHolder;
	private CameraChangeListener mCameraListener;
	
	private boolean mHasStartedCamera = false;

	
	
	public CameraPreview(Context mContext, Camera camera, CameraChangeListener cameraListener) {
		super(mContext);
		mCamera = camera;
		this.mContext = mContext;

	    mSurfaceHolder = this.getHolder();
		mSurfaceHolder.addCallback(this);
		mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		
		mCameraListener = cameraListener;
	}

	
	
	public void focusCamera() {
	    
		if (mCamera != null) {

		    Camera.Parameters parameters = mCamera.getParameters();
	        
			if (parameters.getFocusMode().equals(Camera.Parameters.FOCUS_MODE_AUTO)) {
				log.d(TAG, "!!!!! AUTOFOCUS !!!!!");
				
				mCamera.autoFocus(null);
			} else {
				log.d(TAG, "!!!!! NO AUTOFOCUS !!!!!");				
			}
		}
	}

    
    
    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h, boolean hasRotated) {
        
        final double ASPECT_TOLERANCE = 0.05;
        double targetRatio = (double) w / h;
        if (sizes == null)
            return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            
            double ratio = hasRotated ? (double) size.height / size.width : (double) size.width / size.height;
            
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    
    
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {

        log.d(TAG, String.format("surfaceChanged: %d x %d. mHasStartedCamera: %b", w, h, mHasStartedCamera));
        
        if (mSurfaceHolder.getSurface() == null) {
            return;
        }

        if (!mHasStartedCamera) {
            if (mCamera == null) {
                mCamera = Camera.open();
            }

            mHasStartedCamera = true;
            
        } else {
            
            try {
                mCamera.stopPreview();
            } catch (Exception e) {
                // ignore: tried to stop a non-existent preview
            }
        }
        
        try {
            Parameters parameters   = mCamera.getParameters();
            List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();

            for (Camera.Size size : sizes) {
                log.d(TAG, String.format(">>>> %d x %d", size.width, size.height));
            }
            
            WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
            int displayRotation =  windowManager.getDefaultDisplay().getRotation();
            boolean hasRotated = false;
            
            if ((displayRotation == Surface.ROTATION_0) || (displayRotation == Surface.ROTATION_180)) {
                hasRotated = true;
            }
            
            
            Camera.Size optimalSize = getOptimalPreviewSize(sizes, w, h, hasRotated);
            
            log.d(TAG, "optimalSize:" + optimalSize.width + "," + optimalSize.height);
            
            parameters.setPreviewSize(optimalSize.width, optimalSize.height);
            
            if (hasRotated) {
                parameters.setRotation(90);
            }

            mCamera.setParameters(parameters);

            mCamera.setPreviewCallback((PreviewCallback) mContext);
            mCamera.setPreviewDisplay(mSurfaceHolder);
            mCamera.startPreview();
            
            if (hasRotated) {
                mCameraListener.updateChangedCameraSize(optimalSize.height, optimalSize.width);
            } else {
                mCameraListener.updateChangedCameraSize(optimalSize.width, optimalSize.height);                
            }
            
        } catch (Exception e) {
            log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }
	
	
	
	@Override
	public void surfaceCreated(SurfaceHolder surfaceHolder) {

		log.d(TAG, "surfaceCreated. Wait for the first surfaceChanged to get the final surface size");
	}

	
	
	@Override
	public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

		log.d(TAG, "surfaceDestroyed. We might have to do something if this is received...");
	}
}
