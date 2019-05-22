package scorpio.rao.db2sql.util;

/**
 * Created by scorpio.rao on 2019/5/20
 */
public class SqlUtil {

    public static String pageQuery(String sql,int pageIndex,int endIndex){
        String pageSql = "select * from (select t.*,rowNum r from ("+sql+") t where rowNum<"
                +endIndex+") where r>="+pageIndex;
        return pageSql;
    }
}
