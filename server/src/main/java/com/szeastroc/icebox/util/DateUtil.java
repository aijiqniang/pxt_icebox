/*
 * Copyright (c) 2016, Letsun and/or its affiliates. All rights reserved.
 * Use, Copy is subject to authorized license.
 */
package com.szeastroc.icebox.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @author shiw
 * @date 2017年6月12日
 */
public final class DateUtil {

    /**
     * 日期格式常量类：yyyy-MM-dd
     */
    public static final String YYYY_MM_DD = "yyyy-MM-dd";

    /**
     * 日期格式常量类：yyyy-MM-dd HH:mm:ss
     */
    public static final String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";

    /**
     * 日期格式常量类：yyyy-MM-dd HH:mm:ss
     */
    public static final String YYYY_MM_DD_HH_MM = "yyyy-MM-dd HH:mm";

    /**
     * 日期格式常量类：yyyy-MM-dd HH
     */
    public static final String YYYY_MM_DD_HH = "yyyy-MM-dd HH";

    /**
     * 日期格式常量类：yyyyMMddHHmm
     */
    public static final String YYYYMMDDHHMM = "yyyyMMddHHmm";

    /**
     * 日期格式常量类：yyyyMM
     */
    public static final String YYYYMM = "yyyyMM";

    /**
     * 日期格式常量类：yyyy-MM
     */
    public static final String YYYY_MM = "yyyy-MM";

    /**
     * 日期格式常量类：yyyyMMdd
     */
    public static final String YYYYMMdd = "yyyyMMdd";


    /**
     * 日期格式
     **/
    public static final String YMD24H_DATA = "yyyy-MM-dd HH:mm:ss";

    /**
     * 日期格式 yyyy-MM-dd HH:mm:ss.s
     **/
    public static final String YYYY_MM_DD_HH_MM_SS_S = "yyyy-MM-dd HH:mm:ss.S";

    /**
     * 日期格式 yyyyMMddHHmmssS
     **/
    public static final String YYYYMMDDHHMMSSS = "yyyyMMddHHmmssS";

    public static enum TimeValue {
        D, H, M, S;
    }

    /**
     * 获取两个时间差
     *
     * @param startTime
     * @param endTime   d:返回天数差，h：返回小数差，m：返回分钟差
     * @return
     */
    public static int minus(Date startTime, Date endTime, TimeValue timeValue) {
        long times = startTime.getTime() - endTime.getTime();
        int result = 0;
        switch (timeValue) {
            case D:
                result = (int) (times / 1000 / 60 / 60 / 24);
                break;
            case H:
                result = (int) (times / 1000 / 60 / 60);
                break;
            case M:
                result = (int) (times / 1000 / 60);
                break;
            case S:
                result = (int) (times / 1000 / 60 / 60 / 60);
                break;
        }
        return result;
    }

