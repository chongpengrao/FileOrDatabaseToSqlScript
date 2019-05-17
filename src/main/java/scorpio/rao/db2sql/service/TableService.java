package scorpio.rao.db2sql.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Created by scorpio.rao on 2019/5/17
 */
@Slf4j
@Service
public class TableService {

    /**
     * 将id 转化为可执行的查询语句,例:
     * programid -->  "select programid from program where code = "
     * + (select code from program where programid = tableid)查询的结果
     * @param tableid
     * @return
     */
    public String convertSql(String tableid) {
        //todo
        return tableid;
    }
}
