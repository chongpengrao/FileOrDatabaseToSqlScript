package scorpio.rao.db2sql.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Service;
import scorpio.rao.db2sql.config.TableConfig;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by scorpio.rao on 2019/5/24
 *
 * 导入smp基础媒资数据到多域sop作为库存域数据
 */
@Service
@Slf4j
public class MediaExportService {

    @Resource(name = "jdbcTemplate2")
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TableConfig tableConfig;

    //库存域下基础媒资表
    private static List<String> baseMediaTables = new ArrayList<>();

    //smp数据库中的表和sop库中的表的公共column
    private static Map<String,String> columnMap = new HashMap<>();

    //dbLink的连接名
    private static String linkName;

    public void start(){
        //1)初始化
        init();

        //2)使用dblink,直接向多域sop割接基础数据
        generateAndExecuteSql();
    }

    private void generateAndExecuteSql() {
        baseMediaTables.forEach(table->{
            String values = columnMap.get(table);
            String sql = "insert into "+table+" ("+values+") "+"select "+values+" from "+table+"@"+linkName;
            jdbcTemplate.execute(sql);
            //TODO sequence的更新
//            alter sequence DOMAINID increment by 5 nocache;
//            select DOMAINID.nextval from dual;
//            alter sequence DOMAINID increment by 1 nocache;
            String tableId = table + "ID";
            SqlRowSet rowSet = jdbcTemplate.queryForRowSet("select " + tableId + " from " + table +" where rownum=1 order by " + tableId + " desc");
            int increment = 1;
            if (rowSet.next()){
                increment = rowSet.getInt(1);
            }
            String str1 = "alter sequence "+table+"ID increment by "+increment+" nocache";
            String str2 = "select "+table+"ID.nextval from dual";
            String str3 = "alter sequence "+table+"ID increment by 1 nocache";
            jdbcTemplate.execute(str1);
            jdbcTemplate.execute(str2);
            jdbcTemplate.execute(str3);
            log.info("********{}表的数据导入完成...******",table);
        });
        log.info("*************smp向sop数据导入完成...***************");
    }

    private void init() {
        log.info("**********loading...**************");
        //外键约束...先导进去
        baseMediaTables.add("VSP");
        baseMediaTables.add("CONTENTDEF");
        baseMediaTables.add("PROGRAM");
        baseMediaTables.add("SERIES");
        baseMediaTables.add("CHANNEL");
        baseMediaTables.add("METAPICTURE");
        baseMediaTables.add("PICTURETYPE");
        baseMediaTables.add("PICTUREMAP");
        baseMediaTables.add("PHYSICALCHANNEL");
        baseMediaTables.add("MEDIACONTENT");
        baseMediaTables.add("PROGRAMMEDIACONTENT");

        //dblink的名称
        linkName = tableConfig.getLinkName();

        String baseSql1 = "select COLUMN_NAME,DATA_TYPE from user_tab_columns where table_name=";
        String baseSql2 = "select COLUMN_NAME,DATA_TYPE from user_tab_columns@"+linkName+" where table_name=";
        baseMediaTables.forEach(table->{
            String str = "'"+table+"'";
            SqlRowSet rowSet1 = jdbcTemplate.queryForRowSet(baseSql1 + str);
            SqlRowSet rowSet2 = jdbcTemplate.queryForRowSet(baseSql2+str);
            List<String> list1 = handlerRowSet(rowSet1);
            List<String> list2 = handlerRowSet(rowSet2);
            columnMap.put(table,getCommonColumn(list1,list2));
        });
        log.info("**********complete init...**************");
        log.info("****************基础媒资表size:{}, list:{}**************",baseMediaTables.size(),baseMediaTables);
        log.info("****************columnMap-->size:{}, map:{}**************",columnMap.size(),columnMap);
    }

    private String getCommonColumn(List<String> list1,List<String> list2){
        List<String> list = list1.stream().filter(list2::contains).collect(Collectors.toList());
        StringBuilder builder = new StringBuilder();
        list.forEach(e->builder.append(e).append(","));
        return builder.deleteCharAt(builder.length()-1).toString();
    }

    private List<String> handlerRowSet(SqlRowSet result){
        List<String> list = new ArrayList<>();
        while(result.next()){
            String column = result.getString("COLUMN_NAME");
            //查询的时候带上type防止出现未知$可能索引啥的字段?
//            String dataType = result.getString("DATA_TYPE");
            list.add(column);
        }
        return list;
    }
}
