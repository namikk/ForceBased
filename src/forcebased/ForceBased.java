/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package forcebased;

import de.uni_stuttgart.vis.data.Vector2D;
import de.uni_stuttgart.vis.data.GraphNode;
import de.uni_stuttgart.vis.data.Graph;
import de.uni_stuttgart.vis.data.Data;
import de.uni_stuttgart.vis.geom.AbstractGeometry;
import de.uni_stuttgart.vis.geom.Circle;
import de.uni_stuttgart.vis.geom.Line;
import java.awt.Color;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Namik
 */
public class ForceBased extends de.uni_stuttgart.vis.framework.InfoVisFramework {
    Collection<GraphNode> nodes;
    Graph graph;
    final double c1 = 10;
    final double c2 = 80;
    final double c3 = 100;
    final double c4 = 1;

    //calculate the force between non-neighbours according to the distance 
    //force = c3 / d^2
    public double calculateSecondaryForce(double distance) {
        double force = c3 / Math.pow(distance, 2);
        return force;
    }

    //calculate the force between neighbours according to the distance 
    //force=c1 ∗ log(distance/c2)
    public double calculatePrimaryForce(double distance) {
        double force = c1 * Math.log(distance / c2);
        return force;
    }

    // apply force to a vector
    public Vector2D applyForce(Vector2D vector, double force) {
        return new Vector2D(
                vector.getX() * force,
                vector.getY() * force);
    }

    //get the vector between two points
    public Vector2D calculateVector(Vector2D point1, Vector2D point2) {
        Vector2D resultingVector = new Vector2D(0, 0);
        resultingVector.setX(point2.getX() - point1.getX());
        resultingVector.setY(point2.getY() - point1.getY());
        return resultingVector;
    }

    //calculate the distance between two points
    public double calculateDistance(Vector2D point1, Vector2D point2) {
        double d;
        d = Math.sqrt(Math.pow( (point2.getX() - point1.getX()), 2 )
                + Math.pow( (point2.getY() - point1.getY()), 2) );
        return d;
    }
    
    //get the unit vector of the specified vector
    public Vector2D normalize(Vector2D vector) {
        Vector2D normalized = vector;
        double normalizationFactor
                = Math.sqrt(Math.pow(vector.getX(), 2) + Math.pow(vector.getY(), 2));
        normalized.setX(vector.getX() / normalizationFactor);
        normalized.setY(vector.getY() / normalizationFactor);
        return normalized;
    }

    //main algorithm
    public void adjustNodes() {
        //initialize map of displacement vectors
        Map<GraphNode, Vector2D> displacementMap = new HashMap<GraphNode, Vector2D>();

        //calculate the resulting displacement vector for each node
        for (GraphNode baseNode : nodes) {
            //baseNode = the node for which we are calculating the displacement vector
//            System.out.println("Calculating displacement vector for node: " + baseNode.getNodeID());

            //holds the final vector representing the effects of
            //both neighbours and non-neighbours on this node
            Vector2D finalVector = null;

            //iterate through all other vectors
            for (GraphNode otherNode : nodes) {
                if (otherNode == baseNode) {
                    continue;
                } else if (baseNode.getAdjacentNodes().contains(otherNode)) {
                    //is neighbour
                    
                    //calculate the displacement vector
                    Vector2D currentDisplacementVector
                            = calculateVector(baseNode.getPosition(), otherNode.getPosition());

                    //normalize it
                    currentDisplacementVector = normalize(currentDisplacementVector);

                    //calculate force
                    double force = calculatePrimaryForce(
                            calculateDistance(baseNode.getPosition(), otherNode.getPosition()));

                    // apply force
                    currentDisplacementVector = applyForce(currentDisplacementVector, force);

                    //combine with final vector
                    if (finalVector == null) {
                        finalVector = currentDisplacementVector;
                    }
                    else{
                        finalVector = addVectors(finalVector, currentDisplacementVector);
                    }
                } else {
                    //is non neighbour
                    
                    //calculate the displacement vector
                    Vector2D currentDisplacementVector
                            = calculateVector(baseNode.getPosition(), otherNode.getPosition());

                    //normalize it
                    currentDisplacementVector = normalize(currentDisplacementVector);
                    //make it repulsive
                    currentDisplacementVector = applyForce(currentDisplacementVector, -1.0);

                    //calculate force
                    double force = calculateSecondaryForce(
                            calculateDistance(baseNode.getPosition(), otherNode.getPosition()));

                    // apply force
                    currentDisplacementVector = applyForce(currentDisplacementVector, force);

                    //combine with final vector
                    if (finalVector == null) {
                        finalVector = currentDisplacementVector;
                    }
                    else{
                        finalVector = addVectors(finalVector, currentDisplacementVector);
                    }
                }
            }
            //store the displacement vector for this node, and move on to the next
            displacementMap.put(baseNode, finalVector);

        }

        //move teh nodes
        for (GraphNode node : nodes) {
            if (displacementMap.containsKey(node)) {
                //move the node
                Vector2D displacementVector = displacementMap.get(node);
                moveNode(node, displacementVector);
            }
        }

    }

