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
 * Resilience.java
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
package gtna.projects;

import gtna.communities.Roles;
import gtna.graph.Graph;
import gtna.graph.Node;
import gtna.graph.sorting.DegreeNodeSorter;
import gtna.graph.sorting.NodeSorter;
import gtna.graph.sorting.Roles2NodeSorter;
import gtna.graph.sorting.RolesNodeSorter;
import gtna.networks.Network;
import gtna.networks.model.ErdosRenyi;
import gtna.networks.model.placementmodels.NodeConnector;
import gtna.networks.model.placementmodels.Partitioner;
import gtna.networks.model.placementmodels.PlacementModel;
import gtna.networks.model.placementmodels.PlacementModelContainer;
import gtna.networks.model.placementmodels.connectors.UDGConnector;
import gtna.networks.model.placementmodels.models.CommunityPlacementModel;
import gtna.networks.model.placementmodels.partitioners.SimplePartitioner;
import gtna.transformation.Transformation;
import gtna.transformation.communities.CommunityColors;
import gtna.transformation.communities.CommunityDetectionLPA;
import gtna.transformation.communities.Roles2Generation;
import gtna.transformation.communities.RolesGeneration;

import java.util.Random;

/**
 * @author benni
 * 
 */
public class Resilience {

	public static void main(String[] args) {
		if (false) {
			Network nw = new ErdosRenyi(20, 5, false, null, null);
			Graph g = nw.generate();
			NodeSorter sorter = new DegreeNodeSorter(
					NodeSorter.NodeSorterMode.ASC);
			System.out.println(sorter.getKey() + ":");
			Random rand = new Random();
			Resilience.print(sorter.sort(g, rand), g);
			System.out.println();
			for (int i = 0; i < 30; i++) {
				System.out.println(Resilience.toString(sorter.sort(g, rand)));
			}
			return;
		}

		Network nw = Resilience.communityNew(2000);
		Transformation t_lpa = new CommunityDetectionLPA();
		Transformation t_cc = new CommunityColors();
		Transformation t_r = new RolesGeneration();
		Transformation t_r2 = new Roles2Generation(true);
		Graph g = nw.generate();
		g = t_lpa.transform(g);
		g = t_cc.transform(g);
		g = t_r.transform(g);
		g = t_r2.transform(g);

		Random rand = new Random();
		NodeSorter sorter = new Roles2NodeSorter(
				Roles2NodeSorter.HS_HB_HC_S_B_C);
		sorter = new RolesNodeSorter(RolesNodeSorter.GH_CH_PH_SC_P_UP_KN);

		System.out.println(sorter.getKey() + ":");
		Resilience.print(sorter.sort(g, rand), g);
		System.out.println();
		// for (int i = 0; i < 30; i++) {
		// System.out.println(Resilience.toString(sorter.sort(g, rand)));
		// }
	}

	private static Network communityNew(int nodes) {
		PlacementModel p1 = new CommunityPlacementModel(20, 20, 0.5, false);
		PlacementModel p2 = new CommunityPlacementModel(20, 20, 0.2, false);
		Partitioner partitioner = new SimplePartitioner();
		NodeConnector connector = new UDGConnector(1);
		return new PlacementModelContainer(nodes, nodes / 100, 40, 40, p1, p2,
				partitioner, connector, null, null);
	}

	private static void print(Node[] nodes, Graph g) {
		// Roles2 roles = (Roles2) g.getProperty("ROLES2_0");
		Roles roles = (Roles) g.getProperty("ROLES_0");
		for (int i = 0; i < nodes.length; i++) {
			if (i > 0
					&& roles.getRoleOfNode(nodes[i].getIndex()) == roles
							.getRoleOfNode(nodes[i - 1].getIndex())) {
				continue;
			}
			System.out.println("  " + nodes[i] + " @ "
					+ roles.getRoleOfNode(nodes[i].getIndex()));
			// System.out.println("  " + nodes[i]);
		}
	}

	private static String toString(Node[] nodes) {
		StringBuffer buff = new StringBuffer();
		for (Node n : nodes) {
			buff.append(n.getIndex() + " ");
		}
		return buff.toString();
	}

}
