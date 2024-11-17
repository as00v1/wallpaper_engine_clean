package com.jiay.wec;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MainApplication {
    public static void main(String[] args) {
        String configPath = "E:\\Program Files (x86)\\Steam\\steamapps\\common\\wallpaper_engine\\bin";
        String resPath = "E:\\Program Files (x86)\\Steam\\steamapps\\workshop\\content\\431960\\";


        JSONObject usedJson = readFile(configPath+ "\\workshopcache.json");
        if (usedJson == null) {
            System.out.println("读取配置文件为空");
            return;
        }
        System.out.println("读取配置文件成功！");
//        System.out.println("usedJson:\n" + usedJson.toString());
        JSONArray wallpapers = usedJson.getJSONArray("wallpapers");
        JSONObject wallpaperDetails = new JSONObject();
        // 已订阅占用空间
        AtomicLong totalSubSize = new AtomicLong();
        // 已取消订阅占用空间
        AtomicLong totalUnSubSize = new AtomicLong();
        // 总空间
        AtomicLong totalSize = new AtomicLong();
        // 已订阅集合
        Set<String> subscribeSet = new HashSet<>(wallpapers.size());
        // 已取消订阅集合
        Set<String> unsSubscribeSet = new HashSet<>(wallpapers.size());
        wallpapers.forEach(paper->{
            JSONObject paperDetail = JSONObject.from(paper);
            String paperId = paperDetail.getString("workshopid");
            subscribeSet.add(paperId);

        });

        // 读取下载目录
        File[] filesList = new File(resPath).listFiles();
        if (filesList != null) {
            for (File downFileFolder: filesList) {
                String paperId = downFileFolder.getName();
                // 读取壁纸信息
                String paperDetailPath = resPath + paperId+ "\\project.json";
                JSONObject paperDetail = readFile(paperDetailPath);
                wallpaperDetails.put(paperId, paperDetail);
                // 读取目录大小
                long folderSize = getFolderSize(new File(resPath + paperId));
                totalSize.addAndGet(folderSize);
                if (subscribeSet.contains(paperId)){// 已订阅计数
                    totalSubSize.addAndGet(folderSize);
                }else {// 已取消订阅计数
                    totalUnSubSize.addAndGet(folderSize);
                    unsSubscribeSet.add(paperId);
                }
            }
        }else{
            System.out.println("读取下载目录为空！");
            return;
        }
        System.out.println("****************** 汇总 **********************");
        System.out.println("总占用：" + new BigDecimal(totalSize.longValue()).divide(BigDecimal.valueOf(1024*1024), 2, RoundingMode.HALF_DOWN)
                + "MB (" + new BigDecimal(totalSize.longValue()).divide(BigDecimal.valueOf(1024*1024*1024), 2, RoundingMode.HALF_DOWN) + "GB)");
        System.out.println("已订阅：" + new BigDecimal(totalSubSize.longValue()).divide(BigDecimal.valueOf(1024*1024), 2, RoundingMode.HALF_DOWN)
                + "MB (" + new BigDecimal(totalSubSize.longValue()).divide(BigDecimal.valueOf(1024*1024*1024), 2, RoundingMode.HALF_DOWN) + "GB)");
        System.out.println("已取消：" + new BigDecimal(totalUnSubSize.longValue()).divide(BigDecimal.valueOf(1024*1024), 2, RoundingMode.HALF_DOWN)
                + "MB (" + new BigDecimal(totalUnSubSize.longValue()).divide(BigDecimal.valueOf(1024*1024*1024), 2, RoundingMode.HALF_DOWN) + "GB)");

        System.out.print("是否释放空间?(yes/no):");
        Scanner sc = new Scanner(System.in);
        if ("yes".equals(sc.nextLine())){
            System.out.println("开始删除...");
            unsSubscribeSet.forEach(paperId->{
                String unUseFolder=resPath + paperId;
                JSONObject paperDetail = wallpaperDetails.getJSONObject(paperId);
                System.out.println("开始删除[" + paperDetail.getString("title") +"]：" + unUseFolder);
                File delFileFolder = new File(unUseFolder);
                try {
                    File[] files = delFileFolder.listFiles();
                    if (files != null) {
                        for (File delFile: files) {
                            Files.deleteIfExists(Paths.get(delFile.getAbsolutePath()));
                        }
                    }
                    boolean res = Files.deleteIfExists(Paths.get(unUseFolder));
                    if (!res){
                        System.out.println("目录删除失败：" + unUseFolder);
                    }
                }catch (DirectoryNotEmptyException e){
                    System.out.println("目录非空，删除失败：" + unUseFolder);
                }catch (IOException e){
                    System.out.println("删除异常：" + unUseFolder);
                }

            });
            System.out.println("删除完成！");
        }else{
            System.out.println("并不清理，退出...");
        }
        sc.nextLine();
    }

    public static JSONObject readFile(String configPath) {
        StringBuilder sb = new StringBuilder();
        try (Stream<String> fileLines = Files.lines(Paths.get(configPath))){
            List<String> lines = fileLines.collect(Collectors.toList());
            lines.forEach(sb::append);
        } catch (FileNotFoundException e) {
            System.out.println("配置文件未找到");
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            System.out.println("读取文件异常");
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            System.out.println("读取文件未知异常");
            e.printStackTrace();
            return null;
        }

        return JSONObject.parseObject(sb.toString());
    }

    /**
     * 递归计算文件夹的大小
     *
     * @param folder 要计算大小的文件夹
     * @return 文件夹的大小（以字节为单位）
     */
    public static long getFolderSize(File folder) {
        long length = 0;
        File[] filesList = folder.listFiles();

        if (filesList != null) {
            for (File file : filesList) {
                if (file.isFile()) {
                    // 如果是文件，累加文件大小
                    length += file.length();
                } else if (file.isDirectory()) {
                    // 如果是文件夹，递归计算子文件夹的大小
                    length += getFolderSize(file);
                }
            }
        }
        return length;
    }

}
