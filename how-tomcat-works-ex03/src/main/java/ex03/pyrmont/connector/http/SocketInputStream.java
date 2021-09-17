package ex03.pyrmont.connector.http;


import org.apache.catalina.util.StringManager;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author yixuan.zhu
 * 2021/9/13
 *
 * Extends InputStream to be more efficient reading lines during HTTP
 * header processing
 **/
public class SocketInputStream extends InputStream {

    // -------------------------------------------------------------- Constants


    /**
     * CR.
     */
    private static final byte CR = (byte) '\r';


    /**
     * LF.
     */
    private static final byte LF = (byte) '\n';


    /**
     * SP.
     */
    private static final byte SP = (byte) ' ';


    /**
     * HT.
     */
    private static final byte HT = (byte) '\t';


    /**
     * COLON.
     */
    private static final byte COLON = (byte) ':';


    /**
     * Lower case offset.
     */
    private static final int LC_OFFSET = 'A' - 'a';


    /**
     * Internal buffer.
     */
    protected byte buf[];


    /**
     * Last valid byte.
     */
    protected int count;


    /**
     * Position in the buffer.
     */
    protected int pos;


    /**
     * Underlying input stream.
     */
    protected InputStream is;


    // ----------------------------------------------------------- Constructors

    /**
     * Construct a servlet input stream associated with the specified socket
     * input.
     * @param is socket input stream
     * @param bufferSize size of the internal buffer
     */
    public SocketInputStream(InputStream is, int bufferSize) {
        this.is = is;
        buf = new byte[bufferSize];
    }

    // -----------------------------------------------------------Variables
    /**
     * The string manager for this package.
     */
    protected static StringManager sm =
            StringManager.getManager(Constants.Package);

    // -----------------------------------------------------------Instance Variables

    // ----------------------------------------------------------- Public Methods

    /**
     * Read the request line, and copies it to the given buffer.
     * This function is meant to be used during the HTTP request header parsing.
     * Do NOT attempt to read the request body using it.
     *
     * @param requestLine Request line object
     * @throws IOException If an exception occurs during the underlying socket
     * read operations, or if the given buffer is not big enough to accomodate
     * the whole line.
     *
     * 解析传进来的request
     */
    public void readRequestLine(HttpRequestLine requestLine) throws IOException {

        //Recycling check 清空requestLine(请求行)
        if (requestLine.methodEnd != 0){
            requestLine.recycle();
        }

        // Checking for a blank line 检查空白行 这个地方的目的是避免 刚开始就读到了空
        // 如果第一个是回车，继续读
        int chr = 0;
        do {    //Skipping CR or LF
                //读到空格或换行 继续读
            try {
                chr = read();
            } catch (IOException e) {
                chr = -1;
            }
        }while ((chr == CR) || (chr == LF));
        if (chr == -1){
            throw new EOFException
                    (sm.getString("requestStream.readline.error"));
        }
        /**
         * 这里为什么又要--
         * 因为刚刚的循环里面，如果读到了空，需要pos++到下一个位置去接着读取，读完后，要从第一个位置开始，则需要--
         */
        pos--;

        // 下面就开始解析读到的请求行数据，存到requestLine中
        //Reading the method name

        int maxRead = requestLine.method.length;
        int readStart = pos;
        int readCount = 0;

        boolean space = false;

        while(!space) {
            //循环解析请求方法
            // if the buffer is full, extend it
            if(readCount >= maxRead) {
                if ((2 * maxRead) <= HttpRequestLine.MAX_METHOD_SIZE){
                    char[] newBuffer = new char[2 * maxRead];
                    System.arraycopy(requestLine.method, 0, newBuffer, 0,
                            maxRead);
                    requestLine.method = newBuffer;
                    maxRead = requestLine.method.length;
                }else {
                    throw new IOException
                            (sm.getString("reqeustStream.readline.toolong"));
                }
            }

            //We‘re at the end of the internal buffer   这个地方 pos索引已经读到了最后一个
            if (pos >= count) {
                int val = read();
                if (val == -1) {
                    throw new IOException
                            (sm.getString("requestStream.readline.error"));
                }
                pos = 0;
                readStart = 0;
            }
            //读到空格了
            if (buf[pos] == SP) {
                space = true;
            }
            requestLine.method[readCount] = (char) buf[pos];
            readCount++;
            pos++;
        }

        requestLine.methodEnd = readCount - 1;

        // Reading URI

        maxRead = requestLine.uri.length;
        readStart = pos;
        readCount = 0;

        space = false;

        boolean eol = false;

        // 循环读取uri 到requestLine.uri里面
        while(!space) {
            // if the buffer is full, extend it
            if (readCount >= maxRead) {
                if ((2 * maxRead) <= HttpRequestLine.MAX_URI_SIZE) {
                    char[] newBuffer = new char[2 * maxRead];
                    System.arraycopy(requestLine.uri, 0, newBuffer, 0,
                            maxRead);
                    requestLine.uri = newBuffer;
                    maxRead = requestLine.uri.length;
                } else {
                    throw new IOException
                            (sm.getString("requestStream.readline.toolong"));
                }
            }
            // We’re at the end of the internal buffer
            if (pos >= count) {
                int val = read();
                if (val == -1){
                    throw new IOException
                            (sm.getString("requestStream.readline.error"));
                }
                pos = 0;
                readStart = 0;
            }
            if(buf[pos] == SP) {
                //空格
                space = true;
            }else if(buf[pos] == CR || (buf[pos] == LF)) {
                //遇到了回车，代表这一行已经读完了，不用再读了
                // HTTP/0.9 style request
                eol = true;//终止了
                space = true;
            }
            requestLine.uri[readCount] = (char) buf[pos];
            readCount++;
            pos++;
        }
        // 已经把读到空格了，把uri读完了，现在要把最后一个uri的索引记下来
        requestLine.uriEnd = readCount - 1;

        // Reading protocol

        maxRead = requestLine.protocol.length;
        readStart = pos;
        readCount = 0;
        //开始循环读取协议信息
        while(!eol) {
            // if the buffer is full,extend it
            if (readCount >= maxRead) {
                if ((2 * maxRead) <= HttpRequestLine.MAX_PROTOCOL_SIZE) {
                    char[] newBuffer = new char[2 * maxRead];
                    System.arraycopy(requestLine.protocol, 0, newBuffer, 0,
                            maxRead);
                    requestLine.protocol = newBuffer;
                    maxRead = requestLine.protocol.length;
                } else {
                    throw new IOException
                            (sm.getString("requestStream.readline.toolong"));
                }
            }

            //We‘re at the end of the internal buffer
            if (pos >= count) {
                // Copying part （or all) of the internal buffer to the line
                // buffer
                int val = read();
                if (val == -1) {
                    throw new IOException
                            (sm.getString("requestStream.readline.error"));
                }
                pos = 0;
                readStart = 0;
            }
            if(buf[pos] == CR){
                // skip CR 回车. 跳过
            }else if(buf[pos] == LF){
                //换行
                eol = true;
            }else {
                requestLine.protocol[readCount] = (char) buf[pos];
                readCount++;
            }
            pos++;
        }
        requestLine.protocolEnd = readCount;
    }

