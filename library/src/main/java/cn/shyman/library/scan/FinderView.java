package cn.shyman.library.scan;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

public class FinderView extends View {
	private static final float ANIMATION_TIME = 1500f;
	
	private static final float ANIMATION_RATIO = 1f;
	private static final float ALPHA_RATIO = 0.2f;
	private static final int OPAQUE_ALPHA = 255;
	
	private final Paint mPaint;
	private int mMaskColor = 0x60000000;
	private int mCornerColor = 0xFFFFFFFF;
	private float mCornerLineSize = 30;
	private float mCornerStrokeWidth = 5;
	
	private Drawable mAnimationDrawable = new ColorDrawable(0xAAFFFFFF);
	private float animationRatio;
	private long animationTime;
	
	private boolean mIsSquare = true;
	private float mFinderWidthRatio = 0.6F;
	private float mFinderHeightRatio = 0.6F;
	private Rect mFinderRect;
	
	private boolean isScanning = true;
	
	public FinderView(Context context) {
		super(context);
		
		mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	}
	
	@SuppressWarnings("SuspiciousNameCombination")
	Rect getFinderRect() {
		if (mFinderRect != null) {
			return mFinderRect;
		}
		
		int measuredWidth = getMeasuredWidth();
		int measuredHeight = getMeasuredHeight();
		if (measuredWidth == 0 || measuredHeight == 0) {
			return null;
		}
		
		int width = (int) (measuredWidth * mFinderWidthRatio);
		int height = (int) (measuredHeight * mFinderHeightRatio);
		
		if (mIsSquare) {
			if (width > height) {
				width = height;
			} else {
				height = width;
			}
		}
		
		int offsetWidth = (measuredWidth - width) / 2;
		int offsetHeight = (measuredHeight - height) / 2;
		mFinderRect = new Rect(offsetWidth, offsetHeight, offsetWidth + width, offsetHeight + height);
		
		return mFinderRect;
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		if (!this.isScanning) {
			return;
		}
		
		Rect frameRect = getFinderRect();
		if (frameRect == null) {
			return;
		}
		
		int width = canvas.getWidth();
		int height = canvas.getHeight();
		
		// 上下左右的阴影区域
		mPaint.setColor(mMaskColor);
		canvas.drawRect(0, 0, width, frameRect.top, mPaint);
		canvas.drawRect(0, frameRect.top, frameRect.left, frameRect.bottom + 1, mPaint);
		canvas.drawRect(frameRect.right + 1, frameRect.top, width, frameRect.bottom + 1, mPaint);
		canvas.drawRect(0, frameRect.bottom + 1, width, height, mPaint);
		
		mPaint.setColor(mCornerColor);
		// 左上角
		drawLeftTopCorner(canvas, frameRect);
		// 右上角
		drawRightTopCorner(canvas, frameRect);
		// 左下角
		drawLeftBottomCorner(canvas, frameRect);
		// 右下角
		drawRightBottomCorner(canvas, frameRect);
		
		long currentTime = System.currentTimeMillis();
		if (this.animationTime == 0L) {
			this.animationTime = currentTime;
		}
		float multiple = (currentTime - this.animationTime) / ANIMATION_TIME;
		this.animationTime = currentTime;
		this.animationRatio = this.animationRatio + multiple;
		if (this.animationRatio > ANIMATION_RATIO + ALPHA_RATIO) {
			this.animationRatio = this.animationRatio - ANIMATION_RATIO - ALPHA_RATIO;
		}
		
		int offsetY;
		int alpha;
		if (this.animationRatio < ANIMATION_RATIO) {
			offsetY = (int) (frameRect.height() * this.animationRatio);
			alpha = OPAQUE_ALPHA;
		} else {
			offsetY = frameRect.height();
			alpha = OPAQUE_ALPHA - (int) ((this.animationRatio - ANIMATION_RATIO) * OPAQUE_ALPHA / ALPHA_RATIO);
		}
		
		canvas.save();
		canvas.clipRect(frameRect.left, frameRect.top, frameRect.right, frameRect.bottom);
		mAnimationDrawable.setBounds(
				frameRect.left, frameRect.top - frameRect.height() + offsetY,
				frameRect.right, frameRect.top + offsetY
		);
		mAnimationDrawable.setAlpha(alpha);
		mAnimationDrawable.draw(canvas);
		canvas.restore();
		
		postInvalidate(frameRect.left, frameRect.top, frameRect.right, frameRect.bottom);
	}
	
	/**
	 * 左上角
	 *
	 * @param canvas 画布工具
	 * @param rect   绘制区域
	 */
	private void drawLeftTopCorner(Canvas canvas, Rect rect) {
		// 点
		canvas.drawRect(
				rect.left - mCornerStrokeWidth, rect.top - mCornerStrokeWidth,
				rect.left, rect.top,
				mPaint);
		// 横线
		canvas.drawRect(
				rect.left, rect.top - mCornerStrokeWidth,
				rect.left + mCornerLineSize, rect.top,
				mPaint);
		// 竖线
		canvas.drawRect(
				rect.left - mCornerStrokeWidth, rect.top,
				rect.left, rect.top + mCornerLineSize,
				mPaint);
	}
	
	/**
	 * 右上角
	 *
	 * @param canvas 画布工具
	 * @param rect   绘制区域
	 */
	private void drawRightTopCorner(Canvas canvas, Rect rect) {
		// 点
		canvas.drawRect(
				rect.right, rect.top - mCornerStrokeWidth,
				rect.right + mCornerStrokeWidth, rect.top,
				mPaint);
		// 横线
		canvas.drawRect(
				rect.right - mCornerLineSize, rect.top - mCornerStrokeWidth,
				rect.right, rect.top,
				mPaint);
		// 竖线
		canvas.drawRect(
				rect.right, rect.top,
				rect.right + mCornerStrokeWidth, rect.top + mCornerLineSize,
				mPaint);
	}
	
	/**
	 * 左下角
	 *
	 * @param canvas 画布工具
	 * @param rect   绘制区域
	 */
	private void drawLeftBottomCorner(Canvas canvas, Rect rect) {
		// 点
		canvas.drawRect(
				rect.left - mCornerStrokeWidth, rect.bottom,
				rect.left, rect.bottom + mCornerStrokeWidth,
				mPaint);
		// 横线
		canvas.drawRect(
				rect.left, rect.bottom,
				rect.left + mCornerLineSize, rect.bottom + mCornerStrokeWidth,
				mPaint);
		// 竖线
		canvas.drawRect(
				rect.left - mCornerStrokeWidth, rect.bottom - mCornerLineSize,
				rect.left, rect.bottom,
				mPaint);
	}
	
	/**
	 * 右下角
	 *
	 * @param canvas 画布工具
	 * @param rect   绘制区域
	 */
	private void drawRightBottomCorner(Canvas canvas, Rect rect) {
		// 点
		canvas.drawRect(
				rect.right, rect.bottom,
				rect.right + mCornerStrokeWidth, rect.bottom + mCornerStrokeWidth,
				mPaint);
		// 横线
		canvas.drawRect(
				rect.right - mCornerLineSize, rect.bottom,
				rect.right, rect.bottom + mCornerStrokeWidth,
				mPaint);
		// 竖线
		canvas.drawRect(
				rect.right, rect.bottom - mCornerLineSize,
				rect.right + mCornerStrokeWidth, rect.bottom,
				mPaint);
	}
}
