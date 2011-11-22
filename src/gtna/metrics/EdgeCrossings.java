/* ===========================================================
 * GTNA : Graph-Theoretic Network Analyzer
 * ===========================================================
 *
 * (C) Copyright 2009-2011, by Benjamin Schiller (P2P, TU Darmstadt)
 * and Contributors
 *
 * Project Info:  http://www.p2p.tu-darmstadt.de/research/gtna/
 *
 * GTNA is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GTNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * ---------------------------------------
 * EdgeCrossings.java
 * ---------------------------------------
 * (C) Copyright 2009-2011, by Benjamin Schiller (P2P, TU Darmstadt)
 * and Contributors 
 *
 * Original Author: Nico;
 * Contributors:    -;
 *
 * Changes since 2011-05-17
 * ---------------------------------------
 *
 */
package gtna.metrics;

import gtna.data.Value;
import gtna.graph.Edge;
import gtna.graph.Graph;
import gtna.graph.Node;
import gtna.id.DIdentifierSpace;
import gtna.id.IdentifierSpace;
import gtna.id.md.MDIdentifierSpaceSimple;
import gtna.id.plane.PlaneIdentifierSpaceSimple;
import gtna.id.ring.RingIdentifierSpace;
import gtna.id.ring.RingPartition;
import gtna.io.DataWriter;
import gtna.networks.Network;
import gtna.util.Distribution;
import gtna.util.Timer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * @author Nico
 * 
 */
public class EdgeCrossings extends MetricImpl implements Metric {
	private Timer runtime;
	private double[] cd;
	private int maxCrossingNumber;
	private HashSet<String> handledEdges;
	private Distribution crossingDistribution;

	public EdgeCrossings() {
		super("EC");
	}

	public void computeData(Graph graph, Network nw, HashMap<String, Metric> m) {
		this.runtime = new Timer();

		Edge[] edges = graph.generateEdges();

		DIdentifierSpace idSpace = (DIdentifierSpace) graph.getProperty("ID_SPACE_0");
		int result = calculateCrossings(edges, idSpace);

		double[] finalCD = new double[maxCrossingNumber];
		for (int i = 0; i < maxCrossingNumber; i++) {
			finalCD[i] = cd[i] / result;
		}
		this.crossingDistribution = new Distribution(finalCD);

		this.runtime.end();
	}

	public int calculateCrossings(Edge[] edges, IdentifierSpace idSpace) {
		int result = 0;
		cd = new double[edges.length];
		maxCrossingNumber = 0;
		this.runtime = new Timer();

		if (idSpace instanceof RingIdentifierSpace) {
			result = calculateRingCrossings(edges, idSpace);
		} else {
			handledEdges = new HashSet<String>();
			for (int outerCounter = 0; outerCounter < edges.length; outerCounter++) {
				int innerResult = 0;
				for (int innerCounter = outerCounter + 1; innerCounter < edges.length; innerCounter++) {
					if (hasCrossing(edges[innerCounter], edges[outerCounter], idSpace)) {
						innerResult++;
					}
				}
				cd[innerResult]++;
				maxCrossingNumber = Math.max(innerResult, maxCrossingNumber);
				result += innerResult;
			}
		}
		this.runtime.end();
		System.out.println("Computed crossings with " + edges.length + " edges in " + runtime.getMsec() + " msec");
		return result;
	}

	private int calculateRingCrossings(Edge[] edges, IdentifierSpace idSpace) {
		/*
		 * The following algorithm is an implementation of Six/Tollis work
		 */
		int i = 0;
		int numCross = 0;

		RingIdentifierSpace ridSpace = (RingIdentifierSpace) idSpace;
		RingPartition[] partitions = (RingPartition[]) ridSpace.getPartitions();

		Arrays.sort(edges, new EdgeComparator(partitions));
		ArrayList<RingEdge> ringEdges = new ArrayList<RingEdge>();
		TreeSet<Double> partPositions = new TreeSet<Double>();

		for (Edge sE : edges) {
			partPositions.add(getPositionRing(sE.getSrc(), ridSpace));
		}

		RingEdge tempRingEdge, lastEdge;
		lastEdge = null;
		for (Edge sE : edges) {
			double srcPos = Math.min(getPositionRing(sE.getSrc(), ridSpace), getPositionRing(sE.getDst(), ridSpace));
			double dstPos = Math.max(getPositionRing(sE.getSrc(), ridSpace), getPositionRing(sE.getDst(), ridSpace));
			tempRingEdge = new RingEdge(srcPos, dstPos, sE.getSrc(), sE.getDst());
			if (!tempRingEdge.equals(lastEdge)) {
				ringEdges.add(tempRingEdge);
			}
			lastEdge = tempRingEdge;
		}

		TreeMap<Double, ArrayList<RingEdge>> startNode = new TreeMap<Double, ArrayList<RingEdge>>();
		TreeMap<Double, ArrayList<RingEdge>> targetNode = new TreeMap<Double, ArrayList<RingEdge>>();
		ArrayList<RingEdge> openEdges = new ArrayList<RingEdge>();

		Double[] posList = new Double[partitions.length];
		for (RingPartition rP : partitions) {
			posList[i] = rP.getStart().getPosition();
			startNode.put(posList[i], new ArrayList<RingEdge>());
			targetNode.put(posList[i], new ArrayList<RingEdge>());
			i++;
		}
		Arrays.sort(posList);

		ArrayList<RingEdge> tempList;
		for (RingEdge sE : ringEdges) {
			tempList = startNode.get(sE.src);
			tempList.add(sE);
			startNode.put(sE.src, tempList);

			tempList = targetNode.get(sE.dst);
			tempList.add(sE);
			targetNode.put(sE.dst, tempList);
		}

		for (i = 0; i < partitions.length; i++) {
			openEdges.removeAll(targetNode.get(posList[i]));
			for (RingEdge sE : targetNode.get(posList[i])) {
				for (RingEdge sOE : openEdges) {
					if (sOE.src > sE.src) {
						numCross++;
					}
				}
			}
			openEdges.addAll(startNode.get(posList[i]));
		}

		return numCross;
	}

