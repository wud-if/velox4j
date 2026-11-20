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
package io.github.zhztheplayer.velox4j.serde;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import io.github.zhztheplayer.velox4j.expression.ConstantTypedExpr;
import io.github.zhztheplayer.velox4j.join.JoinType;
import io.github.zhztheplayer.velox4j.plan.EmptyNode;
import io.github.zhztheplayer.velox4j.plan.HashPartitionFunctionSpec;
import io.github.zhztheplayer.velox4j.plan.NestedLoopJoinNode;
import io.github.zhztheplayer.velox4j.plan.StreamWindowJoinNode;
import io.github.zhztheplayer.velox4j.serializable.ISerializable;
import io.github.zhztheplayer.velox4j.stateful.KeyedStateSpec;
import io.github.zhztheplayer.velox4j.test.Velox4jTests;
import io.github.zhztheplayer.velox4j.type.IntegerType;
import io.github.zhztheplayer.velox4j.type.RowType;
import io.github.zhztheplayer.velox4j.variant.BooleanValue;

public class StreamWindowJoinNodeSerdeTest {

  @BeforeClass
  public static void beforeClass() {
    Velox4jTests.ensureInitialized();
  }

  @Test
  public void testStreamWindowJoinNode() {
    final RowType leftType =
        new RowType(List.of("lk", "lv"), List.of(new IntegerType(), new IntegerType()));
    final RowType rightType =
        new RowType(List.of("rk", "rv"), List.of(new IntegerType(), new IntegerType()));
    final RowType joinOutputType =
        new RowType(
            List.of("lk", "lv", "rk", "rv"),
            List.of(new IntegerType(), new IntegerType(), new IntegerType(), new IntegerType()));
    final NestedLoopJoinNode probe =
        new NestedLoopJoinNode(
            "id-probe",
            JoinType.INNER,
            ConstantTypedExpr.create(new BooleanValue(true)),
            new EmptyNode(leftType),
            new EmptyNode(rightType),
            joinOutputType);
    final KeyedStateSpec keyedStateSpec =
        new KeyedStateSpec(List.of(0), List.of(0), List.of(new IntegerType()));
    final StreamWindowJoinNode node =
        new StreamWindowJoinNode(
            "id-1",
            new EmptyNode(leftType),
            new EmptyNode(rightType),
            new HashPartitionFunctionSpec(leftType, List.of(0)),
            new HashPartitionFunctionSpec(rightType, List.of(0)),
            probe,
            keyedStateSpec,
            joinOutputType,
            128,
            1,
            1);
    final SerdeTests.ObjectAndJson<ISerializable> roundTripped =
        SerdeTests.testISerializableRoundTrip(node);
    final JsonNode tree = Serde.parseTree(roundTripped.getJson());
    Assert.assertTrue(tree.has("keyedStateSpec"));
    final StreamWindowJoinNode deserialized = (StreamWindowJoinNode) roundTripped.getObj();
    Assert.assertEquals(List.of(0), deserialized.getKeyedStateSpec().getKeyChannels());
    Assert.assertEquals(List.of(0), deserialized.getKeyedStateSpec().getRightKeyChannels());
  }
}
