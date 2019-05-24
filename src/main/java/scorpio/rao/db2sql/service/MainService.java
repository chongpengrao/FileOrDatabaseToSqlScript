package scorpio.rao.db2sql.service;

import javafx.util.Pair;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Service;
import scorpio.rao.db2sql.config.TableConfig;
import scorpio.rao.db2sql.util.DateUtil;
import scorpio.rao.db2sql.util.IOUtil;

import javax.annotation.Resource;
import java.io.BufferedWriter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by scorpio.rao on 2019/5/17
 */
@Service
@Slf4j
public class MainService {

    @Resource(name = "jdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TableConfig tableConfig;

    //k:表名,v:列名拼接后的字符串,如id,name,value
    private static Map<String,String> tableMap = new HashMap<>();

    private static Map<String, List<Pair<String,String>>> tableColumnFields = new HashMap<>();

    //数据库中表名list
    private static List<String> tables = new ArrayList<>();

    //含有parentId的表
    private static List<String> tableWithParentId = new ArrayList<>();

    /**
     * 生成脚本的入口
     */
    public void start() {
        //1)初始化,获取到所有表的信息(表名以及表中的列名)
        init();

        //2)生成脚本:给所有表增加一个新字段old_id
        addTempIdentification();

        //3)生成数据库中所有表的insert脚本
        handlerTablesToSql();

        //4)生成脚本:删除这些表中的字段old_id
        deleteTempField();

        log.info("**************所有脚本全都生成完毕!**************");
    }

    /**
     * 处理查询到的表的数据并写入脚本文件中
     */
    private void handlerTablesToSql() {
        List<String> tableForDomainIds = TableConfig.needDomainIds;
        tables.forEach(name->{
            //将数据源中的id存入多域库中的old_id字段中
            String values = tableMap.get(name)+",old_id";
            //需要增加domianid字段 的表进行处理
            if (tableForDomainIds.contains(name)){
                values = values + ",domainid";
            }
            int domainId = tableConfig.getDomainId();
            String sql2txt = "insert into "+name+" ("+values+") values";
            String sql = "select * from "+name;
            if (name.equals("GLOBALCODEMAP")){
                sql = sql+" where status='0'";
            }else if (name.equals("EPGPUBLISHEVENTDETAIL")){
                sql = sql+" where createdate>add_months(sysdate,-1)";
            }else if (name.equals("CTMSHISTORYEVENT")){
                sql = sql +" where serviceeventcreatedate>add_months(sysdate,-1)";
            }
            //查询表中所有数据
            SqlRowSet results = jdbcTemplate.queryForRowSet(sql);
            //处理表中的数据并转换为list再写入脚本中
            List<String> list = handlerRowSet(results,name);
            //数据脚本的存放路径
            String savePath = tableConfig.getBASE_URL()+name+"_insert.sql";
            BufferedWriter writer = IOUtil.getWriter(savePath);
            //todo日期格式还是要转换的!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            if (TableConfig.noChangeTable.contains(name)){
                list.forEach(e->{
                    String str = sql2txt+"("+e+");";
                    IOUtil.write(writer,str);
                });

            }else {
                list.forEach(e->{
                    //需要增加domainid属性的表的处理
                    if (tableForDomainIds.contains(name)){
                        e = e + "," + domainId;
                    }
                    String str = sql2txt+"("+e+");";
                    IOUtil.write(writer,str);
                });
                //含有parentId的表需要去更新parentId字段的值  .. assetIdMap表的处理
                if (tableWithParentId.contains(name)){
                    IOUtil.writeCommit(writer);
                    String str = "update t."+name+" set t.parentid=(select "+name+"ID from "+name
                            +" where old_id=t.parentid) where t.parentid is not null;";
                    IOUtil.write(writer,str);
                }
            }

            IOUtil.writeCommit(writer);
            IOUtil.closeWritter(writer);
            log.info("*************表 {} 的脚本已生成*****************",name);
        });
    }

