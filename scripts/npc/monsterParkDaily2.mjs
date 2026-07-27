import { createCourseEntryState, handleCourseEntryAction } from "../lib/monsterPark/courseEntry.mjs";

const COURSES = Object.freeze([
    Object.freeze({ minLevel: 70, maxLevel: 79, exp: 41043, minCoins: 9, maxCoins: 18 }),
    Object.freeze({ minLevel: 80, maxLevel: 89, exp: 68548, minCoins: 10, maxCoins: 20 }),
    Object.freeze({ minLevel: 90, maxLevel: 99, exp: 120792, minCoins: 11, maxCoins: 22 }),
    Object.freeze({ minLevel: 100, maxLevel: 109, exp: 141936, minCoins: 12, maxCoins: 24 }),
    Object.freeze({ minLevel: 110, maxLevel: 119, exp: 172587, minCoins: 13, maxCoins: 26 })
]);

const state = createCourseEntryState();

export function start(ctx) {
    handleCourseEntryAction(ctx, 1, 0, state, COURSES);
}

export function action(ctx, mode, type, selection) {
    handleCourseEntryAction(ctx, mode, selection, state, COURSES);
}
