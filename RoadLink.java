package cn.com.nbport.zgt.demo.simulation.map;
// 封装地图中道路的起点与终点信息
public class RoadLink {
    // 道路起点节点ID
    private String from;
    // 道路终点节点ID
    private String to;
    // 未来可以扩展：private boolean oneWay; // 单行道

    // 构造函数
    public RoadLink() {}
    public RoadLink(String from, String to) {
        this.from = from;
        this.to = to;
    }
    // 获取道路起点节点ID
    public String getFrom() { return from; }
    // 设置道路起点节点ID
    public void setFrom(String from) { this.from = from; }
    // 获取道路终点节点ID
    public String getTo() { return to; }
    // 设置道路终点节点ID
    public void setTo(String to) { this.to = to; }
}
