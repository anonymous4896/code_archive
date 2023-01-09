package hunter.Utils;

import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.layout.mxIGraphLayout;
import com.mxgraph.util.mxCellRenderer;
import hunter.Constant;
import hunter.Track.GraphEdge;
import org.jgrapht.Graph;
import org.jgrapht.ext.JGraphXAdapter;
import org.jgrapht.graph.DirectedPseudograph;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.dot.DOTExporter;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.pdg.HashMutablePDG;
import soot.toolkits.graph.pdg.PDGNode;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

public class GraphHelper {
    public static void PrintGoodCFG(String method, String filename, String outputPath) {
        SootMethod sootMethod = Scene.v().getMethod(method);
        BriefUnitGraph briefUnitGraph = new BriefUnitGraph(sootMethod.retrieveActiveBody());

        Graph<Unit, String> resultJimpleGraph4VisualTemp = new DirectedPseudograph<>(String.class);

        for (Unit uh : briefUnitGraph.getHeads()) {
//            if (uh instanceof IdentityStmt
//                    && ((IdentityStmt) uh).getRightOp() instanceof CaughtExceptionRef) {
//                continue;
//            }
            _dfs(resultJimpleGraph4VisualTemp, briefUnitGraph, uh);
        }

        JGraphXAdapter<Unit, String> graphAdapter = new JGraphXAdapter<>(resultJimpleGraph4VisualTemp);
        mxIGraphLayout layout = new mxHierarchicalLayout(graphAdapter);
        layout.execute(graphAdapter.getDefaultParent());

        try {
            BufferedImage image = mxCellRenderer.createBufferedImage(graphAdapter, null, 1, Color.WHITE, true, null);
            File dir = new File(outputPath);
            if (!dir.exists()){
                dir.mkdir();
            }
            if (image != null) {
                File imgfile = new File(Paths.get(outputPath, filename + ".png").toString());

                imgfile.createNewFile();
                ImageIO.write(image, "PNG", imgfile);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void PrintPDG(String method, String filename) {
        SootMethod sootMethod = Scene.v().getMethod(method);
        BriefUnitGraph briefUnitGraph = new BriefUnitGraph(sootMethod.retrieveActiveBody());
        HashMutablePDG hmPDG = new HashMutablePDG(briefUnitGraph);

        PDGNode a = hmPDG.getHeads().get(0);
        System.out.println(hmPDG.toString());

//        Graph<Unit, String> resultJimpleGraph4VisualTemp = new DirectedPseudograph<>(String.class);

//        for (Unit uh : briefUnitGraph.getHeads()) {
////            if (uh instanceof IdentityStmt
////                    && ((IdentityStmt) uh).getRightOp() instanceof CaughtExceptionRef) {
////                continue;
////            }
//            _dfs(resultJimpleGraph4VisualTemp, briefUnitGraph, uh);
//        }
//
//        JGraphXAdapter<Unit, String> graphAdapter = new JGraphXAdapter<>(resultJimpleGraph4VisualTemp);
//        mxIGraphLayout layout = new mxHierarchicalLayout(graphAdapter);
//        layout.execute(graphAdapter.getDefaultParent());
//
//        try {
//            BufferedImage image = mxCellRenderer.createBufferedImage(graphAdapter, null, 1, Color.WHITE, true, null);
//            File dir = new File(Constant.OUTPUT_PATH);
//            if (!dir.exists())
//                dir.mkdir();
//            if (image != null) {
//                File imgfile = new File(Constant.OUTPUT_PATH + filename + ".png");
//
//                imgfile.createNewFile();
//                ImageIO.write(image, "PNG", imgfile);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    static void _dfs(Graph<Unit, String> resultJimpleGraph4VisualTemp, BriefUnitGraph method_cfg, Unit now) {
        if (!resultJimpleGraph4VisualTemp.containsVertex(now))
            resultJimpleGraph4VisualTemp.addVertex(now);
        for (Unit next : method_cfg.getSuccsOf(now)) {
            if (!resultJimpleGraph4VisualTemp.containsVertex(next))
                resultJimpleGraph4VisualTemp.addVertex(next);
            if (!resultJimpleGraph4VisualTemp.containsEdge(now, next)) {
                resultJimpleGraph4VisualTemp.addEdge(now, next, "" + next.hashCode() + now.hashCode());
                _dfs(resultJimpleGraph4VisualTemp, method_cfg, next);
            }
        }
    }


    public static void printGraphDot(String apkName, String outputPath, Graph<Unit, GraphEdge> resultJimpleGraph4Visual) {
        DOTExporter<Unit, GraphEdge> exporter = new DOTExporter<>(v -> String.valueOf(v.hashCode()));

        // v.toString().replace("<", "_").replace(">", "_")
        // .replace("[", "_").replace("]", "_")
        // .replace("{", "_").replace("}", "_")
        // .replace("(", "_").replace(")", "_")
        // .replace("$", "_").replace(" ", "_")
        // .replace(":", "_").replace("/", "_")
        // .replace(",", "_").replace("\"", "_")
        // .replace(".", "_").replace("=", "_")
        // .replace("@", "_").replace("-", "_")
        // .replace(";", "_").replace("+","_")
        // .replace("*","_").replace("!","_")
        // .replace("#","_").replace("%","_")
        // .replace("&","_").replace("|","_")
        // .replace("?","_")+
        exporter.setVertexAttributeProvider((v) -> {
            Map<String, Attribute> map = new LinkedHashMap<>();
            map.put("label", DefaultAttribute.createAttribute(v.toString()));
            return map;
        });

        exporter.setEdgeAttributeProvider((v) -> {
            Map<String, Attribute> map = new LinkedHashMap<>();
            map.put("label", DefaultAttribute.createAttribute(v.toString()));
            return map;
        });

        try {
            FileWriter fileWriter = new FileWriter(Paths.get(outputPath, apkName + ".dot").toString());
            exporter.exportGraph(resultJimpleGraph4Visual, fileWriter);
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void printGraphPNG(String apkName, String outputPath, Graph<Unit, GraphEdge> resultJimpleGraph4Visual) {
        JGraphXAdapter<Unit, GraphEdge> graphAdapter = new JGraphXAdapter<>(resultJimpleGraph4Visual);
        mxIGraphLayout layout = new mxHierarchicalLayout(graphAdapter);
        layout.execute(graphAdapter.getDefaultParent());

        try {
            BufferedImage image = mxCellRenderer.createBufferedImage(graphAdapter, null, 1, Color.WHITE, true, null);
            File dir = new File(outputPath);
            if (!dir.exists())
                dir.mkdir();
            if (image != null) {
                File imgfile = new File(Paths.get(outputPath, apkName + ".png").toString());

                imgfile.createNewFile();
                ImageIO.write(image, "PNG", imgfile);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
