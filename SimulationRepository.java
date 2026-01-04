package cn.com.nbport.zgt.demo.simulation;
// 引入jackson库
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// 落库
public class SimulationRepository {
    // 定义一个列表 List 泛型是 <Event> 名字  eventHistory 所有的数据都存在这个list里面
    private List<Event> eventHistory = new ArrayList<>();
    // 仿真开始前确保仓库是空的 防止和上一次的数据混入
    public void init() {
        eventHistory.clear();
        System.out.println("内存仓库完成");
    }
    // 收集数据
    public void saveEvent(Event event) {
        eventHistory.add(event);
    }
    // 无数据 直接返回
    public void commitToJsonFile() {
        if (eventHistory.isEmpty()) {
            System.out.println("无数据保存");
            return;
        }
        String folderPath = "D:\\A大湾区\\discrete-event-simulation-demo\\discrete-event-simulation-demo\\download";

        // 创建文件夹对象
        File folder = new File(folderPath);

        // 检查文件夹是否存在 不存在就建一个
        if (!folder.exists()) {
            boolean created = folder.mkdirs(); // mkdirs() 可以创建多级目录
            if (created) {
                System.out.println("目录不存在 已创建: " + folderPath);
            }
        }

        // 生成文件名
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = "simulation_data_" + timeStamp + ".json";

        // 组合成最终的文件对象
        File targetFile = new File(folder, fileName);

        System.out.println("  正在生成 JSON (共 " + eventHistory.size() + " 条)...");

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);

            // 入到指定文件
            mapper.writeValue(targetFile, eventHistory);

            System.out.println("  JSON 落库成功！\n  文件位置: " + targetFile.getAbsolutePath());

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("  JSON 写入失败: " + e.getMessage());
        }
    }

}

