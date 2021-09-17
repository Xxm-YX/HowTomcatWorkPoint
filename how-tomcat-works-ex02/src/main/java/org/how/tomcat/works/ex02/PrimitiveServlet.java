package org.how.tomcat.works.ex02;

import javax.servlet.*;
import java.io.IOException;
import java.io.PrintWriter;

public class PrimitiveServlet implements Servlet {

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        System.out.println("init");
    }

    @Override
    public ServletConfig getServletConfig() {
        return null;
    }

    @Override
    public void service(ServletRequest request, ServletResponse response)
            throws ServletException, IOException {
        String responseHead = "HTTP/1.1 200 ok\r\nContent-Type: text/html;charset=utf-8\r\n";
        response.setContentLength(1024);
        //不安全
        System.out.println(request instanceof ServletRequest);
        System.out.println(request instanceof Request);
//        Request request1 = (Request)request;
//        request1.parse();
        System.out.println("aaaaaaaaa");
        System.out.println("from service");
        //PrintWriter对象，讲字符串写入客户端浏览器
        PrintWriter out = response.getWriter();
//        out.write(responseHead);
//        out.write("Hello,Roses are red.");
        out.println(responseHead);
        out.println("Hello,Roses are red.");
        out.print("Violets are blue.");
    }

    @Override
    public String getServletInfo() {
        return null;
    }

    @Override
    public void destroy() {
        System.out.println("destroy");
    }
}
