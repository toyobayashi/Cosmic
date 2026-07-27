import { createCourseEntryState, handleCourseEntryAction } from "../lib/monsterPark/courseEntry.mjs";

const COURSES = Object.freeze([
    Object.freeze({ minLevel: 30, maxLevel: 39, exp: 4778, minCoins: 5, maxCoins: 10 }),
    Object.freeze({ minLevel: 40, maxLevel: 49, exp: 12839, minCoins: 6, maxCoins: 12 }),
    Object.freeze({ minLevel: 50, maxLevel: 59, exp: 27717, minCoins: 7, maxCoins: 14 }),
    Object.freeze({ minLevel: 60, maxLevel: 69, exp: 44324, minCoins: 8, maxCoins: 16 })
]);

const state = createCourseEntryState();

export function start(ctx) {
    handleCourseEntryAction(ctx, 1, 0, state, COURSES);
}

export function action(ctx, mode, type, selection) {
    handleCourseEntryAction(ctx, mode, selection, state, COURSES);
}
