package com.hmdp.utils;

/**
 * @author xbhog
 * @describe:
 * @date 2023/2/16
 */
public interface ILock {

    boolean tryLock(Long timeOutSec);

    void unLock();
}
