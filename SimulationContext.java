package cn.com.nbport.zgt.demo.simulation;

import cn.com.nbport.zgt.demo.simulation.entity.*;
import cn.com.nbport.zgt.demo.simulation.map.MapData;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SimulationContext {
    private long simulationClock;
    private PriorityQueue<Event> eventList;
    private Map<String, QC> qcMap;
    private Map<String, ASC> ascMap;
    private Map<String, Truck> truckMap;

    //  队列管理
    private Map<String, List<String>> positionToWaitingEntities = new ConcurrentHashMap<>();
    private Map<String, Queue<String>> roadPassiveWaitingQueue = new ConcurrentHashMap<>();
    private Map<String, List<LockRequest>> nodeWaitQueues = new ConcurrentHashMap<>();

    // 状态记录
    private Map<String, String> positionToCurrentOccupiedEntity = new ConcurrentHashMap<>();
    private Map<String, Set<String>> nodeOccupancyMap = new ConcurrentHashMap<>();
    private Map<String, Integer> nodeCapacityMap = new HashMap<>();
    private Map<String, String> nodeLastEntrySourceMap = new ConcurrentHashMap<>();

    private MapData mapData;
    private Map<String, List<TimeSlot>> reservationTable = new ConcurrentHashMap<>();

    //  位置记忆 设备上次在哪干活
    private Map<String, String> deviceLastWorkPos = new ConcurrentHashMap<>();

    //  JIT 核心 设备最后变为空闲的时间戳
    private Map<String, Long> deviceLastFreeTime = new ConcurrentHashMap<>();

    // 内部类
    public static class TimeSlot {
        long start; long end; String truckId;
        public TimeSlot(long s, long e, String t) { start=s; end=e; truckId=t; }
    }
    public static class LockRequest {
        String truckId; int basePriority; long requestTime; String fromNode;
        public LockRequest(String t, int p, long r, String f) { truckId=t; basePriority=p; requestTime=r; fromNode=f; }
    }

    public SimulationContext() {
        nodeCapacityMap.put("Hub_Main", 1);
        nodeCapacityMap.put("Gate", 999);
    }
    //区间碰撞检测
    public synchronized boolean isReservedInInterval(String nodeId, long checkStart, long checkEnd) {
        List<TimeSlot> slots = reservationTable.get(nodeId);
        if (slots == null || slots.isEmpty()) return false;

        for (TimeSlot slot : slots) {
            // 区间重叠判断: Max(StartA, StartB) < Min(EndA, EndB)
            long overlapStart = Math.max(checkStart, slot.start);
            long overlapEnd = Math.min(checkEnd, slot.end);

            if (overlapStart < overlapEnd) {
                return true; // 发生碰撞
            }
        }
        return false;
    }
    // 支持方法
    public void markDeviceFree(String deviceId) {
        deviceLastFreeTime.put(deviceId, simulationClock);
    }

    public long getDeviceIdleDuration(String deviceId) {
        if (!deviceLastFreeTime.containsKey(deviceId)) return 0;
        return simulationClock - deviceLastFreeTime.get(deviceId);
    }
// 智能调度
    public void updateDeviceWorkPos(String deviceId, String pos) {
        deviceLastWorkPos.put(deviceId, pos);
    }

    public String getDeviceLastWorkPos(String deviceId) {
        return deviceLastWorkPos.get(deviceId);
    }
// 内集卡优先
    public synchronized String pollSmartFromWaitingQueue(String position, String deviceId) {
        List<String> queue = positionToWaitingEntities.get(position);
        if (queue == null || queue.isEmpty()) return null;

        int bestIndex = -1;
        int maxScore = -1;

        for (int i = 0; i < queue.size(); i++) {
            String truckId = queue.get(i);
            Truck t = truckMap.get(truckId);
            if (t == null) continue;

            int score = (t.getType() == TruckType.INTERNAL) ? 60 : 40;
            if (score > maxScore) {
                maxScore = score;
                bestIndex = i;
            }
        }

        if (bestIndex != -1) return queue.remove(bestIndex);
        return queue.remove(0);
    }

    public synchronized String pollSmartFromWaitingQueue(String position) {
        return pollSmartFromWaitingQueue(position, null);
    }

    // 资源管理方法
    public synchronized void addReservation(String nodeId, String truckId, long startTime, long duration) {
        reservationTable.computeIfAbsent(nodeId, k -> new ArrayList<>()).add(new TimeSlot(startTime, startTime + duration, truckId));
    }

    public synchronized boolean isReservedAtTime(String nodeId, long checkTime) {
        return isReservedInInterval(nodeId, checkTime, checkTime + 1);
    }

    public synchronized long findEarliestIdleTime(String nodeId, long earliestArrival) {
        List<TimeSlot> slots = reservationTable.get(nodeId);
        if (slots == null || slots.isEmpty()) return earliestArrival;
        slots.sort(Comparator.comparingLong(s -> s.start));
        long candidateTime = earliestArrival;
        for (TimeSlot slot : slots) {
            if (candidateTime < slot.end && candidateTime >= slot.start) candidateTime = slot.end;
        }
        return candidateTime;
    }

    public void addToWaitingQueue(String p, String t) {
        if(p!=null && t!=null) positionToWaitingEntities.computeIfAbsent(p, k->new ArrayList<>()).add(t);
    }

    public String pollFromWaitingQueue(String p) {
        List<String> l=positionToWaitingEntities.get(p);
        if(l!=null&&!l.isEmpty()) return l.remove(0);
        return null;
    }

    public void addToRoadPassiveWaitingQueue(String t, String truckId) {
        roadPassiveWaitingQueue.computeIfAbsent(t, k->new LinkedList<>()).offer(truckId);
    }

    public synchronized void releaseNodeAndWakeUp(String nodeId, String truckId) {
        if (nodeId == null) return;
        Set<String> occupiers = nodeOccupancyMap.get(nodeId);
        if (occupiers != null) {
            occupiers.remove(truckId);
            if (occupiers.isEmpty()) nodeOccupancyMap.remove(nodeId);
        }
        List<TimeSlot> slots = reservationTable.get(nodeId);
        if (slots != null) slots.removeIf(s -> s.end < simulationClock);

        Queue<String> queue = roadPassiveWaitingQueue.get(nodeId);
        if (queue != null && !queue.isEmpty()) {
            String w = queue.poll(); Truck wt = truckMap.get(w);
            if(wt!=null) eventList.add(new Event(simulationClock+1, EventEnum.TRUCK_ARRIVAL, wt.getCurrentPosition(), w, null, null));
        }
    }

    public synchronized boolean tryLockNode(String nodeId, String truckId, int basePriority, String fromNode) {
        if (nodeId == null) return true;
        nodeOccupancyMap.computeIfAbsent(nodeId, k -> new HashSet<>());
        if (nodeOccupancyMap.get(nodeId).contains(truckId)) return true;

        nodeWaitQueues.computeIfAbsent(nodeId, k -> new ArrayList<>());
        List<LockRequest> queue = nodeWaitQueues.get(nodeId);

        Optional<LockRequest> ex = queue.stream().filter(r -> r.truckId.equals(truckId)).findFirst();
        if (ex.isPresent()) ex.get().basePriority = basePriority;
        else queue.add(new LockRequest(truckId, basePriority, simulationClock, fromNode));

        queue.sort((r1, r2) -> {
            if (r1.basePriority != r2.basePriority) return Integer.compare(r2.basePriority, r1.basePriority);
            return Long.compare(r1.requestTime, r2.requestTime);
        });

        if (!queue.get(0).truckId.equals(truckId)) return false;

        Set<String> occupiers = nodeOccupancyMap.get(nodeId);
        if (occupiers.size() < getNodeCapacity(nodeId)) {
            occupiers.add(truckId);
            queue.remove(0);
            if (queue.isEmpty()) nodeWaitQueues.remove(nodeId);
            if (fromNode != null) nodeLastEntrySourceMap.put(nodeId, fromNode);
            return true;
        }
        return false;
    }

    // Getters
    public int getNodeCapacity(String n) { return nodeCapacityMap.getOrDefault(n, 1); }
    public boolean isNodeFree(String n) { if(n==null||"Gate".equals(n))return true; Set<String> o=nodeOccupancyMap.get(n); return o==null||o.size()<getNodeCapacity(n); }
    public boolean isNodeWaitQueueEmpty(String n) { List<LockRequest> q=nodeWaitQueues.get(n); return q==null||q.isEmpty(); }
    public Map<String,Set<String>> getNodeOccupancyMap(){return nodeOccupancyMap;}
    public PriorityQueue<Event> getEventList() { return eventList; }
    public void setEventList(PriorityQueue<Event> e) { this.eventList = e; }
    public long getSimulationClock() { return simulationClock; }
    public void setSimulationClock(long c) { this.simulationClock = c; }
    public Map<String, QC> getQcMap() { return qcMap; }
    public void setQcMap(Map<String, QC> m) { this.qcMap = m; }
    public Map<String, ASC> getAscMap() { return ascMap; }
    public void setAscMap(Map<String, ASC> m) { this.ascMap = m; }
    public Map<String, Truck> getTruckMap() { return truckMap; }
    public void setTruckMap(Map<String, Truck> m) { this.truckMap = m; }
    public Map<String, String> getPositionToCurrentOccupiedEntity() { return positionToCurrentOccupiedEntity; }
    public void setPositionToCurrentOccupiedEntity(Map<String, String> m) { this.positionToCurrentOccupiedEntity = m; }
    public MapData getMapData() { return mapData; }
    public void setMapData(MapData m) { this.mapData = m; }
}