package org.how.tomcat.works.ex02;



import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * 表示HTTP响应
 *
 * HTTP Response = Status-Line
 *      *(( general-header | response-header | entity-header )  CRLF)
 *      CRLF
 *      [ message-body ]
 *      Status-Line = HTTP-Version SP Status-Code SP Reason-Phrase CRLF
 **/
public class Response {

    private static final int BUFFER_SIZE = 1024;
    Request request;
    OutputStream output;

    public Response(OutputStream output){
        this.output = output;
    }

    public OutputStream getOutput() {
        return output;
    }

    public Request getRequest() {
        return request;
    }

    public void setRequest(Request request) {
        this.request = request;
    }

    /**
     * 发送静态资源到浏览器
     * 首先通过传入父路径和子路径到File类的构造函数中实例化java.io.File类
     **/
    public void sendStaticResource() throws IOException{
        //缓冲区
        byte[] bytes = new byte[BUFFER_SIZE];
        FileInputStream fis = null;
        try{
            File file = new File(HttpServer.WEB_ROOT, request.getUri());
            String responseHead = "HTTP/1.1 200 ok\r\nContent-Type: text/html\r\nContent-Length: "+file.length()+"\r\n\r\n";
            output.write(responseHead.getBytes());
            if (file.exists()){
                fis = new FileInputStream(file);
                //每次读1024
                int ch = fis.read(bytes, 0, BUFFER_SIZE);
                while (ch != -1){
                    output.write(bytes, 0, ch);
                    ch = fis.read(bytes, 0, BUFFER_SIZE);
                }
            }else {
                //file not found
                String errorMessage = "HTTP/1.1 404 File Not Found\r\n" +
                        "Content-Type: text/html\r\n" +
                        "Content-Length: 23\r\n" +
                        "\r\n" +
                        "<h1>File Not Found</h1>";
                output.write(errorMessage.getBytes());
            }
        }catch (Exception e){
            // thrown if cannot instantiate a File object
            System.out.println(e.toString());
        }finally {
            if (fis != null){
                fis.close();
            }
        }
    }
}
