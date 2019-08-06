/*
 * Tencent is pleased to support the open source community by making Angel available.
 *
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/Apache-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package com.tencent.angel.spark.ml.graph.kcore

import com.tencent.angel.ml.math2.VFactory
import com.tencent.angel.ml.math2.vector.IntIntVector
import com.tencent.angel.ml.math2.utils.RowType
import com.tencent.angel.spark.models.PSVector

private[kcore] class KCorePSModel(val core: PSVector) extends Serializable {

  private val dim: Int = core.dimension.toInt

  def updateCoreWithActive(keys: Array[Int], cores: Array[Int]): Unit = {
    core.update(VFactory.sparseIntVector(dim, keys, cores))
  }

  def pull(nodes: Array[Int]): IntIntVector = {
    core.pull(nodes).asInstanceOf[IntIntVector]
  }

  def updateCoreWithActive(vector: IntIntVector): Unit = {
    core.update(vector)
  }
}

private[kcore] object KCorePSModel {
  def fromMaxId(maxId: Int): KCorePSModel = {
    val cores = PSVector.dense(maxId, 1, rowType = RowType.T_INT_DENSE)
    new KCorePSModel(cores)
  }
}