package com.ansen.okhttp.test.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.ansen.okhttp.test.R;
import com.ansen.okhttp.test.entity.User;
import com.ansen.http.net.HTTPCaller;
import com.ansen.http.net.RequestDataCallback;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.tv_get).setOnClickListener(this);
        findViewById(R.id.tv_post).setOnClickListener(this);
        findViewById(R.id.tv_upload_file).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if(v.getId()==R.id.tv_get){
            HTTPCaller.getInstance().get(User.class,"getUserInfo","http://139.196.35.30:8080/OkHttpTest/getUserInfo.do",null,requestDataCallback);
        }else if(v.getId()==R.id.tv_post){

        }else if(v.getId()==R.id.tv_upload_file){

        }
    }

    private RequestDataCallback requestDataCallback=new RequestDataCallback<User>(){
        @Override
        public void dataCallback(User user) {
            Log.i("ansen","获取用户信息:"+user.toString());
        }
    };


}