    //adds the two specified vectors
    public Vector2D addVectors(Vector2D v1, Vector2D v2){
        return new Vector2D(v1.getX() + v2.getX(), v1.getY() + v2.getY());
    }
    
    //moves the specified node using the specified vector
    public void moveNode(GraphNode n, Vector2D vector) {
        Vector2D oldPosition = n.getPosition();
        Vector2D newPosition = addVectors(oldPosition, vector);
        n.setPosition(newPosition);
    }

    //init stuff
    public void initialize() {
        prepareData();
    }

    //setup the data
    private void prepareData() {
        graph = Data.getGraph();
        nodes = graph.getNodes();
    }

    @Override
    public List<AbstractGeometry> mapData() {
        initialize();
        List<AbstractGeometry> result = new LinkedList<AbstractGeometry>();

        //first draw the initial stuff
        int d = 10;
        int r = d / 2;

        for (GraphNode n : nodes) {
//            System.out.println("node" + n.getNodeID() + ": x=" + n.getPosition().getX() + " y=" + n.getPosition().getY());
            Collection<GraphNode> adjacent = n.getAdjacentNodes();

            for (GraphNode m : adjacent) {
//                System.out.println(" adjacent:" + m.getNodeID());
                Line line = new Line(n.getPosition().getX() + r, n.getPosition().getY() + r, m.getPosition().getX() + r, m.getPosition().getY() + r);
                line.setColor(Color.BLACK);
                result.add(line);
            }
            Circle circle = new Circle(n.getPosition().getX(), n.getPosition().getY(), d);
            circle.setColor(Color.PINK);//PINK because reasons.
            circle.setText(Integer.toString(n.getNodeID()));
            result.add(circle);
            renderCanvas(result);
        }
        
        //run the algorithm and redraw along the way
        for (int i = 0; i < 500; i++) {
            result.clear();
            //then draw the updated stuff
            try {
                Thread.sleep(10);
                //recalculate
                adjustNodes();

                //redraw
                for (GraphNode node : nodes) {
//                    System.out.println("node" + n.getNodeID() + ": x=" + n.getPosition().getX() + " y=" + n.getPosition().getY());
                    Collection<GraphNode> adjacent = node.getAdjacentNodes();

                    for (GraphNode m : adjacent) {
//                        System.out.println(" adjacent:" + m.getNodeID());
                        Line line = new Line(node.getPosition().getX() + r, node.getPosition().getY() + r, m.getPosition().getX() + r, m.getPosition().getY() + r);
                        line.setColor(Color.BLACK);
                        result.add(line);
                    }
                    Circle circle = new Circle(node.getPosition().getX(), node.getPosition().getY(), d);
                    circle.setColor(Color.PINK);//PINK because reasons.
                    circle.setText(Integer.toString(node.getNodeID()));
                    result.add(circle);
                    renderCanvas(result);
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(ForceBased.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

        return result;
    }

    public static void main(String[] args) {
        new ForceBased();
    }
    
}