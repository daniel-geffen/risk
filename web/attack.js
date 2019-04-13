import {countriesJson, svgDoc, gameState, dialogDefaultOptions} from './globals.js';
import {
    addTroopsToCurrentPlayerCountry,
    makeCurrentPlayerCountriesResponsive,
    finishTurn,
    initSliderToMoveTroopsFrom,
    getRivalNeighbors
} from './funcs.js';

/**
 * A function called after a battle in which the offense won.
 * It changes the number of troops and moves the defending country to the current player (paints and changes gameState).
 * It checks the current player won, and if so writes it on screen and finishes the turn.
 */
function conquerCountry(attackingCountryName, defendingCountryName, troopsToMove) {
    _.forEach(gameState.players, v => v.countries = _.omit(v.countries, defendingCountryName));

    addTroopsToCurrentPlayerCountry(attackingCountryName, -troopsToMove);
    addTroopsToCurrentPlayerCountry(defendingCountryName, troopsToMove);

    $(`#${countriesJson[defendingCountryName].id}`, svgDoc).remove();
    $(`[id='${defendingCountryName}']`, svgDoc).attr('fill', gameState.players[gameState.currentPlayer].color);

    makeCurrentPlayerCountriesResponsive();

    if (_.size(gameState.players[gameState.currentPlayer].countries) === _.size(countriesJson)) {
        $('#title').text(`${gameState.currentPlayer} has won the game!`);
        finishTurn();
    }
}

/**
 * Displays the winning dialog after a win by the offense.
 * This is in order to take input of the number of troops that should be moved to the occupied country.
 */
function displayWinningDialog(attackingCountryName, defendingCountryName) {
    initSliderToMoveTroopsFrom(attackingCountryName);
    $('#dialogText').text('Enter number of troops to move to the conquered country:');
    $('#numberInputDialog').dialog(_.defaults({
        title: 'You Won!',
        buttons: {
            'Done': () => {
                $('#numberInputDialog').dialog('destroy');
                conquerCountry(attackingCountryName, defendingCountryName, $('#numberSlider').slider('value'));
            }
        },
        dialogClass: 'remove-close',
        closeOnEscape: false
    }, dialogDefaultOptions));
}

/**
 * Simulates a battle (with dice). Simulates rounds until the defense is out of troops or the offense has only 1 left.
 * @param attackingTroopsNum The number of troops on the attacking country.
 * @param defendingTroopsNum The number of troops on the defending country.
 * @return {{attackingTroopsNum: *, defendingTroopsNum: *}} An object with the remaining number of troops on each side at the end of the battle.
 */
function checkWinning(attackingTroopsNum, defendingTroopsNum) {
    while (attackingTroopsNum > 1 && defendingTroopsNum) {
        const attackingDice = _.times(Math.min(3, attackingTroopsNum - 1), () => _.random(1, 6)).sort().reverse();
        const defendingDice = _.times(Math.min(2, defendingTroopsNum), () => _.random(1, 6)).sort().reverse();

        _.times(Math.min(attackingDice.length, defendingDice.length), i => {
            if (attackingDice[i] > defendingDice[i]) defendingTroopsNum--;
            else attackingTroopsNum--;
        });
    }

    return {attackingTroopsNum, defendingTroopsNum}
}

/**
 * A function called when a neighbor is clicked in the attack stage.
 * Initializes a battle with the neighbor, displays its results in a dialog and displays the winning dialog if won.
 */
function mouseClickNeighbor(attackingCountryName, evt) {
    const countryTroopNums = _.assign({}, ..._.map(gameState.players, v => v.countries));
    const defendingCountryName = $(evt.target).attr('id');
    const {attackingTroopsNum, defendingTroopsNum} = checkWinning(countryTroopNums[attackingCountryName], countryTroopNums[defendingCountryName]);
    $('#whoWon').text(`You ${(!defendingTroopsNum ? 'Won' : 'Lost')}!`);
    $('#offense').text('Remaining offense troops: ' + attackingTroopsNum);
    $('#defense').text('Remaining defense troops: ' + defendingTroopsNum);
    $('#attackResultsDialog').dialog(_.defaults({
        buttons: {
            'Continue': () => {
                $('#attackResultsDialog').dialog('close');
                addTroopsToCurrentPlayerCountry(attackingCountryName, attackingTroopsNum - countryTroopNums[attackingCountryName]);
                if (!defendingTroopsNum)
                    displayWinningDialog(attackingCountryName, defendingCountryName);
                else {
                    _.forEach(gameState.players, playerInfo => {
                        if (defendingCountryName in playerInfo.countries) {
                            playerInfo.countries[defendingCountryName] = defendingTroopsNum;
                            $(`#${countriesJson[defendingCountryName].id}Text`, svgDoc).text(defendingTroopsNum);
                        }
                    });
                    makeCurrentPlayerCountriesResponsive();
                }
            }
        },
        dialogClass: 'remove-close',
        closeOnEscape: false
    }, dialogDefaultOptions));
}

/**
 * Keep the clicked country highlighted, and change only its rival neighbors to be responsive.
 */
export default function click(evt) {
    const selectedCountry = $(evt.target);
    $('.country', svgDoc).unbind();
    selectedCountry.click(makeCurrentPlayerCountriesResponsive);
    const attackingTargets = _.map(getRivalNeighbors(selectedCountry.attr('id')), id => _.findKey(countriesJson, {id}));

    const mouseEnterLeaveNeighbor = evt => $(`#${countriesJson[$(evt.target).attr('id')].id}`, svgDoc).toggleClass('selectedNeighbor');
    $(_.join(_.map(attackingTargets, c => `[id='${c}']`), ', '), svgDoc).on({
        mouseenter: mouseEnterLeaveNeighbor,
        click: _.partial(mouseClickNeighbor, selectedCountry.attr('id')),
        mouseleave: mouseEnterLeaveNeighbor
    });
}