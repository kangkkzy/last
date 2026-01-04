package cn.com.nbport.zgt.demo.simulation.entity;
import java.util.List;
// 用于接收 JSON 输入的地图骨架
public class MapDefinition {
    private List<NodeDef> nodes;
    private List<LinkDef> links;

    public List<NodeDef> getNodes() { return nodes; }
    public void setNodes(List<NodeDef> nodes) { this.nodes = nodes; }

    public List<LinkDef> getLinks() { return links; }
    public void setLinks(List<LinkDef> links) { this.links = links; }

    // 内部静态类

    public static class NodeDef {
        private String id;
        private String type; // GATE, YARD, BERTH, ROAD

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }

    public static class LinkDef {
        private String id;       // 道路ID，例如 "Lane_A_In"
        private String fromNode; // 起点ID
        private String toNode;   // 终点ID
        private double distance; // 物理长度

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getFromNode() { return fromNode; }
        public void setFromNode(String fromNode) { this.fromNode = fromNode; }
        public String getToNode() { return toNode; }
        public void setToNode(String toNode) { this.toNode = toNode; }
        public double getDistance() { return distance; }
        public void setDistance(double distance) { this.distance = distance; }
    }
}