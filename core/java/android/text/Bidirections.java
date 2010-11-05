
package android.text;

import com.android.internal.util.ArrayUtils;



/**
 * Describes results of running the Unicode BiDi algorithm logically and visually.
 * @author Tareq Sharafy (tareq.sha@gmail.com)
 */
/*package*/ class Bidirections {

    /** A single left to right run */
    public static final Bidirections ALL_LTR = new Bidirections(new int[]{Integer.MAX_VALUE,0,0},1);
    /** A single right to left run */
    public static final Bidirections ALL_RTL = new Bidirections(new int[]{Integer.MAX_VALUE,0,1},1);
    
    
    // Three consecutive parts:
    // 1- Length of each directional run
    // 2- Visual-to-logical mapping of run indexes
    // 3- Bit string of reversal bits 
    private int[] mRunInfo;
    // Number of runs
    private int mRunCount;

    private Bidirections(int[] runInfo, int runCount) {
        mRunInfo = runInfo;
        mRunCount = runCount;
    }
    
    /**
     * Gets the number of directional runs in the text.
     */
    public int getRunCount() {
        return mRunCount;
    }
    
    /**
     * Get the beginning logical index of the given directional run.
     */
    public int getRunStart(int r) {
        // TODO Maybe we should store logical-to-visual mapping too ?
        int[] runInfo = mRunInfo;
        int s = 0;
        for (int i = 0; i < r; i++)
            s += runInfo[i];
        return s;
    }
    
    /**
     * Get the length of the given directional run.
     */
    public int getRunLength(int r) {
        return mRunInfo[r];
    }
    
    /**
     * Gets whether the given directional run is reversed on display.
     */
    public boolean isReversed(int r) {
        return ((mRunInfo[2*mRunCount + (r/32)] >> (r%32)) & 1) == 1;
    }
    
    /**
     * Get the number of the directional run that comes in the given visual position.
     */
    public int getVisualRun(int visPos) {
        return mRunInfo[mRunCount + visPos];
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("len[");
        for (int i = 0; i < mRunCount; i++) {
            if (i != 0)
                sb.append(',');
            sb.append(mRunInfo[i]);
        }
        sb.append("] vis[");
        for (int i = 0; i < mRunCount; i++) {
            if (i != 0)
                sb.append(',');
            int r = mRunInfo[mRunCount + i];
            sb.append(r);
            if (isReversed(r))
                sb.append('r');
        }
        sb.append(']');
        return sb.toString();
    }

    
    
    /**
     * Creates Bidirections objects from results of the Unicode BiDi algorithm.
     * @author Tareq Sharafy (tareq.sha@gmail.com)
     */
    /*package*/ static class BidirectionsFactory {

        // Reused for creating Bidirections objects
        private byte[] mRunLevels = null;
        // Passed to created Bidirections objects
        private int[] mRunInfo = null;
        private int mRunCount = 0;

        /**
         * Create a Bidirections object describing the directional runs in the range [beg,end).
         * The boundaries are automatically adjusted to the array's size.
         * Automatically returns {@link Bidirections#ALL_LTR} or {@link Bidirections#ALL_RTL} if
         * necessary.
         * @param chLevels Embedding levels of characters.
         * @param beg First index.
         * @param end One place past he last index.
         * @return The created Bidirections object.
         */
        public Bidirections createBidirections(byte[] chLevels, int beg, int end) {
            // Instead of working on the underlying array of characters and their embedding levels,
            // we store the length of each run and treat it as a single unit.
            // Fix boundaries
            if (beg < 0) {
                beg = 0;
            } else if (beg >= chLevels.length) {
                return Bidirections.ALL_LTR;
            }
            if (end <= beg) {
                return Bidirections.ALL_LTR;
            } else if (end > chLevels.length) {
                end = chLevels.length;
            }
            if (beg == end) {
                return Bidirections.ALL_LTR;
            }
            // Count runs
            int runCount = 1;
            byte emb = chLevels[0];
            for (int i = beg + 1; i < end; i++) {
                byte emb2 = chLevels[i];
                if (emb2 != emb) {
                    runCount++;
                    emb = emb2;
                }
            }
            // Predefined
            if (runCount == 1) {
                if ((emb % 2) == 1)
                    return Bidirections.ALL_RTL;
                return Bidirections.ALL_LTR;
            }
            // Run levels
            byte[] runLevels = mRunLevels;
            if (runLevels == null || runLevels.length < runCount) {
                runLevels = new byte[ArrayUtils.idealByteArraySize(runCount)];
            }
            // Information array
            int visualDataLen = 2 * runCount;
            int revStringLen = (runCount / 32) + (runCount % 32 == 0 ? 0 : 1);
            int[] info = new int[visualDataLen + revStringLen];
            // Initialize reversal bits, first integer is initialized later
            for (int i = 1; i < revStringLen; i++) {
                info[visualDataLen + i] = 0;
            }
            // Calculate run lengths and reversal bits
            int curRun = 0;
            int curRunBeg = 0;
            byte curRunLevel = chLevels[beg];
            // Initialize first run
            runLevels[0] = curRunLevel; // Level
            info[visualDataLen] = curRunLevel & 1; // Reversal bit
            for (int i = beg + 1; i < end; i++) {
                byte nextLevel = chLevels[i];
                if (nextLevel != curRunLevel) {
                    // Length of the run we're done with
                    info[curRun] = i - curRunBeg;
                    // Reversal bit and level of the run we're moving to
                    curRun++;
                    curRunBeg = i;
                    curRunLevel = nextLevel;
                    info[visualDataLen + (curRun / 32)] |= (curRunLevel & 1) << (curRun % 32);
                    runLevels[curRun] = curRunLevel;
                }
            }
            // Length of last run
            info[runCount - 1] = (end - beg) - curRunBeg;
            // Initialize default visual locations
            for (int i = 0; i < runCount; i++)
                info[runCount + i] = i;
            // Reverse segments of runs according to Unicode BiDi algorithm
            mRunLevels = runLevels;
            mRunInfo = info;
            mRunCount = runCount;
            reverseHigher(0, 0);
            // Result
            return new Bidirections(this.mRunInfo, this.mRunCount);
        }

        /**
         * Reverse segments of runs with embedding levels higher than the given one and that and are
         * adjacent to the given starting logical run position.
         * @param start Starting logical position of runs.
         * @param level The embedding level to reverse segments of runs higher than it.
         * @return One logical position past the last run covered by this operation.
         */
        private int reverseHigher(int start, int level) {
            // NOTE Recursion is well optimized in Java. It's safe to use it here because embedding
            // levels are limited to 63 in the Unicode BiDi algorithm
            byte[] runLevels = mRunLevels;
            int[] runInfo = mRunInfo;
            int runCount = mRunCount;
            // Flip adjacent higher levels
            int beg = start;
            while (beg < runCount) {
                int nextLevel = runLevels[beg];
                if (nextLevel > level) {
                    // Get the boundary of the sub-segment to reverse
                    int end = reverseHigher(beg, level + 1);
                    // Reverse the sub-segment of higher levels
                    int mid = (end - beg) / 2;
                    for (int i = 0; i < mid; i++) {
                        int j1 = runCount + (beg + i);
                        int j2 = runCount + (end - i - 1);
                        int tmp = runInfo[j1];
                        runInfo[j1] = runInfo[j2];
                        runInfo[j2] = tmp;
                    }
                    beg = end;
                } else if (nextLevel == level) {
                    beg++;
                } else {
                    break;
                }
            }
            // Result
            return beg;
        }
    }

}
