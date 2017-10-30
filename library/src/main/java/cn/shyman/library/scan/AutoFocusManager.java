package cn.shyman.library.scan;

import android.hardware.Camera;
import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.RejectedExecutionException;

class AutoFocusManager implements Camera.AutoFocusCallback {
	private static final long AUTO_FOCUS_INTERVAL_MS = 1000L;
	private static final Collection<String> FOCUS_MODES_CALLING_AF;
	
	static {
		FOCUS_MODES_CALLING_AF = new ArrayList<>(2);
		FOCUS_MODES_CALLING_AF.add(Camera.Parameters.FOCUS_MODE_AUTO);
		FOCUS_MODES_CALLING_AF.add(Camera.Parameters.FOCUS_MODE_MACRO);
	}
	
	private final Camera mCamera;
	
	private boolean mIsStopped;
	private boolean mIsFocusing;
	private final boolean mIsUseAutoFocus;
	
	private AsyncTask<?, ?, ?> mOutstandingTask;
	
	AutoFocusManager(Camera camera, boolean isAutoFocus) {
		mCamera = camera;
		String currentFocusMode = camera.getParameters().getFocusMode();
		mIsUseAutoFocus = isAutoFocus && FOCUS_MODES_CALLING_AF.contains(currentFocusMode);
		start();
	}
	
	private synchronized void start() {
		if (mIsUseAutoFocus) {
			mOutstandingTask = null;
			if (!mIsStopped && !mIsFocusing) {
				try {
					mCamera.autoFocus(this);
					mIsFocusing = true;
				} catch (RuntimeException ignored) {
					autoFocusAgainLater();
				}
			}
		}
	}
	
	synchronized void stop() {
		mIsStopped = true;
		if (mIsUseAutoFocus) {
			cancelOutstandingTask();
			try {
				mCamera.cancelAutoFocus();
			} catch (RuntimeException ignored) {
			}
		}
	}
	
	private synchronized void cancelOutstandingTask() {
		if (mOutstandingTask != null) {
			if (mOutstandingTask.getStatus() != AsyncTask.Status.FINISHED) {
				mOutstandingTask.cancel(true);
			}
			mOutstandingTask = null;
		}
	}
	
	@Override
	public void onAutoFocus(boolean success, Camera camera) {
		mIsFocusing = false;
		autoFocusAgainLater();
	}
	
	private synchronized void autoFocusAgainLater() {
		if (!mIsStopped && mOutstandingTask == null) {
			AutoFocusTask newTask = new AutoFocusTask();
			try {
				newTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				mOutstandingTask = newTask;
			} catch (RejectedExecutionException ignored) {
			}
		}
	}
	
	private final class AutoFocusTask extends AsyncTask<Object, Object, Object> {
		@Override
		protected Object doInBackground(Object... voids) {
			try {
				Thread.sleep(AUTO_FOCUS_INTERVAL_MS);
			} catch (InterruptedException ignored) {
			}
			start();
			return null;
		}
	}
}
