package com.ansen.okhttp.test.activity;

import android.content.res.AssetManager;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.ansen.http.net.NameValuePair;
import com.ansen.okhttp.test.R;
import com.ansen.okhttp.test.entity.User;
import com.ansen.http.net.HTTPCaller;
import com.ansen.http.net.RequestDataCallback;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.tv_get).setOnClickListener(this);
        findViewById(R.id.tv_post).setOnClickListener(this);
        findViewById(R.id.tv_upload_file).setOnClickListener(this);
        findViewById(R.id.tv_upload_file_content).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.tv_get) {
            HTTPCaller.getInstance().get(User.class, "http://139.196.35.30:8080/OkHttpTest/getUserInfo.do?per=123", null, requestDataCallback);
        } else if (v.getId() == R.id.tv_post) {
            List<NameValuePair> postParam = new ArrayList<>();
            postParam.add(new NameValuePair("username", "ansen"));
            postParam.add(new NameValuePair("password", "123"));
            HTTPCaller.getInstance().post(User.class, "http://139.196.35.30:8080/OkHttpTest/login.do", null, postParam, requestDataCallback);
        } else if (v.getId() == R.id.tv_upload_file) {
            List<NameValuePair> postParam = new ArrayList<>();
            postParam.add(new NameValuePair("username", "ansen"));
            postParam.add(new NameValuePair("password", "123"));
            postParam.add(new NameValuePair("upload_file",copyFile()));

            HTTPCaller.getInstance().updateCommonField("version_code","2");//更新公共字段版本号的值
            HTTPCaller.getInstance().postFile(User.class, "http://139.196.35.30:8080/OkHttpTest/uploadFile.do", null, postParam, requestDataCallback);
        }else if(v.getId()==R.id.tv_upload_file_content){
            byte[] bytes=getUploadFileBytes();//获取文件内容存入byte数组
            HTTPCaller.getInstance().postFile(User.class, "http://139.196.35.30:8080/OkHttpTest/uploadFile.do", null, "upload_file","test.txt",bytes,requestDataCallback);
        }
    }

    private RequestDataCallback requestDataCallback = new RequestDataCallback<User>() {
        @Override
        public void dataCallback(User user) {
            Log.i("ansen", "获取用户信息:" + user.toString());
        }
    };


    private byte[] getUploadFileBytes(){
        byte[] bytes=null;
        try {
            InputStream inputStream = getAssets().open("ansen.txt");
            Log.i("ansen","文件长度:"+inputStream.available());
            bytes = new byte[inputStream.available()];
            inputStream.read(bytes);
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytes;
    }

    /**
     * 如果sdcard没有文件就复制过去
     */
    private String copyFile() {
        AssetManager assetManager = this.getAssets();
        String newFilePath=Environment.getExternalStorageDirectory() + "/test/test.txt";

        File file=new File(newFilePath);
        if(file.exists()){
            file.delete();
        }

        try {
            InputStream in = assetManager.open("test.txt");
            OutputStream out = new FileOutputStream(newFilePath);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            out.flush();
            out.close();
        } catch (Exception e) {
            Log.e("tag", e.getMessage());
        }
        return newFilePath;
    }
}
