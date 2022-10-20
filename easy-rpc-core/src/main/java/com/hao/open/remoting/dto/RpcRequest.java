package com.hao.open.remoting.dto;

import lombok.*;

import java.io.Serializable;

/**
 * 进行一次 rpc 调用所需的基本信息
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@ToString
public class RpcRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    // 请求 id
    private String requestId;
    // 调用接口
    private String interfaceName;
    // 调用方法名
    private String methodName;
    // 参数值
    private Object[] parameters;
    // 参数类型
    private Class<?>[] paramTypes;
    // 接口版本
    private String version;
    // 接口分组
    private String group;

    public String getRpcServiceName() {
        return this.getInterfaceName() + this.getGroup() + this.getVersion();
    }
}
