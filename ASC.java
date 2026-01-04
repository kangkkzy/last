package cn.com.nbport.zgt.demo.simulation.entity;

public class ASC {
    // 设备唯一ID  用于区分不同ASC设备
    private String id;
    // 设备当前所在位置 格式为业务约定的区域/仓位编码
    private String currentPosition;
    // 设备运行状态（空闲/工作/故障等
    private ASCStatusEnum status;
    // 设备当前绑定的卡车编号 无绑定时为null
    private String currentTruckId;

    // 无参构造方法
    public ASC() {
    }
    public ASC(String id, String currentPosition) {
        this.id = id;
        this.currentPosition = currentPosition;
        this.status = ASCStatusEnum.IDLE; // 默认空闲
    }
    public ASC(String id, ASCStatusEnum status, String currentPosition) {
        this.id = id;
        this.status = status;
        this.currentPosition = currentPosition;
    }

    // 获取设备ID
    public String getId() {
        return id;
    }

    // 设置设备ID
    public void setId(String id) {
        this.id = id;
    }

    // 获取设备当前位置
    public String getCurrentPosition() {
        return currentPosition;
    }

    // 设置设备当前位置
    public void setCurrentPosition(String currentPosition) {
        this.currentPosition = currentPosition;
    }

    // 获取设备运行状态
    public ASCStatusEnum getStatus() {
        return status;
    }

    // 设置设备运行状态
    public void setStatus(ASCStatusEnum status) {
        this.status = status;
    }

    // 获取当前关联卡车编号
    public String getCurrentTruckId() {
        return currentTruckId;
    }

    // 设置当前关联卡车编号
    public void setCurrentTruckId(String currentTruckId) {
        this.currentTruckId = currentTruckId;
    }
}