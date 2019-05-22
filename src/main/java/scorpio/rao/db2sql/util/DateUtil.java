package scorpio.rao.db2sql.util;

/**
 * Created by scorpio.rao on 2019/5/17
 */
public class DateUtil {

    public static String dateConvert(String date){
        if (date == null || "null".equals(date.toLowerCase())){
            return "null";
        }
        return "TO_DATE('"+date+"','yyyy-MM-dd HH24:Mi:ss')";
    }
}
