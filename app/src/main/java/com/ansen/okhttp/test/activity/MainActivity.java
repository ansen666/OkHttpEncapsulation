package com.ansen.okhttp.test.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Build;
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
    private String[] perms = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private final int PERMS_REQUEST_CODE = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.tv_get).setOnClickListener(this);
        findViewById(R.id.tv_post).setOnClickListener(this);
        findViewById(R.id.tv_upload_file).setOnClickListener(this);
        findViewById(R.id.tv_upload_file_content).setOnClickListener(this);
        findViewById(R.id.tv_modify_version).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.tv_get) {
            HTTPCaller.getInstance().get(User.class, "http://139.196.35.30:8080/OkHttpTest/getUserInfo.do?per=123", null, requestDataCallback);
        } else if (v.getId() == R.id.tv_post) {
            List<NameValuePair> postParam = new ArrayList<>();
            postParam.add(new NameValuePair("username","ansen"));
            postParam.add(new NameValuePair("password","123"));
            HTTPCaller.getInstance().post(User.class, "http://139.196.35.30:8080/OkHttpTest/login.do", null, postParam, requestDataCallback);
        } else if (v.getId() == R.id.tv_upload_file) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {//Android 6.0以上版本需要获取权限
                requestPermissions(perms,PERMS_REQUEST_CODE);//请求权限
            } else {
                uploadFile();
            }
        }else if(v.getId()==R.id.tv_upload_file_content){
            byte[] bytes=getUploadFileBytes();//获取文件内容存入byte数组
            HTTPCaller.getInstance().postFile(User.class, "http://139.196.35.30:8080/OkHttpTest/uploadFile.do", null, "upload_file","test.txt",bytes,requestDataCallback);
        }else if(v.getId()==R.id.tv_modify_version){
            HTTPCaller.getInstance().updateCommonField("version_code","2");//更新公共字段版本号的值
        }
    }

    @Override
    public void onRequestPermissionsResult(int permsRequestCode, String[] permissions, int[] grantResults) {
        switch (permsRequestCode) {
            case PERMS_REQUEST_CODE:
                boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                if (storageAccepted) {
                    uploadFile();
                } else {
                    Log.i("MainActivity", "没有权限操作这个请求");
                }
                break;

        }
    }

    private RequestDataCallback requestDataCallback = new RequestDataCallback<User>() {
        @Override
        public void dataCallback(User user) {
            if(user==null){
                Log.i("ansen", "请求失败");
            }else{
                Log.i("ansen", "获取用户信息:" + user.toString());
            }

        }
    };

    private void uploadFile(){
        List<NameValuePair> postParam = new ArrayList<>();
        postParam.add(new NameValuePair("username", "ansen"));
        postParam.add(new NameValuePair("password", "123"));
        String filePath=copyFile();//获取文件路径
        postParam.add(new NameValuePair("upload_file",filePath,true));
        HTTPCaller.getInstance().postFile(User.class, "http://139.196.35.30:8080/OkHttpTest/uploadFile.do", null, postParam, requestDataCallback);
    }

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
     * 如果sd卡存在这个文件就先删除
     * 然后再从assets下把test.txt复制到sd卡上
     * @return
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
