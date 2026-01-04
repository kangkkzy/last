package cn.com.nbport.zgt.demo.simulation.entity;
public class QC {
    // QC设备唯一标识ID 用于区分不同的QC设备
    private String id;
    // QC设备当前所在位置 格式遵循业务约定的区域/泊位编码
    private String currentPosition;
    // QC设备运行状态
    private QCStatusEnum status;
    // QC设备当前绑定的卡车编号 无关联卡车时该值为null
    private String currentTruckId;
    public QC() {
    }
    public QC(String id, String currentPosition) {
        this.id = id;
        this.currentPosition = currentPosition;
        this.status = QCStatusEnum.IDLE; // 默认状态为空闲
    }
    public QC(String id, QCStatusEnum status, String currentPosition) {
        this.id = id;
        this.status = status;
        this.currentPosition = currentPosition;
    }

    // 获取QC设备唯一标识ID
    public String getId() { return id; }
    // 设置QC设备唯一标识ID
    public void setId(String id) { this.id = id; }
    // 获取QC设备当前位置
    public String getCurrentPosition() { return currentPosition; }
    // 设置QC设备当前位置
    public void setCurrentPosition(String currentPosition) { this.currentPosition = currentPosition; }
    // 获取QC设备运行状态
    public QCStatusEnum getStatus() { return status; }
    // 设置QC设备运行状态
    public void setStatus(QCStatusEnum status) { this.status = status; }
    // 获取QC设备当前关联的卡车编号
    public String getCurrentTruckId() { return currentTruckId; }
    // 设置QC设备当前关联的卡车编号
    public void setCurrentTruckId(String currentTruckId) { this.currentTruckId = currentTruckId; }
}
