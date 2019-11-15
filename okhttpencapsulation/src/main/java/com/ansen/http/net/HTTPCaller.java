package com.ansen.http.net;

import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.ansen.http.entity.HttpConfig;
import com.ansen.http.util.Util;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;

import io.github.lizhangqu.coreprogress.ProgressHelper;
import io.github.lizhangqu.coreprogress.ProgressUIListener;
import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

/**
 * HTTP请求发起和数据解析转换
 * Created by  ansen
 * Create Time 2014年9月18日 下午9:24:50
 * update time 2017-06-10
 */
public class HTTPCaller {
	private static HTTPCaller _instance = null;
	private OkHttpClient client;//okhttp对象
	private Map<String,Call> requestHandleMap = null;//以URL为KEY存储的请求
	private CacheControl cacheControl = null;//缓存控制器

	private Gson gson = null;

	private HttpConfig httpConfig=new HttpConfig();//配置信息

	private HTTPCaller() {}

	public static HTTPCaller getInstance(){
		if (_instance == null) {
			_instance = new HTTPCaller();
		}
		return _instance;
	}

	/**
	 * 设置配置信息 这个方法必需要调用一次
	 * @param httpConfig
     */
	public void setHttpConfig(HttpConfig httpConfig) {
		this.httpConfig = httpConfig;

		client = new OkHttpClient.Builder()
				.connectTimeout(httpConfig.getConnectTimeout(), TimeUnit.SECONDS)
				.writeTimeout(httpConfig.getWriteTimeout(), TimeUnit.SECONDS)
				.readTimeout(httpConfig.getReadTimeout(), TimeUnit.SECONDS)
				.build();

		gson = new Gson();
		requestHandleMap = Collections.synchronizedMap(new WeakHashMap<String, Call>());
		cacheControl =new CacheControl.Builder().noStore().noCache().build();//不使用缓存
	}

	public <T> void get(Class<T> clazz,final String url,final RequestDataCallback<T> callback) {
		this.get(clazz,url,null,callback,true);
	}

	public <T> void get(Class<T> clazz,final String url,Header[] header,final RequestDataCallback<T> callback) {
		this.get(clazz,url,header,callback,true);
	}

	/**
	 * get请求
	 * @param clazz json对应类的类型
	 * @param url 请求url
	 * @param header 请求头
	 * @param callback 回调接口
	 * @param autoCancel 是否自动取消 true:同一时间请求一个接口多次  只保留最后一个
     * @param <T>
     */
	public <T> void get(final Class<T> clazz,final String url,Header[] header,final RequestDataCallback<T> callback, boolean autoCancel){
		if (checkAgent()) {
			return;
		}
		add(url,getBuilder(url, header, new MyHttpResponseHandler(clazz,url,callback)),autoCancel);
	}

	private Call getBuilder(String url, Header[] header, HttpResponseHandler responseCallback) {
		url=Util.getMosaicParameter(url,httpConfig.getCommonField());//拼接公共参数
//		Log.i("ansen","访问的url"+url);
		Request.Builder builder = new Request.Builder();
		builder.url(url);
		builder.get();
		return execute(builder, header, responseCallback);
	}

	public <T> T getSync(Class<T> clazz, String url){
		return getSync(clazz,url,null);
	}

	public <T> T getSync(Class<T> clazz, String url, Header[] header) {
		if (checkAgent()) {
			return null;
		}
		url=Util.getMosaicParameter(url,httpConfig.getCommonField());//拼接公共参数
		Request.Builder builder = new Request.Builder();
		builder.url(url);
		builder.get();
		byte[] bytes = execute(builder,header);
		try {
			String str = new String(bytes, "utf-8");
			if (clazz != null) {
				T t = gson.fromJson(str, clazz);
				return t;
			}
		} catch (Exception e) {
			printLog("getSync HTTPCaller:" + e.toString());
		}
		return null;
	}

	public <T> void post(final Class<T> clazz,final String url, List<NameValuePair> params, RequestDataCallback<T> callback) {
		this.post(clazz,url, null, params, callback,true);
	}

	public <T> void post(final Class<T> clazz,final String url, Header[] header, List<NameValuePair> params, RequestDataCallback<T> callback) {
		this.post(clazz,url, header, params, callback,true);
	}

