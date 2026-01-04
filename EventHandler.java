package cn.com.nbport.zgt.demo.simulation;

import cn.com.nbport.zgt.demo.simulation.entity.*;
import java.util.List;
import java.util.Random;

public class EventHandler {

    private static final Random random = new Random();
    private static final long SAME_POS_BONUS_TIME = 20;
    private static final long INTERACTION_TIME = 30;

    public static void handleEvent(SimulationContext context, Event currentEvent, DecisionMaker dm, DataGenerator dg) {
        switch (currentEvent.getEventEnum()) {
            case TRUCK_ARRIVAL: handleTruckArrival(context, currentEvent, dm, dg); break;
            case TRUCK_WORK_DONE: handleTruckWorkDone(context, currentEvent, dm, dg); break;
            case QC_WORK_DONE: handleQCWorkDone(context, currentEvent, dm, dg); break;
            case ASC_WORK_DONE: handleASCWorkDone(context, currentEvent, dm, dg); break;
        }
    }

    private static void handleTruckArrival(SimulationContext context, Event event, DecisionMaker dm, DataGenerator dg) {
        String truckId = event.getTruckId();
        Truck truck = context.getTruckMap().get(truckId);
        String currentPos = event.getPosition();

        String recordPos = truck.getCurrentPosition();
        if (recordPos != null && !recordPos.equals(currentPos)) {
            context.releaseNodeAndWakeUp(recordPos, truckId);
            truck.setPreviousPosition(recordPos);
        }
        truck.setCurrentPosition(currentPos);
        context.tryLockNode(currentPos, truckId, 999, null);

        // 任务处理逻辑
        WorkInstruction wi = truck.getCurrentInstruction();

        // 如果当前没有任务 或者刚完成任务处于空档期
        if (wi == null) {
            //  先看队列里有没有缓存的任务
            if (truck.startNextInstruction()) {
                wi = truck.getCurrentInstruction();
            } else {
                //  队列空了 向 DecisionMaker 申请一批新任务
                List<WorkInstruction> newChain = dm.assignInstructionChain(context, truckId);
                if (newChain != null && !newChain.isEmpty()) {
                    truck.addInstructionChain(newChain);
                    truck.startNextInstruction();
                    wi = truck.getCurrentInstruction();
                } else {
                    // 没任务或流控 原地休息
                    truck.setStatus(TruckStatusEnum.IDLE);
                    context.getEventList().add(new Event(context.getSimulationClock() + 20, EventEnum.TRUCK_ARRIVAL, currentPos, truckId, null, null));
                    return;
                }
            }
        }

        String finalTarget = wi.getTargetPosition();
        if (currentPos.equals(finalTarget)) {
            handleDestinationArrival(context, event, truck, dm, dg);
        } else {
            // A* 寻路移动逻辑
            String nextNode = dm.getNextStepNode(context, currentPos, finalTarget);
            if (nextNode == null) return;
            if ("Hub_Main".equals(nextNode)) {
                String nodeAfterHub = dm.getNextStepNode(context, nextNode, finalTarget);
                if (!context.isNodeFree(nodeAfterHub) || !context.isNodeWaitQueueEmpty(nodeAfterHub)) {
                    truck.setStatus(TruckStatusEnum.WAITING);
                    context.addToRoadPassiveWaitingQueue(nodeAfterHub, truckId);
                    return;
                }
            }
            int priority = calculatePriority(currentPos, nextNode);
            if (context.tryLockNode(nextNode, truckId, priority, currentPos)) {
                long stepTime = dg.predictStepTravelTime(context, currentPos, nextNode);
                context.addReservation(nextNode, truckId, context.getSimulationClock(), stepTime);
                context.getEventList().add(new Event(context.getSimulationClock() + stepTime, EventEnum.TRUCK_ARRIVAL, nextNode, truckId, null, null));
                truck.setStatus(TruckStatusEnum.MOVING);
            } else {
                truck.setStatus(TruckStatusEnum.WAITING);
                context.addToRoadPassiveWaitingQueue(nextNode, truckId);
            }
        }
    }

    private static void handleTruckWorkDone(SimulationContext context, Event e, DecisionMaker dm, DataGenerator dg) {
        Truck truck = context.getTruckMap().get(e.getTruckId());

        // 标记当前任务完成
        truck.setCurrentInstruction(null);

        // 不需要等待
        handleTruckArrival(context, e, dm, dg);
    }

    private static int calculatePriority(String currentPos, String nextPos) {
        if (currentPos.equals("Hub_Main")) return 20;
        if (nextPos.equals("Hub_Main")) return 10;
        return 5;
    }

    private static void handleDestinationArrival(SimulationContext context, Event event, Truck truck, DecisionMaker dm, DataGenerator dg) {
        String position = truck.getCurrentPosition();
        String deviceId = context.getPositionToCurrentOccupiedEntity().get(position);
        if (deviceId != null) {
            if (context.getQcMap().containsKey(deviceId)) {
                event.setQcId(deviceId); handleArrivalAtQC(context, event, truck, dg);
            } else if (context.getAscMap().containsKey(deviceId)) {
                event.setAscId(deviceId); handleArrivalAtASC(context, event, truck, dg);
            }
        } else if ("Gate".equals(position)) {
            // 外集卡出大门 清空任务队列
            truck.setCurrentInstruction(null);
            context.releaseNodeAndWakeUp("Gate", truck.getId());
            truck.setStatus(TruckStatusEnum.IDLE);
            context.getEventList().add(new Event(context.getSimulationClock() + 60, EventEnum.TRUCK_ARRIVAL, "Gate", truck.getId(), null, null));
        }
    }

