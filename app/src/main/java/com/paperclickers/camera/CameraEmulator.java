/*
 * Paperclickers - Affordable solution for classroom response system.
 *
 * Copyright (C) 2015-2017 Eduardo Valle Jr <dovalle@dca.fee.unicamp.br>
 * Copyright (C) 2015-2017 Eduardo Seiti de Oliveira <eduseiti@dca.fee.unicamp.br>
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

import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;

import android.os.Bundle;
import android.view.Surface;
import android.view.TextureView;

import com.paperclickers.result.AnswersLog;

import java.io.IOException;

import com.paperclickers.log;

/**
 * Created by eduseiti on 11/06/17.
 */

public class CameraEmulator extends CameraAbstraction implements TextureView.SurfaceTextureListener {

    final static String TAG = "paperclickers.CameraEmulator";
    final static String TEST_VIDEO_FILENAME = "testVideo.mp4";


    MediaPlayer mMediaPlayer = null;
    TextureView mTextureView = null;

    boolean mVideoIsPrepared = false;
    boolean mActivityPauseIndicated = false;
    boolean mActivityStopIndicated  = false;

    int mData[] = null;



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        mTextureView = new TextureView(this);
        mTextureView.setSurfaceTextureListener(this);

        mTextureView.setOnTouchListener(new TouchListener(mTextureView));

        mPreview.addView(mTextureView);
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();

        log.d(TAG, "===> onDestroy()");

        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }



    @Override
    protected void onPause() {
        super.onPause();

        mActivityPauseIndicated = true;

        if ((mMediaPlayer != null) && (mMediaPlayer.isPlaying())){
            mMediaPlayer.pause();
        }
    }



    protected void onResume() {
        super.onResume();

        mActivityPauseIndicated = false;

        if (mVideoIsPrepared) {
            mMediaPlayer.start();
        }
    }



    @Override
    protected void onStart() {
        super.onStart();

        mActivityStopIndicated = false;

        if (mMediaPlayer != null) {
            mMediaPlayer.prepareAsync();
        }
    }



    @Override
    protected void onStop() {
        super.onStop();

        mActivityStopIndicated = true;

        if ((mMediaPlayer != null) && (mVideoIsPrepared)) {

            mMediaPlayer.stop();

            mVideoIsPrepared = false;

            releaseTopCodesFeedbackPreview();
        }
    }



    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {

        log.d(TAG, "===> onSurfaceTextureAvailable");

        mImageWidth  = width;
        mImageHeight = height;

        mAudienceResponses.setImageSize(mImageWidth, mImageHeight);

        mData = new int[mImageWidth * mImageHeight];

        Surface surface = new Surface(surfaceTexture);

        try {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setDataSource(AnswersLog.getPaperclickersFolder() + TEST_VIDEO_FILENAME);
            mMediaPlayer.setSurface(surface);
            mMediaPlayer.setLooping(true);

            mMediaPlayer.prepareAsync();

            mVideoIsPrepared = false;

            // Play video when the media source is ready for playback.
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {

                    log.d(TAG, "onPrepared");

                    if (mActivityStopIndicated) {
                        mediaPlayer.stop();
                    } else if (!mActivityPauseIndicated) {
                        mVideoIsPrepared = true;

                        mediaPlayer.start();

                        setTopCodesFeedbackPreview();
                    }
                }
            });

        } catch (IllegalArgumentException e) {
            log.d(TAG, e.getMessage());
        } catch (SecurityException e) {
            log.d(TAG, e.getMessage());
        } catch (IllegalStateException e) {
            log.d(TAG, e.getMessage());
        } catch (IOException e) {
            log.d(TAG, e.getMessage());
        }
    }



    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {

        log.d(TAG, "onSurfaceTextureSizeChanged");

    }



    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {

        log.d(TAG, "onSurfaceTextureDestroyed");

        return true;
    }



    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        log.d(TAG, "onSurfaceTextureUpdated");

        if (isFinishing()) {
            return;
        } else {
            Bitmap imageBitmap = mTextureView.getBitmap();

            imageBitmap.getPixels(mData, 0, mImageWidth, 0, 0, mImageWidth, mImageHeight);

            onNewFrame(mData);
        }
    }
}
