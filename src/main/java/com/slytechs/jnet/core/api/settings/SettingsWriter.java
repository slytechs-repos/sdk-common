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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

/**
 * The Class SettingsWriter.
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
class SettingsWriter {

	/**
	 * Format section header.
	 *
	 * @param sectionName the section name
	 * @return the string
	 */
	public static String formatSectionHeader(String sectionName) {
		return "[%s]".formatted(sectionName);
	}

	/** The output. */
	private final OutputStream output;

	/**
	 * Instantiates a new settings writer.
	 *
	 * @param output the output
	 */
	public SettingsWriter(OutputStream output) {
		this.output = output;
	}

	/**
	 * Write.
	 *
	 * @param settings the settings
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public void write(Settings<?> settings) throws IOException {
		Properties properties = new Properties();

		for (var p : settings.properties) {
			if (p.isEmpty())
				continue;

			String name = p.name();
			String value = p.serializeValue();

			properties.setProperty(name, value);
		}

		String header = formatSectionHeader(settings.name());

		properties.store(output, header);
	}

}
