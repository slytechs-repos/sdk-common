package com.slytechs.sdk.common.license;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class KeyResolver {

	// Kubernetes/Docker secret paths
	private static final String[] CONTAINER_SECRET_PATHS = {
			"/run/secrets", // Kubernetes + Docker secrets
			"/var/run/secrets",
			"/secrets" // Custom common path
	};

	// Common secret filenames (in order of preference)
	private static final String[] SECRET_FILENAMES = {
			"license.key",
			"license",
			"key",
			"jnetworks.lic",
			"exascale.lic",
			"jnetpcap.lic"
	};

	public KeyResolver() {}

	public String findLicenseKey(ProductConfig config) throws LicenseException {
		try {
			return findFor(config);
		} catch (IOException e) {
			throw new LicenseException(e);
		}
	}

	private String findFor(ProductConfig config) throws IOException {
		String fileName = config.fsName + ".lic";

		// 1-4: Product-specific env/prop
		String k = System.getenv(config.envPrefix + "_LICENSE_KEY");
		if (k != null && k.length() > 20)
			return k;

		String dir = System.getenv(config.envPrefix + "_LICENSE_DIR");
		if (dir != null)
			return read(Path.of(dir, fileName));

		k = System.getProperty(config.fsName + ".license.key");
		if (k != null && k.length() > 20)
			return k;

		dir = System.getProperty(config.fsName + ".license.dir");
		if (dir != null)
			return read(Path.of(dir, fileName));

		// 5-8: Universal env/prop
		k = System.getenv("LICENSE_KEY");
		if (k != null && k.length() > 20)
			return k;

		dir = System.getenv("LICENSE_DIR");
		if (dir != null)
			return read(Path.of(dir, fileName));

		k = System.getProperty("license.key");
		if (k != null && k.length() > 20)
			return k;

		dir = System.getProperty("license.dir");
		if (dir != null)
			return read(Path.of(dir, fileName));

		// KUBERNETES / DOCKER SECRETS — HIGHEST PRIORITY
		for (String secretDir : CONTAINER_SECRET_PATHS) {
			Path dirPath = Path.of(secretDir);
			if (!Files.isDirectory(dirPath))
				continue;

			// Try exact product file first
			Path exact = dirPath.resolve(fileName);
			if (Files.isRegularFile(exact)) {
				String content = read(exact);
				if (content != null)
					return content;
			}

			// Try common secret filenames
			for (String secretName : SECRET_FILENAMES) {
				Path p = dirPath.resolve(secretName);
				if (Files.isRegularFile(p)) {
					String content = read(p);
					if (content != null) {
						System.out.println("Using Kubernetes secret: " + p);
						return content;
					}
				}
			}
		}

		// 9-10: Product-specific traditional
		Path home = getHomePath(config.homeDir, fileName);
		if (home != null)
			return read(home);

		Path system = getSystemPath(config.fsName, fileName);
		if (system != null)
			return read(system);

		// 11-12: Universal fallbacks
		Path universalHome = getHomePath(".license", fileName);
		if (universalHome != null)
			return read(universalHome);

		Path universalSystem = getSystemPath("license", fileName);
		if (universalSystem != null)
			return read(universalSystem);

		// 13: Classpath lookup
		return readClassPath(fileName);
	}

	private String read(Path path) throws IOException {
		if (Files.isRegularFile(path) && Files.isReadable(path) && Files.size(path) > 20) {
			String content = Files.readString(path).trim();
			if (content.matches("^[A-Za-z0-9+/=\\-]+$")) {
				return content;
			}
		}
		return null;
	}

	private Path getHomePath(String dir, String file) {
		String home = System.getProperty("user.home");
		if (home == null || home.isEmpty())
			return null;
		return Path.of(home, dir, file);
	}

	private Path getSystemPath(String dir, String file) {
		if (System.getProperty("os.name").startsWith("Win")) {
			String pf = System.getenv("PROGRAMFILES");
			if (pf == null)
				pf = "C:\\Program Files";
			return Path.of(pf, dir, file);
		} else {
			return Path.of("/etc", dir, file);
		}
	}

	private String readClassPath(String fileName) throws IOException {
		try (InputStream is = getClass().getResourceAsStream("/license/" + fileName)) {
			if (is != null) {
				return new BufferedReader(new InputStreamReader(is))
						.lines().collect(Collectors.joining("\n")).trim();
			}
		}
		return null;
	}
}