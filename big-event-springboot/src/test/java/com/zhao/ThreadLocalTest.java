package com.zhao;

import org.junit.jupiter.api.Test;

/**
 * ThreadLocal提供线程局部变量
 * */

public class ThreadLocalTest  {



    @Test
    public void  testThreadLocalTestSetAndGet() {
        //提供一个ThreadLocal对象
        ThreadLocal tl = new ThreadLocal();
        //开启两个线程
        new Thread(()->{
            tl.set("萧炎");
            //线程名Thread.currentThread().getName()
            System.out.println(Thread.currentThread().getName() + ": "+tl.get());
            System.out.println(Thread.currentThread().getName() + ": "+tl.get());
            System.out.println(Thread.currentThread().getName() + ": "+tl.get());
        },"蓝色").start();
        new Thread(()->{
            tl.set("药尘");
            //线程名Thread.currentThread().getName()
            System.out.println(Thread.currentThread().getName() + ": "+tl.get());
            System.out.println(Thread.currentThread().getName() + ": "+tl.get());
            System.out.println(Thread.currentThread().getName() + ": "+tl.get());
        },"黑色").start();


    }
}
