/**
 * BussinessCoreLib
 * com.yuanfen.app.model.net
 * HttpProgress.java
 * 
 * guopeng 创建于 2014年12月4日-下午4:45:49
 * 
 */
package com.ansen.http.net;

/**
 *
 * HttpProgress
 * <p>功能:HTTP进度回调</p>
 * <p>guopeng 创建于 2014年12月4日 下午4:45:49</p>
 * 
 * @version 1.0.0
 *
 */
public abstract class CustomerProgress {
	public void onProgress(int bytesWritten, int totalSize){}
	public void onProgress(int progress){}
	public boolean isCancel(){return false;}
}
