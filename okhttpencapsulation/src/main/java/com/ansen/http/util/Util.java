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
    public static String getFileName(String filename){
        int start=filename.lastIndexOf("/");
        int end=filename.lastIndexOf(".");
        if(start!=-1 && end!=-1){
            return filename.substring(start+1,end);
        }else{
            return null;
        }
    }

    public static String get(String url, List<NameValuePair> commonField){
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
