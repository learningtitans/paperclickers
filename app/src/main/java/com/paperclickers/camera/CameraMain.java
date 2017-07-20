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
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import com.paperclickers.log;

/**
 * Real camera hardware Activity implementation - provides audience response with image frames from
 * camera preview.
 *
 * @author eduseiti, on 10/06/2017
 */

public class CameraMain extends CameraAbstraction implements Camera.PreviewCallback, CameraChangeListener {

	final static String TAG = "paperclickers.CameraMain";

	private Camera mCamera;
	private CameraPreview mCameraPreview;

	
	
	/** A safe way to get an instance of the Camera object. */
	public static Camera getCameraInstance() {
		Camera c = null;
		try {
			c = Camera.open(); // attempt to get a Camera instance

			log.d(TAG, "> getCameraInstance - size: " + c.getParameters().getPreviewSize());
			log.d(TAG, "> getCameraInstance - frame rate: " + c.getParameters().getPreviewFrameRate());
			
		} catch (Exception e) {
			// Camera is not available (in use or does not exist)
		    log.e(TAG, e.toString());
		}
		return c; // returns null if camera is unavailable
	}



	@Override
	protected void onPause() {
		super.onPause();

		releaseCamera();
	}

	
	
	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {

		onNewFrame(data);
	}

	
	
	@Override
	protected void onResume() {
		super.onResume();

		setCamera();
		setCameraPreview();
	}
	
	
	
	@Override
	protected void onStart() {
		super.onStart();

		setCamera();
	}
	
	
	
    @Override
    protected void onStop() {
        super.onStop();

        releaseCamera();
    }
	
	
	
	private void releaseCamera() {

		if (mCamera != null) {
			mCamera.stopPreview();
			mCamera.setPreviewCallback(null);
			mCamera.release();
			mCamera = null;
		}

		releaseTopCodesFeedbackPreview();

		if (mCameraPreview != null) {
			mPreview.removeView(mCameraPreview);

			mCameraPreview = null;
		}
	}


	
	private void setCamera() {

		if (mCamera == null) {
			try {
				mCamera = getCameraInstance();
				
				Camera.Parameters cameraParameters = mCamera.getParameters();
				
				Camera.Size cameraSize = cameraParameters.getPreviewSize();
				
				mImageWidth  = cameraSize.width;
				mImageHeight = cameraSize.height;
				
				log.d(TAG, String.format("Size %d x %d", mImageWidth, mImageHeight));
				
                List<String> supportedFocusModes = cameraParameters.getSupportedFocusModes();
                
                if (supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                    cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                } else {
                    cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                }
				
				mCamera.setParameters(cameraParameters);
				
				int displayRotation = getWindowManager().getDefaultDisplay().getRotation();
				int cameraRotation = 0;
				
				if ((displayRotation == Surface.ROTATION_0) || (displayRotation == Surface.ROTATION_180)) {
				    
				    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
				    
				    cameraRotation = 90;
				} else {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
				}
				
				if (cameraRotation != 0) {
				    
				    mCamera.setDisplayOrientation(cameraRotation);
				    
				    mImageWidth  = cameraSize.height;
				    mImageHeight = cameraSize.width;
				    
				    mHasRotated = true;
				    
				    log.d(TAG, String.format("Changing camera orientation (%d); width=%d, height=%d", cameraRotation, mImageWidth, mImageHeight));
				} else {
                    mHasRotated = false;
                }

				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
		
		
	
	private void setCameraPreview() {	
		
		if ((mCamera != null) && (mCameraPreview == null)) {
			
			mCameraPreview = new CameraPreview(this, mCamera, this);
			mPreview.addView(mCameraPreview);

			setTopCodesFeedbackPreview(false);

			mCameraPreview.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					mCameraPreview.focusCamera();
				}
			});

			mCameraPreview.setOnTouchListener(new TouchListener(mCameraPreview));
		}
	}

	

	public void updateChangedCameraSize(int width, int height) {
			 
        log.d(TAG, String.format("New camera size: %d x %d. Null camera reference? %b", width, height, (mCamera == null))); 
        
        if (width < height) {
            mHasRotated = true;
            
            // Camera preview always comes in landscape format...
            
            mImageWidth  = height;
            mImageHeight = width;
        } else {
            mHasRotated = false;

            mImageWidth  = width;
            mImageHeight = height;
        }

        if (mAudienceResponses == null) {
		} else {
			mAudienceResponses.setImageSize(mImageWidth, mImageHeight);
		}

        if (mDraw != null) {
            if (mHasRotated) {
                mDraw.updateScreenSize(mImageHeight, mImageWidth);
            } else {
                mDraw.updateScreenSize(mImageWidth, mImageHeight);
                
            }
        }		
	}
}
