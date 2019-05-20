package scorpio.rao.db2sql.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by scorpio.rao on 2019/5/17
 */
@Slf4j
@Service
public class TableService {

    //表中含有objType/objectType,objId/objectId与其他表的关系
    public static Map<String,Map<String,String>> map = new HashMap<>();

    //单域升级多域需要增加domainid字段的表
    public static List<String> needDomainIds = new ArrayList<>();

    static {
        init();
    }
//1)CTMSHISTORYEVENT, CATEGORYDTL, SUPERSCRIPTDTL, STAFFDATAPRIVILEGE, CONTENTSERVICE, GLOBALCODEMAP_BAK,
//EXTENDFIELD, PICTUREMAP, MECOPYEVENT, EPGCATEGORYBLACKLIST, EPGCATEGORYWHITELIST, GLOBALCODEDEF,
// ROLEDATAPRIVILEGE,BUNDLEDCONTENTDTL, CTMSOBJECT, EPGCATEGORYDTL, CTMSHISTORYEVENTDETAIL,

// 2)FAVORITEOBJECTS, TAGSDTL,PUBLISHLOG, GLOBALCODEMAP,EPGPUBLISHEVENTDETAIL, SCHEDULE, PICTURETYPE,
// NODEOBJECTS, CTMSSUCCESSEVENTDETAIL, EPGCATEGORY,EPGPUBLISHJOB, PROGRAMMEDIACONTENT,
//COPYRIGHTMAP, MECOPYEVENTHISTORY
    private static void init() {
        //1)需要增加domainId字段的表
        needDomainIds.add("program");
        needDomainIds.add("series");
        needDomainIds.add("channel");
        needDomainIds.add("epgcategory");
        needDomainIds.add("bundledcontent");
        needDomainIds.add("epgpage");
        needDomainIds.add("adprogram");
        needDomainIds.add("adplan");
        needDomainIds.add("paper");
        needDomainIds.add("question");
        //2)objId找归属

    }

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
