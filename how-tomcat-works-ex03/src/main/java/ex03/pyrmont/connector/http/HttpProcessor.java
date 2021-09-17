package ex03.pyrmont.connector.http;

import ex03.pyrmont.ServletProcessor;
import ex03.pyrmont.StaticResourceProcessor;
import org.apache.catalina.util.RequestUtil;
import org.apache.catalina.util.StringManager;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;



/* this class used to be called HttpServer
 * 创建一个HttpRequest对象
 * 创建一个HttpResponse对象
 * 解析HTTP请求的第1行 和 请求头信息，填充HttpRequest对象
 * 将HttpRequest对象和HttpResponse对象传递给servletProcessor 或 StaticResourceProcessor
 * 的 service()方法。
 */

public class HttpProcessor {

    public HttpProcessor(HttpConnector connector) {
        this.connector = connector;
    }

    /**
     * The HttpConnector with which this processor is associated
     **/
    private HttpConnector connector = null;
    private HttpRequest request;
    private HttpRequestLine requestLine = new HttpRequestLine();
    private HttpResponse response;

    protected String method = null;
    protected String queryString = null;

    /**
     * The string manager for this package.
     */
    protected StringManager sm = StringManager
            .getManager("ex03.pyrmont.connector.http");

    public void process(Socket socket){
        SocketInputStream input = null;
        OutputStream output = null;
        try{
            input = new SocketInputStream(socket.getInputStream(), 2048);
            output = socket.getOutputStream();

            // create HttpRequest object and parse
            request = new HttpRequest(input);

            // create HttpResponse object
            response = new HttpResponse(output);
            response.setRequest(request);

            response.setHeader("Server","Pyrmont Servlet Container");

            //2. 解析请求行
            parseRequest(input, output);
            //3. 解析请求头
            parseHeaders(input);

            request.parseParameters();
            //check if this is a request for a servlet or a static resource
            //a request for a servlet begins with "/servlet/"
            if(request.getRequestURI().startsWith("/servlet/")){
                ServletProcessor processor = new ServletProcessor();
                processor.process(request, response);
            }else {
                StaticResourceProcessor processor =
                        new StaticResourceProcessor();
                processor.process(request, response);
            }

            //Close the socket
            socket.close();
            //no shutdown for this application
        } catch (IOException | ServletException e) {
            e.printStackTrace();
        }
    }

    private void parseHeaders(SocketInputStream input) throws ServletException, IOException {
        while (true) { // 循环读取？？？？？
            HttpHeader header = new HttpHeader();

            // Read the next header
            input.readHeader(header);
            if (header.nameEnd == 0) {
                if (header.valueEnd == 0) {
                    return ;
                } else {
                    throw new ServletException
                            (sm.getString("httpProcessor.parseHeaders.colon"));
                }
            }
            String name = new String(header.name, 0, header.nameEnd);
            String value = new String(header.value, 0, header.valueEnd);
            request.addHeader(name, value);
            // do something for some headers, ignore others.
/*
            CookieProcessor cookieProcessor = new LegacyCookieProcessor();
            ServerCookies serverCookies = new ServerCookies(10);

            MimeHeaders mimeHeaders = new MimeHeaders();
            mimeHeaders.setValue(value);
            cookieProcessor.parseCookieHeader(mimeHeaders,serverCookies);
            ServerCookie serverCookie = new ServerCookie();
            */

            if (name.equals("cookie")) {
                //4. 解析 cookie
                Cookie cookies[] = RequestUtil.parseCookieHeader(value);
                for (int i = 0; i < cookies.length; i++) {
                    if ("jsessionid".equals(cookies[i].getName())) {
                        // Override anything requested in the URL
                        if (!request.isRequestedSessionIdFromCookie()) {
                            request.setRequestedSessionId(cookies[i].getValue());
                            request.setRequestedSessionCookie(true);
                            request.setRequestedSessionURL(false);
                        }
                    }
                    request.addCookie(cookies[i]);
                }
            }else if (name.equals("content-length")) {
                int n = -1;
                try {
                    n = Integer.parseInt(value);
                } catch (Exception e) {
                    throw new ServletException(sm.getString("httpProcessor.parseHeaders.contentLength"));
                }
                request.setContentLength(n);
            }else if (name.equals("content-type")) {
                request.setContentType(value);
            }
        }//end while
    }

