package scorpio.rao.db2sql;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import scorpio.rao.db2sql.util.IOUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class Db2sqlApplicationTests {

    @Test
    public void contextLoads() throws IOException {
        BufferedReader reader = IOUtil.getReader("D:\\scorpio_data\\data\\result\\import.txt");
        BufferedWriter writer = IOUtil.getWriter("D:\\scorpio_data\\data\\result\\wu_import.sql");
        String str = "";
        while (reader.ready()){
            str = reader.readLine().trim();
            IOUtil.write(writer,"spool /opt/wacos/sqltest/resultsql/"+str+"_insert.log");
            IOUtil.write(writer,"prompt importing "+str);
            IOUtil.write(writer,"set term off");
            IOUtil.write(writer,"@/opt/wacos/sqltest/resultsql/"+str+"_insert.sql");
            IOUtil.write(writer,"set term on");
            IOUtil.write(writer,"select count(*) from "+str);
            IOUtil.writeNewLine(writer);
        }
        IOUtil.closeAll(writer,reader);
        log.info("总的脚本已生成");
    }

    @Test
    public void test1() throws IOException {
        StringBuilder str = new StringBuilder("a,f,cs,a,d,f,");
       System.out.println(str.deleteCharAt(str.length()-1));
    }

}
