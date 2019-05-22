package scorpio.rao.db2sql.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TableConfig tableConfig;

    //k:表名,v:列名拼接后的字符串,如id,name,value
    private static Map<String,String> tableMap = new HashMap<>();

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
            list.forEach(e->{
                //需要增加domainid属性的表的处理
                if (tableForDomainIds.contains(name)){
                    e = e + tableConfig.getDomainId();
                }
                String str = sql2txt+"("+e+");";
                IOUtil.write(writer,str);
            });
            //含有parentId的表需要去更新parentId字段的值
            if (tableWithParentId.contains(name)){
                String str = "update t."+name+" set t.parentid=(select "+name+"ID from "+name
                        +" where old_id=t.parentid) where t.parentid is not null";
                IOUtil.write(writer,str);
            }
            IOUtil.writeCommit(writer);
            IOUtil.closeWritter(writer);
            log.info("*************表 {} 的脚本已生成*****************",name);
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

        Map<String, String> typeTableMap = TableConfig.map.get(table);

        //获取这个表的列的总数
        int columnCount = columns.length;

        //1)日期字段所在的位置
        List<Integer> dateIndex = new ArrayList<>();
        //2)表的id所在的位置
        int idIndex = 0;
        //3)表中涉及到的关联表的id所在的位置以及id的转换
        int objIdIndex = 0;
        int typeIndex = 0;
        List<Integer> otherIdIndex = new ArrayList<>();
        Map<Integer,String> otherIdSql = new HashMap<>();

        String tableId = (table + "ID").toUpperCase();

        //4)其他表中关联的EPGPUBLISHEVENTDETAILID设置为空
        int epgIndex = 0;

        for (int i=0;i<columnCount;i++) {
            int index = i + 1;
            String column = columns[i].toUpperCase();
            if (column.endsWith("OBJTYPE") || column.endsWith("OBJECTTYPE")){
                typeIndex = index;
            }
            if (column.endsWith("DATE") || column.endsWith("TIME")){
                dateIndex.add(index);
            }
            if (column.equals(tableId)){
                idIndex = index;
            } else if (column.equals("EPGPUBLISHEVENTDETAILID")) {
                epgIndex = index;
            }else if (isOtherTableId(column)){
                //objId/objectId->objType/objectType
                if (column.equals("OBJID") || column.equals("OBJECTID")){
                    objIdIndex = index;
                }else {
                    //对其他id的处理
                    otherIdIndex.add(index);
                    String sql = "(select "+column+" from "+column.substring(0,column.length()-2)
                            +" where old_id=";
                    otherIdSql.put(index,sql);
                }
            }
        }

        String idStr = (table+"ID.nextVal").toLowerCase();

        while (results.next()){
            String old_id = "";
            StringBuilder builder = new StringBuilder();
            //踩坑了!!! 下标从1开始到columnCount结束!!!
            for (int i=1;i<=columnCount;i++){
                if (idIndex!=0 && idIndex == i){
                    old_id = results.getString(i);
                    builder.append(idStr).append(",");
                }else if (dateIndex.contains(i)){
                    builder.append(DateUtil.dateConvert(results.getString(i))).append(",");
                }else if (otherIdIndex.contains(i)){
                    String id = results.getString(i);
                    if (id != null){
                        String str = otherIdSql.get(i)+id+" and rowNum=1)";
                        builder.append(str).append(",");
                    }else {
                        builder.append(id).append(",");
                    }
                }else if (epgIndex !=0 && epgIndex == i){
                    builder.append("null,");
                } else if (objIdIndex != 0 && objIdIndex == i){
                    String objType = results.getString(typeIndex);
                    String objId = results.getString(i);
                    //当objid和obitype都存在时替换sql
                    if (objId != null && objType != null && typeTableMap != null){
                        String realTable = typeTableMap.get(objType);
                        String id = realTable + "id";
                        String strConvert = "(select "+id+" from "+realTable+" where old_id="+id+" and rowNum=1)";
                        builder.append(strConvert).append(",");
                    }else{
                        builder.append(objId).append(",");
                    }
                }else {
                    //字符加''   !!!　null的处理
                    String strConvert = "'"+results.getString(i)+"'";
                    strConvert = "'null'".equals(strConvert.toLowerCase()) ? "''" : strConvert;
                    builder.append(strConvert).append(",");
                }
            }
            resultList.add(builder.append(old_id).toString());
        }

        return resultList;
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
        String sql = "select column_name from user_tab_cols where table_name=";
        tables.forEach(name->{
            SqlRowSet columns = jdbcTemplate.queryForRowSet(sql+"'"+name+"'");
            handlerColumnResult(columns,name);
        });
        log.info("*************初始化完成,一共有{}张有效的表****************",tables.size());

