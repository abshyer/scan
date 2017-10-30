package cn.shyman.library.scan;

public interface OnDecodeListener {
	
	/**
	 * 解码成功
	 *
	 * @param result 结果信息
	 */
	void decodeSuccess(String result);
	
	/**
	 * 解码失败
	 */
	void decodeFailure();
}
