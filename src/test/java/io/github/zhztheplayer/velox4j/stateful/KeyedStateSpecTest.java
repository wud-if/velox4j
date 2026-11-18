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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import io.github.zhztheplayer.velox4j.exception.VeloxException;
import io.github.zhztheplayer.velox4j.serde.Serde;
import io.github.zhztheplayer.velox4j.test.Velox4jTests;
import io.github.zhztheplayer.velox4j.type.IntegerType;

public class KeyedStateSpecTest {

  @BeforeClass
  public static void beforeClass() {
    Velox4jTests.ensureInitialized();
  }

  @Test
  public void testConstructorRejectsEmptyKeyChannels() {
    Assert.assertThrows(
        IllegalArgumentException.class,
        () -> new KeyedStateSpec(List.of(), List.of(new IntegerType())));
  }

  @Test
  public void testConstructorRejectsMismatchedRightKeyChannels() {
    Assert.assertThrows(
        IllegalArgumentException.class,
        () -> new KeyedStateSpec(List.of(0), List.of(0, 1), List.of(new IntegerType())));
  }

  @Test
  public void testConstructorRejectsMismatchedKeyTypes() {
    Assert.assertThrows(
        IllegalArgumentException.class,
        () -> new KeyedStateSpec(List.of(0, 1), List.of(0, 1), List.of(new IntegerType())));
  }

  @Test
  public void testSingleInputRoundTrip() {
    final KeyedStateSpec spec = new KeyedStateSpec(List.of(0), List.of(new IntegerType()));
    final KeyedStateSpec deserialized = Serde.fromJson(Serde.toJson(spec), KeyedStateSpec.class);
    Assert.assertEquals(List.of(0), deserialized.getKeyChannels());
    Assert.assertEquals(List.of(), deserialized.getRightKeyChannels());
    Assert.assertEquals(
        Serde.parseTree(Serde.toJson(spec)), Serde.parseTree(Serde.toJson(deserialized)));
  }

  @Test
  public void testTwoInputRoundTrip() {
    final KeyedStateSpec spec =
        new KeyedStateSpec(
            List.of(0, 1), List.of(0, 1), List.of(new IntegerType(), new IntegerType()));
    final String json = Serde.toJson(spec);
    final JsonNode tree = Serde.parseTree(json);
    Assert.assertTrue(tree.has("keyChannels"));
    Assert.assertTrue(tree.has("rightKeyChannels"));
    Assert.assertTrue(tree.has("keyTypes"));
    final KeyedStateSpec deserialized = Serde.fromJson(json, KeyedStateSpec.class);
    Assert.assertEquals(List.of(0, 1), deserialized.getKeyChannels());
    Assert.assertEquals(List.of(0, 1), deserialized.getRightKeyChannels());
    Assert.assertEquals(tree, Serde.parseTree(Serde.toJson(deserialized)));
  }

  @Test
  public void testDeserializeEmptyRightKeyChannelsAsSingleInput() {
    // The C++ serializer always emits the right channel list (empty for a
    // one-input spec) and no Java-only "name" discriminator; strip the
    // discriminator from the serialized form to get that payload shape.
    final KeyedStateSpec spec = new KeyedStateSpec(List.of(0), List.of(new IntegerType()));
    final ObjectNode cppForm = (ObjectNode) Serde.parseTree(Serde.toJson(spec));
    cppForm.remove("name");
    final KeyedStateSpec deserialized = Serde.fromJson(cppForm.toString(), KeyedStateSpec.class);
    Assert.assertEquals(List.of(0), deserialized.getKeyChannels());
    Assert.assertEquals(List.of(), deserialized.getRightKeyChannels());
  }

  @Test
  public void testDeserializeMismatchedRightKeyChannelsFailsLoud() {
    final KeyedStateSpec spec =
        new KeyedStateSpec(
            List.of(0, 1), List.of(0, 1), List.of(new IntegerType(), new IntegerType()));
    final ObjectNode payload = (ObjectNode) Serde.parseTree(Serde.toJson(spec));
    payload.remove("name");
    payload.set("rightKeyChannels", Serde.parseTree("[2]"));
    final Throwable thrown =
        Assert.assertThrows(
            VeloxException.class, () -> Serde.fromJson(payload.toString(), KeyedStateSpec.class));
    Throwable cause = thrown;
    while (cause != null && !(cause instanceof IllegalArgumentException)) {
      cause = cause.getCause();
    }
    Assert.assertNotNull(cause);
    Assert.assertEquals("Two-input keyed state needs a key channel per input", cause.getMessage());
  }
}
