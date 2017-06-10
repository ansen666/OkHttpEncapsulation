package com.ansen.http.net;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.Response;

/**
 * @ClassName: HttpResponseHandler
 * @Description: TODO(HTTP回调类)
 * @author guopeng
 * @date 2014 2014年9月21日 下午5:42:04
 *
 */
public abstract class HttpResponseHandler implements Callback {
	public HttpResponseHandler(){

	}

	public void onFailure(Call call, IOException e){
		onFailure(-1,e.getMessage().getBytes());
	}

	public void onResponse(Call call, Response response) throws IOException {
		int code =response.code();
		byte[] body = response.body().bytes();
		if(code>299){
			onFailure(response.code(),body);
		}
		else{
			Headers headers = response.headers();
			Header[] hs = new Header[headers.size()];

			for (int i=0;i<headers.size();i++){
				hs[i] = new Header(headers.name(i),headers.value(i));
			}
			onSuccess(code,hs,body);
		}
	}

	public void onFailure(int status,byte[] data){

	}

	public void onProgress(int bytesWritten, int totalSize) {
	}

	public abstract void onSuccess(int statusCode, Header[] headers, byte[] responseBody);

}
