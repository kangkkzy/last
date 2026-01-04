package cn.com.nbport.zgt.demo.simulation;

import cn.com.nbport.zgt.demo.simulation.entity.Truck;
import cn.com.nbport.zgt.demo.simulation.map.MapData;
import java.util.Random;

public class DataGenerator {
    private Random random = new Random(12345); // 固定种子以便复现
    // 计算时间
    public long predictStepTravelTime(SimulationContext context, String from, String to) {
        MapData map = context.getMapData();
        if (map == null) return 30; // 默认30秒

        double distance = map.getDistance(from, to);
        if (distance > 9000) return 30; // 异常距离处理

        // 假设集卡速度 30 km/h ≈ 8.3 m/s
        double speed = 8.3;
        // 加上一点随机波动 (路况)
        double noise = random.nextDouble() * 2.0;

        long timeSeconds = (long) (distance / (speed - noise));
        return Math.max(5, timeSeconds); // 至少5秒
    }
    public long predictTruckMoveTime(SimulationContext context, String truckId, String targetPos) {
        return 60;
    }

    public long predictContainerTransferTime() {
        // 集卡交互时间 30s - 60s
        return 30 + random.nextInt(30);
    }

    public long predictQCTotalCycleTime(SimulationContext context, String qcId) {
        // 岸桥完整循环 90s - 150s
        return 90 + random.nextInt(60);
    }

    public long predictASCTotalCycleTime(SimulationContext context, String ascId) {
        // 场桥完整循环 80s - 140s
        return 80 + random.nextInt(60);
    }
}
