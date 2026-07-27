import { createCourseEntryState, handleCourseEntryAction } from "../lib/monsterPark/courseEntry.mjs";

const COURSES = Object.freeze([
    Object.freeze({ minLevel: 120, maxLevel: 129, exp: 259176, minCoins: 14, maxCoins: 28 }),
    Object.freeze({ minLevel: 130, maxLevel: 139, exp: 304067, minCoins: 15, maxCoins: 30 }),
    Object.freeze({ minLevel: 140, maxLevel: 149, exp: 455549, minCoins: 16, maxCoins: 32 }),
    Object.freeze({ minLevel: 150, maxLevel: 159, exp: 626450, minCoins: 17, maxCoins: 34 }),
    Object.freeze({ minLevel: 160, maxLevel: 169, exp: 842320, minCoins: 18, maxCoins: 36 }),
    Object.freeze({ minLevel: 170, maxLevel: 179, exp: 1110003, minCoins: 19, maxCoins: 38 }),
    Object.freeze({ minLevel: 180, maxLevel: 189, exp: 1356058, minCoins: 20, maxCoins: 40 }),
    Object.freeze({ minLevel: 190, maxLevel: 199, exp: 1840730, minCoins: 21, maxCoins: 42 })
]);

const state = createCourseEntryState();

export function start(ctx) {
    handleCourseEntryAction(ctx, 1, 0, state, COURSES);
}

export function action(ctx, mode, type, selection) {
    handleCourseEntryAction(ctx, mode, selection, state, COURSES);
}
