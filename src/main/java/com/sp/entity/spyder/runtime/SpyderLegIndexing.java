package com.sp.entity.spyder.runtime;

public final class SpyderLegIndexing {
    private SpyderLegIndexing() {
    }

    public static boolean isLeftLeg(int leg) {
        return leg % 2 == 0;
    }

    public static boolean isRightLeg(int leg) {
        return !isLeftLeg(leg);
    }

    public static int pairIndex(int leg) {
        return leg / 2;
    }

    public static boolean isDiagonal1(int leg) {
        return pairIndex(leg) % 2 == 0 ? isLeftLeg(leg) : isRightLeg(leg);
    }

    public static boolean isDiagonal2(int leg) {
        return !isDiagonal1(leg);
    }

    public static int diagonalFront(int leg) {
        return isLeftLeg(leg) ? leg - 1 : leg - 3;
    }

    public static int diagonalBack(int leg) {
        return isLeftLeg(leg) ? leg + 3 : leg + 1;
    }

    public static int front(int leg) {
        return leg - 2;
    }

    public static int back(int leg) {
        return leg + 2;
    }

    public static int horizontal(int leg) {
        return isLeftLeg(leg) ? leg + 1 : leg - 1;
    }

    public static int[] diagonal(int leg) {
        return new int[]{diagonalFront(leg), diagonalBack(leg)};
    }

    public static int[] adjacent(int leg) {
        return new int[]{front(leg), back(leg), horizontal(leg)};
    }

    public static int[] walkUpdateOrder(int legCount) {
        int[] order = new int[legCount];
        int cursor = 0;
        for (int leg = 0; leg < legCount; leg++) {
            if (isDiagonal1(leg)) {
                order[cursor++] = leg;
            }
        }
        for (int leg = 0; leg < legCount; leg++) {
            if (isDiagonal2(leg)) {
                order[cursor++] = leg;
            }
        }
        return order;
    }
}
