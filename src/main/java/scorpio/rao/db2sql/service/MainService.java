package scorpio.rao.db2sql.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Service;
import scorpio.rao.db2sql.util.DateUtil;
import scorpio.rao.db2sql.util.IOUtil;

import java.io.BufferedWriter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by scorpio.rao on 2019/5/17
 */
@Service
@Slf4j
public class MainService {
    @Value("${db.script.savePath}")
    private static String BASE_URL;

    @Value("${ut.domainId}")
    private static int domainId;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TableService tableService;

    //k:表名,v:列名拼接后的字符串,如id,name,value
    private static Map<String,String> tableMap = new HashMap<>();

    //数据库中表名list
    private static List<String> tables = new ArrayList<>();

    /**
     * 生成脚本的入口
     */
    public void start() {
        //1)初始化,获取到所有表的信息(表名以及表中的列名)
        init();

        //2)生成脚本:给所有表增加一个新字段old_id,并建索引
        addTempIdentification();

        //3)生成数据库中所有表的insert脚本
        handlerTablesToSql();

        //4)生成脚本:删除这些表中的字段old_id,删除索引
        deleteTempField();
    }

    /**
     * 处理查询到的表的数据并写入脚本文件中
     */
    private void handlerTablesToSql() {
        tables.forEach(name->{
            String values = tableMap.get(name);
            String sql2txt = "insert into "+name+" ("+values+") values";
            String sql = "select * from "+name;
            //查询表中所有数据
            SqlRowSet results = jdbcTemplate.queryForRowSet(sql);
            //处理表中的数据并转换为list再写入脚本中
            List<String> list = handlerRowSet(results,name);
            //数据脚本的存放路径
            String savePath = BASE_URL+name+"_insert.sql";
            BufferedWriter writer = IOUtil.getWriter(savePath);
            list.forEach(e->{
                String str = sql2txt+"("+e+");";
                IOUtil.write(writer,str);
            });
            IOUtil.writeCommit(writer);
            IOUtil.closeWritter(writer);
        });
    }

    //todo objid的处理
    /**
     * 由于每个表中id字段的转换规则不一样(媒资的可根据code来查找id,还有其他一些情况的转换)
     * @param results
     * @param table
     * @return
     */
    private List<String> handlerRowSet(SqlRowSet results,String table) {
        List<String> resultList = new ArrayList<>();
        //todo 对表中含有id字段,日期,字符的处理
        String[] columns = tableMap.get(table).split(",");

        //获取这个表的列的总数
        int columnCount = columns.length;

        //1)日期字段所在的位置
        List<Integer> dateIndex = new ArrayList<>();
        //2)表的id所在的位置
        int idIndex = 1;
        //3)表中涉及到的关联表的id所在的位置以及id的转换
        List<Integer> otherIdIndex = new ArrayList<>();

        String tableId = (table + "ID").toUpperCase();

        for (int i=0;i<columnCount;i++) {
            int index = i + 1;
            String column = columns[i].toUpperCase();
            if (column.endsWith("DATE")){
                dateIndex.add(index);
            }
            if (column.equals(tableId)){
                idIndex = index;
            }else if (column.endsWith("ID")){
                otherIdIndex.add(index);
            }
        }

        String idStr = (table+"ID.nextVal").toLowerCase();
        String baseSql = "(select "+tableId+" from "+table+" where old_id=";

        while (results.next()){
            StringBuilder builder = new StringBuilder();
            for (int i=1;i<columnCount;i++){
                if (idIndex == i){
                    builder.append(idStr).append(",");
                }else if (dateIndex.contains(i)){
                    builder.append(DateUtil.dateConvert(results.getString(i))).append(",");
                }else if (otherIdIndex.contains(i)){
                    String str = baseSql+results.getString(i) +" and rowNum=1)";
                    builder.append(str).append(",");
                }else {
                    //字符加''
                    String strConvert = "'"+results.getString(i)+"'";
                    builder.append(strConvert).append(",");
                }
            }
            resultList.add(builder.deleteCharAt(builder.length()-1).append(";").toString());
        }

        return resultList;
    }

    /**
     * 初始化tables以及tableMap
     */
    public void init() {
        //获取当前用户下数据库中所有的表名
        SqlRowSet tableResult = jdbcTemplate.queryForRowSet("select table_name from user_tables");
        while (tableResult.next()){
            tables.add(tableResult.getString("table_name"));
        }

        //获取表与列名拼接后的map
        String sql = "select column_name from user_tab_cols where table_name=";
        tables.forEach(name->{
            SqlRowSet columns = jdbcTemplate.queryForRowSet(sql+"'"+name+"'");
            handlerColumnResult(columns,name);
        });
        log.info("表名list : {}",tables);
        log.info("表名和列的关联map : {}",tableMap);
        List<String> list = tableMap.entrySet().stream().filter(e -> e.getValue().contains("OBJECTTYPE") || e.getValue().contains("OBJTYPE"))
                .map(Map.Entry::getKey).collect(Collectors.toList());
        log.info("含有objecttype字段的表:{}",list);
    }

    private void handlerColumnResult(SqlRowSet columns, String table) {
        StringBuilder builder = new StringBuilder();
        while(columns.next()){
            builder.append(columns.getString("column_name")).append(",");
        }
        tableMap.put(table,builder.deleteCharAt(builder.length()-1).toString());
    }

    /**
     * 给所有表新增一个字段用来存放原数据库中的id,
     * 作为唯一标识使得关联表可以依据这个来找到源数据入库后新的id
     */
    private void addTempIdentification() {
        String[] args = new String[3];
        args[0] = "firstScript.sql";
        args[1] = "alter table ";
        args[2] = " add (old_id number(9) default 0);";
        handlerTemp(args);
    }

    /**
     * 脚本执行完成后删除表中新增的属性
     */
    private void deleteTempField() {
        String[] args = new String[3];
        args[0] = "lastScript.sql";
        args[1] = "alter table ";
        args[2] = " drop column old_id;";
        handlerTemp(args);
    }

    private void handlerTemp(String[] strs){
        String url = BASE_URL+strs[0];
        BufferedWriter writer = IOUtil.getWriter(url);
        tables.forEach(e->{
            String sql = strs[1] + e + strs[2];
            IOUtil.writeWithCommit(writer,sql);
        });
        IOUtil.closeWritter(writer);
    }

}
