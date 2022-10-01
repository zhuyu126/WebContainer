package com.lee.webcon.webcontainer;

public class MyWebContainer {
    public static void main(String[] args) throws Exception {
        MyWebContainerServer server = new MyWebContainerServer("com.lee.webcon.webapp");
        server.start();
    }

}
