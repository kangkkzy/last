package cn.com.nbport.zgt.demo.simulation;

import cn.com.nbport.zgt.demo.simulation.entity.*;
import cn.com.nbport.zgt.demo.simulation.entity.InputData;
import cn.com.nbport.zgt.demo.simulation.map.MapData;
import java.util.*;

public class SimulationInitializer {

    public SimulationContext initialize(InputData inputData) {
        SimulationContext context = new SimulationContext();
        context.setSimulationClock(0);
        context.setEventList(new PriorityQueue<>(Comparator.comparingLong(Event::getEventTime)));

        //  通用地图构建 自动生成栅格
        MapData mapData = new MapData(inputData.getMapDefinition());
        context.setMapData(mapData);

        //  初始化 QC
        Map<String, QC> qcMap = new HashMap<>();
        if (inputData.getQcList() != null) {
            for (String s : inputData.getQcList()) {
                String[] parts = s.split(":");
                qcMap.put(parts[0], new QC(parts[0], parts[1]));
            }
        }
        context.setQcMap(qcMap);

        //  初始化 ASC
        Map<String, ASC> ascMap = new HashMap<>();
        List<String> yardBlocks = new ArrayList<>(); // 用于随机分配内集卡初始位置

        if (inputData.getAscList() != null) {
            for (String s : inputData.getAscList()) {
                String[] parts = s.split(":");
                String pos = parts[1];
                ascMap.put(parts[0], new ASC(parts[0], pos));

                if (!yardBlocks.contains(pos)) {
                    yardBlocks.add(pos);
                }
            }
        }
        context.setAscMap(ascMap);
        // 如果列表为空但需要跑内集卡逻辑 手动加一个默认值
        if (yardBlocks.isEmpty()) yardBlocks.add("Block-A");

        //  初始化集卡
        Map<String, Truck> truckMap = new HashMap<>();
        Random random = new Random();
        int internalIndex = 0;

        if (inputData.getTruckList() != null) {
            for (String s : inputData.getTruckList()) {
                String tId = s.split(":")[0];
                Truck truck = new Truck(tId);

                if (tId.endsWith("1") || tId.endsWith("2") || tId.endsWith("3")) {
                    truck.setType(TruckType.INTERNAL);
                    // 随机分配到某个堆场
                    String startPos = yardBlocks.get(internalIndex % yardBlocks.size());
                    truck.setCurrentPosition(startPos);
                    internalIndex++;
                    System.out.println("init: Internal " + tId + " starts at " + startPos);
                } else {
                    truck.setType(TruckType.EXTERNAL);
                    truck.setCurrentPosition("Gate");
                }

                truckMap.put(tId, truck);

                // 初始事件
                long initialDelay = 0;
                try {
                    String numStr = tId.replaceAll("\\D", "");
                    if (!numStr.isEmpty()) initialDelay = Long.parseLong(numStr) * 10;
                } catch (Exception e) {}

                context.getEventList().add(new Event(initialDelay, EventEnum.TRUCK_ARRIVAL, truck.getCurrentPosition(), tId, null, null));
            }
        }
        context.setTruckMap(truckMap);

        //  初始化占用表
        Map<String, String> occupied = new HashMap<>();
        for (ASC asc : ascMap.values()) occupied.put(asc.getCurrentPosition(), asc.getId());
        for (QC qc : qcMap.values()) occupied.put(qc.getCurrentPosition(), qc.getId());
        context.setPositionToCurrentOccupiedEntity(occupied);

        return context;
    }
}
