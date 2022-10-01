package com.lee.webcon.webapp;

import com.lee.webcon.servlet.MyRequest;
import com.lee.webcon.servlet.MyResponse;
import com.lee.webcon.servlet.MyServlet;

public class UserServlet extends MyServlet {
    @Override
    public void doGet(MyRequest request, MyResponse response) throws Exception {
        String uri = request.getUri();
        String path = request.getPath();
        String method = request.getMethod();
        String name = request.getParameter("name");

        String content = "uri = " + uri + "\n" +
                "path = " + path + "\n" +
                "method = " + method + "\n" +
                "param = " + name;
        response.write(content);
    }

    @Override
    public void doPost(MyRequest request, MyResponse response) throws Exception {
        this.doPost(request,response);
    }
}
