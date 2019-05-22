package scorpio.rao.db2sql;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import scorpio.rao.db2sql.service.MainService;

@SpringBootApplication
public class Db2sqlApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(Db2sqlApplication.class, args);
        MainService bean = context.getBean(MainService.class);
        bean.start();
    }

}
