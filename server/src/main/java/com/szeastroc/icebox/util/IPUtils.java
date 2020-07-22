package com.szeastroc.icebox.util;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

@Component
@RequestMapping("ipUtil")
public class IPUtils {

    @RequestMapping("getIP")
    @ResponseBody
    public String getIP(HttpServletRequest request){
        return request.getRemoteAddr();

    }
}
