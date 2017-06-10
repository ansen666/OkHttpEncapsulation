package com.ansen.okhttp.test.app;

import android.app.Application;

import com.ansen.http.entity.HttpConfig;
import com.ansen.http.net.HTTPCaller;

/**
 * Created by  ansen
 * Create Time 2017-06-10
 */
public class MyApplication extends Application{
    @Override
    public void onCreate() {
        super.onCreate();

        HttpConfig httpConfig=new HttpConfig();
        httpConfig.setAgent(true);//有代理的情况能不能访问
        httpConfig.setDebug(true);//是否debug模式 如果是debug模式打印log
        httpConfig.setTagName("ansen");//打印log的tagname

        //可以添加一些公共字段 每个接口都会带上
        httpConfig.addCommonField("pf","android");
        httpConfig.addCommonField("version_code","1");

        HTTPCaller.getInstance().setHttpConfig(httpConfig);
    }
}
