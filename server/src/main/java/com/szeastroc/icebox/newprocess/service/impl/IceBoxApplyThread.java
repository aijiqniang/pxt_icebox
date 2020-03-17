package com.szeastroc.icebox.newprocess.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.icebox.newprocess.dao.IceBoxDao;
import com.szeastroc.icebox.newprocess.entity.IceBox;
import com.szeastroc.icebox.newprocess.enums.PutStatus;
import com.szeastroc.icebox.newprocess.vo.request.IceBoxRequestVo;
import com.szeastroc.icebox.util.RedisTool;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;

import java.util.Date;
import java.util.List;

@Slf4j
public class IceBoxApplyThread implements Runnable{

    private static final Integer  Lock_Timeout = 3000;    // 过期时间 代表 3秒后过期
    private static final Integer  ExecuteTime = 5000;
    private static final Integer  RetryInterval = 20;
    private static final String  lockKey = "LockKey";
    private volatile static  boolean  working  = true;


    private IceBoxDao iceBoxDao;

    private IceBoxRequestVo iceBoxRequestVo;

    public IceBoxApplyThread(IceBoxDao iceBoxDao,IceBoxRequestVo iceBoxRequestVo) {
        this.iceBoxDao = iceBoxDao;
        this.iceBoxRequestVo = iceBoxRequestVo;
    }


    public void applyIceBox(Jedis jedis) {
        String name = Thread.currentThread().getName();
        boolean gotLock = false;
        List<IceBox> iceBoxes = iceBoxDao.selectList(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getModelId, iceBoxRequestVo.getModelId()).eq(IceBox::getSupplierId, iceBoxRequestVo.getSupplierId()).eq(IceBox::getPutStatus, PutStatus.NO_PUT.getStatus()));
        try{
            gotLock = RedisTool.tryGetDistributedLock(jedis,lockKey, name,Lock_Timeout);
            if( gotLock && working){
                // Do your job
                if (CollectionUtil.isNotEmpty(iceBoxes)) {
                    IceBox iceBox = iceBoxes.get(0);
                    log.info("asjfksldfhvdjkghkjh-->"+ JSON.toJSONString(iceBox));
                    iceBox.setPutStoreNumber(iceBoxRequestVo.getStoreNumber()); //
                    iceBox.setPutStatus(PutStatus.LOCK_PUT.getStatus());
                    iceBox.setUpdatedTime(new Date());
                    iceBoxDao.updateById(iceBox);
                }else{
                    working = false;
                    throw new ImproperOptionException("无可申请冰柜");
                }
                Thread.sleep(ExecuteTime);
            }else{
                Thread.sleep(RetryInterval);
            }
        }catch(Exception e){
            System.out.println(e);
        }finally {
            try {
                //未获取到锁的线程不用解锁
                if(!gotLock||!working){
                    return;
                }
                /**
                 * 解锁成功后 sleep, 尝试让出cpu给其他线程机会
                 * 解锁失败 说明锁已经失效 被其他线程获取到
                 */
                if(RedisTool.releaseDistributedLock(jedis,lockKey,name)){
                    Thread.sleep(100);
                }
            }catch (Exception e ) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void run() {
        Jedis jedis = new Jedis("10.136.15.102", 6379);
        jedis.auth("myredis");
        try{
            while (working) {
                applyIceBox(jedis);
            }
        }catch (Exception e){e.printStackTrace();}


    }
}