    /**
     * 开始解析头信息,将头信息存到HttpHeader中
     *
     * Read a header, and copies it to the given buffer.
     * This function is meant to be used during the HTTP request header parsing.
     * Do NOT attempt to read the request body using it.
     *
     * @param header
     */
    public void readHeader(HttpHeader header) throws IOException {

        // Recycling check
        if (header.nameEnd != 0) {
            header.recycle();
        }

        // Checking for a blank line
        // 检查空白行
        int chr = read();
        if ((chr == CR) || (chr == LF)) { //Skipping CR //我懂了，这个地方的意思是 已经没有请求头了，堵到了CRLF，再下面就是请求体了，正文
            if (chr == CR) {
                read(); // Skipping LF
            }
            header.nameEnd = 0;
            header.valueEnd = 0;
            return;
        } else {
            pos--;
        }

        // Reading the header name   读取请求头name

        int maxRead = header.name.length;
        int readStart = pos;
        int readCount = 0;

        boolean colon = false;

        while(!colon) {
            // if the buffer is full, extend it
            if (readCount >= maxRead) {
                if ((2 * maxRead) <= HttpHeader.MAX_NAME_SIZE) {
                    // 扩容
                    char[] newBuffer = new char[2 * maxRead];
                    System.arraycopy(header.name, 0, newBuffer, 0, maxRead);
                    header.name = newBuffer;
                    maxRead = header.name.length;
                } else {
                    throw new IOException
                            (sm.getString("requestStream.readline.toolong"));
                }
            }
            // We‘re at the end of the internal buffer
            if (pos >= count) {
                int val = read();
                if (val == -1) {
                    throw new IOException
                            (sm.getString("requestStream.readline.error"));
                }
                pos = 0;
                readStart = 0;
            }
            // 读到冒号了
            if (buf[pos] == COLON) {
                colon = true;
            }
            char val = (char) buf[pos];
            if ((val >= 'A') && (val <= 'Z')) {
                //小写偏移量 变成小写
                val = (char) (val - LC_OFFSET);
            }
            header.name[readCount] = val;
            readCount++;
            pos++;
        }

        // 读到冒号了
        header.nameEnd = readCount - 1;

        // 开始读请求头 value
        // Reading the header value (which can be spanned over multiple lines)

        maxRead = header.value.length;
        readStart = pos;
        readCount = 0;
        // 位置
        int crPos = -2;

        boolean eol = false;
        // 有效行
        boolean validLine = true;

        while (validLine) {

            boolean space = true;

            /**
             * Skipping spaces
             * Note : Only leading white spaces are removed.
             * Trailing white spaces are not
             */
            while (space) {
                // We're at the end of the internal buffer
                if (pos >= count) {
                    // Copying part (or all) of the internal buffer to the line buffer
                    int val = read();
                    if (val == -1) {
                        throw new IOException
                                (sm.getString("requestStream.readline.error"));
                    }
                    pos = 0;
                    readStart = 0;
                }
                // 如果 碰到了空格   或者  table 就跳过
                if ((buf[pos] == SP) || (buf[pos] == HT)) {
                    pos++;
                } else {
                    space = false;
                }
            }

            while (!eol) {
                // if the buffer is full, extend it   扩容
                if (readCount >= maxRead) {
                    if ((2 * maxRead) <= HttpHeader.MAX_VALUE_SIZE) {
                        char[] newBuffer = new char[2 * maxRead];
                        System.arraycopy(header.value, 0, newBuffer, 0,
                                maxRead);
                        header.value = newBuffer;
                        maxRead = header.value.length;
                    } else {
                        throw new IOException
                                (sm.getString("requestStream.readline.toolong"));
                    }
                }

                // We're at the end of the internal buffer  在内存数组的末尾
                if (pos >= count) {
                    // Copying part (or all) of the internal buffer to the line
                    // buffer
                    int val = read();
                    if (val == -1) {
                        throw new IOException
                                (sm.getString("requestStream.readline.error"));
                    }
                    pos = 0;
                    readStart = 0;
                }
                if (buf[pos] == CR) {

                } else if (buf[pos] == LF) {
                    //读到了换行，这个时候pos是换行 后面+1，就到换行下一个字符
                    eol = true;
                } else {
                    // FIXME : Check if binary conversion is working fine
                    int ch = buf[pos] & 0xff;
                    header.value[readCount] = (char) ch;
                    readCount++;
                }
                int pp = buf[pos];
                pos++;
            }
            // 这里为什么要再读一遍 奇了怪了
            // 这个时候 就换行了，原来每次换行先是 CR LF 因为 换行 可能还是上一个header
            int nextChr = read();
            // 这个地方 不为空格 或者 table，说明 第二行 开头不为空格 和 table，到了 这一个header以及结束了，到第二个header了 因为在read里面 pos++了，所以要减回来
            if ((nextChr != SP) && (nextChr != HT)) {
                pos--;
                validLine = false;
            } else {
                eol = false;
                // if the buffer is full, extend it
                if (readCount >= maxRead) {
                    if ((2 * maxRead) <= HttpHeader.MAX_VALUE_SIZE) {
                        char[] newBuffer = new char[2 * maxRead];
                        System.arraycopy(header.value, 0, newBuffer, 0, maxRead);
                        header.value = newBuffer;
                        maxRead = header.value.length;
                    } else {
                        throw new IOException
                                (sm.getString("requestStream.readline.toolong"));
                    }
                }
                header.value[readCount] = ' ';
                readCount++;
            }
        }
        header.valueEnd = readCount;
    }

    /**
     * Read byte
     * @return
     * @throws IOException
     */
    @Override
    public int read() throws IOException {
        // 这个地方还没有到最后一个数组，所以不会再继续读
        if (pos >= count){
            fill();
            if (pos >= count ){
                return -1;
            }
        }

        // 取低8位  转换成无符号的数字,强制将高位符号位去掉
        // 这里return 的是int类型，因为int是有符号位的，为了保证一致性，把高位都去掉
        return buf[pos++] & 0xff;
    }

    /**
     * Returns the number of bytes that can be read from this input
     * stream without blocking.
     */
    @Override
    public int available() throws IOException {
        return (count - pos) + is.available();
    }

    /**
     * Close the input stream.
     */
    @Override
    public void close() throws IOException {
        if (is == null){
            return;
        }
        is.close();
        is = null;
        buf = null;
    }


    // ------------------------------------------------------ Protected Methods

    /**
     * Fill the internal buffer using data from the undelying input stream.
     */
    protected void fill() throws IOException {
        pos = 0;
        count = 0;
        //把输入流的内容读入缓存buf这个数组中
        int nRead = is.read(buf, 0, buf.length);
        if (nRead > 0){
            count = nRead;
        }
    }
}