    /**
     * 由于每个表中id字段的转换规则不一样(媒资的可根据code来查找id,还有其他一些情况的转换)
     * @param results
     * @param table
     * @return
     */
    private List<String> handlerRowSet(SqlRowSet results,String table) {
        List<String> resultList = new ArrayList<>();

        //K->column  V->type
        List<Pair<String, String>> columnFields = tableColumnFields.get(table);

        //objid/objtype的映射
        Map<String, String> typeTableMap = TableConfig.map.get(table);

        String[] columns = tableMap.get(table).split(",");

        //获取这个表的列的总数
        int columnCount =columnFields.size();

        //1)日期类型的列要转换
        List<String> dateColumns = columnFields.stream()
                .filter(pair -> pair.getValue().equals("DATE"))
                .map(Pair::getKey)
                .collect(Collectors.toList());

        //3)数字类型的column
        List<String> numberColumns = columnFields.stream()
                .filter(pair -> pair.getValue().contains("NUMBER"))
                .map(Pair::getKey)
                .collect(Collectors.toList());

        //部分表数据插入时不需要做任何修改:: 日期之类的还是要转换的!!!!!!!!!!!!!
        if (TableConfig.noChangeTable.contains(table)){
            while (results.next()){
                StringBuilder builder = new StringBuilder();
                for (int i=0;i<columnCount;i++){
                    String column = columns[i];
                    String str = results.getString(column);
                    if (dateColumns.contains(column)) {
                        builder.append(DateUtil.dateConvert(results.getString(column))).append(",");
                    }else if (numberColumns.contains(column)){
                        builder.append(str).append(",");
                    }else {
                        String s = "'" + str + "',";
                        if (str == null) {
                            builder.append("null,");
                        } else {
                            builder.append(s);
                        }
                    }
                }
                resultList.add(builder.append("0").toString());
            }
            return resultList;
        }

        //2)含id字段的列的处理
        //parentid,objid/objecti,tables中没有的id
        Map<String,String> otherIdSql = new HashMap<>();
        List<String> otherIds = columnFields.stream()
                .filter(e -> isOtherTableId(e.getKey()))
                .map(pair->{
                    String column = pair.getKey();
                    String sql = "(select "+column+" from "+column.substring(0,column.length()-2)
                            +" where old_id=";
                    otherIdSql.put(column,sql);
                    return column;
                }).collect(Collectors.toList());

        //4)表的id替换成自增id,(部分静态表没有sequence的除外)
        String tableId = table+"ID";
        String idStr = (tableId+".nextval").toLowerCase();

        while(results.next()){
            String old_id = "''";
            StringBuilder builder = new StringBuilder();
            String objType = null;
            for (int i=0;i<columns.length;i++){
                String column = columns[i];
                if (tableId.equals(column)){
                    old_id = results.getString(column);
                    builder.append(idStr).append(",");
                }else if (dateColumns.contains(column)){
                    builder.append(DateUtil.dateConvert(results.getString(column))).append(",");
                }else if (otherIds.contains(column)){
                    String id = results.getString(column);
                    if (id != null){
                        String str = otherIdSql.get(column)+id+" and rowNum=1)";
                        builder.append(str).append(",");
                    }else {
                        builder.append("null,");
                    }
                }else {
                    // todo objid objtype 的先后顺序是个问题  ... 放在tablemap初始化的时候处理吧
                    if (isObjType(column)){
                        objType = results.getString(column);
                        if (objType == null) {
                            builder.append("'',");
                        } else {
                            builder.append("'").append(objType).append("',");
                        }
                    }else if (isObjId(column)){
                        String objId = results.getString(column);
                        if (objId != null && objType != null && typeTableMap != null){
                            String realTable = typeTableMap.get(objType);
                            String id = realTable + "id";
                            String strConvert = "(select "+id+" from "+realTable+" where old_id="+objId+" and rowNum=1)";
                            builder.append(strConvert).append(",");
                        }else{
                            builder.append("null,");
                        }
                    }else if (column.equals("EPGPUBLISHEVENTDETAILID")){
                        builder.append("null,");
                    }else if (numberColumns.contains(column)){
                        builder.append(results.getString(column)).append(",");
                    }else {
                        //字符串的处理
                        String str = results.getString(column);
                        if (str == null) {
                            builder.append("'',");
                        } else {
                            builder.append("'").append(str).append("',");
                        }
                    }
                }
            }

            resultList.add(builder.append(old_id).toString());
        }

        return resultList;
    }

