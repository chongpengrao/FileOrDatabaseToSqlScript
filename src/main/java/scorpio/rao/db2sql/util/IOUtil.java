package scorpio.rao.db2sql.util;

import java.io.*;

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

    public static void writeNewLine(BufferedWriter writer){
        try {
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static BufferedReader getReader(String url){
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(url), "UTF8"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return reader;
    }

    public static void closeWritter(BufferedWriter writer){
        try {
            writer.flush();
            writer.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void closeAll(BufferedWriter writer,BufferedReader reader){
        try {
            writer.flush();
            writer.close();
            reader.close();
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
            writer.newLine();
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
