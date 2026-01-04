package cn.com.nbport.zgt.demo.simulation.map;
// 位置的实体
public class LogicalLocation {
    // 位置类型：闸口
    public static final String TYPE_GATE = "GATE";
    // 位置类型：堆场
    public static final String TYPE_YARD = "YARD";
    // 位置类型：泊位
    public static final String TYPE_BERTH = "BERTH";
    // 位置类型：道路
    public static final String TYPE_ROAD = "ROAD";
    // 位置名称
    private String name;
    // 位置类型
    private String type;
    public LogicalLocation(String name, String type) {
        this.name = name;
        this.type = type;
    }
    // 获取位置名称
    public String getName() { return name; }
    // 获取位置类型
    public String getType() { return type; }
}