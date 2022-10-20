package com.hao.open;

import com.hao.open.config.BaseConfig;
import com.hao.open.config.RpcServiceConfig;
import com.hao.open.remoting.transport.socket.SocketRpcServer;
import com.hao.open.service.HelloService;
import com.hao.open.service.HelloServiceImpl;
import com.hao.open.utils.PropertiesFileUtil;
import com.hao.open.utils.StringUtil;

import java.util.Properties;

public class UserServiceApplication {

    public static void main(String[] args) {
        startWithSocket(args);
    }

    private static void startWithSocket(String[] args) {

        Properties properties = PropertiesFileUtil.readPropertiesFile("application.properties");
        String portStr = properties.getProperty("server.port");
        if (!StringUtil.isBlank(portStr)) {
            int port = Integer.parseInt(portStr);
            BaseConfig.NETTY_PORT = port;
        }

        HelloService helloService = new HelloServiceImpl();
        SocketRpcServer socketRpcServer = new SocketRpcServer();
        RpcServiceConfig rpcServiceConfig = new RpcServiceConfig();
        rpcServiceConfig.setService(helloService);
        socketRpcServer.registerService(rpcServiceConfig);
        socketRpcServer.start();
    }
}
