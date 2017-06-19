package com.ansen.http.net;

/**
 * Created by apple on 16/11/30.
 */

public class NameValuePair {
    private String name;//请求名称
    private String value;//请求值
    private boolean isFile=false;//是否是文件

    public NameValuePair(String name, String value){
        this.name=name;
        this.value = value;
    }

    public NameValuePair(String name, String value,boolean isFile){
        this.name=name;
        this.value = value;
        this.isFile=isFile;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isFile() {
        return isFile;
    }

    public void setFile(boolean file) {
        isFile = file;
    }
}
