package org.genmapp.golayout.partition;

import cytoscape.view.NetworkTreeNode;

public class GOLayoutNetworkTreeNode extends NetworkTreeNode {	
	private String network_uid;

	public GOLayoutNetworkTreeNode(Object userobj, String id) {
		super(userobj.toString(), id);
		network_uid = id;
	}

    @Override
	protected void setNetworkID(String id) {
		network_uid = id;
	}

    @Override
	protected String getNetworkID() {
		return network_uid;
	}
}
