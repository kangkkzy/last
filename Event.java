package cn.com.nbport.zgt.demo.simulation;

public class Event implements Comparable<Event> {
    // 全局计数器
    private static long cnt = 1;

    // 通过 set 方法修改
    private long id;
    private long eventTime;
    private EventEnum eventEnum;
    private String position;
    private String truckId;
    private String ascId;
    private String qcId;

    // 增加无参构造函数
    public Event() {
        this.id = cnt++; // 自动生成ID
    }

    // 原有的全参构造函数
    public Event(long eventTime, EventEnum eventEnum, String position, String truckId, String ascId, String qcId) {
        this.id = cnt++;
        this.eventTime = eventTime;
        this.eventEnum = eventEnum;
        this.position = position;
        this.truckId = truckId;
        this.ascId = ascId;
        this.qcId = qcId;
    }
    public void setEventTime(long eventTime) { this.eventTime = eventTime; }
    public void setEventEnum(EventEnum eventEnum) { this.eventEnum = eventEnum; }
    public void setPosition(String position) { this.position = position; }
    public void setTruckId(String truckId) { this.truckId = truckId; }
    public void setAscId(String ascId) { this.ascId = ascId; }
    public void setQcId(String qcId) { this.qcId = qcId; }
    public long getId() { return id; }
    public long getEventTime() { return eventTime; }
    public EventEnum getEventEnum() { return eventEnum; }
    public String getPosition() { return position; }
    public String getTruckId() { return truckId; }
    public String getAscId() { return ascId; }
    public String getQcId() { return qcId; }

    @Override
    public int compareTo(Event other) {
        int timeComparison = Long.compare(this.eventTime, other.eventTime);
        if (timeComparison != 0) {
            return timeComparison;
        }
        return Long.compare(this.id, other.id);
    }

    @Override
    public String toString() {
        return String.format("[T:%d] %s at %s (Trk:%s, ASC:%s, QC:%s)",
                eventTime, eventEnum, position,
                truckId == null ? "-" : truckId,
                ascId == null ? "-" : ascId,
                qcId == null ? "-" : qcId
        );
    }
}