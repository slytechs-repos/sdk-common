/*
 * Copyright 2005-2026 Sly Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.slytechs.sdk.common.session.state;

/**
 * Defines recovery behavior for error handling in state machines.
 * 
 * <p>
 * Implement this interface with an enum to define custom recovery actions
 * for specific state machines. Each recovery action specifies whether it
 * is terminal, whether to restart, and any delay before restart.
 * </p>
 * 
 * <h2>Default Implementation</h2>
 * <pre>{@code
 * public enum ServiceRecovery implements Recovery<ServiceRecovery> {
 *     FAIL,              // Terminal - no recovery
 *     RESTART,           // Immediate restart
 *     RESTART_DELAYED,   // Restart after configured delay
 *     ESCALATE;          // Notify parent to decide
 *     
 *     @Override
 *     public boolean isTerminal() {
 *         return this == FAIL;
 *     }
 *     
 *     @Override
 *     public boolean shouldRestart() {
 *         return this == RESTART || this == RESTART_DELAYED;
 *     }
 *     
 *     @Override
 *     public boolean isDelayed() {
 *         return this == RESTART_DELAYED;
 *     }
 * }
 * }</pre>
 * 
 * <h2>Custom Recovery Actions</h2>
 * <pre>{@code
 * public enum TaskRecovery implements Recovery<TaskRecovery> {
 *     FAIL,
 *     RESTART,
 *     RESTART_DELAYED,
 *     SHUTDOWN_GROUP;    // Task-specific: terminate sibling tasks
 *     
 *     // ...
 * }
 * }</pre>
 *
 * @param <R> the recovery action enum type
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @see ErrorPolicy
 * @see ErrorContext
 */
public interface Recovery<R extends Enum<R> & Recovery<R>> {

    /**
     * Checks if this recovery action is terminal (no recovery possible).
     * 
     * @return true if error is terminal and state machine should enter ERROR state
     */
    boolean isTerminal();

    /**
     * Checks if this recovery action should trigger a restart.
     * 
     * @return true if state machine should restart
     */
    boolean shouldRestart();

    /**
     * Checks if restart should be delayed.
     * 
     * @return true if restart should wait for configured delay
     */
    boolean isDelayed();

    /**
     * Checks if error should be escalated to parent.
     * 
     * @return true if parent should be notified to make recovery decision
     */
    default boolean shouldEscalate() {
        return false;
    }

    /**
     * Default recovery actions for general use.
     */
    enum Default implements Recovery<Default> {
        
        /** Terminal error - no recovery, transition to ERROR state. */
        FAIL {
            @Override
            public boolean isTerminal() { return true; }
            @Override
            public boolean shouldRestart() { return false; }
            @Override
            public boolean isDelayed() { return false; }
        },
        
        /** Immediate restart - no delay. */
        RESTART {
            @Override
            public boolean isTerminal() { return false; }
            @Override
            public boolean shouldRestart() { return true; }
            @Override
            public boolean isDelayed() { return false; }
        },
        
        /** Delayed restart - wait for configured duration before restart. */
        RESTART_DELAYED {
            @Override
            public boolean isTerminal() { return false; }
            @Override
            public boolean shouldRestart() { return true; }
            @Override
            public boolean isDelayed() { return true; }
        },
        
        /** Escalate to parent - let parent decide recovery action. */
        ESCALATE {
            @Override
            public boolean isTerminal() { return false; }
            @Override
            public boolean shouldRestart() { return false; }
            @Override
            public boolean isDelayed() { return false; }
            @Override
            public boolean shouldEscalate() { return true; }
        }
    }
}