package com.alibaba.fescar.tm.test;

import java.util.ArrayList;

/**
 * @author: wenyixicodedog
 * @create: 2020-12-17
 * @description:
 */
public class TestMain {

    public static void main(String[] args) {
        ArrayList<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        list.add(4);
        list.add(5);
        list.stream().forEach(item -> {
            System.out.println(item);
        });
    }

}
