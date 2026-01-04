package cn.com.nbport.zgt.demo.simulation.map;

import cn.com.nbport.zgt.demo.simulation.entity.MapDefinition;
import java.util.*;

public class MapData {
    // 邻接表 存储图结构的核数据
    private Map<String, List<String>> adjacencyList = new HashMap<>();

    // 距离表
    private Map<String, Double> distances = new HashMap<>();

    // 节点属性表
    private Map<String, LogicalLocation> locations = new HashMap<>();

    // 每个格子的物理长度
    private static final double CELL_SIZE = 20.0;

    // 构造函数
    public MapData(MapDefinition mapDef) {
        if (mapDef != null) {
            initializeFromConfig(mapDef);
        } else {
            System.err.println("Warning: No MapDefinition provided in InputData. Building empty map.");
        }
    }

    // 根据配置自动生成栅格化地图
    private void initializeFromConfig(MapDefinition mapDef) {
        // 1. 注册关键节点 (Block-A, Hub_Main 等)
        if (mapDef.getNodes() != null) {
            for (MapDefinition.NodeDef node : mapDef.getNodes()) {
                addLocation(node.getId(), node.getType());
            }
        }

        // 自动栅格化道路 (生成中间的 Lane_1, Lane_2...)
        if (mapDef.getLinks() != null) {
            for (MapDefinition.LinkDef link : mapDef.getLinks()) {
                // 计算格子数量 = 距离 / 格子大小 (向上取整)
                int cells = (int) Math.ceil(link.getDistance() / CELL_SIZE);
                if (cells < 1) cells = 1;

                // 自动生成中间的磁钉节点链
                createLane(link.getId(), link.getFromNode(), link.getToNode(), cells);

                System.out.println("Auto-Generated: " + link.getId() +
                        " [" + link.getFromNode() + "->" + link.getToNode() +
                        "] Split into " + cells + " cells.");
            }
        }
    }

    // 生成中间节点链
    private void createLane(String prefix, String from, String to, int cells) {
        String previousNode = from;
        for (int i = 1; i <= cells; i++) {
            String cellId = prefix + "_" + i;

            // 中间生成的节点统一为 ROAD 类型
            addLocation(cellId, LogicalLocation.TYPE_ROAD);

            // 连接：前一个节点 -> 当前格子
            connectDirected(previousNode, cellId, CELL_SIZE);

            previousNode = cellId;
        }
        // 连接：最后一个格子 -> 终点
        connectDirected(previousNode, to, CELL_SIZE);
    }

    private void addLocation(String id, String type) {
        locations.put(id, new LogicalLocation(id, type));
    }

    private void connectDirected(String from, String to, double dist) {
        adjacencyList.computeIfAbsent(from, k -> new ArrayList<>()).add(to);
        distances.put(from + "->" + to, dist);
    }

// 获取邻居节点列表
    public List<String> getNeighbors(String nodeId) {
        // 返回该节点能通往的所有下级节点
        // 使用 getOrDefault 防止 NullPointerException
        return adjacencyList.getOrDefault(nodeId, Collections.emptyList());
    }
//BFS 寻路 A*不能工作时寻路
    public String findNextHop(String start, String target) {
        if (start.equals(target)) return start;
        if (!adjacencyList.containsKey(start)) return start;

        Map<String, String> cameFrom = new HashMap<>();
        Queue<String> frontier = new LinkedList<>();
        frontier.add(start);
        cameFrom.put(start, null);

        boolean found = false;
        while (!frontier.isEmpty()) {
            String current = frontier.poll();
            if (current.equals(target)) {
                found = true;
                break;
            }
            for (String next : adjacencyList.getOrDefault(current, Collections.emptyList())) {
                if (!cameFrom.containsKey(next)) {
                    frontier.add(next);
                    cameFrom.put(next, current);
                }
            }
        }

        if (!found) return start; // 如果找不到路 保持原地

        // 回溯 找到路径中的第一步
        String curr = target;
        while (curr != null && !start.equals(cameFrom.get(curr))) {
            curr = cameFrom.get(curr);
        }
        return curr;
    }

    public double getDistance(String from, String to) {
        return distances.getOrDefault(from + "->" + to, 9999.0);
    }

    public List<LogicalLocation> getLocationsByType(String type) {
        List<LogicalLocation> result = new ArrayList<>();
        for (LogicalLocation loc : locations.values()) {
            if (loc.getType().equals(type)) result.add(loc);
        }
        return result;
    }

    public LogicalLocation getLocation(String id) {
        return locations.get(id);
    }
}