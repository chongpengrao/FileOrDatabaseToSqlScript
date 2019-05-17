package scorpio.rao.db2sql.util;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * Created by scorpio.rao on 2019/5/17
 */
public class IOUtil {
    public static BufferedWriter getWriter(String url){
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(url), "UTF8"));
            writer.write("set define off");
            writer.newLine();
            return writer;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return writer;
    }

    public static void closeWritter(BufferedWriter writer){
        try {
            writer.flush();
            writer.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void write(BufferedWriter writer,String str){
        try {
            writer.write(str);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeCommit(BufferedWriter writer){
        try {
            writer.write("commit;");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeWithCommit(BufferedWriter writer,String str){
        try {
            writer.write(str);
            writer.newLine();
            writer.write("commit;");
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
