### 前言
http请求基本上每一个app都会使用，进行好的封装提供Activity调用是非常有必要的，并且当我们切换http请求库的时候也只会修改封装的地方，而不需要修改Activity代码。最近比较火的http请求库就属[OkHttp](https://github.com/square/okhttp)了，这里我们对[OkHttp](https://github.com/square/okhttp)的get请求、post请求、上传文件进行了封装。

### 封装的目的
```
1.封装基本的公共方法给外部调用。get请求,Post请求,PostFile。简化代码。
2.官方建议OkHttpClient实例只new一次。
3.如果同一时间访问同一个api多次，那我们默认情况只会保留最后一个请求。这个调用的可以也可以通过参数控制。
4.如果用户连接Http代理了，就不让访问，防止用户通过抓包工具看我们的接口数据。这个也可以在初始化的时候通过改变HttpConfig的agent属性控制。
5.每个接口都要带上的参数应该封装起来，例如app版本号，设备号,登录之后的用户token，这些参数可能每次请求都要带上。当然我们需要的时候也可以对公共参数进行修改。
6.把返回的json字符串转成对象再回调回来。当然你想要byte数组也可以。只需要重写RequestDataCallback接口的不同方法。
7.我们访问服务器用的是异步请求，不可能每个调用的地方拿到数据还要通过handler来刷新ui，底层直接封装了一个handler统一处理。
```

### 使用
##### 1.依赖
如果是android studio开发支持在线依赖:
```
compile 'com.ansen.http:okhttpencapsulation:1.0.1'
```

如果是eclipse那你先把ide切换到android studio吧。。。不闲麻烦的话也可以把源码module的源码copy出来，反正也就几个类。

##### 2.初始化HTTPCaller类
初始化的工作可以放Application，新建MyApplication类继承Application。初始化的时候通过HttpConfig设置一些参数，也可以添加公共参数。
```
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

        //初始化HTTPCaller类
        HTTPCaller.getInstance().setHttpConfig(httpConfig);
    }
}
```

因为自定义Application，需要给AndroidManifest.xml文件application标签中的android:name属性赋值，指定自己重写的MyApplication。

#### get请求
发送get请求就一行代码。
```
HTTPCaller.getInstance().get(User.class, "http://139.196.35.30:8080/OkHttpTest/getUserInfo.do?per=123", null, requestDataCallback);
```

http请求回调接口,无论成功或者失败都会回调。
```
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
```

#### post请求
post请求参数不是跟在url后面的，所以需要把请求参数放到集合里面。因为登录接口也是返回的用户信息，所以可以跟get请求用同一回调。
```
List<NameValuePair> postParam = new ArrayList<>();
postParam.add(new NameValuePair("username", "ansen"));
postParam.add(new NameValuePair("password", "123"));
HTTPCaller.getInstance().post(User.class, "http://139.196.35.30:8080/OkHttpTest/login.do", null, postParam, requestDataCallback);
```

#### 上传文件
上传文件跟其他表单参数不一样的地方就是new NameValuePair对象的时候需要传入三个参数，最后一个参数需要设置成true。
```
List<NameValuePair> postParam = new ArrayList<>();
postParam.add(new NameValuePair("username", "ansen"));
postParam.add(new NameValuePair("password", "123"));
String filePath=copyFile();//获取文件路径
postParam.add(new NameValuePair("upload_file",filePath,true));
HTTPCaller.getInstance().postFile(User.class, "http://139.196.35.30:8080/OkHttpTest/uploadFile.do", null, postParam, requestDataCallback);
```

copyFile方法就是把项目下assets文件夹下的test.txt文件复制到sdcard下。这样我们才有文件路径。用这个copy文件的方法只是为了测试。
```
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
```

#### 上传文件(传入byte数组)
```
byte[] bytes=getUploadFileBytes();//获取文件内容存入byte数组
            HTTPCaller.getInstance().postFile(User.class, "http://139.196.35.30:8080/OkHttpTest/uploadFile.do", null, "upload_file","test.txt",bytes,requestDataCallback);
```

getUploadFileBytes方法就是从assets文件夹下的test.txt文件中读取所有byte数据。
```
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
```
