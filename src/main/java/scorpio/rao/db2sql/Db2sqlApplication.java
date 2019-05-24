package scorpio.rao.db2sql;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import scorpio.rao.db2sql.service.MainService;
import scorpio.rao.db2sql.service.MediaExportService;

@SpringBootApplication
public class Db2sqlApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(Db2sqlApplication.class, args);
        //1)单域库生成导入数据到多域sop的脚本
//        MainService bean = context.getBean(MainService.class);
//        bean.start();

        //todo 执行该方法前要先建立数据库直接的dbLink连接
        //2)SMP数据直接割接进多域sop库存域数据
        MediaExportService mediaExportService = context.getBean(MediaExportService.class);
        mediaExportService.start();
    }

}
