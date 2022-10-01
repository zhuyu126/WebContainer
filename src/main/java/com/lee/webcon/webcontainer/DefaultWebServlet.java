package com.lee.webcon.webcontainer;

import com.lee.webcon.servlet.MyRequest;
import com.lee.webcon.servlet.MyResponse;
import com.lee.webcon.servlet.MyServlet;

/**
 * WebContainer 中对Servlet规范的默认实现
 */
public class DefaultWebServlet extends MyServlet {
    @Override
    public void doGet(MyRequest request, MyResponse response) throws Exception {
        //servlet资源访问 http://localhost:8080/aaa/bbb/userservlet?name=lee
        String uri = request.getUri();
        response.write( (uri.contains("?")?uri.substring(0,uri.lastIndexOf("?")):uri));
//        response.write("404 - no this servlet : " + (uri.contains("?")?uri.substring(0,uri.lastIndexOf("?")):uri));
    }

    @Override
    public void doPost(MyRequest request, MyResponse response) throws Exception {
        doGet(request, response);
    }
}
