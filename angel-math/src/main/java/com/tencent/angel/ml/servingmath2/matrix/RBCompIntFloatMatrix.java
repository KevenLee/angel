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


package com.tencent.angel.ml.servingmath2.matrix;

import com.tencent.angel.ml.servingmath2.VFactory;
import com.tencent.angel.ml.servingmath2.storage.IntFloatDenseVectorStorage;
import com.tencent.angel.ml.servingmath2.storage.IntFloatSparseVectorStorage;
import com.tencent.angel.ml.servingmath2.ufuncs.executor.BinaryExecutor;
import com.tencent.angel.ml.servingmath2.ufuncs.executor.UnaryExecutor;
import com.tencent.angel.ml.servingmath2.ufuncs.expression.Binary;
import com.tencent.angel.ml.servingmath2.ufuncs.expression.Unary;
import com.tencent.angel.ml.servingmath2.vector.CompIntFloatVector;
import com.tencent.angel.ml.servingmath2.vector.IntFloatVector;
import com.tencent.angel.ml.servingmath2.vector.Vector;

public class RBCompIntFloatMatrix extends RowBasedMatrix<CompIntFloatVector> {

  private int subDim;

  public RBCompIntFloatMatrix() {
    super();
  }

  public RBCompIntFloatMatrix(int matrixId, int clock, CompIntFloatVector[] rows) {
    super(matrixId, clock, rows[0].getDim(), rows);
    assert null != rows[0];
    subDim = rows[0].getSubDim();
  }

  public RBCompIntFloatMatrix(CompIntFloatVector[] rows) {
    this(0, 0, rows);
  }

  public RBCompIntFloatMatrix(int matrixId, int clock, int numRows, int numCols, int subDim) {
    super(matrixId, clock, numCols, new CompIntFloatVector[numRows]);
    this.subDim = subDim;
  }

  public RBCompIntFloatMatrix(int numRows, int numCols, int subDim) {
    this(0, 0, numRows, numCols, subDim);
  }

  public int getSubDim() {
    return subDim;
  }

  public void setSubDim(int subDim) {
    this.subDim = subDim;
  }

  public double get(int i, int j) {
    if (null == rows[i]) {
      initEmpty(i);
      return 0;
    } else {
      return rows[i].get(j);
    }
  }

  public void set(int i, int j, float value) {
    if (null == rows[i]) {
      initEmpty(i);
    }
    rows[i].set(j, value);
  }

  @Override
  public Vector diag() {
    float[] resArr = new float[rows.length];
    for (int i = 0; i < rows.length; i++) {
      if (null == rows[i]) {
        resArr[i] = 0;
      } else {
        resArr[i] = rows[i].get(i);
      }
    }

    IntFloatDenseVectorStorage storage = new IntFloatDenseVectorStorage(resArr);
    return new IntFloatVector(getMatrixId(), 0, getClock(), resArr.length, storage);
  }

  @Override
  public Vector dot(Vector other) {
    float[] resArr = new float[rows.length];
    for (int i = 0; i < rows.length; i++) {
      resArr[i] = (float) rows[i].dot(other);
    }
    IntFloatDenseVectorStorage storage = new IntFloatDenseVectorStorage(resArr);
    return new IntFloatVector(matrixId, 0, clock, rows.length, storage);
  }

  @Override
  public RowBasedMatrix calulate(int rowId, Vector other, Binary op) {
    assert other != null;
    RBCompIntFloatMatrix res;
    if (op.isInplace()) {
      res = this;
    } else {
      res = new RBCompIntFloatMatrix(matrixId, clock, rows.length, (int) cols, subDim);
    }

    if (null == rows[rowId]) {
      initEmpty(rowId);
    }

    if (op.isInplace()) {
      BinaryExecutor.apply(rows[rowId], other, op);
    } else {
      for (int i = 0; i < rows.length; i++) {
        if (i == rowId) {
          res.setRow(rowId, (CompIntFloatVector) BinaryExecutor.apply(rows[rowId], other, op));
        } else if (rows[i] != null) {
          res.setRow(rowId, rows[i].copy());
        }
      }
    }

    return res;
  }

  @Override
  public RowBasedMatrix calulate(Vector other, Binary op) {
    assert other != null;
    RBCompIntFloatMatrix res;
    if (op.isInplace()) {
      res = this;
    } else {
      res = new RBCompIntFloatMatrix(matrixId, clock, rows.length, (int) cols, subDim);
    }
    if (op.isInplace()) {
      for (int rowId = 0; rowId < rows.length; rowId++) {
        if (null == rows[rowId]) {
          initEmpty(rowId);
        }
        BinaryExecutor.apply(rows[rowId], other, op);
      }
    } else {
      for (int rowId = 0; rowId < rows.length; rowId++) {
        if (null == rows[rowId]) {
          initEmpty(rowId);
        }
        res.setRow(rowId, (CompIntFloatVector) BinaryExecutor.apply(rows[rowId], other, op));
      }
    }
    return res;
  }

  @Override
  public RowBasedMatrix calulate(Matrix other, Binary op) {
    assert other instanceof RowBasedMatrix;

    if (op.isInplace()) {
      for (int i = 0; i < rows.length; i++) {
        if (null == rows[i]) {
          initEmpty(i);
        }
        if (null == ((RowBasedMatrix) other).rows[i]) {
          ((RowBasedMatrix) other).initEmpty(i);
        }
        BinaryExecutor.apply(rows[i], ((RowBasedMatrix) other).rows[i], op);
      }
      return this;
    } else {
      CompIntFloatVector[] outRows = new CompIntFloatVector[rows.length];
      for (int i = 0; i < rows.length; i++) {
        if (null == rows[i]) {
          initEmpty(i);
        }
        if (null == ((RowBasedMatrix) other).rows[i]) {
          ((RowBasedMatrix) other).initEmpty(i);
        }
        outRows[i] =
            (CompIntFloatVector) BinaryExecutor
                .apply(rows[i], ((RowBasedMatrix) other).rows[i], op);
      }
      return new RBCompIntFloatMatrix(matrixId, clock, outRows);
    }
  }

