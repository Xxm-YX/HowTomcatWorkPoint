package ex03.pyrmont.connector.http;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * @author yixuan.zhu
 * 2021/9/13
 * 连接器
 **/
public class HttpConnector implements Runnable{

    boolean stopped;
    private String scheme = "http";

    public String getScheme() {
        return scheme;
    }

    /**
     * 处理所有的请求
     * 负责创建 服务器套接字
     **/
    @Override
    public void run() {
        ServerSocket serverSocket = null;
        int port = 7870;
        try{
            serverSocket =
                    new ServerSocket(port, 1, InetAddress.getByName("127.0.0.1"));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        while(!stopped){
            //Accept the next incoming connection from the server socket
            Socket socket = null;
            try {
                socket = serverSocket.accept();
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }
            //Hand this socket off to an HttpProcessor
            HttpProcessor processor = new HttpProcessor(this);
            processor.process(socket);
        }
    }


    public void start(){
        Thread thread = new Thread(this);
        thread.start();
    }
}
