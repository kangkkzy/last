package cn.com.nbport.zgt.demo.simulation.entity;

import java.util.List;

public class InputData {
    private List<String> qcList;
    private List<String> ascList;
    private List<String> truckList;

    // 地图定义数据
    private MapDefinition mapDefinition;

    public List<String> getQcList() { return qcList; }
    public void setQcList(List<String> qcList) { this.qcList = qcList; }

    public List<String> getAscList() { return ascList; }
    public void setAscList(List<String> ascList) { this.ascList = ascList; }

    public List<String> getTruckList() { return truckList; }
    public void setTruckList(List<String> truckList) { this.truckList = truckList; }

    // Getter/Setter
    public MapDefinition getMapDefinition() { return mapDefinition; }
    public void setMapDefinition(MapDefinition mapDefinition) { this.mapDefinition = mapDefinition; }
}