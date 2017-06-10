package com.ansen.http.net;

import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.ansen.http.entity.HttpConfig;
import com.ansen.http.util.Util;
import com.google.gson.Gson;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;

import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;


/**
 * @author guopeng
 * @ClassName: HTTPCaller
 * @Description: TODO(HTTP请求发起和数据解析转换)
 * @date 2014 2014年9月18日 下午9:24:50
 */

public class HTTPCaller {
	private static HTTPCaller _instance = null;
	private final OkHttpClient client;// 实例话对象
	private Map<String,Call> requestHandleMap = null;//以URL为KEY存储的请求
	private CacheControl cacheControl = null;

	private Gson gson = null;

	private HttpConfig httpConfig=new HttpConfig();

	private HTTPCaller() {
		gson = new Gson();

		client = new OkHttpClient.Builder()
				.connectTimeout(10, TimeUnit.SECONDS)
				.writeTimeout(10, TimeUnit.SECONDS)
				.readTimeout(30, TimeUnit.SECONDS)
				.build();

		requestHandleMap = Collections.synchronizedMap(new WeakHashMap<String, Call>());
		//不使用缓存
		cacheControl =new CacheControl.Builder().noStore().noCache().build();
	}

	public static HTTPCaller getInstance() {
		if (_instance == null) {
			_instance = new HTTPCaller();
		}
		return _instance;
	}


	private Call execute(Request.Builder builder, Header[] header, Callback callback) {
		boolean hasUa = false;
		if (header == null) {
			builder.header("Connection", "close");
			builder.header("Accept", "*/*");
		} else {
			for (Header h : header) {
				builder.header(h.getName(), h.getValue());
				if (!hasUa && h.getName().equals("User-Agent")) {
					hasUa = true;
				}
			}
		}
		if (!hasUa&&!TextUtils.isEmpty(httpConfig.getUserAgent())){
			builder.header("User-Agent",httpConfig.getUserAgent());
		}
		Request request = builder.cacheControl(cacheControl).build();
		Call call = client.newCall(request);
		call.enqueue(callback);
		return call;
	}

	public Call delete(String url, Header[] header, HttpResponseHandler callback) {
		url=Util.get(url,httpConfig.getCommonField());

		Request.Builder builder = new Request.Builder();
		builder.url(url);
		builder.delete();

		return execute(builder, header, callback);
	}

	public Call get(String url, Header[] header, HttpResponseHandler callback) {
		url=Util.get(url,httpConfig.getCommonField());

		Request.Builder builder = new Request.Builder();
		builder.url(url);
		builder.get();
		return execute(builder, header, callback);
	}

	public Call post(String url, Header[] header, List<NameValuePair> form, HttpResponseHandler callback) {
		try {
			if (form == null) {
				form = new ArrayList<>(2);
			}

			form.addAll(httpConfig.getCommonField());//添加公共字段

			FormBody.Builder formBuilder = new FormBody.Builder();
			for (NameValuePair item : form) {
				formBuilder.add(item.getName(), item.getValue());
			}
			RequestBody requestBody = formBuilder.build();

			Request.Builder builder = new Request.Builder();
			builder.url(url);
			builder.post(requestBody);

			return execute(builder, header, callback);
		} catch (Exception e) {
			if (callback != null)
				callback.onFailure(-1, e.getMessage().getBytes());
		}
		return null;
	}

	public Call put(String url, Header[] header, List<NameValuePair> form, HttpResponseHandler callback) {
		try {
			if (form == null) {
				form = new ArrayList<NameValuePair>(2);
			}

			form.addAll(httpConfig.getCommonField());//添加公共字段

			FormBody.Builder formBuilder = new FormBody.Builder();
			for (NameValuePair item : form) {
				formBuilder.add(item.getName(), item.getValue());
			}
			RequestBody requestBody = formBuilder.build();

			Request.Builder builder = new Request.Builder();
			builder.url(url);
			builder.put(requestBody);

			return execute(builder, header, callback);
		} catch (Exception e) {
			if (callback != null)
				callback.onFailure(-1, e.getMessage().getBytes());
		}
		return null;
	}

