package com.hello.core.lifecycle;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

public class NetworkClient {
    private  String url;

    public NetworkClient(){
        System.out.println("생성자 호출, url = " + url);

    }

    public void setUrl(String url) {
        this.url = url;
    }

    // 서비스 시작 시 호출
    public void connect(){
        System.out.println("connect: " + url);
    }

    public void call(String message) {
        System.out.println("call: " + url + " message = " + message);
    }

    // 서비스 종료시 호출
    public void disconnect(){
        System.out.println("close: " + url);
    }

    // 의존 관계 주입 끝나면 호출
    @PostConstruct
    public void init() throws Exception {
        System.out.println("NetworkClient.init");
        connect();
        call("초기화 연결 메시지");
    }

    // 스프링 빈이 종료 될 때 호출
    @PreDestroy
    public void close() throws Exception {
        System.out.println("NetworkClient.close");
       disconnect();
    }
}
