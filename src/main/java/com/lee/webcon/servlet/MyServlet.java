package com.lee.webcon.servlet;

/**
 * 定义Servlet规范
 */
public abstract class MyServlet {
    public abstract void doGet(MyRequest request, MyResponse response)throws Exception;
    public abstract void doPost(MyRequest request, MyResponse response)throws Exception;
    public void service(MyRequest myRequest,MyResponse myResponse) throws Exception {
        if(myRequest.getMethod().equalsIgnoreCase("POST")){
            doPost(myRequest,myResponse);
        }else if (myRequest.getMethod().equalsIgnoreCase("GET")){
            doGet(myRequest,myResponse);
        }
    }
}
