package com.hao.open;

import com.hao.open.config.RpcServiceConfig;
import com.hao.open.proxy.RpcClientProxy;
import com.hao.open.remoting.transport.RpcRequestTransport;
import com.hao.open.remoting.transport.netty.client.NettyRpcClient;
import com.hao.open.remoting.transport.socket.SocketRpcClient;
import com.hao.open.service.Hello;
import com.hao.open.service.HelloService;

public class OrderServiceApplication {

    public static void main(String[] args) {
        startAsUserServiceConsumerByNetty();
    }

    private static void startAsUserServiceConsumerByNetty() {
        NettyRpcClient nettyRpcClient = new NettyRpcClient();
        RpcServiceConfig rpcServiceConfig = new RpcServiceConfig();
        rpcServiceConfig.setGroup("test2");
        rpcServiceConfig.setVersion("version2");
        RpcClientProxy rpcClientProxy = new RpcClientProxy(nettyRpcClient, rpcServiceConfig);
        HelloService helloService = rpcClientProxy.getProxy(HelloService.class);

        System.out.println("============================");
        for (int i = 0; i < 10; i++) {
            String hello = helloService.hello(new Hello("11" + i, "22" + i));
            System.out.println(hello);
        }
    }

    private static void startAsUserServiceConsumerBySocket() {
        RpcRequestTransport rpcRequestTransport = new SocketRpcClient();
        RpcServiceConfig rpcServiceConfig = new RpcServiceConfig();
        RpcClientProxy rpcClientProxy = new RpcClientProxy(rpcRequestTransport, rpcServiceConfig);
        HelloService helloService = rpcClientProxy.getProxy(HelloService.class);

        System.out.println("============================");
        for (int i = 0; i < 10; i++) {
            String hello = helloService.hello(new Hello("11" + i, "22" + i));
            System.out.println(hello);
        }
    }

}
