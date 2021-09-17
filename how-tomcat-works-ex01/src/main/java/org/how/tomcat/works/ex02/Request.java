package org.how.tomcat.works.ex02;

import java.io.IOException;
import java.io.InputStream;

public class Request {

    private InputStream input;
    private String uri;

    public InputStream getInput() {
        return input;
    }

    public void setInput(InputStream input) {
        this.input = input;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    /**
     * 解析HTTP请求中的原始数据
     *
     * 从传入到Request对象中的套接字的InputStream对象中读取整个字节流
     * 并将字节数组存储在缓冲区中，试用缓冲区字节数组中的数组填充StringBuffer对象request
     * 并将StringBuffer的String表示传递给parseUri（）方法
     **/
    public void parse(){
        //Read a set of characters from the socket
        StringBuffer request = new StringBuffer(2048);
        int i;
        //缓冲区
        byte[] buffer = new byte[2048];
        try {
            i = input.read(buffer);
        } catch (IOException e) {
            e.printStackTrace();
            i = -1;
        }
        for (int j = 0; j < i; j++) {
            request.append((char) buffer[j]);
        }
        System.out.println(request.toString());
        uri = parseUri(request.toString());

    }

    /**
     * 解析HTTP请求中的URI
     * 将URI存储在变量uri中
     * 从请求行中获取URI
     * 在请求中搜索第一个和第二个空格，从中找出URI。
     **/
    private String parseUri(String requestString){
        int index1, index2;
        index1 = requestString.indexOf(' ');
        if(index1 != -1){
            index2 = requestString.indexOf(' ', index1 + 1);
            if(index2 > index1){
                return requestString.substring(index1 + 1, index2);
            }
        }
        return null;
    }

    public Request(InputStream input) {
        this.input = input;
    }


}
