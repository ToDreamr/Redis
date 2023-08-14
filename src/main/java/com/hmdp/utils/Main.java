package com.hmdp.utils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Rainy-Heights
 * @version 1.0
 * @Date 2023/8/4 13:14
 */
public class Main {
    private static final ExecutorService theadPool= Executors.newFixedThreadPool(10);

    private static final ReentrantLock lock=new ReentrantLock();
    public static void main(String[] args) throws URISyntaxException, IOException {

        URL url=new URL("https://www.baidu.com/");

        InputStream inputStream = url.openStream();
        BufferedInputStream bufferedInputStream=new BufferedInputStream(inputStream);

        byte[] bytes=new byte[bufferedInputStream.available()];
        PrintStream printStream=new PrintStream(System.out);
        while ((inputStream.read(bytes)!=-1)){
            printStream.write(bytes);
        }
    }
}