	public Call postFile(String url, Header[] header, List<NameValuePair> form, HttpResponseHandler callback) {
		try {
			MultipartBody.Builder builder = new MultipartBody.Builder();
			builder.setType(MultipartBody.FORM);
			MediaType mediaType = MediaType.parse("application/octet-stream");
			for (int i = 0; i < form.size(); i++) {
				NameValuePair item = form.get(i);
				if (item.getName().startsWith("upload_file")){
					File myFile = new File(item.getValue());
					if (myFile.exists()) {
						String fileName = Util.getFileName(item.getValue());

						builder.addFormDataPart(item.getName(), fileName,RequestBody.create(mediaType, myFile));
					}
					form.remove(i);
					i--;
				}
			}

			form.addAll(httpConfig.getCommonField());//添加公共字段

			for (NameValuePair item : form) {
				builder.addFormDataPart(item.getName(), item.getValue());
			}

			RequestBody requestBody = builder.build();

			Request.Builder requestBuider = new Request.Builder();
			requestBuider.url(url);
			requestBuider.post(requestBody);

			return execute(requestBuider, header, callback);

		} catch (Exception e) {
			e.printStackTrace();
			Log.e("XX", e.toString());
			if (callback != null)
				callback.onFailure(-1, e.getMessage().getBytes());
		}
		return null;
	}

	public Call postFile(String url, Header[] header, byte[] fileContent, HttpResponseHandler callback) {
		try {
			MultipartBody.Builder builder = new MultipartBody.Builder();
			builder.setType(MultipartBody.FORM);
			MediaType mediaType = MediaType.parse("application/octet-stream");
			builder.addFormDataPart("upload_file", "",RequestBody.create(mediaType, fileContent));

			List<NameValuePair> form = new ArrayList<NameValuePair>(2);
			form.addAll(httpConfig.getCommonField());//添加公共字段
			for (NameValuePair item : form) {
				builder.addFormDataPart(item.getName(), item.getValue());
			}

			RequestBody requestBody = builder.build();
			Request.Builder requestBuider = new Request.Builder();
			requestBuider.url(url);
			requestBuider.put(requestBody);

			return execute(requestBuider, header, callback);

		} catch (Exception e) {
			if (httpConfig.isDebug()) {
				e.printStackTrace();
				Log.e("XX", e.toString());
			}
			if (callback != null)
				callback.onFailure(-1, e.getMessage().getBytes());

		}
		return null;
	}

	/**
	 *
	 * 检查代理
	 * @return
	 */
	private boolean checkAgent() {
		if (httpConfig.isAgent()){
			return false;
		} else {
			String proHost = android.net.Proxy.getDefaultHost();
			int proPort = android.net.Proxy.getDefaultPort();
			if (proHost==null || proPort<0){
				return false;
			}else {
				Log.i(httpConfig.getTagName(),"有代理,不能访问");
				return true;
			}
		}
	}

	public <T> void get(final Class<T> clazz, final String key, final String url,Header[] header,final RequestDataCallback<T> callback) {
		if (checkAgent()) {
			return;
		}
		HttpResponseHandler handler = new HttpResponseHandler() {
			@Override
			public void onFailure(int status, byte[] data) {
				// TODO Auto-generated method stub
				super.onFailure(status, data);
				clear(key);
				printError(url, status, data);//打印错误信息
				if (httpConfig.isDebug()) {
					try {
						String str = new String(data, "utf-8");
						Log.i(httpConfig.getTagName(), url + " " + status + " " + str);
					} catch (Exception e) {
					}
				}
				sendCallback(callback);
			}

			@Override
			public void onSuccess(int status, Header[] header, byte[] data) {
				try {
					clear(key);
					String str = new String(data, "utf-8");
					if (httpConfig.isDebug()) {
						Log.i(httpConfig.getTagName(), url + " " + status + " " + str);
					}
					T t = gson.fromJson(str, clazz);
					sendCallback(status,t,data,callback);
				} catch (Exception e) {
					if (httpConfig.isDebug()) {
						e.printStackTrace();
						Log.e(httpConfig.getTagName(),"自动解析错误:" + e.toString());
					}
					printError(url, status, data);//打印错误信息
					sendCallback(callback);

				}
			}
		};
		add(key, get(url, header, handler));
	}

	public <T> void post(final Class<T> clazz, String key, final String url, Header[] header, List<NameValuePair> params, final RequestDataCallback<T> callback) {
		post(clazz, key, url, header, params, callback, true);
	}

