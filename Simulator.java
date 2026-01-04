package cn.com.nbport.zgt.demo.simulation;

import cn.com.nbport.zgt.demo.simulation.entity.*;
import cn.com.nbport.zgt.demo.simulation.map.LogicalLocation;
import java.util.*;

public class Simulator {
    public static void main(String[] args) {
        InputData inputData = new InputData();
        inputData.setQcList(Arrays.asList("QC-01:Bay-01", "QC-02:Bay-02"));
        inputData.setAscList(Arrays.asList("ASC-01:Block-A", "ASC-02:Block-B"));
        inputData.setTruckList(Arrays.asList("T-01:Block-A", "T-02:Block-B", "T-03:Block-A", "T-04:Gate", "T-05:Gate", "T-06:Gate"));

        MapDefinition mapDef = buildDefaultMap();
        inputData.setMapDefinition(mapDef);

        SimulationInitializer initializer = new SimulationInitializer();
        SimulationContext context = initializer.initialize(inputData);

        DecisionMaker dm = new DecisionMaker();
        DataGenerator dg = new DataGenerator();
        SimulationRepository repo = new SimulationRepository();
        repo.init();

        System.out.println("Starting Rasterized Simulation (Atomic Locking)...");

        while (!context.getEventList().isEmpty()) {
            Event currentEvent = context.getEventList().poll();
            context.setSimulationClock(currentEvent.getEventTime());
            repo.saveEvent(currentEvent);
            EventHandler.handleEvent(context, currentEvent, dm, dg);
            if (currentEvent.getEventTime() > 8000) break;
        }

        repo.commitToJsonFile();
        System.out.println("Simulation Finished.");
    }

    private static MapDefinition buildDefaultMap() {
        MapDefinition map = new MapDefinition();
        List<MapDefinition.NodeDef> nodes = new ArrayList<>();
        List<MapDefinition.LinkDef> links = new ArrayList<>();

        nodes.add(createNode("Gate", LogicalLocation.TYPE_GATE));
        nodes.add(createNode("Hub_Main", LogicalLocation.TYPE_ROAD));
        nodes.add(createNode("Block-A", LogicalLocation.TYPE_YARD));
        nodes.add(createNode("Block-B", LogicalLocation.TYPE_YARD));
        nodes.add(createNode("Bay-01", LogicalLocation.TYPE_BERTH));
        nodes.add(createNode("Bay-02", LogicalLocation.TYPE_BERTH));

        // 恢复紧凑型地图 (不扩容)
        links.add(createLink("Road_In", "Gate", "Hub_Main", 100.0));
        links.add(createLink("Road_Out", "Hub_Main", "Gate", 100.0));

        links.add(createLink("Lane_A_In", "Hub_Main", "Block-A", 60.0));
        links.add(createLink("Lane_A_Out", "Block-A", "Hub_Main", 60.0));
        links.add(createLink("Lane_B_In", "Hub_Main", "Block-B", 60.0));
        links.add(createLink("Lane_B_Out", "Block-B", "Hub_Main", 60.0));

        links.add(createLink("Lane_Berth_In", "Hub_Main", "Bay-01", 80.0));
        links.add(createLink("Lane_Berth_Out_1", "Bay-01", "Hub_Main", 80.0));
        links.add(createLink("Lane_Berth_Out_2", "Bay-02", "Hub_Main", 80.0));
        links.add(createLink("Lane_Bay_Conn", "Bay-01", "Bay-02", 40.0));

        map.setNodes(nodes);
        map.setLinks(links);
        return map;
    }

    private static MapDefinition.NodeDef createNode(String id, String type) {
        MapDefinition.NodeDef node = new MapDefinition.NodeDef();
        node.setId(id);
        node.setType(type);
        return node;
    }
    private static MapDefinition.LinkDef createLink(String id, String from, String to, double dist) {
        MapDefinition.LinkDef link = new MapDefinition.LinkDef();
        link.setId(id);
        link.setFromNode(from);
        link.setToNode(to);
        link.setDistance(dist);
        return link;
    }
}