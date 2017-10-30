package cn.shyman.library.scan;

import android.content.Context;
import android.graphics.Point;
import android.hardware.Camera;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

class CameraConfigManager {
	private static final int MIN_PREVIEW_PIXELS = 480 * 320;
	
	/** 相机旋转角度 */
	int mCameraRotationAngle;
	/** 相机分辨率 */
	Point mCameraResolution;
	/** 预览分辨率 */
	Point mPreviewResolution;
	
	void initFromCameraParameters(Context context, OpenCamera openCamera) {
		Camera.Parameters cameraParameters = openCamera.getCamera().getParameters();
		WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		Display display = manager.getDefaultDisplay();
		
		int displayRotation = display.getRotation();
		int rotationFromNaturalToDisplay;
		switch (displayRotation) {
			case Surface.ROTATION_0:
				rotationFromNaturalToDisplay = 0;
				break;
			case Surface.ROTATION_90:
				rotationFromNaturalToDisplay = 90;
				break;
			case Surface.ROTATION_180:
				rotationFromNaturalToDisplay = 180;
				break;
			case Surface.ROTATION_270:
				rotationFromNaturalToDisplay = 270;
				break;
			default:
				if (displayRotation % 90 == 0) {
					rotationFromNaturalToDisplay = (360 + displayRotation) % 360;
				} else {
					throw new IllegalArgumentException("Bad rotation: " + displayRotation);
				}
		}
		
		int rotationFromNaturalToCamera = openCamera.getOrientation();
		
		if (openCamera.getFacing() == CameraFacing.FRONT) {
			rotationFromNaturalToCamera = (360 - rotationFromNaturalToCamera) % 360;
		}
		
		mCameraRotationAngle = (360 + rotationFromNaturalToCamera - rotationFromNaturalToDisplay) % 360;
		
		Point screenResolution = new Point();
		display.getSize(screenResolution);
		
		mCameraResolution = findBestPreviewSizeValue(cameraParameters, screenResolution);
		
		boolean isScreenPortrait = screenResolution.x < screenResolution.y;
		boolean isPreviewSizePortrait = mCameraResolution.x < mCameraResolution.y;
		
		if (isScreenPortrait == isPreviewSizePortrait) {
			mPreviewResolution = new Point(mCameraResolution.x, mCameraResolution.y);
		} else {
			// noinspection SuspiciousNameCombination
			mPreviewResolution = new Point(mCameraResolution.y, mCameraResolution.x);
		}
	}
	
	void setDesiredCameraParameters(OpenCamera openCamera, boolean safeMode) {
		Camera theCamera = openCamera.getCamera();
		Camera.Parameters cameraParameters = theCamera.getParameters();
		if (cameraParameters == null) {
			return;
		}
		
		String focusMode = null;
		if (!safeMode) {
			List<String> supportedFocusModes = cameraParameters.getSupportedFocusModes();
			focusMode = findSettableValue(supportedFocusModes, Camera.Parameters.FOCUS_MODE_AUTO);
		}
		if (focusMode != null) {
			cameraParameters.setFocusMode(focusMode);
		}
		
		cameraParameters.setPreviewSize(mCameraResolution.x, mCameraResolution.y);
		if (cameraParameters.isZoomSupported()) {
			cameraParameters.setZoom(cameraParameters.getMaxZoom() / 10);
		}
		theCamera.setParameters(cameraParameters);
		
		theCamera.setDisplayOrientation(mCameraRotationAngle);
		
		Camera.Parameters afterParameters = theCamera.getParameters();
		Camera.Size afterSize = afterParameters.getPreviewSize();
		if (afterSize != null &&
				(mCameraResolution.x != afterSize.width || mCameraResolution.y != afterSize.height)) {
			mCameraResolution.x = afterSize.width;
			mCameraResolution.y = afterSize.height;
		}
	}
	
	private static Point findBestPreviewSizeValue(Camera.Parameters parameters, Point screenResolution) {
		List<Camera.Size> rawSupportedSizes = parameters.getSupportedPreviewSizes();
		if (rawSupportedSizes == null) {
			Camera.Size defaultSize = parameters.getPreviewSize();
			if (defaultSize == null) {
				throw new IllegalStateException("Parameters contained no preview size!");
			}
			return new Point(defaultSize.width, defaultSize.height);
		}
		
		List<Camera.Size> supportedPreviewSizes = new ArrayList<>(rawSupportedSizes);
		Collections.sort(supportedPreviewSizes, new Comparator<Camera.Size>() {
			@Override
			public int compare(Camera.Size a, Camera.Size b) {
				int aPixels = a.height * a.width;
				int bPixels = b.height * b.width;
				// 高分辨率排前，低分辨率排后
				if (bPixels < aPixels) {
					return -1;
				}
				if (bPixels > aPixels) {
					return 1;
				}
				return 0;
			}
		});
		
		Iterator<Camera.Size> it = supportedPreviewSizes.iterator();
		while (it.hasNext()) {
			Camera.Size supportedPreviewSize = it.next();
			int realWidth = supportedPreviewSize.width;
			int realHeight = supportedPreviewSize.height;
			if (realWidth * realHeight < MIN_PREVIEW_PIXELS) {
				it.remove();
				continue;
			}
			
			boolean isCandidatePortrait = realWidth < realHeight;
			int maybeFlippedWidth = isCandidatePortrait ? realHeight : realWidth;
			int maybeFlippedHeight = isCandidatePortrait ? realWidth : realHeight;
			
			// 宽高比例一致
			if (maybeFlippedWidth == screenResolution.x && maybeFlippedHeight == screenResolution.y) {
				return new Point(realWidth, realHeight);
			}
		}
		
		// 宽高比例无法一致，取最优
		if (!supportedPreviewSizes.isEmpty()) {
			Camera.Size largestPreview = supportedPreviewSizes.get(0);
			return new Point(largestPreview.width, largestPreview.height);
		}
		
		// 支持列表为空，取默认值
		Camera.Size defaultPreview = parameters.getPreviewSize();
		if (defaultPreview == null) {
			throw new IllegalStateException("Parameters contained no preview size!");
		}
		return new Point(defaultPreview.width, defaultPreview.height);
	}
	
	private static String findSettableValue(Collection<String> supportedValues,
	                                        String... desiredValues) {
		if (supportedValues != null) {
			for (String desiredValue : desiredValues) {
				if (supportedValues.contains(desiredValue)) {
					return desiredValue;
				}
			}
		}
		return null;
	}
}
