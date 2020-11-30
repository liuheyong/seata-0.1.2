package com.alibaba.fescar.server.multhread;

import java.util.concurrent.CountDownLatch;

/**
 * @author: liuheyng
 * @date: 2020/11/30 16:11
 * @description: TestCountDownLatch
 */
public class TestCountDownLatch {

    public static void main(String[] args) {
        testCountDownLatch();
    }

    public static void testCountDownLatch() {
        int threadCount = 10;
        final CountDownLatch latch = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    System.out.println("线程===" + Thread.currentThread().getId() + "===开始执行");
                    try {
                        Thread.sleep(1000);
                        System.out.println("线程===" + Thread.currentThread().getId() + "===执行结束");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
                }
            }).start();
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("子线程已经执行完毕，main开始执行后续任务。");
    }

}