  @Override
  public RowBasedMatrix calulate(Unary op) {
    if (op.isInplace()) {
      for (Vector vec : rows) {
        UnaryExecutor.apply(vec, op);
      }
      return this;
    } else {
      CompIntFloatVector[] outRows = new CompIntFloatVector[rows.length];
      for (int i = 0; i < rows.length; i++) {
        if (null == rows[i]) {
          initEmpty(i);
        }
        outRows[i] = (CompIntFloatVector) UnaryExecutor.apply(rows[i], op);
      }
      return new RBCompIntFloatMatrix(matrixId, clock, outRows);
    }
  }

  @Override
  public void setRow(int idx, CompIntFloatVector v) {
    assert cols == v.getDim();
    rows[idx] = v;
  }

  @Override
  public void setRows(CompIntFloatVector[] rows) {
    for (CompIntFloatVector v : rows) {
      assert cols == v.getDim();
    }
    this.rows = rows;
  }

  @Override
  public void initEmpty(int idx) {
    int numComp = (int) ((getDim() + subDim - 1) / subDim);

    if (null == rows[idx]) {
      IntFloatVector[] tmpParts = new IntFloatVector[numComp];
      for (int i = 0; i < numComp; i++) {
        IntFloatSparseVectorStorage storage = new IntFloatSparseVectorStorage(subDim);
        tmpParts[i] = new IntFloatVector(matrixId, idx, clock, (int) getDim(), storage);
      }
      CompIntFloatVector tmpVect =
          new CompIntFloatVector(matrixId, idx, clock, (int) getDim(), tmpParts, subDim);
      rows[idx] = tmpVect;
    }
  }

  @Override
  public double min() {
    double minVal = Double.MAX_VALUE;
    for (CompIntFloatVector ele : rows) {
      if (null != ele) {
        double rowMin = ele.min();
        if (rowMin < minVal) {
          minVal = rowMin;
        }
      }
    }

    if (minVal == Double.MAX_VALUE) {
      minVal = Double.NaN;
    }

    return minVal;
  }

  @Override
  public Vector min(int axis) {
    assert axis == 1;
    double[] minArr = new double[rows.length];
    for (int i = 0; i < rows.length; i++) {
      if (rows[i] != null) {
        minArr[i] = rows[i].min();
      } else {
        minArr[i] = Double.NaN;
      }

    }
    return VFactory.denseDoubleVector(matrixId, 0, clock, minArr);
  }

  @Override
  public Vector max(int axis) {
    assert axis == 1;
    double[] maxArr = new double[rows.length];
    for (int i = 0; i < rows.length; i++) {
      if (rows[i] != null) {
        maxArr[i] = rows[i].max();
      } else {
        maxArr[i] = Double.NaN;
      }

    }
    return VFactory.denseDoubleVector(matrixId, 0, clock, maxArr);
  }

  @Override
  public double max() {
    double maxVal = Double.MIN_VALUE;
    for (CompIntFloatVector ele : rows) {
      if (null != ele) {
        double rowMin = ele.min();
        if (rowMin > maxVal) {
          maxVal = rowMin;
        }
      }
    }

    if (maxVal == Double.MIN_VALUE) {
      maxVal = Double.NaN;
    }

    return maxVal;
  }

  @Override
  public Vector sum(int axis) {
    assert axis == 1;
    float[] maxArr = new float[rows.length];
    for (int i = 0; i < rows.length; i++) {
      if (rows[i] != null) {
        maxArr[i] = (float)rows[i].sum();
      } else {
        maxArr[i] = Float.NaN;
      }
    }
    return VFactory.denseFloatVector(matrixId, 0, clock, maxArr);
  }

  @Override
  public Vector average(int axis) {
    assert axis == 1;
    double[] maxArr = new double[rows.length];
    for (int i = 0; i < rows.length; i++) {
      if (rows[i] != null) {
        maxArr[i] = rows[i].average();
      } else {
        maxArr[i] = Double.NaN;
      }
    }
    return VFactory.denseDoubleVector(matrixId, 0, clock, maxArr);
  }

  @Override
  public Vector std(int axis) {
    assert axis == 1;
    double[] maxArr = new double[rows.length];
    for (int i = 0; i < rows.length; i++) {
      if (rows[i] != null) {
        maxArr[i] = rows[i].std();
      } else {
        maxArr[i] = Double.NaN;
      }
    }
    return VFactory.denseDoubleVector(matrixId, 0, clock, maxArr);
  }

  @Override
  public Vector norm(int axis) {
    assert axis == 1;
    double[] maxArr = new double[rows.length];
    for (int i = 0; i < rows.length; i++) {
      if (rows[i] != null) {
        maxArr[i] = rows[i].norm();
      } else {
        maxArr[i] = Double.NaN;
      }
    }
    return VFactory.denseDoubleVector(matrixId, 0, clock, maxArr);
  }

  @Override
  public Matrix copy() {
    CompIntFloatVector[] newRows = new CompIntFloatVector[rows.length];
    for (int i = 0; i < rows.length; i++) {
      newRows[i] = rows[i].copy();
    }
    return new RBCompIntFloatMatrix(matrixId, clock, newRows);
  }

}