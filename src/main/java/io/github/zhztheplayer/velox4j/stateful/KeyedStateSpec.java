/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package io.github.zhztheplayer.velox4j.stateful;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;

import io.github.zhztheplayer.velox4j.serializable.ISerializable;
import io.github.zhztheplayer.velox4j.type.Type;

/**
 * The keyed-state configuration of a stateful operator as declared by the plan: the key channels of
 * the input(s) and the key column types. Pure plan data - the keyed state itself is built from this
 * spec in the operator's initializeStateBackend.
 *
 * <p>One-input operators pass a single channel list; two-input operators (window join) pass the
 * channels of each input, pairing by position. The right channel list is optional in the serialized
 * form (absent means one-input), matching the C++ reader.
 */
public class KeyedStateSpec extends ISerializable {
  private final List<Integer> keyChannels;
  private final List<Integer> rightKeyChannels;
  private final List<Type> keyTypes;

  public KeyedStateSpec(List<Integer> keyChannels, List<Type> keyTypes) {
    this(keyChannels, null, keyTypes);
  }

  /** Two-input operators: the key channels of each input, pairing by position. */
  public KeyedStateSpec(
      List<Integer> keyChannels, List<Integer> rightKeyChannels, List<Type> keyTypes) {
    Preconditions.checkArgument(
        keyChannels != null && !keyChannels.isEmpty(), "A keyed state needs a key channel");
    if (rightKeyChannels != null) {
      Preconditions.checkArgument(
          keyChannels.size() == rightKeyChannels.size(),
          "Two-input keyed state needs a key channel per input");
    }
    Preconditions.checkArgument(
        keyTypes != null && keyChannels.size() == keyTypes.size(),
        "Every key channel needs a key type");
    this.keyChannels = keyChannels;
    this.rightKeyChannels = rightKeyChannels;
    this.keyTypes = keyTypes;
  }

  @JsonCreator
  private static KeyedStateSpec create(
      @JsonProperty("keyChannels") List<Integer> keyChannels,
      @JsonProperty("rightKeyChannels") List<Integer> rightKeyChannels,
      @JsonProperty("keyTypes") List<Type> keyTypes) {
    // An empty right channel list is a one-input spec, matching the C++ reader.
    return new KeyedStateSpec(
        keyChannels,
        rightKeyChannels != null && rightKeyChannels.isEmpty() ? null : rightKeyChannels,
        keyTypes);
  }

  @JsonGetter("keyChannels")
  public List<Integer> getKeyChannels() {
    return keyChannels;
  }

  @JsonGetter("rightKeyChannels")
  public List<Integer> getRightKeyChannels() {
    return rightKeyChannels != null ? rightKeyChannels : List.of();
  }

  @JsonGetter("keyTypes")
  public List<Type> getKeyTypes() {
    return keyTypes;
  }
}
