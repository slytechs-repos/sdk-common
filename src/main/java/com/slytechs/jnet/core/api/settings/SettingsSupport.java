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

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.slytechs.jnet.core.api.settings.Property.Action;
import com.slytechs.jnet.core.api.util.Registration;

/**
 * The Class SettingsSupport.
 */
public class SettingsSupport {
	
	/** The actions. */
	Map<String, List<Reference<Property.Action<?>>>> actions = new HashMap<>();

	/** The enable fire events. */
	boolean enableFireEvents = true;

	/**
	 * Adds the action.
	 *
	 * @param <T>    the generic type
	 * @param name   the name
	 * @param action the action
	 * @return the registration
	 */
	public synchronized <T> Registration addAction(String name, Property.Action<T> action) {
		List<Reference<Property.Action<?>>> list = actions.computeIfAbsent(name,
				_ -> new ArrayList<Reference<Property.Action<?>>>());

		Reference<Property.Action<?>> weak = new WeakReference<>(action);
		list.add(weak);

		return () -> remove(name, weak);
	}

	/**
	 * Enable event dispatching.
	 *
	 * @param b the b
	 */
	public void enableEventDispatching(boolean b) {
		this.enableFireEvents = b;
	}

	/**
	 * Fire value change.
	 *
	 * @param <T>      the generic type
	 * @param action   the action
	 * @param name     the name
	 * @param oldValue the old value
	 * @param newValue the new value
	 * @param source   the source
	 */
	public synchronized <T> void fireValueChange(Action<T> action, String name, T oldValue, T newValue,
			Object source) {

		if (!enableFireEvents)
			return;

		action.propertyChangeAction(name, oldValue, newValue, source);
	}

	/**
	 * Fire value change.
	 *
	 * @param <T>      the generic type
	 * @param name     the name
	 * @param oldValue the old value
	 * @param newValue the new value
	 */
	public synchronized <T> void fireValueChange(String name, T oldValue, T newValue) {
		fireValueChange(name, oldValue, newValue, null);
	}

	/**
	 * Fire value change.
	 *
	 * @param <T>      the generic type
	 * @param name     the name
	 * @param oldValue the old value
	 * @param newValue the new value
	 * @param source   the source
	 */
	public synchronized <T> void fireValueChange(String name, T oldValue, T newValue, Object source) {
		if (!actions.containsKey(name) || !enableFireEvents)
			return;

		boolean needsCleanup = false;

		for (var ref : actions.get(name)) {
			@SuppressWarnings("unchecked")
			Property.Action<T> action = (Action<T>) ref.get();

			if (action == null) {
				needsCleanup = true;
				continue;
			}

			if (action.canFireFromSource(source))
				action.propertyChangeAction(name, oldValue, newValue, source);
		}

		if (needsCleanup)
			pruneUnreferencedActions();
	}

	/**
	 * Prune unreferenced actions.
	 */
	private synchronized void pruneUnreferencedActions() {
		for (Iterator<String> iterator = actions.keySet().iterator(); iterator.hasNext();) {
			pruneUnreferencedActions(iterator.next());
		}
	}

	/**
	 * Prune unreferenced actions.
	 *
	 * @param key the key
	 */
	private synchronized void pruneUnreferencedActions(String key) {
		List<Reference<Property.Action<?>>> list = actions.get(key);
		if (list == null || list.isEmpty())
			return;

		for (Iterator<Reference<Property.Action<?>>> iterator = list.iterator(); iterator.hasNext();) {
			Reference<Property.Action<?>> reference = iterator.next();
			if (reference.get() == null)
				iterator.remove();
		}

		if (list.isEmpty())
			actions.remove(key);

	}

	/**
	 * Removes the.
	 *
	 * @param key   the key
	 * @param value the value
	 */
	private synchronized void remove(String key, Reference<Property.Action<?>> value) {
		List<Reference<Property.Action<?>>> list = actions.get(key);
		if (list == null || list.isEmpty())
			return;

		Property.Action<?> action = value.get(); // Lock it in
		if (action != null)
			list.remove(value);

		pruneUnreferencedActions();
	}

}