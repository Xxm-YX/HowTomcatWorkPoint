package org.how.tomcat.works.ex02;

import javax.servlet.Servlet;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandler;

/**
 * @author yixuan.zhu
 * 2021/9/9
 * 处理对servlet资源的HTTP请求
 **/
public class ServletProcessor1 {

    public void process(Request request, Response response) {
        //获取URL   /servlet/servletName
        String uri = request.getUri();
        // 需要从URI中获取servlet的类名
        String servletName = uri.substring(uri.lastIndexOf("/") + 1);
        /**
         * 创建类加载器 java.net.URLClassLoader
         *
         * java.lang.ClassLoader类的一个直接子类
         *
         * 一旦创建了实例后，可以使用loadClass()方法 载入servlet类
         *
         * public URLClassLoader(URL[] urls);
         *
         * urls 是java.net.URL 对象数组
         * 若一个URL 以"/"结尾，表面指向的是一个目录
         * 否则 URL默认指向一个JAR文件，根据需要载入器会下载并打开这个JAR文件
         **/
        URLClassLoader loader = null;

        try{
            // create a URLClassLoader
            URL[] urls = new URL[1];
            URLStreamHandler streamHandler = null;

            /**
             * 并且指明去哪查找要载入的类
             * 到Constant.WEB_ROOT指定的工作目录下查找要载入的类
             **/
            File classPath = new File(Constants.WEB_ROOT);

            /**
             * 在servlet容器中，类载入器查找servlet类的目录称为仓库(repository)
             *
             * 需要查找一个位置，需要先创建只有一个URL的数组，URL类提供了一系列构造函数
             * 因此有很多方法可以创建
             *
             * 本程序使用了和Tomcat中另一个类中使用的相同构造函数
             * :上下文      要解析成URL的字符串   hander 流处理程序
             * public URL(URL context, java.lang.String spec, URLStreamHandler hander)
             *
             * 可以为第2个参数指定一个目录，指定第1个和第3个参数为null，
             *
             * 还有一个构造函数，它接受3个参数:协议  主机名称  文件名
             * public URL(java.lang.String protocol, java.lang.String host, java.lang.String file)
             *
             * new URL(null, aString, null)
             * 会报错，对于编译器需要指明第三个参数的类型：
             *      streamHandler
             **/

            /**
             *  the forming of repository is taken from the
             *  createClassLoader method in
             *  org.apache.catalina.startup.ClassLoaderFactory
             *
             *  生成仓库后会调用 org.apache.catalina.startup.ClassLoaderFactory类里的
             *  createClassLoader()方法
             **/
            // 生成仓库路径：查找servlet类的目录
/*            String repository =
                    (new URL("file", null, classPath.getCanonicalPath() +
                            File.separator)).toString();*/

            String repository =
                    (new URL("file", null, classPath.getCanonicalPath() +
                            File.separator)).toString();

            /**
             * the code for forming the URL is taken from
             * the addRepository method in
             * org.apache.catalina.loader.StandardClassLoader
             * 生成URL对象后会调用org.apache.catalina.loader.StandardClassLoader类里的
             * addRepository() 方法
             **/
            urls[0] = new URL(null, repository, streamHandler);
            loader = new URLClassLoader(urls);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Class<Servlet> myClass = null;
        try {
            //加载servlet类
            assert loader != null;
            myClass = (Class<Servlet>) loader.loadClass(servletName);
        } catch (ClassNotFoundException e) {
            System.out.println(e.toString());
            e.printStackTrace();
        }
        Servlet servlet = null;

        try{
            // 通过反射创建servlet实例容器
            assert myClass != null;
            servlet = myClass.getDeclaredConstructor().newInstance();
            //调用 service方法
            servlet.init(null);
            servlet.service(request, response);

        } catch (Throwable e) {
            System.out.println(e.toString());
            e.printStackTrace();
        }
    }
}
