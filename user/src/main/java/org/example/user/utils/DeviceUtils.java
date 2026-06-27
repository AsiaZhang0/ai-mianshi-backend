package org.example.user.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.Header;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.example.common.exception.ErrorCode;
import org.example.common.exception.ThrowUtils;

public class DeviceUtils {

    /**
     * 根据秦秋获取设备信息
     */

    public static String getRequestDevice(HttpServletRequest request){
        String userAgentStr = request.getHeader(Header.USER_AGENT.toString());

        //使用Hutool解析UserAgent
        UserAgent userAgent = UserAgentUtil.parse(userAgentStr);
        ThrowUtils.throwIf(userAgent == null, ErrorCode.OPERATION_ERROR, "非法请求");

        //默认值是PC
        String device = "pc";

        //是否为小程序
        if(isMiniProgram(userAgentStr)){
            device = "miniProgram";
        }else if(isPad(userAgentStr)){
            //是否为Pad
            device = "pad";
        }else if(userAgent.isMobile()){
            //是否为手机
            device= "mobile";
        }

        return device;
    }

    /*
     * 判断是否是小程序
     * 一般通过User-Agent 字符串中的 "MicroMessenger" 来片段是否是微信小程序
     */
    public static boolean isMiniProgram(String userAgentStr){
        //判断User-Agent 是否包含 "MicroMessenger" 表示微信环境

        return StrUtil.containsIgnoreCase(userAgentStr, "MicroMessenger")
                && StrUtil.containsIgnoreCase(userAgentStr, "MiniProgram");
    }

    /**
     * 判断是否为平板设备
     * 支持 IOS（如ipad)和Android 平板的监测
     */

    private static boolean isPad(String userAgentStr){
        boolean isIpad = StrUtil.containsIgnoreCase(userAgentStr, "iPad");

        //检查Android 平板（包含 "Android"且不包含 "Mobile"）
        boolean isAndroidTablet = StrUtil.containsIgnoreCase(userAgentStr, "Android")
                && !StrUtil.containsIgnoreCase(userAgentStr, "Mobile");


        //如果是iPad 或Android 平板，则返回true;

        return isIpad || isAndroidTablet;
    }
}
