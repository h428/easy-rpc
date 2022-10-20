package com.hao.open.remoting.handler;

import com.hao.open.exception.RpcException;
import com.hao.open.factory.SingletonFactory;
import com.hao.open.provider.ServiceProvider;
import com.hao.open.provider.impl.ZkServiceProviderImpl;
import com.hao.open.remoting.dto.RpcRequest;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 通用 Rpc 请求处理器，可供 Socket 和 Netty 两种连接方式进行复用
 */
@Slf4j
public class RpcRequestHandler {

    private final ServiceProvider serviceProvider;

    public RpcRequestHandler() {
        // 基于 zookeeper 的服务提供管理器
        serviceProvider = SingletonFactory.getInstance(ZkServiceProviderImpl.class);
    }

    /**
     * 服务端处理 rpcRequest，找到对应的 service 然后调用对应方法
     */
    public Object handle(RpcRequest rpcRequest) {
        // 获取已注册的服务
        Object service = serviceProvider.getService(rpcRequest.getRpcServiceName());
        // 调用方法并返回
        return invokeTargetMethod(rpcRequest, service);
    }

    /**
     * 根据传入的 rpcRequest，服务端真正进行调用并获得结果
     *
     * @param rpcRequest client request
     * @param service    service object
     * @return the result of the target method execution
     */
    private Object invokeTargetMethod(RpcRequest rpcRequest, Object service) {
        Object result;
        try {
            Method method = service.getClass().getMethod(rpcRequest.getMethodName(), rpcRequest.getParamTypes());
            result = method.invoke(service, rpcRequest.getParameters());
            log.info("service:[{}] successful invoke method:[{}]", rpcRequest.getInterfaceName(), rpcRequest.getMethodName());
        } catch (NoSuchMethodException | IllegalArgumentException | InvocationTargetException | IllegalAccessException e) {
            throw new RpcException(e.getMessage(), e);
        }
        return result;
    }
}