    private static void handleArrivalAtQC(SimulationContext c, Event e, Truck t, DataGenerator g) {
        QC qc = c.getQcMap().get(e.getQcId());
        processDeviceArrival(c, e, t, qc.getStatus() == QCStatusEnum.IDLE, qc.getCurrentPosition(), e.getQcId(), null, g);
        if (qc.getStatus() == QCStatusEnum.IDLE) { qc.setStatus(QCStatusEnum.WORKING); qc.setCurrentTruckId(t.getId()); }
    }

    private static void handleArrivalAtASC(SimulationContext c, Event e, Truck t, DataGenerator g) {
        ASC asc = c.getAscMap().get(e.getAscId());
        processDeviceArrival(c, e, t, asc.getStatus() == ASCStatusEnum.IDLE, asc.getCurrentPosition(), null, e.getAscId(), g);
        if (asc.getStatus() == ASCStatusEnum.IDLE) { asc.setStatus(ASCStatusEnum.WORKING); asc.setCurrentTruckId(t.getId()); }
    }

    private static void processDeviceArrival(SimulationContext c, Event e, Truck t, boolean isDeviceIdle, String pos, String qcId, String ascId, DataGenerator g) {
        if (isDeviceIdle) {
            t.setStatus(TruckStatusEnum.WORKING);
            long rawCycleTime = (qcId != null) ? g.predictQCTotalCycleTime(c, qcId) : g.predictASCTotalCycleTime(c, ascId);
            String deviceId = (qcId != null) ? qcId : ascId;
            long idleDuration = c.getDeviceIdleDuration(deviceId);
            long effectiveCycleTime = Math.max(INTERACTION_TIME, rawCycleTime - idleDuration);
            String lastWorkPos = c.getDeviceLastWorkPos(deviceId);
            if (pos.equals(lastWorkPos)) effectiveCycleTime = Math.max(INTERACTION_TIME, effectiveCycleTime - SAME_POS_BONUS_TIME);
            c.updateDeviceWorkPos(deviceId, pos);

            boolean isDropOff = random.nextBoolean();
            long truckFinishDelta = (isDropOff && ascId != null) ? INTERACTION_TIME : effectiveCycleTime;
            long deviceFinishDelta = effectiveCycleTime;

            long currentClock = c.getSimulationClock();
            if (qcId != null) {
                c.getEventList().add(new Event(currentClock + deviceFinishDelta, EventEnum.TRUCK_WORK_DONE, pos, t.getId(), null, qcId));
                c.getEventList().add(new Event(currentClock + deviceFinishDelta + 1, EventEnum.QC_WORK_DONE, pos, t.getId(), null, qcId));
            } else {
                c.getEventList().add(new Event(currentClock + truckFinishDelta, EventEnum.TRUCK_WORK_DONE, pos, t.getId(), ascId, null));
                c.getEventList().add(new Event(currentClock + deviceFinishDelta + 1, EventEnum.ASC_WORK_DONE, pos, t.getId(), ascId, null));
            }
        } else {
            t.setStatus(TruckStatusEnum.WAITING);
            c.addToWaitingQueue(pos, t.getId());
        }
    }

    private static void handleQCWorkDone(SimulationContext c, Event e, DecisionMaker dm, DataGenerator dg) {
        QC qc = c.getQcMap().get(e.getQcId());
        qc.setStatus(QCStatusEnum.IDLE);
        qc.setCurrentTruckId(null);
        c.markDeviceFree(e.getQcId());
        wakeUpNextEntity(c, e, qc.getCurrentPosition(), dg, true);
    }

    private static void handleASCWorkDone(SimulationContext c, Event e, DecisionMaker dm, DataGenerator dg) {
        ASC asc = c.getAscMap().get(e.getAscId());
        asc.setStatus(ASCStatusEnum.IDLE);
        asc.setCurrentTruckId(null);
        c.markDeviceFree(e.getAscId());
        wakeUpNextEntity(c, e, asc.getCurrentPosition(), dg, false);
    }

    private static void wakeUpNextEntity(SimulationContext c, Event e, String pos, DataGenerator g, boolean isQC) {
        String deviceId = c.getPositionToCurrentOccupiedEntity().get(pos);
        String nextTruckId = (deviceId != null) ? c.pollSmartFromWaitingQueue(pos, deviceId) : c.pollSmartFromWaitingQueue(pos);
        if(nextTruckId != null) {
            e.setTruckId(nextTruckId);
            Truck nextTruck = c.getTruckMap().get(nextTruckId);
            if (isQC) handleArrivalAtQC(c, e, nextTruck, g);
            else handleArrivalAtASC(c, e, nextTruck, g);
        }
    }
}