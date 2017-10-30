package cn.shyman.library.scan;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.DisplayMetrics;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.lang.ref.WeakReference;

class DecodeTask extends AsyncTask<DecodeInfo, Void, Result> {
	private DisplayMetrics mDisplayMetrics;
	
	private final WeakReference<ScanView> mScanViewReference;
	private final WeakReference<MultiFormatReader> mMultiFormatReaderReference;
	private OnScanListener mOnScanListener;
	private OnDecodeListener mOnDecodeListener;
	
	DecodeTask(ScanView scanView,
	           MultiFormatReader multiFormatReader) {
		mDisplayMetrics = scanView.getContext().getResources().getDisplayMetrics();
		
		mScanViewReference = new WeakReference<>(scanView);
		mMultiFormatReaderReference = new WeakReference<>(multiFormatReader);
	}
	
	void setOnScanListener(OnScanListener onScanListener) {
		mOnScanListener = onScanListener;
	}
	
	void setOnDecodeListener(OnDecodeListener onDecodeListener) {
		mOnDecodeListener = onDecodeListener;
	}
	
	@Override
	protected Result doInBackground(DecodeInfo... decodeInfoArray) {
		DecodeInfo decodeInfo = decodeInfoArray[0];
		MultiFormatReader multiFormatReader = mMultiFormatReaderReference.get();
		if (multiFormatReader == null) {
			return null;
		}
		
		try {
			if (decodeInfo.data != null) {
				return decodeByte(decodeInfo, multiFormatReader);
			} else if (decodeInfo.file != null) {
				return decodeFile(decodeInfo, multiFormatReader);
			}
		} catch (Exception ignored) {
		} finally {
			multiFormatReader.reset();
		}
		return null;
	}
	
	private Result decodeByte(DecodeInfo decodeInfo, MultiFormatReader multiFormatReader) throws Exception {
		DecodeInfo resultInfo = rotateData(decodeInfo.data,
				decodeInfo.dataWidth, decodeInfo.dataHeight,
				decodeInfo.rotationAngle);
		if (isCancelled()) {
			return null;
		}
		
		PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(
				resultInfo.data, resultInfo.dataWidth, resultInfo.dataHeight,
				decodeInfo.decodeRect.left, decodeInfo.decodeRect.top,
				decodeInfo.decodeRect.width(), decodeInfo.decodeRect.height(),
				false);
		if (isCancelled()) {
			return null;
		}
		
		BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));
		return multiFormatReader.decodeWithState(binaryBitmap);
	}
	
	private Result decodeFile(DecodeInfo decodeInfo, MultiFormatReader multiFormatReader) throws Exception {
		try {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(decodeInfo.file.getPath(), options);
			
			options.inSampleSize = calculateInSampleSize(options, mDisplayMetrics.widthPixels, mDisplayMetrics.heightPixels);
			
			double widthScale = ((double) options.outWidth) / ((double) mDisplayMetrics.widthPixels);
			double heightScale = ((double) options.outHeight) / ((double) mDisplayMetrics.heightPixels);
			double startScale = widthScale > heightScale ? widthScale : heightScale;
			options.inScaled = true;
			options.inDensity = (int) (mDisplayMetrics.densityDpi * startScale);
			options.inTargetDensity = mDisplayMetrics.densityDpi;
			
			options.inJustDecodeBounds = false;
			options.inMutable = true;
			options.inPreferredConfig = Bitmap.Config.RGB_565;
			Bitmap bitmap = BitmapFactory.decodeFile(decodeInfo.file.getPath(), options);
			int width = bitmap.getWidth();
			int height = bitmap.getHeight();
			
			int[] pixels = new int[width * height];
			
			bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
			bitmap.recycle();
			
			RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
			BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));
			return multiFormatReader.decodeWithState(binaryBitmap);
		} catch (OutOfMemoryError error) {
			System.gc();
		}
		return null;
	}
	
	private int calculateInSampleSize(BitmapFactory.Options options, int requestWidth, int requestHeight) {
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;
		
		if (height > requestHeight || width > requestWidth) {
			final int halfHeight = height / 2;
			final int halfWidth = width / 2;
			
			while ((halfHeight / inSampleSize) > requestHeight
					&& (halfWidth / inSampleSize) > requestWidth) {
				inSampleSize *= 2;
			}
		}
		
		return inSampleSize;
	}
	
	@Override
	protected void onPostExecute(Result result) {
		if (isCancelled()) {
			return;
		}
		
		ScanView scanView = mScanViewReference.get();
		if (scanView == null) {
			return;
		}
		
		if (result == null) {
			scanView.requestPreview();
			if (mOnDecodeListener != null) {
				mOnDecodeListener.decodeFailure();
			}
			return;
		}
		
		if (mOnScanListener != null) {
			mOnScanListener.scanSuccess(result.getText());
		}
		if (mOnDecodeListener != null) {
			mOnDecodeListener.decodeSuccess(result.getText());
		}
	}
	
	@SuppressWarnings("SuspiciousNameCombination")
	private DecodeInfo rotateData(byte[] data,
	                              int dataWidth, int dataHeight,
	                              int rotationAngle) {
		DecodeInfo resultInfo = new DecodeInfo();
		resultInfo.data = new byte[data.length];
		switch (rotationAngle) {
			case 0: {
				resultInfo.data = data;
				resultInfo.dataWidth = dataWidth;
				resultInfo.dataHeight = dataHeight;
				break;
			}
			case 90: {
				for (int y = 0; y < dataHeight; y++) {
					for (int x = 0; x < dataWidth; x++) {
						resultInfo.data[x * dataHeight + dataHeight - y - 1] = data[x + y * dataWidth];
					}
				}
				resultInfo.dataWidth = dataHeight;
				resultInfo.dataHeight = dataWidth;
				break;
			}
			case 180: {
				int length = dataWidth * dataHeight;
				for (int index = 0; index < length; index++) {
					resultInfo.data[index] = data[length - index - 1];
				}
				resultInfo.dataWidth = dataWidth;
				resultInfo.dataHeight = dataHeight;
				break;
			}
			case 270: {
				for (int y = 0; y < dataHeight; y++) {
					for (int x = 0; x < dataWidth; x++) {
						resultInfo.data[(dataWidth - x - 1) * dataHeight + y] = data[x + y * dataWidth];
					}
				}
				resultInfo.dataWidth = dataHeight;
				resultInfo.dataHeight = dataWidth;
				break;
			}
		}
		return resultInfo;
	}
	
}
