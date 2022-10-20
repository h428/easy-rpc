package com.hao.open.remoting.transport.socket;

import com.hao.open.factory.SingletonFactory;
import com.hao.open.remoting.dto.RpcRequest;
import com.hao.open.remoting.dto.RpcResponse;
import com.hao.open.remoting.handler.RpcRequestHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Socket 版本的 rpcRequest 处理器
 */
@Slf4j
public class SocketRpcRequestHandlerRunnable implements Runnable {
    private final Socket socket;
    // 通用处理器
    private final RpcRequestHandler rpcRequestHandler;


    public SocketRpcRequestHandlerRunnable(Socket socket) {
        this.socket = socket;
        this.rpcRequestHandler = SingletonFactory.getInstance(RpcRequestHandler.class);
    }

    @Override
    public void run() {
        log.info("server handle message from client by thread: [{}]", Thread.currentThread().getName());
        try (ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream())) {
            // 多线程处理 io 拿到当前连接的 rpcRequest 对象
            RpcRequest rpcRequest = (RpcRequest) objectInputStream.readObject();
            // 调用通用处理器进行处理，即进行真正的方法调用
            Object result = rpcRequestHandler.handle(rpcRequest);
            // 向调用方写入调用结果
            objectOutputStream.writeObject(RpcResponse.success(result, rpcRequest.getRequestId()));
            objectOutputStream.flush();
        } catch (IOException | ClassNotFoundException e) {
            log.error("occur exception:", e);
        }
    }

}
