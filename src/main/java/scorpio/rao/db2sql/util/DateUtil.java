package scorpio.rao.db2sql.util;

/**
 * Created by scorpio.rao on 2019/5/17
 */
public class DateUtil {

    public static String dateConvert(String date){
        return "TO_DATE('"+date+"','yyyy-MM-dd HH24:Mi:ss')";
    }
}