	public int calculateCrossings(Graph g, Node n, IdentifierSpace idSpace) {
		int numCross = 0;
		Edge[] nodeEdges = n.generateAllEdges();
		Edge[] graphEdges = g.generateEdges();
		handledEdges = new HashSet<String>();
		for (Edge x : nodeEdges) {
			for (Edge y : graphEdges) {
				if (hasCrossing(x, y, idSpace))
					numCross++;
			}
		}
		return numCross;
	}

	public int calculateCrossings(Node n, Node m, IdentifierSpace idSpace) {
		int numCross = 0;
		Edge[] nEdges = n.generateAllEdges();
		Edge[] mEdges = m.generateAllEdges();
		handledEdges = new HashSet<String>();
		for (Edge nEdge : nEdges) {
			for (Edge mEdge : mEdges) {
				if (hasCrossing(nEdge, mEdge, idSpace))
					numCross++;
			}
		}
		return numCross;
	}

	private boolean hasCrossing(Edge x, Edge y, IdentifierSpace idSpace) {
		/*
		 * There cannot be a crossing between only one edge
		 */
		if (x.equals(y))
			return false;
		if ((x.getSrc() == y.getSrc()) || (x.getSrc() == y.getDst()) || (x.getDst() == y.getSrc())
				|| (x.getDst() == y.getDst()))
			return false;
		if (idSpace instanceof PlaneIdentifierSpaceSimple) {
			return hasCrossingPlane(x, y, (PlaneIdentifierSpaceSimple) idSpace);
		} else if (idSpace instanceof RingIdentifierSpace) {
			return hasCrossingRing(x, y, (RingIdentifierSpace) idSpace);
		} else if (idSpace instanceof MDIdentifierSpaceSimple) {
			int dim = ((MDIdentifierSpaceSimple) idSpace).getDimensions();
			if (dim == 2) {
				return hasCrossingMD(x, y, (MDIdentifierSpaceSimple) idSpace);
			} else {
				throw new RuntimeException("Cannot calculate crossings in " + idSpace.getClass() + " with " + dim
						+ " dimensions");
			}
		} else {
			throw new RuntimeException("Cannot calculate crossings in " + idSpace.getClass());
		}
	}

	/**
	 * @param x
	 * @param y
	 * @param idSpace
	 * @return
	 */
	private boolean hasCrossingMD(Edge x, Edge y, MDIdentifierSpaceSimple idSpace) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * @param x
	 * @param y
	 * @param idSpace
	 * @return
	 */
	private boolean hasCrossingRing(Edge x, Edge y, RingIdentifierSpace idSpace) {
		double xStart = Math.min(getPositionRing(x.getSrc(), idSpace), getPositionRing(x.getDst(), idSpace));
		double xEnd = Math.max(getPositionRing(x.getSrc(), idSpace), getPositionRing(x.getDst(), idSpace));
		double yStart = Math.min(getPositionRing(y.getSrc(), idSpace), getPositionRing(y.getDst(), idSpace));
		double yEnd = Math.max(getPositionRing(y.getSrc(), idSpace), getPositionRing(y.getDst(), idSpace));

		String xString = xStart + "-" + xEnd;
		String yString = yStart + "-" + yEnd;
		String edgeString;
		if (xStart < yStart)
			edgeString = xString + "|" + yString;
		else
			edgeString = yString + "|" + xString;

		/*
		 * Have we already handled this edge?
		 */
		if (handledEdges.contains(edgeString)) {
			return false;
		}
		handledEdges.add(edgeString);

		if ((xStart < yStart && xEnd > yEnd) || (yStart < xStart && yEnd > xEnd) || (yStart > xEnd || xStart > yEnd)) {
			// System.out.println( "No crossing between " + edgeString );
			return false;
		}
		if ((xStart < yStart && xEnd < yEnd) || (xStart > yStart && xEnd > yEnd)) {
			// System.out.println("Got a crossing between " + edgeString);
			return true;
		}

		System.err.println("Unknown case " + edgeString);
		return false;
	}

	protected double getPositionRing(int i, RingIdentifierSpace idSpace) {
		return ((RingPartition) (idSpace.getPartitions())[i]).getStart().getPosition();
	}

	/**
	 * @param x
	 * @param y
	 * @param idSpace
	 * @return
	 */
	private boolean hasCrossingPlane(Edge x, Edge y, PlaneIdentifierSpaceSimple idSpace) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean writeData(String folder) {
		boolean success = true;
		success &= DataWriter.writeWithIndex(this.crossingDistribution.getDistribution(), "EC_DISTRIBUTION", folder);
		success &= DataWriter.writeWithIndex(this.crossingDistribution.getCdf(), "EC_DISTRIBUTION_CDF", folder);
		return success;
	}

	@Override
	public Value[] getValues() {
		Value ecAVG = new Value("EC_AVG", this.crossingDistribution.getAverage());
		return new Value[] { ecAVG };
	}

	private class RingEdge {
		double src, dst;
		int graphSrc, graphDst;

		public RingEdge(double src, double dst, int graphSrc, int graphDst) {
			this.src = src;
			this.dst = dst;
			this.graphSrc = graphSrc;
			this.graphDst = graphDst;
		}

		public boolean equals(RingEdge r) {
			if (r == null)
				return false;
			return (r.src == this.src) && (r.dst == this.dst);
		}

		public String toString() {
			return this.src + "->" + this.dst;
		}
	}
}