    private boolean isObjId(String str){
        return str.equals("OBJID") || str.equals("OBJECTID") || str.toLowerCase().equals("asset_id");
    }

    private boolean isObjType(String str){
        return str.equals("OBJTYPE") || str.equals("OBJECTTYPE") || str.toLowerCase().equals("assettype");
    }

    //parentId && epgpublisheventdetailid(新库其他表关联的都设为空) 此处不处理
    private boolean isOtherTableId(String column){
        return !"EPGPUBLISHEVENTDETAILID".equals(column)
                && !"PARENTID".equals(column)
                && column.endsWith("ID") &&
                tables.contains(column.substring(0,column.length()-2));
    }

    /**
     * 初始化tables以及tableMap
     */
    public void init() {
        //获取当前用户下数据库中所有的表名
        SqlRowSet tableResult = jdbcTemplate.queryForRowSet("select table_name from user_tables");
        while (tableResult.next()){
            String table_name = tableResult.getString("table_name");
            //过滤掉不再使用的表
            if (!TableConfig.tableNoUse.contains(table_name)){
                tables.add(table_name);
            }
        }

        //过滤掉一些备份的表 以及没有数据的表
        tables = tables.stream().filter(e->{
            String sql = "select count(*) from "+e;
            SqlRowSet rowSet = jdbcTemplate.queryForRowSet(sql);
            int count = 0;
            if (rowSet.next()){
                count = rowSet.getInt(1);
            }
            return !e.endsWith("_BAK") && count>0;
        }).collect(Collectors.toList());

        //获取表与列名拼接后的map
        String sql = "select COLUMN_NAME,DATA_TYPE from user_tab_columns where table_name=";
        tables.forEach(name->{
            SqlRowSet columns = jdbcTemplate.queryForRowSet(sql+"'"+name+"'");
            handlerColumnResult(columns,name);
        });
        log.info("*************初始化完成,一共有{}张有效的表****************",tables.size());

        //1)::含有parentId的表()-->数据更新完后才去更新parentId
        tableWithParentId = tables.stream().filter(e ->
                Arrays.asList(tableMap.get(e).split(",")).contains("PARENTID")
        ).collect(Collectors.toList());
        log.info("含有parentId的表size:{},list:{}",tableWithParentId.size(),tableWithParentId);

    }

    //todo 对tablemap重新排序,使得objectType一定在objectId前面
    private void handlerColumnResult(SqlRowSet columns, String table) {
        StringBuilder builder = new StringBuilder();
        List<Pair<String,String>> list = new ArrayList<>();
        while(columns.next()){
            String column = columns.getString("COLUMN_NAME");
            String dataType = columns.getString("DATA_TYPE");
            //todo ...
            if (isObjType(column) && TableConfig.noChangeTable.contains(table)){
                builder = new StringBuilder(column).append(",").append(builder.toString());
            }else {
                builder.append(column).append(",");
            }
            list.add(new Pair<>(column,dataType));
        }
        tableMap.put(table,builder.deleteCharAt(builder.length()-1).toString());
        tableColumnFields.put(table,list);
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
        log.info("***************数据库所有表中新增临时字段old_id脚本****************");
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
        log.info("***************数据库所有表中临时字段old_id删除脚本****************");
    }

    private void handlerTemp(String[] strs){
        String url = tableConfig.getBASE_URL()+strs[0];
        BufferedWriter writer = IOUtil.getWriter(url);
        tables.forEach(e->{
            String sql = strs[1] + e + strs[2];
            IOUtil.writeWithCommit(writer,sql);
        });
        IOUtil.closeWritter(writer);
    }

}
