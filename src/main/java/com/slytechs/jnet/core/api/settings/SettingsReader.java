/*
 * Sly Technologies Free License
 * 
 * Copyright 2024 Sly Technologies Inc.
 *
 * Licensed under the Sly Technologies Free License (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.slytechs.com/free-license-text
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.slytechs.jnet.core.api.settings;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
class SettingsReader {

	private static final String DEFAULT_SECTION = "<__default__>";

	private static final Pattern HEADER_REGEX = Pattern.compile(""
			+ "[#\\!]\\s+\\[([_-+:;\\w\\d]+)\\]"

	);

	private final InputStream input;
	private final Map<String, Properties> sections = new HashMap<>();

	public static String formatSectionHeader(String sectionName) {
		return SettingsWriter.formatSectionHeader(sectionName);
	}

	public static boolean isSectionStart(String line) {
		var matcher = HEADER_REGEX.matcher(line);

		return matcher.find();
	}

	public static String getSectionName(String line) {
		var matcher = HEADER_REGEX.matcher(line);

		if (matcher.find())
			return matcher.group(1);

		return null;
	}

	public SettingsReader(InputStream input) throws IOException {
		this.input = input;

		readAllSections();
	}

	public boolean read(Settings<?> settings) throws IOException {
		Properties savedProperties = sections.get(settings.name());
		if (savedProperties == null)
			savedProperties = sections.get(DEFAULT_SECTION);

		if (savedProperties == null)
			return false;

		for (var p : settings.properties) {
			String name = p.name();

			if (!savedProperties.contains(name))
				continue;

			String storedValue = savedProperties.getProperty(name);

			p.deserializeValue(storedValue);
		}

		return true;
	}

	private void readAllSections() throws IOException {

		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		StringBuilder cacheSectionText = new StringBuilder();

		String sectionName = DEFAULT_SECTION;
		String line;
		while ((line = reader.readLine()) != null) {

			if (isSectionStart(line)) {

				if (cacheSectionText.length() > 0) {
					Properties properties = readSection(sectionName, cacheSectionText.toString());
					sections.put(sectionName, properties);
				}

				sectionName = getSectionName(line);
				cacheSectionText.setLength(0);
			}

			// Collect all the section lines in between section headers
			cacheSectionText.append(line);
		}

	}

	private Properties readSection(String sectionName, String sectionText) throws IOException {
		try (Reader textReader = new StringReader(sectionText)) {

			Properties properties = new Properties();
			properties.load(textReader);

			sections.put(sectionName, properties);

			return properties;
		}

	}
}
