import { createSelector } from 'reselect'

const settingsSelector = state => state.settings;

// export const exampleIsNegative = createSelector(
//     settingsSelector,
//     settings => {
//         return settings.counti < 0;
//     }
// );