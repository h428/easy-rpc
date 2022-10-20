package com.hao.open.remoting.transport.socket;

import com.hao.open.config.BaseConfig;
import com.hao.open.config.CustomShutdownHook;
import com.hao.open.config.RpcServiceConfig;
import com.hao.open.factory.SingletonFactory;
import com.hao.open.provider.ServiceProvider;
import com.hao.open.provider.impl.ZkServiceProviderImpl;
import com.hao.open.utils.concurrent.pool.ThreadPoolFactoryUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

@Slf4j
public class SocketRpcServer {

    private final ExecutorService threadPool;
    private final ServiceProvider serviceProvider;

    public SocketRpcServer() {
        threadPool = ThreadPoolFactoryUtil.createCustomThreadPoolIfAbsent("socket-rpc-server-pool");
        serviceProvider = SingletonFactory.getInstance(ZkServiceProviderImpl.class);
    }

    public void start() {
        try (ServerSocket server = new ServerSocket()) {
            String host = InetAddress.getLocalHost().getHostAddress();
            server.bind(new InetSocketAddress(host, BaseConfig.NETTY_PORT));
            CustomShutdownHook.getCustomShutdownHook().clearAll();
            Socket socket;
            while ((socket = server.accept()) != null) {
                log.info("客户端连接当前服务 [{}]", socket.getInetAddress());
                threadPool.execute(new SocketRpcRequestHandlerRunnable(socket));
            }


        } catch (IOException e) {
            log.error("服务器运行出现异常：", e);
        }
    }

    public void registerService(RpcServiceConfig rpcServiceConfig) {
        serviceProvider.publishService(rpcServiceConfig);
    }
}
