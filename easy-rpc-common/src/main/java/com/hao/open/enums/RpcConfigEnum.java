package com.hao.open.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum RpcConfigEnum {
    // 资源文件名
    RPC_CONFIG_PATH("rpc.properties"),
    // zookeeper 配置项
    ZK_ADDRESS("rpc.zookeeper.address");

    private final String propertyValue;
}
