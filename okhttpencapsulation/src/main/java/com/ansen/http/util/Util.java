package com.ansen.http.util;

import android.text.TextUtils;

import com.ansen.http.net.NameValuePair;

import java.net.URLEncoder;
import java.util.List;

/**
 * Created by  ansen
 * Create Time 2017-06-09
 */
public class Util {
    /**
     * 获取文件名称
     * @param filename
     * @return
     */
    public static String getFileName(String filename){
        int start=filename.lastIndexOf("/");
//        int end=filename.lastIndexOf(".");
        if(start!=-1){
            return filename.substring(start+1,filename.length());
        }else{
            return null;
        }
    }

    /**
     * 拼接公共参数
     * @param url
     * @param commonField
     * @return
     */
    public static String getMosaicParameter(String url, List<NameValuePair> commonField){
        if (TextUtils.isEmpty(url))
            return "";
        if (url.contains("?")) {
            url = url + "&";
        } else {
            url = url + "?";
        }
        url += getCommonFieldString(commonField);
        return url;
    }

    private static String getCommonFieldString(List<NameValuePair> commonField){
        StringBuffer sb = new StringBuffer();
        try{
            int i=0;
            for (NameValuePair item:commonField) {
                if(i>0){
                    sb.append("&");
                }
                sb.append(item.getName());
                sb.append('=');
                sb.append(URLEncoder.encode(item.getValue(),"utf-8"));
                i++;
            }
        }catch (Exception e){

        }
        return  sb.toString();
    }
}
