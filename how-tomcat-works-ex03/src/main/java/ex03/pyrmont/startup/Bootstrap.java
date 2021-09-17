package ex03.pyrmont.startup;

import ex03.pyrmont.connector.http.HttpConnector;

/**
 * @author yixuan.zhu
 * 2021/9/13
 * 启动类
 **/
public final class Bootstrap {

    public static void main(String[] args) {
        HttpConnector connector = new HttpConnector();
        connector.start();
    }
}