    /**
     * 把日期加天数
     *
     * @param date 需要处理日期
     * @param day  增加天数
     * @return Date
     */
    public static Date addDay(Date date, int day) {
        if (date == null) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, day);
        return calendar.getTime();
    }

    /**
     * 日期处理
     *
     * @param date
     * @param type Calendar单位类型,天小时分钟秒
     * @param unit 增量
     * @return
     * @author zhonngzhu
     * @since 2018年2月6日
     */
    public static Date addTimeUnit(Date date, int type, int unit) {
        if (date == null) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        switch (type) {
            case Calendar.DAY_OF_MONTH:
                calendar.add(Calendar.DAY_OF_MONTH, unit);
                break;
            case Calendar.HOUR_OF_DAY:
                calendar.add(Calendar.HOUR_OF_DAY, unit);
                break;
            case Calendar.MINUTE:
                calendar.add(Calendar.MINUTE, unit);
                break;
            case Calendar.SECOND:
                calendar.add(Calendar.SECOND, unit);
                break;
            default:
                return null;
        }
        return calendar.getTime();
    }

    /**
     * 获得当天开始时间
     *
     * @param date
     * @return
     */
    public static Date dayBegin(Date date) {
        Calendar calendar = Calendar.getInstance();
        if (date != null) {
            calendar.setTime(date);
        }
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        return calendar.getTime();
    }

    /**
     * 获得当天的结束时间
     *
     * @param date
     * @return
     */

    public static Date dayEnd(Date date) {
        Calendar calendar = Calendar.getInstance();
        if (date != null) {
            calendar.setTime(date);
        }
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        return calendar.getTime();
    }

    /**
     * 将 Calendar 对象转换为制定格式字符串
     *
     * @param calendar
     * @param format
     * @return
     */
    public static String getDateFormat(Calendar calendar, String format) {
        return getDateFormat(calendar.getTime(), format);
    }

    /**
     * 将 Date 对象转换为制定格式字符串
     *
     * @param date
     * @param format
     * @return
     */
    public static String getDateFormat(Date date, String format) {
        String result = "";
        try {
            if (date != null) {
                DateFormat dateFormat = new SimpleDateFormat(format);
                result = dateFormat.format(date);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 获取当前月的第一天
     *
     * @return
     */
    public static String getMonthStart() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH));

        return getDateFormat(calendar, "yyyy-MM-dd");
    }

    /**
     * <判断是否是当月第一天>
     *
     * @param date
     * @return
     * @author island(YQ)
     * @since 2018年3月27日
     */
    public static boolean isFirstDayOfMonth(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.DAY_OF_MONTH) == 1;
    }

    /**
     * <获取当月最后一天>
     *
     * @return
     * @author island(YQ)
     * @since 2018年2月27日
     */
    public static String getMonthEnd() {
        Calendar ca = Calendar.getInstance();
        ca.set(Calendar.DAY_OF_MONTH, ca.getActualMaximum(Calendar.DAY_OF_MONTH));
        return getDateFormat(ca, "yyyy-MM-dd");
    }

    /**
     * 获取对应月份的最后一天
     *
     * @return
     */
    public static String doGetMonthEnd(String querymonth) {
        String monthend = "";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date date = sdf.parse(querymonth);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(Calendar.MONTH, 1);
            calendar.add(Calendar.DAY_OF_YEAR, -1);
            monthend = sdf.format(calendar.getTime());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return monthend;
    }

    /**
     * 日期转化为字串
     *
     * @param date    传入日期
     * @param pattern 格式
     * @return 转化结果
     */
    public static String dateToStr(Date date, String pattern) {
        if (date == null || pattern == null) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        return sdf.format(date);
    }

    /**
     * 将 日期字符串转为 Date 对象
     *
     * @param dateStr
     * @param format
     * @return Date 类型时间
     */
    public static Date strToDate(String dateStr, String format) {
        if (dateStr == null || dateStr.length() == 0) {
            return null;
        }
        Date date = new Date();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            date = sdf.parse(dateStr);
        } catch (Exception e) {
            date = null;
        }
        return date;
    }

    /**
     * <增加月份>
     *
     * @param datetime 操作时间
     * @param month    增加月份数
     * @param format   转换格式
     * @return String
     * @author island(YQ)
     * @since 2018年3月27日
     */
    public static String addMonth(Date datetime, int month, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        try {
            Calendar cl = Calendar.getInstance();
            cl.setTime(datetime);
            cl.add(Calendar.MONTH, month);
            datetime = cl.getTime();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sdf.format(datetime);
    }

    /**
     * <增加月份>
     */
    public static Date addMonth(Date datetime, int month) {
        try {
            Calendar cl = Calendar.getInstance();
            cl.setTime(datetime);
            cl.add(Calendar.MONTH, month);
            datetime = cl.getTime();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return datetime;
    }

    /**
     * <时间字符串格式化>
     *
     * @param strFormat    参数格式
     * @param dateStr      转换时间
     * @param resultFormat 转换格式
     * @return strDate
     * @author island(YQ)
     * @since 2018年3月28日
     */
    public static String formatStr(String strFormat, String dateStr, String resultFormat) {
        try {
            SimpleDateFormat sdf2 = new SimpleDateFormat(resultFormat);
            SimpleDateFormat sdf = new SimpleDateFormat(strFormat);
            Date date = sdf.parse(dateStr);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            return sdf2.format(calendar.getTime());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * <获取项目开始到new Date()的 月份集合      YYYYMM>
     *
     * @return List<String>
     * @author island(YQ)
     * @since 2018年1月24日
     */
    public static List<String> getMonthList() {
        try {
            //定义日期实例
            Calendar dd = Calendar.getInstance();
            //设置日期起始时间
            dd.setTime(new Date());
            String str = "0";
            List<String> monthList = new ArrayList<String>();
            do {//判断是否到结束日期
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMM");
                str = sdf.format(dd.getTime());
                monthList.add(str);
                dd.add(Calendar.MONTH, -1);
            } while (Integer.valueOf(str) > 201706);
            return monthList;
        } catch (Exception e) {
            return null;
        }

    }

    /**
     * 获取指定时间加减年
     *
     * @param difference
     * @param date
     * @return
     */
    public static Date getDifferenceYear(Integer difference, Date date) {
        //定义日期实例
        Calendar calendar = Calendar.getInstance();
        //设置日期起始时间
        calendar.setTime(date);
        calendar.add(Calendar.YEAR, difference);
        return new Date(calendar.getTime().getTime());
    }

    /**
     * 获取指定时间加减月
     *
     * @param difference
     * @param date
     * @return
     */
    public static Date getDifferenceMonth(Integer difference, Date date) {
        //定义日期实例
        Calendar calendar = Calendar.getInstance();
        //设置日期起始时间
        calendar.setTime(date);
        calendar.add(Calendar.MONTH, difference);
        return new Date(calendar.getTime().getTime());
    }


    /**
     * 获取指定时间加减天
     *
     * @param difference
     * @param date
     * @return
     */
    public static Date getDifferenceDay(Integer difference, Date date) {
        //定义日期实例
        Calendar calendar = Calendar.getInstance();
        //设置日期起始时间
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, difference);
        return new Date(calendar.getTime().getTime());
    }

    /**
     * 获取指定时间加减分钟
     *
     * @param difference
     * @param date
     * @return
     */
    public static Date getDifferenceMinute(Integer difference, Date date) {
        //定义日期实例
        Calendar calendar = Calendar.getInstance();
        //设置日期起始时间
        calendar.setTime(date);
        calendar.add(Calendar.MINUTE, difference);
        return new Date(calendar.getTime().getTime());
    }

    /**
     * 比较两个时间年月的大小
     *
     * @param date1
     * @param date2
     * @return
     */
    public static int compareTo(Date date1, Date date2) {
        int sum1 = date1.getYear() * 100 + date1.getMonth();
        int sum2 = date2.getYear() * 100 + date2.getMonth();
        return (sum1 < sum2 ? -1 : (sum1 == sum2 ? 0 : 1));
    }

    /**
     * 获取某个月的天数
     * @param date
     * @return
     */
    public static int getDaysOfMonth(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    /**
     * 将unix时间戳转成指定格式时间
     * @param time
     * @param dateFormat
     * @return
     */
    public static String formatDateByPattern(long time, String dateFormat) {
        Date date = new Date(time * 1000);
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
        String formatTimeStr = null;
        if (date != null) {
            formatTimeStr = sdf.format(date);
        }
        return formatTimeStr;
    }

    /**
     * 将date转为unix时间戳
     *
     * @param date
     * @return
     */
    public static long dateFormatUnix(Date date) {
        return (date.getTime()) / 1000;
    }

    /**
     * 获取本周星期一
     */
    public static String getWeekStartDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int dayWeek = calendar.get(Calendar.DAY_OF_WEEK);
        if (1 == dayWeek) {
            calendar.add(Calendar.DAY_OF_MONTH, -1);
        }
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        int current = calendar.get(Calendar.DAY_OF_WEEK); //获取当天周内天数

        calendar.add(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek()-current); //当天-基准，获取周开始日期
        return getDateFormat(calendar, "yyyy-MM-dd");
    }

    /**
     * 获取本周星期日
     */
    public static String getWeekEndDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int dayWeek = calendar.get(Calendar.DAY_OF_WEEK);
        if (1 == dayWeek) {
            calendar.add(Calendar.DAY_OF_MONTH, -1);
        }
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        int current = calendar.get(Calendar.DAY_OF_WEEK); //获取当天周内天数
        calendar.add(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek()-current); //当天-基准，获取周开始日期
        calendar.add(Calendar.DAY_OF_WEEK, 6); //开始+6，获取周结束日期
        return getDateFormat(calendar, "yyyy-MM-dd");
    }

    /**
     * 获取当前月份最后一天
     */
    public static String dateTimeMonthEnd(Date date){
        Calendar cale = Calendar.getInstance();
        cale.setTime(date);
        cale.add(Calendar.MONTH, 1);
        cale.set(Calendar.DAY_OF_MONTH, 0);
        return getDateFormat(cale, "yyyy-MM-dd");
    }

    /**
     * 获取当前月份第一天
     */
    public static String dateTimeMonthStart(Date date){
        Calendar cale = Calendar.getInstance();
        cale.add(Calendar.MONTH, 0);
        cale.setTime(date);
        cale.set(Calendar.DAY_OF_MONTH, 1);
        return getDateFormat(cale, "yyyy-MM-dd");
    }

}