	public <T> void post(final Class<T> clazz, final String key, final String url, Header[] header, final List<NameValuePair> params, final RequestDataCallback<T> callback, boolean autoCancel) {
		if (checkAgent()) {
			return;
		}
		// TODO Auto-generated method stub
		final HttpResponseHandler handler = new HttpResponseHandler() {
			@Override
			public void onFailure(int status, byte[] data) {
				// TODO Auto-generated method stub
				super.onFailure(status, data);
				clear(key);
				printError(url, status, data);//打印错误信息
				try {
					printLog(url + " " + status + " " + new String(data, "utf-8"));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				sendCallback(callback);
			}

			@Override
			public void onSuccess(int status,final Header[] header, byte[] data) {
				try {
					clear(key);
					// TODO Auto-generated method stub
					String str = new String(data, "utf-8");
					printLog(url + " " + status + " " + str);
					T t = gson.fromJson(str, clazz);
					sendCallback(status,t,data,callback);
				} catch (Exception e) {
					if (httpConfig.isDebug()) {
						e.printStackTrace();
						printLog("自动解析错误:" + e.toString());
					}
					printError(url, status, data);//打印错误信息
					sendCallback(callback);
				}
			}
		};
		add(key, post(url, header, params, handler));
	}

	public <T> void put(final Class<T> clazz, final String key, final String url, Header[] header, List<NameValuePair> params, final RequestDataCallback<T> callback) {
		HttpResponseHandler handler = new HttpResponseHandler() {
			@Override
			public void onFailure(int status, byte[] data) {
				super.onFailure(status, data);
				clear(key);

				printError(url, status, data);//打印错误信息

				if (httpConfig.isDebug()) {
					try {
						String str = new String(data, "utf-8");
						Log.i(httpConfig.getTagName(), url + " " + status + " " + str);
					} catch (Exception e) {
					}
				}
				sendCallback(callback);
			}

			@Override
			public void onSuccess(int status,Header[] header, byte[] data) {
				try {
					clear(key);
					String str = new String(data, "utf-8");
					printLog(url + " " + status + " " + str);
					T t = gson.fromJson(str, clazz);
					sendCallback(status,t,data,callback);
				} catch (Exception e) {
					if (httpConfig.isDebug())
						e.printStackTrace();
					printError(url, status, data);//打印错误信息
					sendCallback(callback);
				}
			}
		};
		add(key, put(url, header, params, handler));
	}

	public <T> void delete(final Class<T> clazz, final String key, final String url, Header[] header, final RequestDataCallback<T> callback) {
		HttpResponseHandler handler = new HttpResponseHandler() {
			@Override
			public void onFailure(int status, byte[] data) {
				super.onFailure(status, data);
				clear(key);
				printError(url, status, data);//打印错误信息
				if (httpConfig.isDebug()) {
					try {
						String str = new String(data, "utf-8");
						printLog(url + " " + status + " " + str);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				sendCallback(callback);
			}

			@Override
			public void onSuccess(int status,Header[] header, byte[] data) {
				try {
					clear(key);
					String str = new String(data, "utf-8");
					printLog(url + " " + status + " " + str);
					T t = gson.fromJson(str, clazz);
					sendCallback(status,t,data,callback);
				} catch (Exception e) {
					if (httpConfig.isDebug())
						e.printStackTrace();
					printError(url, status, data);//打印错误信息
					sendCallback(callback);
				}
			}
		};

		add(key, delete(url, header, handler));
	}

	public <T> void postFile(final Class<T> clazz, final String key, final String url, Header[] header, List<NameValuePair> form, final CustomerProgress progress, final RequestDataCallback<T> callback) {
		// TODO Auto-generated method stub
		HttpResponseHandler handler = new HttpResponseHandler() {
			@Override
			public void onFailure(int status, byte[] data) {
				// TODO Auto-generated method stub
				super.onFailure(status, data);
				clear(key);
				printError(url, status, data);//打印错误信息
				if (httpConfig.isDebug()) {
					try {
						String str = new String(data, "utf-8");
						printLog(url + " " + status + " " + str);
					} catch (Exception e) {
					}
				}
				sendCallback(callback);
			}

			@Override
			public void onSuccess(int status,Header[] header, byte[] data) {
				try {
					clear(key);
					String str = new String(data, "utf-8");
					printLog(url + " " + status + " " + str);
					T t = gson.fromJson(str, clazz);
					sendCallback(status,t,data,callback);
				} catch (Exception e) {
					if (httpConfig.isDebug())
						e.printStackTrace();
					printError(url, status, data);//打印错误信息
					sendCallback(callback);
				}
			}

			@Override
			public void onProgress(int bytesWritten, int totalSize) {
				// TODO Auto-generated method stub
				if (progress != null) {
					progress.onProgress(bytesWritten, totalSize);
				}
			}
		};
		add(key, postFile(url, header, form, handler));
	}

	public <T> void postFile(final Class<T> clazz, final String key, final String url, Header[] header, byte[] fileContent, final CustomerProgress progress, final RequestDataCallback<T> callback) {
		// TODO Auto-generated method stub
		HttpResponseHandler handler = new HttpResponseHandler() {
			@Override
			public void onFailure(int status, byte[] data) {
				// TODO Auto-generated method stub
				super.onFailure(status, data);
				clear(key);
				printError(url, status, data);//打印错误信息
				if (httpConfig.isDebug()) {
					try {
						String str = new String(data, "utf-8");
						printLog(url + " " + status + " " + str);
					} catch (Exception e) {
					}
				}
//                if (callback != null)
//                    callback.dataCallback(null);
				sendCallback(callback);
			}

			@Override
			public void onSuccess(int status,
								  Header[] header, byte[] data) {
				clear(key);
				try {
					// TODO Auto-generated method stub
					String str = new String(data, "utf-8");
					printLog(url + " " + status + " " + str);
					T t = gson.fromJson(str, clazz);
//                    if (callback != null)
//                        callback.dataCallback(status, t, data);
					sendCallback(status,t,data,callback);
				} catch (Exception e) {
					if (httpConfig.isDebug())
						e.printStackTrace();
					printError(url, status, data);//打印错误信息
					sendCallback(callback);
				}
			}

			@Override
			public void onProgress(int bytesWritten, int totalSize) {
				if (progress != null) {
					progress.onProgress(bytesWritten, totalSize);
				}
			}
		};
		add(key, postFile(url, header, fileContent, handler));
	}

	private void autoCancel(String function) {
		Call call = requestHandleMap.remove(function);
		if (call != null) {
			call.cancel();
		}
	}

	private void add(String function, Call call) {
		if (!TextUtils.isEmpty(function)) {
//			autoCancel(function);
			requestHandleMap.put(function, call);
		}
	}

	private void clear(String url) {
		requestHandleMap.remove(url);
	}

	public void setHttpConfig(HttpConfig httpConfig) {
		this.httpConfig = httpConfig;
	}

	private void printLog(String content){
		if(httpConfig.isDebug()){
			Log.i(httpConfig.getTagName(),content);
		}
	}

	/**
	 * 打印错误信息
	 * @param url
	 * @param status
	 * @param data
     */
	private void printError(String url, int status, byte[] data) {
		StringBuilder	errorSB = new StringBuilder();
		errorSB.setLength(0);
		errorSB.append(url);
		errorSB.append("\r\nStatus:");
		errorSB.append(status);
		errorSB.append("\r\n");
		try {
			if (data != null)
				errorSB.append(new String(data, "utf-8"));
		} catch (Exception e) {
			if(httpConfig.isDebug()){
				e.printStackTrace();
			}
		}
		printLog(errorSB.toString());
	}

	//更新字段值
	public void updateCommonField(String key,String value){
		httpConfig.updateCommonField(key,value);
	}

	public void removeCommonField(String key){
		httpConfig.removeCommonField(key);
	}

	public void addCommonField(String key,String value){
		httpConfig.addCommonField(key,value);
	}

	private <T> void sendCallback(RequestDataCallback<T> callback){
		sendCallback(-1,null,null,callback);
	}

	private <T> void sendCallback(int status,T data,byte[] body,RequestDataCallback<T> callback){
		CallbackMessage<T> msgData = new CallbackMessage<T>();
		msgData.body = body;
		msgData.status = status;
		msgData.data = data;
		msgData.callback = callback;

		Message msg = handler.obtainMessage();
		msg.obj = msgData;
		handler.sendMessage(msg);
	}

	private Handler handler=new Handler(){
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			CallbackMessage data = (CallbackMessage)msg.obj;
			data.callback();
		}
	};

	private class CallbackMessage<T>{
		public RequestDataCallback<T> callback;
		public T data;
		public byte[] body;
		public int status;

		public void callback(){
			if(data==null && callback!=null) {
				callback.dataCallback(null);
			}else{
				if(callback!=null)
					callback.dataCallback(status,data,body);
			}

		}
	}
}
