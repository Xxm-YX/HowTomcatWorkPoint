package org.how.tomcat.works.ex02;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;


public class HttpServer {

    /**
     * WEB_ROOT is the directory where our HTML and other files reside.
     * For this package,WEB_ROOT is the "webroot" directory under the
     * working directory
     * The working directory is the location in the file system
     * from where the java command was invoked
     **/
    public static final String WEB_ROOT =
            System.getProperty("user.dir") + File.separator + "webroot";

    //shutdown command
    private static final String SHUTDOWN_COMMAND = "/SHUTDOWN";

    //the shutdown command received
    private boolean shutdown = false;

    public static void main(String[] args) {
        HttpServer server = new HttpServer();
        server.await();
    }

    private void await() {

        ServerSocket serverSocket = null;
        System.out.println(WEB_ROOT);
        int port = 7870;
        try{
            //创建服务器套接字
            serverSocket = new ServerSocket(port,
                    1, InetAddress.getByName("127.0.0.1"));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        //Loop waiting for a request
        while (!shutdown){
            Socket socket = null;
            InputStream input = null;
            OutputStream output = null;

            try{
                //监听地址+端口
                socket = serverSocket.accept();
                input = socket.getInputStream();
                output = socket.getOutputStream();
                //create Request object and parse
                Request request = new Request(input);
                request.parse();

                //create Response object
                Response response = new Response(output);
                response.setRequest(request);
                //响应请求
                response.sendStaticResource();

                //Close the socket
                socket.close();

                //check if the previous URI is a shutdown command
                //检查请求是不是关闭
                shutdown = request.getUri().equals(SHUTDOWN_COMMAND);
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }
        }
    }

}
