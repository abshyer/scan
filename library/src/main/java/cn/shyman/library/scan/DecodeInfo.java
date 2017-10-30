package cn.shyman.library.scan;

import android.graphics.Rect;

import java.io.File;

class DecodeInfo {
	File file;
	byte[] data;
	int dataWidth;
	int dataHeight;
	int rotationAngle;
	Rect decodeRect;
}
