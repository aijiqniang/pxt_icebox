package com.szeastroc.icebox.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.icebox.dao.ClientInfoDao;
import com.szeastroc.icebox.entity.ClientInfo;
import com.szeastroc.icebox.service.ClientInfoService;
import org.springframework.stereotype.Service;

/**
 * Created by Tulane
 * 2019/5/21
 */
@Service
public class ClientInfoServiceImpl  extends ServiceImpl<ClientInfoDao, ClientInfo> implements ClientInfoService {
}
