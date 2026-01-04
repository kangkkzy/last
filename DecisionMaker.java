package cn.com.nbport.zgt.demo.simulation;

import cn.com.nbport.zgt.demo.simulation.entity.*;
import cn.com.nbport.zgt.demo.simulation.map.LogicalLocation;
import cn.com.nbport.zgt.demo.simulation.map.MapData;
import java.util.*;

public class DecisionMaker {
    private int wiCounter = 0;
    private Random random = new Random();

    // 参数配置
    private static final int MAX_QUEUE_THRESHOLD = 5;
    private static final double BASE_COST_PER_HOP = 10.0;
    private static final double CURRENT_CONGESTION_PENALTY = 100.0;
    private static final double TRUCK_SPEED = 1.0;
    private static final long SAFETY_BUFFER = 5;

    private static class NodeWrapper implements Comparable<NodeWrapper> {
        String nodeId; double gScore; double fScore; String parentId;
        public NodeWrapper(String n, double g, double f, String p) { nodeId=n; gScore=g; fScore=f; parentId=p; }
        @Override public int compareTo(NodeWrapper o) { return Double.compare(this.fScore, o.fScore); }
    }
// 通过任务链完成工作
    public List<WorkInstruction> assignInstructionChain(SimulationContext context, String truckId) {
        Truck truck = context.getTruckMap().get(truckId);
        String currentPos = truck.getCurrentPosition();

        List<WorkInstruction> chain = new ArrayList<>();

        // 获取位置类型
        String currentType = "GATE";
        if (context.getMapData() != null) {
            LogicalLocation loc = context.getMapData().getLocation(currentPos);
            if (loc != null) currentType = loc.getType();
        }
        // 场景 A: 外集卡 (进门 -> 堆场作业 -> [可选堆场作业] -> 出门)
        if (truck.getType() == TruckType.EXTERNAL) {
            if (LogicalLocation.TYPE_GATE.equals(currentType)) {
                //去堆场
                String yardTarget = pickSmartTarget(context, LogicalLocation.TYPE_YARD, currentPos, truck);
                if (yardTarget == null) return null; // 流控限制

                chain.add(createWI(currentPos, yardTarget));

                //  可能进行双循环
                // 假设 60% 概率有双循环任务
                if (random.nextDouble() < 0.6) {
                    // 优先选择同堆场  -> 效率最高
                    chain.add(createWI(yardTarget, yardTarget));
                    // 回大门
                    chain.add(createWI(yardTarget, "Gate"));
                } else {
                    // 没有双循环 直接回大门
                    chain.add(createWI(yardTarget, "Gate"));
                }

                truck.resetTasksInCurrentTrip();
                return chain;
            }
            // 如果已经在堆场且没任务 回大门
            chain.add(createWI(currentPos, "Gate"));
            return chain;
        }

        // 场景 B: 内集卡 (岸边 <-> 堆场 永动机) 卸货后立刻提货
        if (truck.getType() == TruckType.INTERNAL) {
            String targetPos;

            // 如果在岸边 去堆场
            if (LogicalLocation.TYPE_BERTH.equals(currentType)) {
                targetPos = pickSmartTarget(context, LogicalLocation.TYPE_YARD, currentPos, truck);
                if (targetPos == null) return null;
                chain.add(createWI(currentPos, targetPos));

                // 到达堆场后，立刻预判回岸边的任务
                String backToBerth = pickSmartTarget(context, LogicalLocation.TYPE_BERTH, targetPos, truck);
                if (backToBerth != null) {
                    chain.add(createWI(targetPos, backToBerth));
                }
            }
            // 如果在堆场 去岸边
            else if (LogicalLocation.TYPE_YARD.equals(currentType)) {
                targetPos = pickSmartTarget(context, LogicalLocation.TYPE_BERTH, currentPos, truck);
                if (targetPos == null) return null;
                chain.add(createWI(currentPos, targetPos));

                //  到达岸边后，立刻预判去堆场的任务
                String backToYard = pickSmartTarget(context, LogicalLocation.TYPE_YARD, targetPos, truck);
                if (backToYard != null) {
                    chain.add(createWI(targetPos, backToYard));
                }
            }
            // 异常兜底
            else {
                targetPos = pickSmartTarget(context, LogicalLocation.TYPE_YARD, currentPos, truck);
                if (targetPos != null) chain.add(createWI(currentPos, targetPos));
            }

            return chain.isEmpty() ? null : chain;
        }

        return null;
    }

    private WorkInstruction createWI(String from, String to) {
        wiCounter++;
        return new WorkInstruction("WI-" + wiCounter, "TRANSPORT", from, to);
    }

