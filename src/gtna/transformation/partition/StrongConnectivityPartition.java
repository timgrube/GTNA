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
 * StrongConnectivityPartition.java
 * ---------------------------------------
 * (C) Copyright 2009-2011, by Benjamin Schiller (P2P, TU Darmstadt)
 * and Contributors 
 *
 * Original Author: benni;
 * Contributors:    -;
 *
 * Changes since 2011-05-17
 * ---------------------------------------
 *
 */
package gtna.transformation.partition;

import gtna.graph.Graph;
import gtna.graph.partition.Partition;
import gtna.transformation.Transformation;
import gtna.util.Util;

import java.util.ArrayList;
import java.util.Stack;

/**
 * @author benni uses the algorithm of Tarjan to compute the strongly connected
 *         components of the given graph http://en.wikipedia.org/wiki/Tarjan
 *         's_strongly_connected_components_algorithm
 */
public class StrongConnectivityPartition extends Transformation {

	public StrongConnectivityPartition() {
		super("STRONG_CONNECTIVITY_PARTITION");
	}

	private int index;

	@Override
	public Graph transform(Graph g) {
		ArrayList<ArrayList<Integer>> components = new ArrayList<ArrayList<Integer>>();
		int[] index = Util.initIntArray(g.getNodes().length, -1);
		int[] lowlink = new int[g.getNodes().length];
		Stack<Integer> S = new Stack<Integer>();
		this.index = 0;

		for (int v = 0; v < g.getNodes().length; v++) {
			if (index[v] == -1) {
				this.strongConnect(v, g, index, lowlink, S, components);
			}
		}

		Partition p = new Partition(components);
		g.addProperty(g.getNextKey("STRONG_CONNECTIVITY_PARTITION"), p);
		g.addProperty(g.getNextKey("PARTITION"), p);

		return g;
	}

	private void strongConnect(int v, Graph g, int[] index, int[] lowlink,
			Stack<Integer> S, ArrayList<ArrayList<Integer>> components) {
		// Set the depth index for v to the smallest unused index
		index[v] = this.index;
		lowlink[v] = this.index;
		this.index++;
		S.push(v);

		// Consider successors of v
		for (int w : g.getNode(v).getOutgoingEdges()) {
			if (index[w] == -1) {
				// Successor w has not yet been visited; recurse on it
				this.strongConnect(w, g, index, lowlink, S, components);
				lowlink[v] = Math.min(lowlink[v], lowlink[w]);
			} else if (S.contains(w)) {
				// Successor w is in stack S and hence in the current SCC
				lowlink[v] = Math.min(lowlink[v], index[w]);
			}
		}

		// If v is a root node, pop the stack and generate an SCC
		if (lowlink[v] == index[v]) {
			ArrayList<Integer> newComponent = new ArrayList<Integer>();
			int w = -1;
			while (w != v) {
				w = S.pop();
				newComponent.add(w);
			}
			components.add(newComponent);
		}
	}

	@Override
	public boolean applicable(Graph g) {
		return true;
	}

}
