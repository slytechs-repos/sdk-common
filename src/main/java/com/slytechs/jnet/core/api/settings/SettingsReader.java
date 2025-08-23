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
 * The Class SettingsReader.
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
class SettingsReader {

	/** The Constant DEFAULT_SECTION. */
	private static final String DEFAULT_SECTION = "<__default__>";

	/** The Constant HEADER_REGEX. */
	private static final Pattern HEADER_REGEX = Pattern.compile(""
			+ "[#\\!]\\s+\\[([_-+:;\\w\\d]+)\\]"

	);

	/** The input. */
	private final InputStream input;
	
	/** The sections. */
	private final Map<String, Properties> sections = new HashMap<>();

	/**
	 * Format section header.
	 *
	 * @param sectionName the section name
	 * @return the string
	 */
	public static String formatSectionHeader(String sectionName) {
		return SettingsWriter.formatSectionHeader(sectionName);
	}

	/**
	 * Checks if is section start.
	 *
	 * @param line the line
	 * @return true, if is section start
	 */
	public static boolean isSectionStart(String line) {
		var matcher = HEADER_REGEX.matcher(line);

		return matcher.find();
	}

	/**
	 * Gets the section name.
	 *
	 * @param line the line
	 * @return the section name
	 */
	public static String getSectionName(String line) {
		var matcher = HEADER_REGEX.matcher(line);

		if (matcher.find())
			return matcher.group(1);

		return null;
	}

	/**
	 * Instantiates a new settings reader.
	 *
	 * @param input the input
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public SettingsReader(InputStream input) throws IOException {
		this.input = input;

		readAllSections();
	}

	/**
	 * Read.
	 *
	 * @param settings the settings
	 * @return true, if successful
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
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

	/**
	 * Read all sections.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
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

	/**
	 * Read section.
	 *
	 * @param sectionName the section name
	 * @param sectionText the section text
	 * @return the properties
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private Properties readSection(String sectionName, String sectionText) throws IOException {
		try (Reader textReader = new StringReader(sectionText)) {

			Properties properties = new Properties();
			properties.load(textReader);

			sections.put(sectionName, properties);

			return properties;
		}

	}
}
