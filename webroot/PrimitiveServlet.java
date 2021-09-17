
import javax.servlet.*;
import java.io.IOException;
import java.io.PrintWriter;

public class PrimitiveServlet implements Servlet {

    public void init(ServletConfig servletConfig) throws ServletException {
        System.out.println("init");
    }

    public ServletConfig getServletConfig() {
        return null;
    }

    public void service(ServletRequest request, ServletResponse response)
            throws ServletException, IOException {
        String responseHead = "HTTP/1.1 200 ok\r\nContent-Type: text/html;charset=utf-8\r\n";
//        response.setContentType("text/html;charset=utf-8");
        ServletOutputStream outputStream = response.getOutputStream();
        byte[] bytes = responseHead.getBytes();

//        outputStream.write(responseHead.getBytes());
        outputStream.write(bytes);
        System.out.println("aaaaaaaaa");
        System.out.println("from service");
        //PrintWriter对象，讲字符串写入客户端浏览器
        PrintWriter out = response.getWriter();
        out.write("Hello,Roses are red.");
        out.println("Hello,Roses are red.");
        out.print("Violets are blue.");
    }

    public String getServletInfo() {
        return null;
    }

    public void destroy() {
        System.out.println("destroy");
    }
}
