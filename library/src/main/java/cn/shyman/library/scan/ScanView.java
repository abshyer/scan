package cn.shyman.library.scan;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;

import java.io.File;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

public class ScanView extends FrameLayout implements SurfaceHolder.Callback, Camera.PreviewCallback {
	private SurfaceView mPreviewView;
	private FinderView mFinderView;
	
	private CameraManager mCameraManager;
	private boolean mHasSurface;
	
	private DecodeTask mDecodeTask;
	private MultiFormatReader mMultiFormatReader;
	
	private OnScanListener mOnScanListener;
	
	public ScanView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		mFinderView = new FinderView(getContext());
		addView(mFinderView);
		
		mMultiFormatReader = new MultiFormatReader();
		Collection<BarcodeFormat> decodeFormats = EnumSet.noneOf(BarcodeFormat.class);
		decodeFormats.addAll(DecodeFormat.PRODUCT_FORMATS);
		decodeFormats.addAll(DecodeFormat.INDUSTRIAL_FORMATS);
		decodeFormats.addAll(DecodeFormat.QR_CODE_FORMATS);
		decodeFormats.addAll(DecodeFormat.DATA_MATRIX_FORMATS);
		decodeFormats.addAll(DecodeFormat.AZTEC_FORMATS);
		decodeFormats.addAll(DecodeFormat.PDF417_FORMATS);
		Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
		hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);
		hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
		mMultiFormatReader.setHints(hints);
	}
	
	public void setOnScanListener(OnScanListener onScanListener) {
		mOnScanListener = onScanListener;
	}
	
	public void decodeFile(File file, OnDecodeListener onDecodeListener) {
		DecodeInfo decodeInfo = new DecodeInfo();
		decodeInfo.file = file;
		
		if (mDecodeTask != null) {
			mDecodeTask.cancel(true);
		}
		
		mDecodeTask = new DecodeTask(this, mMultiFormatReader);
		mDecodeTask.setOnDecodeListener(onDecodeListener);
		mDecodeTask.execute(decodeInfo);
	}
	
	public void setFlashMode(boolean flashMode) {
		if (mCameraManager == null) {
			return;
		}
		mCameraManager.setFlashMode(flashMode);
	}
	
	public boolean isFlashMode() {
		return mCameraManager != null && mCameraManager.isFlashMode();
	}
	
	public void initScan() {
		mPreviewView = new SurfaceView(getContext());
		addView(mPreviewView, 0);
	}
	
	public void startScan() {
		if (mPreviewView == null) {
			return;
		}
		
		mCameraManager = new CameraManager(getContext());
		
		SurfaceHolder surfaceHolder = mPreviewView.getHolder();
		if (mHasSurface) {
			initCamera(surfaceHolder);
		} else {
			surfaceHolder.addCallback(this);
		}
	}
	
	public void stopScan() {
		if (mPreviewView == null) {
			return;
		}
		
		if (mCameraManager != null) {
			mCameraManager.closeDriver();
		}
		if (!mHasSurface) {
			SurfaceHolder surfaceHolder = mPreviewView.getHolder();
			surfaceHolder.removeCallback(this);
		}
	}
	
	private void initCamera(SurfaceHolder surfaceHolder) {
		if (surfaceHolder == null) {
			throw new IllegalStateException("No SurfaceHolder provided");
		}
		if (mCameraManager.isOpen()) {
			return;
		}
		
		try {
			mCameraManager.openDriver(surfaceHolder);
			requestLayout();
			mCameraManager.startPreview();
			mCameraManager.requestPreviewFrame(this);
		} catch (Exception e) {
		}
	}
	
	void requestPreview() {
		if (mDecodeTask != null) {
			mDecodeTask.cancel(true);
			mDecodeTask = null;
		}
		mCameraManager.requestPreviewFrame(this);
	}
	
	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		if (mDecodeTask != null && mDecodeTask.getStatus() == AsyncTask.Status.RUNNING) {
			return;
		}
		
		DecodeInfo decodeInfo = new DecodeInfo();
		decodeInfo.data = data;
		decodeInfo.dataWidth = mCameraManager.getCameraResolution().x;
		decodeInfo.dataHeight = mCameraManager.getCameraResolution().y;
		decodeInfo.rotationAngle = mCameraManager.getCameraRotationAngle();
		Rect finderRect = mFinderView.getFinderRect();
		if (finderRect != null) {
			Rect decodeRect = new Rect();
			int widthOffset = (mPreviewView.getMeasuredWidth() - getMeasuredWidth()) / 2;
			int heightOffset = (mPreviewView.getMeasuredHeight() - getMeasuredHeight()) / 2;
			
			decodeRect.left = finderRect.left + widthOffset;
			decodeRect.top = finderRect.top + heightOffset;
			decodeRect.right = finderRect.right + widthOffset;
			decodeRect.bottom = finderRect.bottom + heightOffset;
			decodeInfo.decodeRect = decodeRect;
		}
		mDecodeTask = new DecodeTask(this, mMultiFormatReader);
		mDecodeTask.setOnScanListener(mOnScanListener);
		mDecodeTask.execute(decodeInfo);
	}
	
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (!mHasSurface) {
			mHasSurface = true;
			initCamera(holder);
		}
	}
	
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		mHasSurface = false;
	}
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		
		if (mPreviewView == null || mCameraManager == null) {
			return;
		}
		Point previewResolution = mCameraManager.getPreviewResolution();
		if (previewResolution == null) {
			return;
		}
		
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);
		
		int previewWidth = previewResolution.x;
		int previewHeight = previewResolution.y;
		
		double ratio = (double) previewWidth / (double) previewHeight;
		
		int widthOffset = previewWidth - widthSize;
		int heightOffset = previewHeight - heightSize;
		
		int absWidthOffset = Math.abs(widthOffset);
		int absHeightOffset = Math.abs(heightOffset);
		
		int measuredWidth;
		int measuredHeight;
		
		if (widthOffset < 0 && heightOffset < 0) {
			// 预览分辨率宽高比控件分辨率宽高小
			if (absWidthOffset > absHeightOffset) {
				measuredWidth = widthSize;
				measuredHeight = (int) (widthSize / ratio);
			} else {
				measuredWidth = (int) (heightSize * ratio);
				measuredHeight = heightSize;
			}
		} else if (widthOffset < 0 && heightOffset > 0) {
			// 预览分辨率宽度比控件分辨率宽度大，预览分辨率高度比控件分辨率高度小
			measuredWidth = widthSize;
			measuredHeight = (int) (widthSize / ratio);
		} else if (widthOffset > 0 && heightOffset < 0) {
			// 预览分辨率宽度比控件分辨率宽度小，预览分辨率高度比控件分辨率高度大
			measuredWidth = (int) (heightSize * ratio);
			measuredHeight = heightSize;
		} else {
			measuredWidth = previewWidth;
			measuredHeight = previewHeight;
		}
		
		mPreviewView.measure(
				MeasureSpec.makeMeasureSpec(measuredWidth, MeasureSpec.EXACTLY),
				MeasureSpec.makeMeasureSpec(measuredHeight, MeasureSpec.EXACTLY)
		);
	}
	
	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		
		if (mPreviewView == null) {
			return;
		}
		
		int width = right - left;
		int height = bottom - top;
		
		int measuredWidth = mPreviewView.getMeasuredWidth();
		int measureHeight = mPreviewView.getMeasuredHeight();
		int widthOffset = (width - measuredWidth) / 2;
		int heightOffset = (height - measureHeight) / 2;
		mPreviewView.layout(
				widthOffset,
				heightOffset,
				widthOffset + measuredWidth,
				heightOffset + measureHeight
		);
	}
	
	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		
		if (mDecodeTask == null) {
			return;
		}
		mDecodeTask.cancel(true);
		mDecodeTask = null;
	}
}
