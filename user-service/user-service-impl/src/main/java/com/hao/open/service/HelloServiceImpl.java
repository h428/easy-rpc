package com.hao.open.service;

public class HelloServiceImpl implements HelloService {

    static {
        System.out.println("静态加载 HelloServiceImpl");
    }

    @Override
    public String hello(Hello hello) {
        System.out.println("调用 HelloServiceImpl.hello 方法，入参为：" + hello.getMessage());
        String result = "Hello description is " + hello.getDescription();
        System.out.println("调用 HelloServiceImpl.hello 方法结束，返回值为：" + result);
        return result;
    }

}
