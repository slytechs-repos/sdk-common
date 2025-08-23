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
package com.slytechs.jnet.core.api.util;

/**
 * An interface for objects that can be enabled or disabled. Classes
 * implementing this interface can switch between an enabled and disabled state,
 * with methods to check and modify this state.
 * 
 * <p>
 * The enabled state typically indicates that the object is operational or
 * active, while the disabled state indicates it is inactive or suspended. The
 * exact semantics of what "enabled" means depends on the implementing class.
 * </p>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc
 */
public interface Enableable {

	/**
	 * A fluent interface for components that can be enabled or disabled, supporting
	 * method chaining by returning the base type.
	 *
	 * @param <T_BASE> the base type to return for method chaining
	 */
	interface FluentEnableable<T_BASE> {

		/**
		 * Checks if this object is currently enabled.
		 *
		 * @return true if this object is enabled, false if it is disabled
		 */
		boolean isEnabled();

		/**
		 * Sets the enabled state of this object.
		 *
		 * @param newState true to enable this object, false to disable it
		 * @return the base instance for method chaining
		 * @see #onEnableChange(boolean)
		 */
		T_BASE setEnable(boolean newState);

		/**
		 * Enables this object. This is equivalent to calling {@code setEnable(true)}.
		 *
		 * @return the base instance for method chaining
		 */
		default T_BASE enable() {
			return setEnable(true);
		}

		/**
		 * Disables this object. This is equivalent to calling {@code setEnable(false)}.
		 *
		 * @return the base instance for method chaining
		 */
		default T_BASE disable() {
			return setEnable(false);
		}

		/**
		 * Called when the enabled state changes. Implementing classes can override this
		 * method to perform any necessary actions when the state changes.
		 *
		 * <p>
		 * This method is called after the state has been changed through
		 * {@link #setEnable(boolean)}, {@link #enable()}, or {@link #disable()}.
		 * </p>
		 *
		 * @param newState the new enabled state; true if the object was enabled, false
		 *                 if it was disabled
		 * @return the base instance for method chaining
		 */
		@SuppressWarnings("unchecked")
		default T_BASE onEnableChange(boolean newState) {
			return (T_BASE) this;
		}
	}

	/**
	 * Checks if this object is currently enabled.
	 *
	 * @return true if this object is enabled, false if it is disabled
	 */
	boolean isEnabled();

	/**
	 * Sets the enabled state of this object.
	 *
	 * @param newState true to enable this object, false to disable it
	 * @see #onEnableChange(boolean)
	 */
	void setEnable(boolean newState);

	/**
	 * Enables this object. This is equivalent to calling {@code setEnable(true)}.
	 */
	default void enable() {
		setEnable(true);
	}

	/**
	 * Disables this object. This is equivalent to calling {@code setEnable(false)}.
	 */
	default void disable() {
		setEnable(false);
	}

	/**
	 * Called when the enabled state changes. Implementing classes can override this
	 * method to perform any necessary actions when the state changes.
	 * 
	 * <p>
	 * This method is called after the state has been changed through
	 * {@link #setEnable(boolean)}, {@link #enable()}, or {@link #disable()}.
	 * </p>
	 *
	 * @param newState the new enabled state; true if the object was enabled, false
	 *                 if it was disabled
	 */
	default void onEnableChange(boolean newState) {
		// Default empty implementation
	}
}