	/**
	 *
	 * @param clazz json对应类的类型
	 * @param url  请求url
	 * @param header 请求头
	 * @param params 参数
	 * @param callback 回调
	 * @param autoCancel 是否自动取消 true:同一时间请求一个接口多次  只保留最后一个
     * @param <T>
     */
	public <T> void post(final Class<T> clazz,final String url, Header[] header, final List<NameValuePair> params, final RequestDataCallback<T> callback, boolean autoCancel) {
		if (checkAgent()) {
			return;
		}
		add(url,postBuilder(url, header, params, new MyHttpResponseHandler(clazz,url,callback)),autoCancel);
	}

	public <T> T postSync(Class<T> clazz, String url, List<NameValuePair> form) {
		return postSync(clazz,url,form,null);
	}

	public <T> T postSync(Class<T> clazz, String url, List<NameValuePair> form,Header[] header) {
		if (checkAgent()) {
			return null;
		}
//		Log.i("ansen","url:"+url);
		Request.Builder builder=getRequestBuild(url,form);
		byte[] bytes = execute(builder,header);
		try {
			String result = new String(bytes, "utf-8");
			if (clazz != null && !TextUtils.isEmpty(result)) {
				T t = gson.fromJson(result,clazz);
				return t;
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}

	public byte[] postSync(String url,List<NameValuePair> form){
		return postSync(url,form);
	}

	public byte[] postSync(String url,List<NameValuePair> form,Header[] header) {
		if (checkAgent()) {
			return null;
		}
		Request.Builder builder=getRequestBuild(url,form);
		return execute(builder, header);
	}

	private Call postBuilder(String url, Header[] header, List<NameValuePair> form, HttpResponseHandler responseCallback) {
		try {
			Request.Builder builder=getRequestBuild(url,form);
			return execute(builder, header, responseCallback);
		} catch (Exception e) {
			if (responseCallback != null)
				responseCallback.onFailure(-1, e.getMessage().getBytes());
		}
		return null;
	}

	private Request.Builder getRequestBuild(String url,List<NameValuePair> form){
		if(form==null){
			form=new ArrayList<>();
		}
		form.addAll(httpConfig.getCommonField());//添加公共字段
		FormBody.Builder formBuilder = new FormBody.Builder();
		for (NameValuePair item : form) {
			if(TextUtils.isEmpty(item.getValue())){
				printLog("字段:"+item.getName()+"的值为null");
				continue;
			}
			formBuilder.add(item.getName(),item.getValue());
		}
		RequestBody requestBody = formBuilder.build();
		Request.Builder builder = new Request.Builder();
		builder.url(url);
		builder.post(requestBody);
		return builder;
	}

	/**
	 * 上传文件
	 * @param clazz json对应类的类型
	 * @param url 请求url
	 * @param header 请求头
	 * @param form 请求参数
	 * @param callback 回调
	 * @param <T>
	 */
	public <T> void postFile(final Class<T> clazz, final String url, Header[] header,List<NameValuePair> form,final RequestDataCallback<T> callback) {
		postFile(url, header, form, new MyHttpResponseHandler(clazz,url,callback),null);
	}

	/**
	 * 上传文件
	 * @param clazz json对应类的类型
	 * @param url 请求url
	 * @param header 请求头
	 * @param form 请求参数
	 * @param callback 回调
	 * @param progressUIListener  上传文件进度
     * @param <T>
     */
	public <T> void postFile(final Class<T> clazz, final String url, Header[] header,List<NameValuePair> form,final RequestDataCallback<T> callback,ProgressUIListener progressUIListener) {
		add(url, postFile(url, header, form, new MyHttpResponseHandler(clazz,url,callback),progressUIListener));
	}

	/**
	 * 上传文件
	 * @param clazz json对应类的类型
	 * @param url 请求url
	 * @param header 请求头
	 * @param name 名字
	 * @param fileName 文件名
	 * @param fileContent 文件内容
	 * @param callback 回调
	 * @param <T>
	 */
	public <T> void postFile(final Class<T> clazz,final String url,Header[] header,String name,String fileName,byte[] fileContent,final RequestDataCallback<T> callback){
		postFile(clazz,url,header,name,fileName,fileContent,callback,null);
	}

	/**
	 * 上传文件
	 * @param clazz json对应类的类型
	 * @param url 请求url
	 * @param header 请求头
	 * @param name 名字
	 * @param fileName 文件名
	 * @param fileContent 文件内容
     * @param callback 回调
	 * @param progressUIListener 回调上传进度
     * @param <T>
     */
	public <T> void postFile(Class<T> clazz,final String url,Header[] header,String name,String fileName,byte[] fileContent,final RequestDataCallback<T> callback,ProgressUIListener progressUIListener) {
		add(url,postFile(url, header,name,fileName,fileContent,new MyHttpResponseHandler(clazz,url,callback),progressUIListener));
	}

	public void downloadFile(String url,String saveFilePath, Header[] header,ProgressUIListener progressUIListener) {
		downloadFile(url,saveFilePath, header, progressUIListener,true);
	}

	public void downloadFile(String url,String saveFilePath, Header[] header,ProgressUIListener progressUIListener,boolean autoCancel) {
		if (checkAgent()) {
			return;
		}
		add(url,downloadFileSendRequest(url,saveFilePath, header, progressUIListener),autoCancel);
	}

	private Call downloadFileSendRequest(String url,final String saveFilePath,Header[] header,final ProgressUIListener progressUIListener){
		Request.Builder builder = new Request.Builder();
		builder.url(url);
		builder.get();
		return execute(builder, header, new DownloadFileResponseHandler(url,saveFilePath,progressUIListener));
	}

	private Call postFile(String url, Header[] header,List<NameValuePair> form,HttpResponseHandler responseCallback,ProgressUIListener progressUIListener){
		try {
			MultipartBody.Builder builder = new MultipartBody.Builder();
			builder.setType(MultipartBody.FORM);
			MediaType mediaType = MediaType.parse("application/octet-stream");

			form.addAll(httpConfig.getCommonField());//添加公共字段

			for(int i=form.size()-1;i>=0;i--){
				NameValuePair item = form.get(i);
				if(item.isFile()){//上传文件
					File myFile = new File(item.getValue());
					if (myFile.exists()){
						String fileName = Util.getFileName(item.getValue());
						builder.addFormDataPart(item.getName(), fileName,RequestBody.create(mediaType, myFile));
					}
				}else{
					builder.addFormDataPart(item.getName(), item.getValue());
				}
			}

			RequestBody requestBody;
			if(progressUIListener==null){//不需要回调进度
				requestBody=builder.build();
			}else{//需要回调进度
				requestBody = ProgressHelper.withProgress(builder.build(),progressUIListener);
			}
			Request.Builder requestBuider = new Request.Builder();
			requestBuider.url(url);
			requestBuider.post(requestBody);
			return execute(requestBuider, header, responseCallback);
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(httpConfig.getTagName(),e.toString());
			if (responseCallback != null)
				responseCallback.onFailure(-1, e.getMessage().getBytes());
		}
		return null;
	}

	private Call postFile(String url, Header[] header,String name,String filename,byte[] fileContent, HttpResponseHandler responseCallback,ProgressUIListener progressUIListener) {
		try {
			MultipartBody.Builder builder = new MultipartBody.Builder();
			builder.setType(MultipartBody.FORM);
			MediaType mediaType = MediaType.parse("application/octet-stream");
			builder.addFormDataPart(name,filename,RequestBody.create(mediaType, fileContent));

			List<NameValuePair> form = new ArrayList<>(2);
			form.addAll(httpConfig.getCommonField());//添加公共字段
			for (NameValuePair item : form) {
				builder.addFormDataPart(item.getName(),item.getValue());
			}

			RequestBody requestBody;
			if(progressUIListener==null){//不需要回调进度
				requestBody=builder.build();
			}else{//需要回调进度
				requestBody = ProgressHelper.withProgress(builder.build(),progressUIListener);
			}
			Request.Builder requestBuider = new Request.Builder();
			requestBuider.url(url);
			requestBuider.post(requestBody);
			return execute(requestBuider, header,responseCallback);
		} catch (Exception e) {
			if (httpConfig.isDebug()) {
				e.printStackTrace();
				Log.e(httpConfig.getTagName(), e.toString());
			}
			if (responseCallback != null)
				responseCallback.onFailure(-1, e.getMessage().getBytes());
		}
		return null;
	}

	//异步执行
	private Call execute(Request.Builder builder, Header[] header, Callback responseCallback) {
		Call call = getCall(builder, header);
		if (call != null) {
			call.enqueue(responseCallback);
		}
		return call;
	}

	//同步执行
	private byte[] execute(Request.Builder builder, Header[] header) {
		Call call = getCall(builder, header);
		byte[] body = "".getBytes();
		try {
			Response response = call.execute();
			body = response.body().bytes();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return body;
	}

	private Call getCall(Request.Builder builder, Header[] header) {
		boolean hasUa = false;
		if (header == null) {
			builder.header("Connection","close");
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
		return client.newCall(request);
	}

	public class DownloadFileResponseHandler implements Callback{
		private String saveFilePath;
		private ProgressUIListener progressUIListener;
		private String url;

		public DownloadFileResponseHandler(String url,String saveFilePath,ProgressUIListener progressUIListener){
			this.url=url;
			this.saveFilePath=saveFilePath;
			this.progressUIListener=progressUIListener;
		}

		@Override
		public void onFailure(Call call, IOException e) {
			clear(url);
			try {
				printLog(url + " " + -1 + " " + new String(e.getMessage().getBytes(), "utf-8"));
			} catch (UnsupportedEncodingException encodingException) {
				encodingException.printStackTrace();
			}
		}

		@Override
		public void onResponse(Call call, Response response) throws IOException {
			printLog(url + " code:" + response.code());
			clear(url);

			ResponseBody responseBody = ProgressHelper.withProgress(response.body(),progressUIListener);
			BufferedSource source = responseBody.source();

			File outFile = new File(saveFilePath);
			outFile.delete();
			outFile.createNewFile();

			BufferedSink sink = Okio.buffer(Okio.sink(outFile));
			source.readAll(sink);
			sink.flush();
			source.close();
		}
	}

	public class MyHttpResponseHandler<T> extends HttpResponseHandler {
		private Class<T> clazz;
		private String url;
		private RequestDataCallback<T> callback;

		public MyHttpResponseHandler(Class<T> clazz,String url,RequestDataCallback<T> callback){
			this.clazz=clazz;
			this.url=url;
			this.callback=callback;
		}

		@Override
		public void onFailure(int status, byte[] data) {
			clear(url);
			try {
				printLog(url + " " + status + " " + new String(data, "utf-8"));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			sendCallback(callback);
		}

		@Override
		public void onSuccess(int status,final Header[] headers, byte[] responseBody) {
			try {
				clear(url);
				String str = new String(responseBody,"utf-8");
				printLog(url + " " + status + " " + str);
				T t = gson.fromJson(str, clazz);
				sendCallback(status,t,responseBody,callback);
			} catch (Exception e){
				if (httpConfig.isDebug()) {
					e.printStackTrace();
					printLog("自动解析错误:" + e.toString());
				}
				sendCallback(callback);
			}
		}
	}

	private void autoCancel(String function){
		Call call = requestHandleMap.remove(function);
		if (call != null) {
			call.cancel();
		}
	}

	private void add(String url,Call call) {
		add(url,call,true);
	}

	/**
	 * 保存请求信息
	 * @param url 请求url
	 * @param call http请求call
	 * @param autoCancel 自动取消
     */
	private void add(String url,Call call,boolean autoCancel) {
		if (!TextUtils.isEmpty(url)){
			if (url.contains("?")) {//get请求需要去掉后面的参数
				url=url.substring(0,url.indexOf("?"));
			}
			if(autoCancel){
				autoCancel(url);//如果同一时间对api进行多次请求，自动取消之前的
			}
			requestHandleMap.put(url,call);
		}
	}

	private void clear(String url){
		if (url.contains("?")) {//get请求需要去掉后面的参数
			url=url.substring(0,url.indexOf("?"));
		}
		requestHandleMap.remove(url);
	}

	private void printLog(String content){
		if(httpConfig.isDebug()){
			Log.i(httpConfig.getTagName(),content);
		}
	}

	/**
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
			if(callback!=null){
				if(data==null){
					callback.dataCallback(null);
				}else{
					callback.dataCallback(status,data,body);
				}
			}
		}
	}
}
