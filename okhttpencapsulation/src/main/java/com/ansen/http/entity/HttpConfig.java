package com.ansen.http.entity;

import com.ansen.http.net.NameValuePair;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by  ansen
 * Create Time 2017-06-10
 */
public class HttpConfig {
    private boolean debug=false;
    private String userAgent="";//用户代理 它是一个特殊字符串头，使得服务器能够识别客户使用的操作系统及版本、CPU 类型、浏览器及版本、浏览器渲染引擎、浏览器语言、浏览器插件等。
    private boolean agent=true;//有代理的情况能不能访问，true:有代理能访问 false:有代理不能访问
    private String tagName="ansen";

    //通用字段
    private List<NameValuePair> commonField=new ArrayList<>();

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public boolean isAgent() {
        return agent;
    }

    public void setAgent(boolean agent) {
        this.agent = agent;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public List<NameValuePair> getCommonField() {
        return commonField;
    }

    //更新字段值
    public void updateCommonField(String key,String value){
        boolean result = true;
        for(int i=0;i<commonField.size();i++){
            NameValuePair nameValuePair = commonField.get(i);
            if(nameValuePair.getName().equals(key)){
                commonField.set(i,new NameValuePair(key,value));
                result = false;
                break;
            }
        }
        if(result){
            commonField.add(new NameValuePair(key,value));
        }
    }

    public void removeCommonField(String key){
        for(int i=commonField.size()-1;i>=0;i--){
            if(commonField.get(i).equals("key")){
                commonField.remove(i);
            }
        }
    }

    public void addCommonField(String key,String value){
        commonField.add(new NameValuePair(key,value));
    }
}