    // 智能选点逻辑
    private String pickSmartTarget(SimulationContext context, String targetType, String currentPos, Truck truck) {
        MapData map = context.getMapData();
        List<LogicalLocation> candidates = map.getLocationsByType(targetType);
        if (candidates == null || candidates.isEmpty()) return null;

        //  同位优先
        if (LogicalLocation.TYPE_YARD.equals(targetType)) {
            for (LogicalLocation loc : candidates) {
                if (currentPos.contains(loc.getName())) { // 如果已经在该区域
                    if (calculateEffectiveLoad(context, loc.getName()) < MAX_QUEUE_THRESHOLD) {
                        return loc.getName();
                    }
                }
            }
        }

        // 负载均衡
        int minLoad = Integer.MAX_VALUE;
        List<String> bestCandidates = new ArrayList<>();

        for (LogicalLocation loc : candidates) {
            int load = calculateEffectiveLoad(context, loc.getName());
            if (load < minLoad) {
                minLoad = load;
                bestCandidates.clear();
                bestCandidates.add(loc.getName());
            } else if (load == minLoad) {
                bestCandidates.add(loc.getName());
            }
        }

        if (bestCandidates.isEmpty()) return null;
        return bestCandidates.get(random.nextInt(bestCandidates.size()));
    }

    //  A* 寻路
    public String getNextStepNode(SimulationContext context, String currentPos, String finalTarget) {
        if (currentPos.equals(finalTarget)) return currentPos;
        if (context.getMapData() == null) return currentPos;
        return findNextHopWithSoftCostAStar(context, currentPos, finalTarget);
    }

    private String findNextHopWithSoftCostAStar(SimulationContext context, String start, String goal) {
        MapData mapData = context.getMapData();
        PriorityQueue<NodeWrapper> openSet = new PriorityQueue<>();
        Map<String, Double> gScore = new HashMap<>();
        Map<String, String> cameFrom = new HashMap<>();

        gScore.put(start, 0.0);
        openSet.add(new NodeWrapper(start, 0.0, 0.0, null));
        Set<String> visited = new HashSet<>();

        while (!openSet.isEmpty()) {
            NodeWrapper currentWrapper = openSet.poll();
            String current = currentWrapper.nodeId;

            if (visited.contains(current)) continue;
            visited.add(current);

            if (current.equals(goal)) return reconstructFirstStep(cameFrom, start, goal);

            List<String> neighbors = mapData.getNeighbors(current);
            if (neighbors == null) continue;

            for (String neighbor : neighbors) {
                if (visited.contains(neighbor)) continue;

                double dist = mapData.getDistance(current, neighbor);
                long travelTime = (long) (dist / TRUCK_SPEED);
                if (travelTime <= 0) travelTime = 1;

                long currentG = gScore.getOrDefault(current, 0.0).longValue();
                long arrivalTime = context.getSimulationClock() + currentG + travelTime;
                long leaveTime = arrivalTime + SAFETY_BUFFER;

                double stepCost = BASE_COST_PER_HOP;
                if (!context.isNodeFree(neighbor) || !context.isNodeWaitQueueEmpty(neighbor)) {
                    stepCost += CURRENT_CONGESTION_PENALTY;
                }
                if (context.isReservedInInterval(neighbor, arrivalTime, leaveTime)) {
                    long earliestIdle = context.findEarliestIdleTime(neighbor, arrivalTime);
                    long waitTime = earliestIdle - arrivalTime;
                    if (waitTime > 0) stepCost += (double) waitTime;
                }

                double tentativeG = gScore.getOrDefault(current, Double.MAX_VALUE) + stepCost + travelTime;

                if (tentativeG < gScore.getOrDefault(neighbor, Double.MAX_VALUE)) {
                    cameFrom.put(neighbor, current);
                    gScore.put(neighbor, tentativeG);
                    openSet.add(new NodeWrapper(neighbor, tentativeG, tentativeG, current));
                }
            }
        }
        return mapData.findNextHop(start, goal);
    }

    private String reconstructFirstStep(Map<String, String> cameFrom, String start, String goal) {
        String curr = goal; int limit = 1000;
        while (cameFrom.containsKey(curr) && limit-- > 0) {
            String prev = cameFrom.get(curr);
            if (prev.equals(start)) return curr;
            curr = prev;
        }
        return null;
    }

    private int calculateEffectiveLoad(SimulationContext context, String locationId) {
        int load = 0;
        for (Truck t : context.getTruckMap().values()) {
            WorkInstruction wi = t.getCurrentInstruction();
            if (wi != null && locationId.equals(wi.getTargetPosition()) && !t.getCurrentPosition().equals(locationId)) load++;
        }
        return load;
    }
}