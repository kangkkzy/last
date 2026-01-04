package cn.com.nbport.zgt.demo.simulation.entity;

public class WorkInstruction {
    // 作业指令唯一标识ID
    private String id;
    // 作业指令类型
    private String instructionType;
    // 作业起始位置
    private String currentPosition;
    // 作业目标位置
    private String targetPosition;

    public WorkInstruction() {}

    public WorkInstruction(String id, String instructionType, String currentPosition, String targetPosition) {
        this.id = id;
        this.instructionType = instructionType;
        this.currentPosition = currentPosition;
        this.targetPosition = targetPosition;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getInstructionType() { return instructionType; }
    public void setInstructionType(String instructionType) { this.instructionType = instructionType; }

    public String getCurrentPosition() { return currentPosition; }
    public void setCurrentPosition(String currentPosition) { this.currentPosition = currentPosition; }

    public String getTargetPosition() { return targetPosition; }
    public void setTargetPosition(String targetPosition) { this.targetPosition = targetPosition; }

    @Override
    public String toString() {
        return "WI[" + id + ":" + currentPosition + "->" + targetPosition + "]";
    }
}
