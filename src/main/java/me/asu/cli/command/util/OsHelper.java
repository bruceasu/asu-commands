package me.asu.cli.command.util;

import java.io.File;
import java.io.UnsupportedEncodingException;

/**
 * Created by suk on 2019/6/3.
 */
public class OsHelper {
    public static String getMyDIR(){//获取当前类文件的绝对路径
        String path = OsHelper.class.getProtectionDomain().getCodeSource().getLocation().getFile();
        if (path.endsWith(".jar")) {
            try {
                //保险起见，将路径进行decode转码
                path = java.net.URLDecoder.decode(path, "UTF-8");
            } catch (UnsupportedEncodingException e) { System.out.println(e.toString()); }
            //获取jar包的上级目录
            String jarPath = new File(path).getParentFile().getAbsolutePath();
            return jarPath;
        } else {
            try {
                //保险起见，将路径进行decode转码
                path = java.net.URLDecoder.decode(path, "UTF-8");
            } catch (UnsupportedEncodingException e) { System.out.println(e.toString()); }
            return new File(path).getAbsolutePath();
        }

    }

    public static void main(String[] args) {
        System.out.println(OsHelper.getMyDIR());
    }
}
