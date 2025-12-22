package com.slytechs.jnet.core.api.detail;

import java.util.List;

import com.slytechs.jnet.core.api.detail.render.TextRenderer;

/**
 * Interface for objects that can provide detail information.
 */
@FunctionalInterface
public interface Detailable {

	/**
	 * Build detail representation using the provided builder.
	 * 
	 * @param detail the builder to use
	 */
	void buildDetail(DetailBuilder detail);

	/**
	 * Get the detail as an immutable tree. Default implementation creates a
	 * builder, calls buildDetail, and returns result.
	 */
	default List<DetailNode> getDetail() {
		DetailBuilder builder = new DetailBuilder();
		buildDetail(builder);
		return builder.build();
	}

	default String toDetailString() {
		return new TextRenderer().render(getDetail());
	}
}