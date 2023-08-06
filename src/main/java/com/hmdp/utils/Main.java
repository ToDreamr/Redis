package com.hmdp.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Rainy-Heights
 * @version 1.0
 * @Date 2023/8/4 13:14
 */
public class Main {
    private static final ExecutorService theadPool= Executors.newFixedThreadPool(10);

    private static final ReentrantLock lock=new ReentrantLock();
    private static int sum=0;
    public static void main(String[] args) {

        Runnable task=()->{
            lock.lock();
            synchronized (lock){
                for (int i = 0; i < 10; i++) {
                    sum+=i;
                }
            }
//            lock.unlock();
        };
        Runnable task2=()->{
            synchronized (lock){
                for (int i = 0; i < 10; i++) {
                    sum+=i;
                }
            }
//            lock.unlock();
        };
        task.run();
        task2.run();
        System.out.println(sum);
    }
}
