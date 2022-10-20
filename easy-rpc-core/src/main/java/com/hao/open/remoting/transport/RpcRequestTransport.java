package com.hao.open.remoting.transport;


import com.hao.open.extension.SPI;
import com.hao.open.remoting.dto.RpcRequest;

/**
 * 抽象 rpc 接口，用于服务消费者进行 rpc 调用
 */
@SPI
public interface RpcRequestTransport {
    /**
     * send rpc request to server and get result
     *
     * @param rpcRequest message body
     * @return data from server
     */
    Object sendRpcRequest(RpcRequest rpcRequest);
}
