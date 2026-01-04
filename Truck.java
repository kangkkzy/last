package cn.com.nbport.zgt.demo.simulation.entity;

import java.util.LinkedList;
import java.util.Queue;
import java.util.List;

public class Truck {
    private String id;
    private String currentPosition;
    private String previousPosition;
    private TruckStatusEnum status;

    // 当前正在执行的指令
    private WorkInstruction currentInstruction;

    // 任务队列：存储后续要执行的指令链
    private Queue<WorkInstruction> taskQueue = new LinkedList<>();

    private TruckType type;
    private int tasksInCurrentTrip = 0; // 用于外集卡双循环计数

    public Truck(String id) {
        this.id = id;
        this.type = TruckType.EXTERNAL;
        this.status = TruckStatusEnum.IDLE;
    }

// 接收任务链
    public void addInstructionChain(List<WorkInstruction> instructions) {
        if (instructions != null) {
            this.taskQueue.addAll(instructions);
        }
    }
// 开始下一个任务
    public boolean startNextInstruction() {
        if (!taskQueue.isEmpty()) {
            this.currentInstruction = taskQueue.poll();
            return true;
        }
        this.currentInstruction = null;
        return false;
    }

    public boolean hasPendingTasks() {
        return !taskQueue.isEmpty();
    }

    //  Getters/Setters
    public void setType(TruckType type) { this.type = type; }
    public TruckType getType() { return type; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCurrentPosition() { return currentPosition; }
    public void setCurrentPosition(String currentPosition) { this.currentPosition = currentPosition; }
    public String getPreviousPosition() { return previousPosition; }
    public void setPreviousPosition(String previousPosition) { this.previousPosition = previousPosition; }
    public TruckStatusEnum getStatus() { return status; }
    public void setStatus(TruckStatusEnum status) { this.status = status; }
    public WorkInstruction getCurrentInstruction() { return currentInstruction; }
    public void setCurrentInstruction(WorkInstruction currentInstruction) { this.currentInstruction = currentInstruction; }
    public int getTasksInCurrentTrip() { return tasksInCurrentTrip; }
    public void incrementTasksInCurrentTrip() { this.tasksInCurrentTrip++; }
    public void resetTasksInCurrentTrip() { this.tasksInCurrentTrip = 0; }
}
