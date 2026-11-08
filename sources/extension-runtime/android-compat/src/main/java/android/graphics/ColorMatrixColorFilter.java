/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modified for Katari's desktop extension host from the Android Open Source Project's ColorMatrixColorFilter: the
// matrix it holds, which Canvas applies to the bitmaps it draws with it.

package android.graphics;

public class ColorMatrixColorFilter extends ColorFilter {
    private final ColorMatrix mMatrix = new ColorMatrix();

    public ColorMatrixColorFilter(ColorMatrix matrix) {
        mMatrix.set(matrix);
    }

    public ColorMatrixColorFilter(float[] array) {
        if (array.length < 20) {
            throw new ArrayIndexOutOfBoundsException();
        }
        mMatrix.set(array);
    }

    public void getColorMatrix(ColorMatrix colorMatrix) {
        colorMatrix.set(mMatrix);
    }

    /** Applies the matrix to one unpremultiplied ARGB colour, as Skia's matrix filter does. */
    int filter(int argb) {
        float[] m = mMatrix.getArray();
        int a = argb >>> 24;
        int r = (argb >> 16) & 0xff;
        int g = (argb >> 8) & 0xff;
        int b = argb & 0xff;
        int r2 = channel(m, 0, r, g, b, a);
        int g2 = channel(m, 5, r, g, b, a);
        int b2 = channel(m, 10, r, g, b, a);
        int a2 = channel(m, 15, r, g, b, a);
        return (a2 << 24) | (r2 << 16) | (g2 << 8) | b2;
    }

    private static int channel(float[] m, int row, int r, int g, int b, int a) {
        float value = m[row] * r + m[row + 1] * g + m[row + 2] * b + m[row + 3] * a + m[row + 4];
        return Math.max(0, Math.min(255, Math.round(value)));
    }
}