    /**
     * 解析传进来的request请求
     * @param input
     * @param output
     * @throws IOException
     * @throws ServletException
     */
    private void parseRequest(SocketInputStream input, OutputStream output) throws IOException, ServletException {

        //Parse the incoming request line
        //解析request，将请求行存到requestLine里面
        input.readRequestLine(requestLine);

        String method =
                new String(requestLine.method, 0, requestLine.methodEnd);
        String uri = null;

        String protocol = new String(requestLine.protocol, 0,
                requestLine.protocolEnd);

        // Validate the incoming request line
        if (method.length() < 1){
            throw new ServletException("Missing HTTP request method");
        }else if (requestLine.uriEnd < 1){
            throw new ServletException("Missing HTTP request URI");
        }
        //Parse any query parameters out of the request URI
        int question = requestLine.indexOf("?");
        if (question >= 0) {
            //将查询参数存到request中再去解析
            request.setQueryString(new String(requestLine.uri, question + 1,
                    requestLine.uriEnd - question -1));
            //截取真正的uri
            uri = new String (requestLine.uri, 0, question);
        }else {
            request.setQueryString(null);
            uri = new String(requestLine.uri, 0, requestLine.uriEnd);
        }

        //Checking for an absolute URI (with the HTTP protocol) 这个地方有可能开头是http://啥的
        if (!uri.startsWith("/")) {
            int pos = uri.indexOf("://");
            // Parsing out protocol and host name
            if (pos != -1){
                pos = uri.indexOf('/', pos + 3);
                if( pos == -1){
                    uri = "";
                }else {
                    uri = uri.substring(pos);
                }
            }
        }

        // Parse any reqeusted session ID out of the request URI
        String match = ";jsessionid=";
        //匹配的索引，第一个;的位置
        int semicolon = uri.indexOf(match);
        if (semicolon >= 0) {
            //得到后面那段
            String rest = uri.substring(semicolon + match.length());
            //如果sessionId后面还有参数，截取掉
            int semicolon2 = rest.indexOf(';');
            if(semicolon2 >= 0){
                request.setRequestedSessionId(rest.substring(0, semicolon2));
                rest = rest.substring(semicolon2);
            }else {
                request.setRequestedSessionId(rest);
                rest = "";
            }
            request.setRequestedSessionURL(true);
            uri = uri.substring(0, semicolon) + rest;
        }else {
            request.setRequestedSessionId(null);
            request.setRequestedSessionURL(false);
        }
        // Normalize URI (using String operations at the moment)
        String normallizedUri = normalize(uri);
        //Set the corresponding request properties
        request.setMethod(method);
        request.setProtocol(protocol);
        if (normallizedUri != null) {
            request.setRequestURI(normallizedUri);
        }else {
            request.setRequestURI(uri);
        }
        if (normallizedUri == null) {
            throw new ServletException("Invalid URI:" + uri + "'");
        }
    }

    /**
     * Return a context-relative path, beginning with a "/",
     * that represents the cannical version of the specified path after ".."
     * and "." elements are resolved out.
     * If the specified path attempts to go outside the boundaries of the
     * current context (i.e too many ".." path elements are present),
     * return <code>null</code> instead.
     *
     * @param path Path to be normalized
     * @return
     */
    protected String normalize(String path) {
        if (path == null) {
            return null;
        }
        // Create a place for the normalized path
        String normalized = path;

        // Normalize "/%7E" and "/%7e" at the beginning to "/~"
        if (normalized.startsWith("/%7E") || normalized.startsWith("/%7e")){
            normalized = "/~" + normalized.substring(4);
        }

        // Prevent encoding '%','/','.' and '\',which are special reserved
        // characters  特殊字符串
        if ((normalized.contains("%25")) //%
                || (normalized.contains("%2F")) // /
                || (normalized.contains("%2E")) // .
                || (normalized.contains("%5C")) // \
                || (normalized.contains("%2f")) // /
                || (normalized.contains("%2e")) //  .
                || (normalized.contains("%5c"))) // \
             {
            return null;
        }

        if ("/.".equals(normalized)){
            return "/";
        }

        // Normalize the slashes and add leading slash if necessary
        if (normalized.indexOf('\\') >= 0){
            normalized = normalized.replace('\\','/');
        }
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized ;
        }

        // Resolve occurrences of "//" in the normalized path
        while (true) {
            int index = normalized.indexOf("//");
            if (index < 0) {
                break;
            }
            normalized = normalized.substring(0, index) +
                    normalized.substring(index + 1);
        }

        // Resolve occurrences of "/./" in the normalized  path
        while (true) {
            int index = normalized.indexOf("/./");
            if (index < 0) {
                break;
            }
            normalized = normalized.substring(0, index) +
                    normalized.substring(index + 2);
        }

        // Resolve occurrences of "/../" in the normalized path
        while (true) {
            int index = normalized.indexOf("/../");//6
            if (index < 0) {
                break;
            }
            if (index == 0) {
                return (null);  // Trying to go outside our context
            }
            int index2 = normalized.lastIndexOf('/', index - 1);
            normalized = normalized.substring(0, index2) +
                    normalized.substring(index + 3);
        }

        // Declare occurrences of "/..." (three or more dots) to be invalid
        // (on some Windows platforms this walks the directory tree!!!)
        if (normalized.contains("/...")) {
            return null;
        }

        // Return the normalized path that we have completed
        return normalized;

    }
}
