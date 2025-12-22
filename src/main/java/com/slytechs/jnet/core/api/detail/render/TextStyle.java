package com.slytechs.jnet.core.api.detail.render;

public enum TextStyle {

	STANDARD {
		@Override
		public String indent(int depth) {
			return "  ".repeat(depth);
		}

		@Override
		public String headerLine(String name, String abbr) {
			return name; // Use full name
		}

		@Override
		public String fieldPrefix(String headerAbbr, String sectionAbbr, int depth) {
			return "  ".repeat(depth + 1);
		}

		@Override
		public String formatFieldName(String name) {
			return String.format("%20s", name);
		}

		@Override
		public String dataIndent(String headerAbbr, String sectionAbbr, int depth) {
			return "  ".repeat(depth + 1);
		}
	},

	PROTOCOL_PREFIX {
		@Override
		public String indent(int depth) {
			return "";
		}

		@Override
		public String headerLine(String name, String abbr) {
			return abbr; // Use abbreviation
		}

		@Override
		public String fieldPrefix(String headerAbbr, String sectionAbbr, int depth) {
			String prefix;
			if (sectionAbbr != null && !sectionAbbr.isEmpty()) {
				prefix = headerAbbr + ":" + sectionAbbr;
			} else {
				prefix = headerAbbr + ":";
			}
			return String.format("%-10s", prefix) + "  ".repeat(depth);
		}

		@Override
		public String formatFieldName(String name) {
			return String.format("%20s", name);
		}

		@Override
		public String dataIndent(String headerAbbr, String sectionAbbr, int depth) {
			return fieldPrefix(headerAbbr, sectionAbbr, depth);
		}
	},

	COMPACT {
		@Override
		public String indent(int depth) {
			return "";
		}

		@Override
		public String headerLine(String name, String abbr) {
			return abbr; // Use abbreviation
		}

		@Override
		public String fieldPrefix(String headerAbbr, String sectionAbbr, int depth) {
			return "  ";
		}

		@Override
		public String formatFieldName(String name) {
			return name;
		}

		@Override
		public String dataIndent(String headerAbbr, String sectionAbbr, int depth) {
			return "  ";
		}
	};

	public abstract String indent(int depth);

	public abstract String headerLine(String name, String abbr);

	public abstract String fieldPrefix(String headerAbbr, String sectionAbbr, int depth);

	public abstract String formatFieldName(String name);

	public abstract String dataIndent(String headerAbbr, String sectionAbbr, int depth);
}