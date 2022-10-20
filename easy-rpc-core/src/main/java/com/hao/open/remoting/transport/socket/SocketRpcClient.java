package com.hao.open.remoting.transport.socket;

import com.hao.open.exception.RpcException;
import com.hao.open.extension.ExtensionLoader;
import com.hao.open.registry.ServiceDiscovery;
import com.hao.open.remoting.dto.RpcRequest;
import com.hao.open.remoting.transport.RpcRequestTransport;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * 基于 Socket 传输 RpcRequest
 */
@AllArgsConstructor
@Slf4j
public class SocketRpcClient implements RpcRequestTransport {
    private final ServiceDiscovery serviceDiscovery;

    public SocketRpcClient() {
        // 使用基于 zookeeper 的注册中心
        this.serviceDiscovery = ExtensionLoader.getExtensionLoader(ServiceDiscovery.class).getExtension("zk");
    }

    @Override
    public Object sendRpcRequest(RpcRequest rpcRequest) {
        // 向注册中心查找可用服务，得到 socket 地址
        InetSocketAddress inetSocketAddress = serviceDiscovery.lookupService(rpcRequest);
        // 服务端采用基于传统 BIO Socket 的线程池模型
        // 对应的，客户端最好采用短连接，否则只能处理线程池数量的消费者，故此处为短连接模型
        try (Socket socket = new Socket()) {
            socket.connect(inetSocketAddress);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            // Send data to the server through the output stream
            objectOutputStream.writeObject(rpcRequest);
            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
            // Read RpcResponse from the input stream
            return objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RpcException("调用服务失败:", e);
        }
    }
}
