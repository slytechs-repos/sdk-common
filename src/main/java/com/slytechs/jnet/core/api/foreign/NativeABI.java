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
package com.slytechs.jnet.core.api.foreign;

import static java.lang.foreign.ValueLayout.*;

/**
* Platforms native ABI (Application Binary Interface, CPU/Instruction set).
* 
* @author mark
*
*/
public enum NativeABI {
   
   /** System V ABI (Linux, BSD on x86_64). */
   SYS_V,
   
   /** Windows 64-bit ABI. */
   WIN64,
   
   /** Linux 64-bit ABI (ARM64). */
   LINUX64,
   
   /** macOS 64-bit ABI (ARM64). */
   MACOS64,
   
   /** BSD 64-bit ABI (FreeBSD, OpenBSD, NetBSD on x86_64). */
   BSD64,
   
   /** BSD 64-bit ABI on ARM64. */
   BSD_AARCH64;

   /** The Constant ABI. */
   private static final NativeABI ABI;
   
   /** The Constant ARCH. */
   private static final String ARCH;
   
   /** The Constant OS. */
   private static final String OS;
   
   /** The Constant ADDRESS_SIZE. */
   private static final long ADDRESS_SIZE;
   
   /** The Constant IS_BSD. */
   private static final boolean IS_BSD;
   
   /** The Constant IS_LINUX. */
   private static final boolean IS_LINUX;
   
   /** The Constant IS_WINDOWS. */
   private static final boolean IS_WINDOWS;
   
   /** The Constant IS_MACOS. */
   private static final boolean IS_MACOS;

   static {
   	ARCH = System.getProperty("os.arch");
   	OS = System.getProperty("os.name").toLowerCase();
   	ADDRESS_SIZE = ADDRESS.byteSize() * 8;
   	
   	// Detect OS family
   	IS_WINDOWS = OS.contains("win");
   	IS_MACOS = OS.contains("mac") || OS.contains("darwin");
   	IS_BSD = OS.contains("bsd") || OS.contains("freebsd") || 
   	         OS.contains("openbsd") || OS.contains("netbsd") || 
   	         OS.contains("dragonfly");
   	IS_LINUX = OS.contains("linux");

   	// Determine ABI based on architecture and OS
   	if ((ARCH.equals("amd64") || ARCH.equals("x86_64")) && ADDRESS_SIZE == 64) {
   		if (IS_WINDOWS) {
   			ABI = WIN64;
   		} else if (IS_BSD) {
   			ABI = BSD64;  // BSD uses System V ABI on x86_64
   		} else {
   			ABI = SYS_V;  // Linux and other Unix-like systems
   		}
   	} else if (ARCH.equals("aarch64") || ARCH.equals("arm64")) {
   		if (IS_MACOS) {
   			ABI = MACOS64;
   		} else if (IS_BSD) {
   			ABI = BSD_AARCH64;
   		} else if (IS_LINUX) {
   			ABI = LINUX64;
   		} else {
   			ABI = null;  // Unsupported ARM64 platform
   		}
   	} else {
   		// Unsupported architecture or 32-bit systems
   		ABI = null;
   	}
   }

   /**
    * Checks if is 64 bit.
    *
    * @return true, if is 64 bit
    */
   public static boolean is64bit() {
   	return ADDRESS_SIZE == 64;
   }

   /**
    * Checks if is 32 bit.
    *
    * @return true, if is 32 bit
    */
   public static boolean is32bit() {
   	return ADDRESS_SIZE == 32;
   }

   /**
    * Current.
    *
    * @return the native ABI
    */
   public static NativeABI current() {
   	if (ABI == null) {
   		throw new UnsupportedOperationException(
   				"Unsupported os, arch, or address size: " + OS + ", " + ARCH + ", " + ADDRESS_SIZE);
   	}
   	return ABI;
   }

   /**
    * Checks if the current platform uses BSD-style ABI.
    * 
    * <p>BSD systems often have slightly different structure layouts,
    * particularly for network-related structures like sockaddr which
    * includes an additional sa_len field.
    *
    * @return true if running on a BSD system (FreeBSD, OpenBSD, NetBSD, DragonFlyBSD)
    */
   public static boolean isBsdAbi() {
   	return IS_BSD || IS_MACOS;  // macOS is BSD-derived and uses BSD sockaddr structure
   }
   
   /**
    * Checks if running on a BSD system.
    *
    * @return true if running on BSD (FreeBSD, OpenBSD, NetBSD, DragonFlyBSD)
    */
   public static boolean isBsd() {
   	return IS_BSD;
   }
   
   /**
    * Checks if running on Linux.
    *
    * @return true if running on Linux
    */
   public static boolean isLinux() {
   	return IS_LINUX;
   }
   
   /**
    * Checks if running on Windows.
    *
    * @return true if running on Windows
    */
   public static boolean isWindows() {
   	return IS_WINDOWS;
   }
   
   /**
    * Checks if running on macOS.
    *
    * @return true if running on macOS
    */
   public static boolean isMacOS() {
   	return IS_MACOS;
   }
   
   /**
    * Checks if the current platform uses System V ABI.
    * 
    * <p>System V ABI is used by Linux and most Unix-like systems on x86_64.
    *
    * @return true if using System V ABI
    */
   public static boolean isSysVAbi() {
   	return ABI == SYS_V || ABI == BSD64;  // BSD64 on x86_64 uses System V calling conventions
   }
   
   /**
    * Gets the OS name.
    *
    * @return the operating system name
    */
   public static String getOsName() {
   	return OS;
   }
   
   /**
    * Gets the architecture.
    *
    * @return the architecture string
    */
   public static String getArch() {
   	return ARCH;
   }
   
   /**
    * Gets the address size in bits.
    *
    * @return the address size (32 or 64)
    */
   public static long getAddressSize() {
   	return ADDRESS_SIZE;
   }
   
   /**
    * Checks if the platform supports POSIX APIs.
    *
    * @return true if POSIX-compliant (Unix-like systems)
    */
   public static boolean isPosix() {
   	return !IS_WINDOWS;
   }
   
   /**
    * Gets a platform-specific library name.
    * 
    * @param baseName the base library name without prefix or extension
    * @return the platform-specific library name (e.g., "libfoo.so", "foo.dll", "libfoo.dylib")
    */
   public static String getLibraryName(String baseName) {
   	if (IS_WINDOWS) {
   		return baseName + ".dll";
   	} else if (IS_MACOS) {
   		return "lib" + baseName + ".dylib";
   	} else {
   		return "lib" + baseName + ".so";
   	}
   }
   
   /**
    * Checks if a specific BSD variant.
    *
    * @param variant the BSD variant name (e.g., "freebsd", "openbsd", "netbsd")
    * @return true if running on the specified BSD variant
    */
   public static boolean isBsdVariant(String variant) {
   	return OS.contains(variant.toLowerCase());
   }
}