//        String program = tableMap.get("PROGRAM");
//        log.info("*****Program表*****{}", program);
//        SqlRowSet sqlRowSet = jdbcTemplate.queryForRowSet("select * from program where rownum=1");
//        int len = program.split(",").length;
//        while (sqlRowSet.next()){
//            StringBuilder builder = new StringBuilder();
//            for (int i=1;i<=len;i++){
//                try{
//                    builder.append(sqlRowSet.getString(i)).append(",");
//                }catch (Exception e){
//                    e.printStackTrace();
//                    continue;
//                }
//            }
//            log.info("*****program****{}",builder.toString());
//        }


        //1)::含有parentId的表()-->数据更新完后才去更新parentId
        tableWithParentId = tables.stream().filter(e ->
                Arrays.asList(tableMap.get(e).split(",")).contains("PARENTID")
        ).collect(Collectors.toList());
        log.info("含有parentId的表size:{},list:{}",tableWithParentId.size(),tableWithParentId);

//        //2)只含有自身id的基础表--parentId也可以有--todo vspid,staffid
//        List<String> baseTables = tables.stream().filter(e -> {
//            List<String> asList = Arrays.stream(tableMap.get(e).split(","))
//                    .filter(column -> column.endsWith("ID") && !column.equals("PARENTID"))
//                    .collect(Collectors.toList());
//            return asList.size() == 1 && (e + "ID").equals(asList.get(0));
//        }).collect(Collectors.toList());
//        log.info("只含有自身id的基础表size:{},list:{}",baseTables.size(),baseTables);
//
//        //3)只含有自身id以及objid/objectid(endWith来判断,有些表字段名会更长)以及基础表id的表--parentId也可以有
//        List<String> tableWithObjId = tables.stream().filter(e -> {
//            List<String> asList = Arrays.stream(tableMap.get(e).split(","))
//                    .filter(column -> column.endsWith("ID") && !column.equals("PARENTID") && !column.equals(e + "ID"))
//                    .collect(Collectors.toList());
//            return asList.size() == 1 && (asList.get(0).endsWith("OBJID") || asList.get(0).endsWith("OBJECTID"));
//        }).collect(Collectors.toList());
//        log.info("tableWithObjId--表的size:{},list:{}",tableWithObjId.size(),tableWithObjId);
//
//        //4)需要单独处理的表
//        List<String> otherTables = tables.stream().filter(e -> !baseTables.contains(e)
//                && !tableWithObjId.contains(e)).collect(Collectors.toList());
//        log.info("需要单独处理的表size:{},list:{}",otherTables.size(),otherTables);

//        log.info("表名list : {}",tables);
//        List<String> list = tableMap.entrySet().stream().filter(e -> e.getValue().contains("OBJECTTYPE") || e.getValue().contains("OBJTYPE"))
//                .map(Map.Entry::getKey).collect(Collectors.toList());
//        log.info("含有objecttype字段的表:{}",list);
    }

    private void handlerColumnResult(SqlRowSet columns, String table) {
        StringBuilder builder = new StringBuilder();
        while(columns.next()){
            String column = columns.getString("column_name");
            if (!column.contains("$")){
                builder.append(column).append(",");
            }
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
