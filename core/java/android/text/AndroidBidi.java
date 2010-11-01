/*
 * Copyright (C) 2010 The Android Open Source Project
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

package android.text;

/**
 * Access the ICU bidi implementation.
 * @hide
 */
/* package */ class AndroidBidi {

    /**
     * Run the Unicode BiDi algorithm on the given characters.
     * @param dir Primary direction of the paragraph.
     * @param chs Paragpraph characters.
     * @param chInfo Character directionalities on inputs, their embedding levels on output.
     * @param n Length of paragraph.
     * @param haveInfo Are the directionalities provided?
     * @return Resulting primary direction of the paragraph.
     */
    public static int bidi(int dir, char[] chs, byte[] chInfo, int n, boolean haveInfo) {
        if (chs == null || chInfo == null) {
            throw new NullPointerException();
        }

        if (n < 0 || chs.length < n || chInfo.length < n) {
            throw new IndexOutOfBoundsException();
        }

        switch(dir) {
            case Layout.DIR_REQUEST_LTR: dir = 0; break;
            case Layout.DIR_REQUEST_RTL: dir = 1; break;
            case Layout.DIR_REQUEST_DEFAULT_LTR: dir = -2; break;
            case Layout.DIR_REQUEST_DEFAULT_RTL: dir = -1; break;
            default: dir = 0; break;
        }
        
        // Get character directionalities if they are not provided
        if (!haveInfo) {
            AndroidCharacter.getDirectionalities(chs, chInfo, n);
        }

        int result = runBidi(dir, chs, chInfo, n, true);
        result = (result & 0x1) == 0 ? Layout.DIR_LEFT_TO_RIGHT : Layout.DIR_RIGHT_TO_LEFT;
        return result;
    }

    private native static int runBidi(int dir, char[] chs, byte[] chInfo, int n, boolean haveInfo);
}