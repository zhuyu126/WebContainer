package com.lee.webcon.servlet;

/**
 * Servlet规范之响应规范
 */
public interface MyResponse {
    // 将响应写入到Channel
    void write(String content) throws Exception;
}
