/*
 * Sly Technologies Free License
 * 
 * Copyright 2025 Sly Technologies Inc.
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
package com.slytechs.sdk.common.session;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Interface for querying the state of a {@link Session} in the jNetworks
 * SDK. Provides read-only access to the lifecycle stages of a network session:
 * running, shutdown scheduled, shutdown initiated, and terminated. This
 * interface allows users to inspect session state without modifying it,
 * ensuring safe interaction with session lifecycle management.
 * 
 * <p>
 * The jNetworks SDK employs a hierarchical structure for managing network
 * sessions, where {@link NetWorks} acts as the root session containing multiple
 * sub-sessions (e.g., {@link Capture} for packet capture, Transmitter for
 * transmission, Config for configuration, FileCapture for file operations,
 * Statistics for metrics, and EventMonitor for events). Each sub-session
 * implements {@link Session} and provides a {@link SessionState} for
 * querying its state. The root {@link NetWorks} also implements
 * {@link Session}, allowing unified lifecycle management—e.g., calling
 * {@link NetWorks#shutdownAfter(Duration)} propagates shutdown to all
 * sub-sessions.
 * </p>
 * 
 * <p>
 * <b>Lifecycle Stages:</b> The interface provides methods to query distinct,
 * non-overlapping stages in the session lifecycle, enabling precise state
 * checks. All queries are thread-safe for concurrent access.
 * </p>
 * <ul>
 * <li><b>Running ({@link #isRunning()}):</b> Indicates the session is active
 * and operational (e.g., capturing or transmitting packets). True after session
 * open (via factory methods like {@link NetWorks#openCapture(String)}) and
 * false when shutdown is initiated. In hierarchical structures, the root
 * {@link NetWorks} is running if any sub-session is active.</li>
 * <li><b>Shutdown Scheduled ({@link #isShutdownScheduled()}):</b> Indicates a
 * shutdown has been scheduled (e.g., via
 * {@link Session#shutdownAfter(Duration)} or
 * {@link Session#shutdownAt(Instant)}), but not yet initiated. The session
 * remains running until the deadline.</li>
 * <li><b>Shutdown Initiated ({@link #isShutdown()}):</b> Indicates shutdown has
 * started (e.g., via {@link Session#shutdown()},
 * {@link Session#shutdownNow()}, or scheduled deadline reached), but
 * internal tasks are still completing. No new operations are accepted.</li>
 * <li><b>Terminated ({@link #isTerminated()}):</b> Indicates all internal tasks
 * and registered sub-sessions/components have completed after shutdown, and the
 * session is fully stopped (ready for close or restart if supported). Queried
 * after {@link Session#awaitCompletion()} to confirm cleanup.</li>
 * </ul>
 * 
 * <p>
 * <b>Usage in Multi-Threaded Environments:</b> Sessions like Capture involve
 * multi-threaded pipelines with producer threads (e.g., hardware capture) and
 * consumer threads (e.g., user-forked tasks in {@link TaskScope} processing
 * streams). This interface ensures thread-safe state queries for reliable
 * operation across threads.
 * </p>
 * <p>
 * Producers check {@link #isRunning()} before operations. Consumers check flags
 * in loops (e.g., {@code while (session.isActive()) { ... }}). During shutdown,
 * stages transition reliably. In hierarchies, root {@link NetWorks} aggregates
 * states.
 * </p>
 * <p>
 * <b>Simple Sessions:</b> Some sessions (e.g., Config for sending commands via
 * sockets) lack a running state—they open, perform operations, and close
 * without ongoing processes. For these:
 * <ul>
 * <li>{@link #isRunning()} returns false (or true on open, false on
 * close).</li>
 * <li>{@link #isShutdownScheduled()} and {@link #isShutdown()} are false until
 * close.</li>
 * <li>{@link #isTerminated()} true after close.</li>
 * <li>{@link #await()} returns immediately (no tasks).</li>
 * </ul>
 * This ensures a uniform interface for simple sessions.
 * </p>
 * <p>
 * <b>Thread Safety:</b> All methods are designed for concurrent access from
 * multiple threads without external synchronization. Queries ensure atomicity
 * and visibility across threads.
 * </p>
 * <p>
 * <b>Recommendations:</b>
 * <ul>
 * <li>Use getters in loops/conditions for non-blocking state checks.</li>
 * <li>For hierarchies, query root {@link NetWorks} states to aggregate
 * sub-session states.</li>
 * <li>Use {@link #await()} or {@link #await(long, TimeUnit)} to wait for
 * termination in monitoring or cleanup code.</li>
 * </ul>
 * </p>
 * <p>
 * <b>Example Usage:</b>
 * 
 * <pre>{@code
 * ResourcePool<Packet> freeListPool = ...; // e.g., from PacketStream
 * ClientSession session = ...;     // if applicable
 * 
 * try {
 *     while (true) {
 *         Packet packet = freeListPool.take();
 *         try {
 *             // Process packet
 *         } finally {
 *             freeListPool.release(packet);
 *         }
 *     }
 * } catch (SessionShutdownException e) {
 *     // Session shutdown, exit loop
 * } catch (InterruptedException e) {
 *     // Handle interruption, possibly re-interrupt thread
 *     Thread.currentThread().interrupt();
 * }
 * 
 * // Or using non-blocking tryTake with yield:
 * while (session.isActive()) {
 *     if (freeListPool.tryTake(this::processPacket)) {
 *         // Processed successfully
 *     } else {
 *         // Temporarily empty, yield to avoid busy-wait
 *         Thread.yield();
 *     }
 * }
 * }</pre>
 * </p>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public sealed interface SessionState permits SessionStateImpl {

	/**
	 * Returns the name of the session.
	 *
	 * @return the session name
	 */
	String name();

	/**
	 * Checks if the session is currently running or has a scheduled shutdown
	 * condition.
	 *
	 * @return true if active, false if shutdown is complete
	 */
	boolean isRunning();

	/**
	 * Checks if shutdown has been initiated for this session.
	 *
	 * @return true if {@link Session#shutdown()} or
	 *         {@link Session#shutdownNow()} has been called, false otherwise
	 */
	boolean isShutdown();

	/**
	 * Checks if a shutdown has been scheduled but not yet initiated.
	 *
	 * @return true if a shutdown is scheduled via
	 *         {@link Session#shutdownAfter(Duration)} or
	 *         {@link Session#shutdownAt(Instant)}, false otherwise
	 */
	boolean isShutdownScheduled();

	/**
	 * Checks if all internal tasks have completed following a shutdown.
	 *
	 * @return true if all internal tasks have completed after shutdown, false
	 *         otherwise
	 */
	boolean isTerminated();

	/**
	 * Waits until all registered components complete.
	 * <p>
	 * Blocks indefinitely or until interrupted. Used in
	 * {@link Session#awaitCompletion()}. Throws {@link InterruptedException} on
	 * interrupt (e.g., during forceful shutdown).
	 * </p>
	 *
	 * @throws InterruptedException if interrupted while waiting
	 */
	void await() throws InterruptedException;

	/**
	 * Waits until all registered components complete or the timeout elapses.
	 * <p>
	 * Used in timed {@link Session#awaitCompletion(long, TimeUnit)}. Returns
	 * true if components completed, false on timeout. Throws
	 * {@link InterruptedException} on interrupt.
	 * </p>
	 *
	 * @param timeout the maximum time to wait
	 * @param unit    the time unit
	 * @return true if terminated (components completed), false on timeout
	 * @throws InterruptedException if interrupted while waiting
	 */
	boolean await(long timeout, TimeUnit unit) throws InterruptedException;
}