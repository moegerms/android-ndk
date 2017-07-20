/*
 * Copyright (C) 2017 The Android Open Source Project
 *
* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sample.textureview;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.view.Display;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import junit.framework.Assert;
import junit.framework.Test;

public class ViewActivity extends Activity
		implements TextureView.SurfaceTextureListener,
		ActivityCompat.OnRequestPermissionsResultCallback {
	private  TextureView textureView_;
	Surface  surface_ = null;
	private Rect cameraPreviewRect_;
    private  int cameraOrientation_;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		onWindowFocusChanged(true);
		setContentView(R.layout.activity_main);
		RequestCamera();
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus)
	{
		super.onWindowFocusChanged(hasFocus);
		if(hasFocus)
		{
			getWindow().getDecorView().setSystemUiVisibility(
					View.SYSTEM_UI_FLAG_LAYOUT_STABLE
					| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
					| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
					| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
					| View.SYSTEM_UI_FLAG_FULLSCREEN
					| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
		}
	}

	private void CreateTextureView() {
		textureView_ = (TextureView)findViewById(R.id.texturePreview);
		textureView_.setSurfaceTextureListener(this);
	}

	public void onSurfaceTextureAvailable(SurfaceTexture surface,
										  int width, int height) {
		CreatePreviewEngine();

		resizeTextureView(width, height);
		surface.setDefaultBufferSize(cameraPreviewRect_.width(), cameraPreviewRect_.height());
        surface_ = new Surface(surface);
		notifySurfaceTextureCreated(surface_);
	}

	private void resizeTextureView(int textureWidth, int textureHeight)
	{
		int rotation = getWindowManager().getDefaultDisplay().getRotation();
		int newWidth = textureWidth;
		int newHeight = textureWidth * cameraPreviewRect_.width() / cameraPreviewRect_.height();

		if(Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
			newHeight = (textureWidth * cameraPreviewRect_.height()) / cameraPreviewRect_.width();
		}
		textureView_.setLayoutParams(
					new FrameLayout.LayoutParams(newWidth, newHeight, Gravity.CENTER));
		configureTransform(newWidth, newHeight);
	}

	/**
	 * configureTransform()
	 *     Courtesy to https://github.com/google/cameraview/blob/master/library/src/main/api14/com/google/android/cameraview/TextureViewPreview.java#L108
	 * @param width TextureView width
	 * @param height is TextureView height
	 */
	void configureTransform(int width, int height) {
		int mDisplayOrientation = getWindowManager().getDefaultDisplay().getRotation() * 90;
		Matrix matrix = new Matrix();
		if (mDisplayOrientation % 180 == 90) {
			//final int width = getWidth();
			//final int height = getHeight();
			// Rotate the camera preview when the screen is landscape.
			matrix.setPolyToPoly(
					new float[]{
							0.f, 0.f, // top left
							width, 0.f, // top right
							0.f, height, // bottom left
							width, height, // bottom right
					}, 0,
					mDisplayOrientation == 90 ?
							// Clockwise
							new float[]{
									0.f, height, // top left
									0.f, 0.f, // top right
									width, height, // bottom left
									width, 0.f, // bottom right
							} : // mDisplayOrientation == 270
							// Counter-clockwise
							new float[]{
									width, 0.f, // top left
									width, height, // top right
									0.f, 0.f, // bottom left
									0.f, height, // bottom right
							}, 0,
					4);
		} else if (mDisplayOrientation == 180) {
			matrix.postRotate(180, width / 2, height/2); // getWidth() / 2, getHeight() / 2);
		}
		textureView_.setTransform(matrix);
	}

	public void onSurfaceTextureSizeChanged(SurfaceTexture surface,
											int width, int height) {}

	public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
		notifySurfaceTextureDestroyed(surface_);
		surface_ = null;
		return true;
	}

	public void onSurfaceTextureUpdated(SurfaceTexture surface) {
	}

	private static final int PERMISSION_REQUEST_CODE_CAMERA = 1;
	public void RequestCamera() {
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
				PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(
					this,
					new String[] { Manifest.permission.CAMERA },
					PERMISSION_REQUEST_CODE_CAMERA);
			return;
		}
		CreateTextureView();
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
										   @NonNull String[] permissions,
										   @NonNull int[] grantResults) {
        /*
         * if any permission failed, the sample could not play
         */
		if (PERMISSION_REQUEST_CODE_CAMERA != requestCode) {
			super.onRequestPermissionsResult(requestCode,
					permissions,
					grantResults);
			return;
		}

		Assert.assertEquals(grantResults.length, 1);
		if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
			CreateTextureView();
		}
	}

	private void CreatePreviewEngine() {
		int rotation = 90 * ((WindowManager)(getSystemService(WINDOW_SERVICE)))
					.getDefaultDisplay()
					.getRotation();
		Display display = getWindowManager().getDefaultDisplay();
		int height = display.getMode().getPhysicalHeight();
		int width = display.getMode().getPhysicalWidth();
        CreateCamera(width, height, rotation);
		cameraPreviewRect_ = new Rect(0, 0,
				GetCameraCompatibleWidth(),
				GetCameraCompatibleHeight());
		cameraOrientation_ = GetCameraSensorOrientation();
	}

	private int GetTextureRotationAngle() {

		int rotation = this.getWindowManager().getDefaultDisplay().getRotation();
		int degrees = 0;

		switch(rotation){
			case Surface.ROTATION_0:
				degrees = 0;
				break;

			case Surface.ROTATION_90:
				degrees = 90;
				break;

			case Surface.ROTATION_180:
				degrees = 180;
				break;

			case Surface.ROTATION_270:
				degrees = 270;
				break;

		}

		int result;
		result = (cameraOrientation_ + degrees) % 360;
		result = (360 - result) % 360;

		return result;
	}

	/*
	 * Functions into NDK side to:
	 *     CreateCamera / DeleteCamera object
	 *     Start/Stop Preview
	 *     Pulling Camera Parameters
	 */
	private native void notifySurfaceTextureCreated(Surface surface);
	private native void notifySurfaceTextureDestroyed(Surface surface);

	private native long CreateCamera(int width, int height, int rotation);
	private native int  GetCameraCompatibleWidth();
	private native int  GetCameraCompatibleHeight();
    private native int  GetCameraSensorOrientation();

	static {
		System.loadLibrary("camera_textureview");
	}

}
