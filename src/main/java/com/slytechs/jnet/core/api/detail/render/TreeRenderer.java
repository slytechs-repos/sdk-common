package com.slytechs.jnet.core.api.detail.render;

import java.util.List;

import com.slytechs.jnet.core.api.detail.DataDetail;
import com.slytechs.jnet.core.api.detail.DetailNode;
import com.slytechs.jnet.core.api.detail.ExpertDetail;
import com.slytechs.jnet.core.api.detail.FieldDetail;
import com.slytechs.jnet.core.api.detail.HeaderDetail;
import com.slytechs.jnet.core.api.detail.SectionDetail;

/**
 * Renders detail to TreeNode for UI
 */
public class TreeRenderer {

	public List<TreeNode> render(List<DetailNode> nodes) {
		return nodes.stream()
				.map(this::renderNode)
				.toList();
	}

	public TreeNode render(DetailNode node) {
		return renderNode(node);
	}

	private TreeNode renderNode(DetailNode node) {
		return switch (node) {
		case HeaderDetail h -> {
			TreeNode tn = new TreeNode(h.name(), TreeNode.Type.HEADER);
			tn.setSummary(h.summary());
			tn.setProtocolId(h.protocolId());
			tn.setByteRange(h.offset(), h.length());
			tn.setFlags(h.flags());
			for (DetailNode child : h.children()) {
				tn.addChild(renderNode(child));
			}
			yield tn;
		}
		case FieldDetail f -> {
			TreeNode tn = new TreeNode(f.name(), TreeNode.Type.FIELD);
			tn.setDisplayValue(f.display());
			tn.setRawValue(f.value());
			tn.setBitRange(f.bitOffset(), f.bitLength());
			for (DetailNode child : f.children()) {
				tn.addChild(renderNode(child));
			}
			yield tn;
		}
		case SectionDetail s -> {
			TreeNode tn = new TreeNode(s.name(), TreeNode.Type.SECTION);
			tn.setExpandable(true);
			for (DetailNode child : s.children()) {
				tn.addChild(renderNode(child));
			}
			yield tn;
		}
		case DataDetail d -> {
			TreeNode tn = new TreeNode(d.name(), TreeNode.Type.DATA);
			tn.setData(d.data(), (int) d.offset(), (int) d.length());
			yield tn;
		}
		case ExpertDetail e -> {
			TreeNode tn = new TreeNode(e.message(), TreeNode.Type.EXPERT);
			tn.setExpertLevel(e.level());
			if (e.actionType() != null) {
				tn.setAction(e.actionType(), e.actionData());
			}
			yield tn;
		}
		};
	}
}