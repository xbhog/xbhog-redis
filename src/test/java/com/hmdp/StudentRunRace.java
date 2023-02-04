package com.hmdp;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CountDownLatch的使用和理解
 */
public class StudentRunRace {

    CountDownLatch stopLatch = new CountDownLatch(1);
    CountDownLatch runLatch = new CountDownLatch(10);

    public void waitSignal() throws Exception{
        System.out.println("选手" + Thread.currentThread().getName() + "正在等待裁判发布口令");
        stopLatch.await();
        System.out.println("选手" + Thread.currentThread().getName() + "已接受裁判口令");
        Thread.sleep((long) (Math.random() * 10000));
        System.out.println("选手" + Thread.currentThread().getName() + "到达终点");
        runLatch.countDown();
    }

    public void waitStop() throws Exception{
        Thread.sleep((long) (Math.random() * 10000));
        System.out.println("裁判"+Thread.currentThread().getName()+"即将发布口令");
        stopLatch.countDown();
        System.out.println("裁判"+Thread.currentThread().getName()+"已发送口令，正在等待所有选手到达终点");
        runLatch.await();
        System.out.println("所有选手都到达终点");
        System.out.println("裁判"+Thread.currentThread().getName()+"汇总成绩排名");
    }

    public static void main(String[] args) {
        ExecutorService service = Executors.newCachedThreadPool();
        StudentRunRace studentRunRace = new StudentRunRace();
        for (int i = 0; i < 10; i++) {
            Runnable runnable = () -> {
                try {
                    studentRunRace.waitSignal();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            };
            service.execute(runnable);
        }
        try {
            studentRunRace.waitStop();
        } catch (Exception e) {
            e.printStackTrace();
        }
        service.shutdown();
    }
}