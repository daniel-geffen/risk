import {gameState} from './globals.js';
import {addTroopsToCurrentPlayerCountry, advanceStage} from './funcs.js';

/**
 * Add a troop to the clicked country and decrease the number of troops left to distribute.
 * If this was the last troop to distribute advance to the next stage.
 */
export default function click(evt) {
    addTroopsToCurrentPlayerCountry($(evt.target).attr('id'), 1, false);
    $('#troopsToDistribute').text(--gameState.newTroops);
    if (!gameState.newTroops) advanceStage();
}