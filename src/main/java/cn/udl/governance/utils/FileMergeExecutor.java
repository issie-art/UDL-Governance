package cn.udl.governance.utils;

import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 文件合并执行器组件
 * 该类使用线程池来执行文件合并相关的任务
 */
@Component
public class FileMergeExecutor {

    /**
     * 创建一个固定大小为2的线程池执行器
     * 用于并发执行文件合并任务
     */
    private final ExecutorService executor =
            Executors.newFixedThreadPool(2);

    /**
     * 提交任务到线程池执行
     * @param task 需要执行的任务，实现Runnable接口
     */
    public void submit(Runnable task) {
        executor.submit(task);
    }
}