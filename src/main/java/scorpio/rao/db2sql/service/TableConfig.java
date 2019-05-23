package scorpio.rao.db2sql.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by scorpio.rao on 2019/5/17
 */
@Slf4j
@Component
@Data
//application.properties，就不用写@PropertyScource
//@PropertySource("classpath:application.properties")
public class TableConfig {

    @Value("${db.script.savePath}")
    private String BASE_URL;

    @Value("${ut.domainId}")
    private int domainId;

    //表中含有objType/objectType,objId/objectId与其他表的关系
    public static Map<String,Map<String,String>> map = new HashMap<>();

    //单域升级多域需要增加domainid字段的表
    public static List<String> needDomainIds = new ArrayList<>();

    //需要单独处理的表
    public static List<String> tablesNeedHandlerAlone = new ArrayList<>();

    //已经没有用的表+备份的表
    public static List<String> tableNoUse = new ArrayList<>();

    //原封不动入库的表
    public static List<String> noChangeTable = new ArrayList<>();

    static {
        init();
    }

    private static void init() {
        //原封不动入库的表
        noChangeTable.add("CTMSOBJECT");
        noChangeTable.add("CTMSPROPERTY");
        noChangeTable.add("CTMSVALUECONVERT");
        noChangeTable.add("LINKSUBTYPE");
        noChangeTable.add("ERRORCODE");


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

        //2)需要单独处理的表
        tablesNeedHandlerAlone.add("ASSETIDMAP");

        //3)没有用的表
        tableNoUse.add("IPTVPRODUCTPACKAGE");
        tableNoUse.add("EPGGROUP");
        tableNoUse.add("PRIVILEGE");
        tableNoUse.add("EPGFOLDER");
        tableNoUse.add("EPGINFO");
        tableNoUse.add("EPGLOG");
        tableNoUse.add("EPGPORTAL");
        tableNoUse.add("EPGPORTALFOLDER");
        tableNoUse.add("EPGSYNCLSP");
        tableNoUse.add("EPGPORTALLSP");
        tableNoUse.add("EPGPROPERTIES");
        tableNoUse.add("EPGSYNCFILE");
        tableNoUse.add("EPGSYNCFILESET");
        tableNoUse.add("EPGSYNCFILESETFILE");
        tableNoUse.add("EPGSYNCFILESETLSP");
        tableNoUse.add("STAFFBUNNDLEDCONTENTASSIGN");
        tableNoUse.add("STAFFCATEGORYASSIGN");
        tableNoUse.add("STAFFVSPASSIGN");
        tableNoUse.add("USERTOKEN");
        tableNoUse.add("WORKGROUP");
        tableNoUse.add("WORKGROUPSTAFF");
        tableNoUse.add("PROGRAM1206");
        tableNoUse.add("WENKE_NEWPARTITION");
        tableNoUse.add("SYS_EXPORT_FULL_01");
        tableNoUse.add("EPGPUBLISHJOB");
        //权限相关的表从多域版本重新导入
        tableNoUse.add("STAFFPRIVILEGE");
        tableNoUse.add("ROLEPRIVILEGE");
        tableNoUse.add("DATAPRIVILEGETYPE");
        tableNoUse.add("ROLEDATAPRIVILEGE");
        tableNoUse.add("STAFFDATAPRIVILEGE");

        //数据库已存在基础数据的表
        tableNoUse.add("CASTROLE");
        tableNoUse.add("EPGOPTION");

        //这几个表现场的数据,单独处理
        tableNoUse.add("PROGRAMPOSTER_CHAD");
        tableNoUse.add("PROGRAMSTILL_CHAD");
        tableNoUse.add("SERIESPOSTER_CHAD");
        tableNoUse.add("SERIESSTILL_CHAD");
        tableNoUse.add("SEARCHMOVIE");

        //日志和数据库历史记录暂不导入
        tableNoUse.add("ACCESSLOG");
        tableNoUse.add("ACCESSLOGITEM");
        tableNoUse.add("DATABASEHISTORY");

        //4)objId找归属
        HashMap<String,String> map1 = new HashMap<>();
        map1.put("1","program");
        map1.put("2","meidiacontent");
        map1.put("12","channel");
        map1.put("13","schedule");
        map1.put("15","category");
        map1.put("30","package");
        map1.put("35","series");
        map1.put("50","outlink");
        map1.put("51","epgcategory");
        map1.put("52","epgcategorydtl");
        map1.put("53","classifyrule");
        map1.put("54","classifyruledtl");
        map1.put("55","epggroup");
        map1.put("56","epgpage");
        map1.put("57","epgposition");
        map1.put("58","epgelement");
        map1.put("59","epgtemplatepkg");
        map1.put("60","epgtemplate");
        map1.put("61","epgtemplatePara");
        map1.put("62","topiclibrary ");
        map1.put("63","apkversion");
        map1.put("64","apkupgrade");
        map1.put("65","adprogram");
        map1.put("66","adplace");
        map1.put("67","adplan");
        map1.put("99","categorydtl");
        map1.put("70","paper");
        map1.put("71","question");
        map1.put("72","msg");
        map1.put("73","msgplan");
        map.put("CTMSHISTORYEVENT",map1);


        HashMap<String,String> map2 = new HashMap<>();
        map2.put("1","program");
        map2.put("2","meidiacontent");
        map2.put("12","channel");
        map2.put("13","schedule");
        map2.put("15","category");
        map2.put("30","package");
        map2.put("35","series");
        map2.put("99","categorydtl");
        map2.put("65","adprogram");
        map2.put("51","epgcategory");
        map.put("CTMSHISTORYEVENTDETAIL",map2);

        HashMap<String,String> map3 = new HashMap<>();
        map3.put("1","series");
        map3.put("8","program");
        map3.put("A","bundledContent");
        map3.put("C","channel");
        map3.put("G","schedule");
        map.put("CATEGORYDTL",map3);

        HashMap<String,String> map4 = new HashMap<>();
        map4.put("1","vsp");
        map4.put("2","programtype");
        map4.put("3","category");
        map4.put("4","bundledcontent");
        map4.put("5","epgcategory");
        map4.put("6","epggroup");
        map4.put("7","epgpage");
        map4.put("8","epgposition");
        map4.put("9","topiclibrary");
        map.put("STAFFDATAPRIVILEGE",map4);

        HashMap<String,String> map5 = new HashMap<>();
        map5.put("1","bundledcontent");
        map5.put("2","channel");
        map5.put("3","category");
        map5.put("4","program");
        map5.put("5","series");
        map5.put("6","epggroup");
        map5.put("7","epgcategorydtl");
        map5.put("9","epgposition");
        map5.put("10","epgelement");
        map5.put("99","outlink");
        map5.put("11","topic");
        map5.put("12","epgcategory");
        map5.put("13","outlink");
        map5.put("20","paper");
        map5.put("21","question");
        map5.put("22","option");
        map5.put("65","adprogram");
        map5.put("30","cast");
        map.put("PICTUREMAP",map5);

        //todo 自动关联栏目改成手工->然后就没有relatedObjectid了
//        HashMap<String,String> map6 = new HashMap<>();
//        map6.put("","");
//        map6.put("","");
//        map6.put("","");
//        map6.put("","");
//        map.put("EPGCATEGORY",map6);

        HashMap<String,String> map7 = new HashMap<>();
        map7.put("1","program");
        map7.put("2","series");
        map.put("MECOPYEVENT",map7);

        HashMap<String,String> map8 = new HashMap<>();
        map8.put("1","program");
        map8.put("2","mediacontent");
        map8.put("3","channel");
        map8.put("4","schedule");
        map8.put("5","bundledcontent");
        map8.put("7","physicalchannel");
        map8.put("8","category");
        map8.put("11","series");
        map8.put("13","picture");
        map8.put("23","iptvpackage");
        map8.put("24","iptvproduct");
        map8.put("25","contentservice");
        map8.put("26","htmlcontent");
        map8.put("30","superscript");
        map8.put("31","tag");
        map8.put("32","cast");
        map8.put("33","outlink");
        map8.put("34","taggroup");
        map8.put("40","epgcategory");
        map8.put("41","epgpage");
        map8.put("50","paper");
        map8.put("51","question");
        map8.put("52","options");
        map8.put("53","superscriptgroup");
        map8.put("54","optag");
        map.put("GLOBALCODEDEF",map8);

        HashMap<String,String> map9 = new HashMap<>();
        map9.put("1","channel");
        map9.put("2","schedule");
        map9.put("3","program");
        map9.put("4","series");
        map.put("BUNDLEDCONTENTDTL",map9);

        //todo 数据字典和数据库中缺注释...
//        HashMap<String,String> map10 = new HashMap<>();
//        map10.put("","");
//        map10.put("","");
//        map10.put("","");
//        map10.put("","");
//        map.put("CTMSOBJECT",map10);

        HashMap<String,String> map11 = new HashMap<>();
        map11.put("1","program");
        map11.put("2","series");
        map11.put("3","series");
        map11.put("4","channel");
        map11.put("5","topiclibrary");
        map11.put("6","schedule");
        map11.put("7","outlink");
        map11.put("8","seriesset");
        map11.put("9","channel");
        map11.put("10","paper");
        map.put("EPGCATEGORYDTL",map11);

        HashMap<String,String> map12 = new HashMap<>();
        map12.put("1","vsp");
        map12.put("2","programtype");
        map12.put("3","category");
        map12.put("4","bundledcontent");
        map12.put("5","epgcategory");
        map12.put("6","epggroup");
        map12.put("7","epgpage");
        map12.put("8","epgposition");
        map12.put("9","channel");
        map12.put("20","domain");
        map.put("ROLEDATAPRIVILEGE",map12);

        Map<String,String> m1 = new HashMap<>();
        //1:program,2:series
        m1.put("1","program");
        m1.put("2","series");
        map.put("MECOPYEVENTHISTORY",m1);

        Map<String,String> m2 = new HashMap<>();
        //1:program 2:series
        m2.put("1","program");
        m2.put("2","series");
        map.put("COPYRIGHTMAP",m2);

        Map<String,String> m3 = new HashMap<>();
        //'1' - program, '2' - series, ‘3’-adprogram
        m3.put("1","program");
        m3.put("2","series");
        m3.put("3","adprogram");
        map.put("PROGRAMMEDIACONTENT",m3);

        Map<String,String> m7 = new HashMap<>();
        m7.put("1","program");
        m7.put("12","channel");
        m7.put("35","series");
        m7.put("36","bundledcontent");
        m7.put("37","category");
        m7.put("40","epgcatetory");
        m7.put("65","adprogram");
        m7.put("56","EPGPage");
        m7.put("57","EPGPosition");
        m7.put("58","EPGElement");
        m7.put("66","TopicLibrary");
        m7.put("67","TopicLibrary");
        m7.put("68","TopicLibrary");
        map.put("NODEOBJECTS",m7);

        Map<String,String> m8 = new HashMap<>();
        m8.put("1","bundledcontent");
        m8.put("2","channel");
        m8.put("3","category");
        m8.put("4","program");
        m8.put("5","series");
        m8.put("6","tvcolumn");
        m8.put("7","tvcolumndtl");
        m8.put("8","epgcolumn");
        m8.put("9","recommendpos");
        m8.put("10","poselement");
        m8.put("11","topiclibary");
        m8.put("13","outlink");
        m8.put("65","adprogram");
        map.put("PICTURETYPE",m8);

        Map<String,String> m9 = new HashMap<>();
        m9.put("1","program");
        m9.put("2","program");
        m9.put("3","channel");
        m9.put("4","playlist");
        m9.put("5","channel");
        m9.put("6","series");
        map.put("SCHEDULE",m9);

        Map<String,String> m10 = new HashMap<>();
        m10.put("1","program");
        m10.put("12","channel");
        m10.put("13","schedule");
        m10.put("14","channel");
        m10.put("15","category");
        m10.put("30","package");
        m10.put("35","series");
        m10.put("80","seriesset");
        m10.put("50","outlink");
        m10.put("51","epgcategory");
        m10.put("53","epgpage");
        m10.put("56","epggroup");
        m10.put("60","channel");
        m10.put("61","topiclibrary ");
        m10.put("63","apkversion");
        m10.put("64","apkupgrade");
        m10.put("99","categorydtl");
        m10.put("PC","picture");
        m10.put("65","adprogram");
        m10.put("66","adplace");
        m10.put("67","adplan");
        m10.put("70","paper");
        m10.put("71","question");
        m10.put("72","msg");
        m10.put("73","msgplan");
        map.put("EPGPUBLISHEVENTDETAIL",m10);

        Map<String,String> m11 = new HashMap<>();
        m11.put("1","program");
        m11.put("2","mediacontent");
        m11.put("12","channel");
        m11.put("13","schedule");
        m11.put("15","category");
        m11.put("25","picture");
        m11.put("26","physicalchannel");
        m11.put("35","series");
        m11.put("44","bundledcontent");
        m11.put("45","ContentService");
        m11.put("46","IPTVProduct");
        m11.put("47","IPTVProductPackage");
        m11.put("48","htmlcontent");
        m11.put("65","adprogram");
        map.put("GLOBALCODEMAP",m11);

        //TAGSDTL
        Map<String,String> m13 = new HashMap<>();
        m13.put("1","program");
        m13.put("2","series");
        m13.put("3","channel");
        m13.put("4","schedule");
        m13.put("5","bundledcontent");
        m13.put("6","category");
        m13.put("7","epgCategory");
        map.put("TAGSDTL",m13);

        //ASSETIDMAP
        Map<String,String> m14 = new HashMap<>();
        m14.put("0","program");
        m14.put("1","MetaPicture");
        m14.put("6","BundledContent");
        m14.put("7","Series");
        m14.put("9","MediaContent");
        m14.put("A","Category");
        m14.put("B","Channel");
        m14.put("C","Schedule");
        m14.put("D","copyright");
        m14.put("M","PhysicalChannel");
        m14.put("Y","htmlcontent");
        map.put("ASSETIDMAP",m14);
    }

}
