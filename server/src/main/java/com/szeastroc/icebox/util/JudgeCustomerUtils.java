package com.szeastroc.icebox.util;

public class JudgeCustomerUtils {

    //通过编号判断是门店还是非门店。
    public static Boolean isStoreType(String customerNumber) {
        Boolean flag = false;
        if (customerNumber.startsWith("C7") ||
                customerNumber.startsWith("C8") ||
                customerNumber.startsWith("C9")) {
            return false;
        } else if (!customerNumber.contains("C")) { //经销商
            return false;
        } else {
            return true;
        }
    }
}
