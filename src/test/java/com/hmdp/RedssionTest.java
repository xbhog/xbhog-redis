package com.hmdp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author xbhog
 * @describe:
 * @date 2023/2/14
 */
@SpringBootTest
public class RedssionTest {
    @Resource
    private RedissonClient redissonClentUtil;

    @Test
    public void remissionDemo() throws InterruptedException {
        //获取锁(可重入)，指定锁的名称
        RLock anyLock = redissonClentUtil.getLock("anyLock");
        //尝试获取锁，参数分别是：获取锁的最大等待时间(期间会重试)，锁自动释放时间，时间单位
        boolean b = anyLock.tryLock(1, 10, TimeUnit.MINUTES);
        if (b) {
            System.out.println("加锁成功");
        } else {
            System.out.println("加锁失败");
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class Person {
        private String name;
        private String age;
    }

    private final List<String> myList =
            Arrays.asList("a1", "a2", "b1", "c2", "c1");

    @Test
    public void demo1() {
        List<Person> list = new ArrayList<>();
        list.add(new Person("1", "1"));
        list.add(new Person("1", "4"));
        list.add(new Person("2", "2"));
        list.add(new Person("3", "3"));
        Map<String, Person> collect = list.stream().collect(Collectors.toMap(Person::getName, Function.identity()));
        //解决冲突
        Map<String, Person> collect1 = list.stream().collect(Collectors.toMap(Person::getName, Function.identity(), (a, b) -> a));
    }

    @Test
    public void FilterAndtoUp() {
        myList.stream().filter(x -> x.startsWith("a"))
                .map(String::toUpperCase)
                .sorted()
                .collect(Collectors.toList()).forEach(System.out::println);
    }

    private static final ExecutorService thread = Executors.newFixedThreadPool(3);
    private static final ThreadPoolTaskExecutor threadA = new ThreadPoolTaskExecutor();

    @Test
    public void supplyAsync() {
        this.supplyAsyncTest(() -> {
            System.out.println("---------------" + Thread.currentThread().getName());
            System.out.println("异步处理：" + Thread.currentThread().getId());
        });
        //等待所有线程执行完成在执行后续操作
        CompletableFuture.allOf().join();
    }

    private void supplyAsyncTest(Runnable asyncTest) {
        CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
            asyncTest.run();
            return Boolean.TRUE;
        }, thread);
        try {
            Boolean aBoolean = future.get();
            System.out.println("异步处理结果：" + aBoolean);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void clazzTest() {
        /*Field[] fields = getFields(Person.class);
        System.out.println(Arrays.toString(fields));*/
        Field[] fields = Arrays.stream(Person.class.getDeclaredClasses())
                .toArray(Field[]::new);
        String[] strings = Arrays.stream(fields).map(Field::getName).toArray(String[]::new);
        for (int i = 0; i < strings.length; i++) {
            System.out.println(strings[i]);
        }
        Person person = new Person("xbhog", "sdas");
        Person person1 = new Person();
        System.out.println(Objects.isNull(null));
        Optional.ofNullable(null).orElseThrow(() ->
            new RuntimeException("对象为空")
        );
    }

}
