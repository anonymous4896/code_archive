package hunter.Track;

import org.jgrapht.graph.DefaultEdge;
import soot.Unit;


public class GraphEdge extends DefaultEdge {

    private String type;

    public GraphEdge(String type){
        this.type = type;
    }

    public Unit getSourceVertex() {
        return (Unit) getSource();
    }

    public Unit getTargetVertex(){
        return (Unit) getTarget();
    }

    @Override
    public String toString()
    {
        return type;
    }

}
