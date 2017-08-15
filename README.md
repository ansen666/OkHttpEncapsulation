### 前言
http请求基本上每一个app都会使用，进行好的封装提供Activity调用是非常有必要的，并且当我们切换http请求库的时候也只会修改封装的地方，而不需要修改Activity代码。最近比较火的http请求库就属[OkHttp](https://github.com/square/okhttp)了，这里我们对[OkHttp](https://github.com/square/okhttp)的get请求、post请求、上传文件进行了封装。

### 封装的目的
- 封装基本的公共方法给外部调用。get请求,Post请求- ,PostFile,downloadFile。简化代码。
- 官方建议OkHttpClient实例只new一次。
- 如果同一时间访问同一个api多次，那我们默认情况只会保留最后一个请求。这个调用的可以也可以通过参数控制。
- 如果用户连接Http代理了，就不让访问，防止用户通过抓包工具看我们的接口数据。这个也可以在初始化的时候通过改变HttpConfig的agent属性控制。
- 每个接口都要带上的参数应该封装起来，例如app版本号，设备号,登录之后的用户token，这些参数可能每次请求都要带上。当然我们需要的时候也可以对公共参数进行修改。
- 把返回的json字符串转成对象再回调回来。当然你想要byte数组也可以。只需要重写RequestDataCallback接口的不同方法。
- 我们访问服务器用的是异步请求，不可能每个调用的地方拿到数据还要通过handler来刷新ui，底层直接封装了一个handler统一处理。
- 上传文件跟下载文件支持进度回调。

### 使用
#### 1.依赖
如果是android studio开发支持在线依赖:
```
compile 'com.ansen.http:okhttpencapsulation:1.0.1'
```

如果是eclipse那你先把ide切换到android studio吧。。。不闲麻烦的话也可以把源码module的源码copy出来，反正也就几个类。

#### 2.初始化HTTPCaller类
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

#### 发送get请求
发送get请求就一行代码。
```
HTTPCaller.getInstance().get(User.class, "http://139.196.35.30:8080/OkHttpTest/getUserInfo.do?per=123", null, requestDataCallback);
```

#### 请求回调
http请求回调接口,无论成功或者失败都会回调。因为是测试所以都用在这个接口来回调，在真实的企业开发中，不同的请求用不同的回调。
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

#### 发送post请求
post请求参数不是跟在url后面的，所以需要把请求参数放到集合里面。因为登录接口也是返回的用户信息，所以可以跟get请求用同一回调。
```
List<NameValuePair> postParam = new ArrayList<>();
postParam.add(new NameValuePair("username","ansen"));
postParam.add(new NameValuePair("password","123"));
HTTPCaller.getInstance().post(User.class, "http://139.196.35.30:8080/OkHttpTest/login.do", null, postParam, requestDataCallback);
```

#### 上传文件
##### 1.上传文件不带回调进度
```
updaloadFile(null);
```

##### 2.上传文件回调上传进度
```
updaloadFile(new ProgressUIListener(){
    @Override
    public void onUIProgressChanged(long numBytes, long totalBytes, float percent, float speed) {
        Log.i("ansen","numBytes:"+numBytes+" totalBytes:"+totalBytes+" percent:"+percent+" speed:"+speed);
    }
});
```

上传文件跟其他表单参数不一样的地方就是new NameValuePair对象的时候需要传入三个参数，最后一个参数需要设置成true。
```
private void updaloadFile(ProgressUIListener progressUIListener){
    List<NameValuePair> postParam = new ArrayList<>();
    postParam.add(new NameValuePair("username", "ansen"));
    postParam.add(new NameValuePair("password", "123"));
    String filePath=copyFile();//复制一份文件到sdcard上，并且获取文件路径
    postParam.add(new NameValuePair("upload_file",filePath,true));
    if(progressUIListener==null){//上传文件没有回调进度条
        HTTPCaller.getInstance().postFile(User.class, "http://139.196.35.30:8080/OkHttpTest/uploadFile.do", null, postParam, requestDataCallback);
    }else{//上传文件并且回调上传进度
        HTTPCaller.getInstance().postFile(User.class, "http://139.196.35.30:8080/OkHttpTest/uploadFile.do", null, postParam, requestDataCallback,progressUIListener);
    }
}
```

#### 上传文件(传入byte数组)
```
byte[] bytes=getUploadFileBytes();//获取文件内容存入byte数组
            HTTPCaller.getInstance().postFile(User.class, "http://139.196.35.30:8080/OkHttpTest/uploadFile.do", null, "upload_file","test.txt",bytes,requestDataCallback);
```

#### 上传文件(传入byte数组)&&回调上传进度
```
byte[] bytes=getUploadFileBytes();//获取文件内容存入byte数组
HTTPCaller.getInstance().postFile(User.class, "http://139.196.35.30:8080/OkHttpTest/uploadFile.do", null, "upload_file", "test.txt", bytes, requestDataCallback, new ProgressUIListener() {
    @Override
    public void onUIProgressChanged(long numBytes, long totalBytes, float percent, float speed) {
        Log.i("ansen","upload file content numBytes:"+numBytes+" totalBytes:"+totalBytes+" percent:"+percent+" speed:"+speed);
    }
});
```

#### 下载文件&&回调下载进度
```
String saveFilePath=Environment.getExternalStorageDirectory() + "/test/test222.txt";
HTTPCaller.getInstance().downloadFile("http://139.196.35.30:8080/OkHttpTest/upload/test.txt",saveFilePath,null,new ProgressUIListener(){
    @Override
    public void onUIProgressChanged(long numBytes, long totalBytes, float percent, float speed) {
        Log.i("ansen","dowload file content numBytes:"+numBytes+" totalBytes:"+totalBytes+" percent:"+percent+" speed:"+speed);
    }
});
```

#### 修改公共参数
```
HTTPCaller.getInstance().updateCommonField("version_code","2");//更新公共字段版本号的值
```

### 关于这几个测试接口的服务器
[想看服务器源码点这里](https://github.com/ansen666/OkHttpTest)
