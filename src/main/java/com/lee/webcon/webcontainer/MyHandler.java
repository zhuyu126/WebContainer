package com.lee.webcon.webcontainer;

import com.lee.webcon.servlet.MyRequest;
import com.lee.webcon.servlet.MyResponse;
import com.lee.webcon.servlet.MyServlet;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedNioFile;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.EventExecutorGroup;

import javax.activation.MimetypesFileTypeMap;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Map;

/**
 * 服务端处理器
 *
 *   1）从用户请求URI中解析出要访问的Servlet名称
 *   2）从nameToServletMap中查找是否存在该名称的key。若存在，则直接使用该实例，否则执行第3）步
 *   3）从nameToClassNameMap中查找是否存在该名称的key，若存在，则获取到其对应的全限定性类名，
 *      使用反射机制创建相应的serlet实例，并写入到nameToServletMap中，若不存在，则直接访问默认Servlet
 *
 */
public class MyHandler extends ChannelInboundHandlerAdapter {

    private Map<String, MyServlet> nameToServletMap;//线程安全  servlet--> 对象
    private Map<String, String> nameToClassNameMap;//线程不安全  servlet--> 全限定名称

    public MyHandler(Map<String, MyServlet> nameToServletMap, Map<String, String> nameToClassNameMap) {
        this.nameToServletMap = nameToServletMap;
        this.nameToClassNameMap = nameToClassNameMap;
    }
    //静态资源
    // 资源所在路径
    private static final String location;
    static {
        // 构建资源所在路径，此处参数可优化为使用配置文件传入
        location = "src/main/resources/static";
    }
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) msg;
            String uri = request.uri();
            System.out.println(uri);
            //-------新增
            if ("/favicon.ico".equals(uri)) {
                return;
            }
            if (!uri.contains("?")&&(!uri.equals("/ "))) {
                String url = location + uri;
                File file = new File(url);
                if (!file.exists()) {
                    handleNotFound(ctx, (HttpRequest) msg);
                    return;
                }
                if (file.isDirectory()) {
                    handleDirectory(ctx, (HttpRequest) msg, file);
                    return;
                }
                handleFile(ctx, (HttpRequest) msg, file);
            }
            // 从请求中解析出要访问的Servlet名称
            //aaa/bbb/twoservlet?name=aa
            String servletName = "";
            if (uri.contains("?") && uri.contains("/")){
                servletName= uri.substring(uri.lastIndexOf("/") + 1, uri.indexOf("?"));
            }

            MyServlet servlet = new DefaultWebServlet();
            //第一次访问，Servlet是不会被加载的
            //初始化加载的只是类全限定名称，懒加载
            //如果访问Servlet才会去初始化它对象
            if (nameToServletMap.containsKey(servletName)) {
                servlet = nameToServletMap.get(servletName);
            } else if (nameToClassNameMap.containsKey(servletName)) {
                // double-check，双重检测锁：为什么要在锁前判断一次，还要在锁后继续判断一次？
                if (nameToServletMap.get(servletName) == null) {
                    synchronized (this) {
                        if (nameToServletMap.get(servletName) == null) {
                            // 获取当前Servlet的全限定性类名
                            String className = nameToClassNameMap.get(servletName);
                            // 使用反射机制创建Servlet实例
                            servlet = (MyServlet) Class.forName(className).newInstance();
                            // 将Servlet实例写入到nameToServletMap
                            nameToServletMap.put(servletName, servlet);
                        }
                    }
                }
            } //  end-else if

            // 代码走到这里，servlet肯定不空
            MyRequest req = new MyHttpRequest(request);
            MyResponse res = new MyHttpResponse(request, ctx);
            // 根据不同的请求类型，调用servlet实例的不同方法 //静态资源该怎么处理
            if (request.method().name().equalsIgnoreCase("GET")) {
                servlet.doGet(req, res);
            } else if(request.method().name().equalsIgnoreCase("POST")) {
                servlet.doPost(req, res);
            }
            ctx.close();
        }
    }

    private void handleNotFound(ChannelHandlerContext ctx, HttpRequest msg) {
        ByteBuf content = Unpooled.copiedBuffer("URL not found", CharsetUtil.UTF_8);
        HttpResponse response = new DefaultFullHttpResponse(msg.protocolVersion(), HttpResponseStatus.NOT_FOUND, content);
        ChannelFuture future = ctx.writeAndFlush(response);
        future.addListener(ChannelFutureListener.CLOSE);
    }

    private void handleFile(ChannelHandlerContext ctx, HttpRequest msg, File file) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        HttpHeaders headers = getContentTypeHeader(file);
        HttpResponse response = new DefaultHttpResponse(msg.protocolVersion(), HttpResponseStatus.OK, headers);
        ctx.write(response);
        ctx.write(new DefaultFileRegion(raf.getChannel(), 0, raf.length()));
        ChannelFuture future = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        future.addListener(ChannelFutureListener.CLOSE);
    }

    private HttpHeaders getContentTypeHeader(File file) {
            MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
            HttpHeaders headers = new DefaultHttpHeaders();
            String contentType = mimeTypesMap.getContentType(file);
            if (contentType.equals("text/plain")) {
                //由于文本在浏览器中会显示乱码，此处指定为utf-8编码
                contentType = "text/plain;charset=utf-8";
            }
            headers.set(HttpHeaderNames.CONTENT_TYPE, contentType);
            return headers;
    }

    private void handleDirectory(ChannelHandlerContext ctx, HttpRequest msg, File file) {
        StringBuilder sb = new StringBuilder();
        sb.append("欢迎使用MyContainerWeb容器");
        ByteBuf buffer = ctx.alloc().buffer(sb.length());
        buffer.writeCharSequence(sb.toString(), CharsetUtil.UTF_8);
        FullHttpResponse response = new DefaultFullHttpResponse(msg.protocolVersion(), HttpResponseStatus.OK, buffer);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
        ChannelFuture future = ctx.writeAndFlush(response);
        future.addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }


}
