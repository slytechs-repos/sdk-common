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
module com.slytechs.jnet.core.api {
    exports com.slytechs.jnet.core.api.memory;
    exports com.slytechs.jnet.core.api.format;
    exports com.slytechs.jnet.core.api.time;
    exports com.slytechs.jnet.core.api.foreign;
    exports com.slytechs.jnet.core.api.util;
    exports com.slytechs.jnet.core.api.util.function;

    requires java.logging;
}