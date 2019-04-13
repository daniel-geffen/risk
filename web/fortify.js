import {svgDoc, countriesJson, gameState, dialogDefaultOptions} from './globals.js';
import {
    makeCurrentPlayerCountriesResponsive,
    initSliderToMoveTroopsFrom,
    addTroopsToCurrentPlayerCountry,
    finishTurn,
    mouseEnterCountry,
    mouseLeaveCountry
} from './funcs.js';

/**
 * A recursive function used for determining which countries a player can fortify to.
 * @param countryId The id of a country controlled by the current player (the country to fortify from).
 * @param checkedCountries That countries that were already checked and shouldn't be called with the function again.
 * @returns {Array} An array of the ids of countries controlled by the current player that are connected together.
 */
function getLinkedCurrentPlayerCountries(countryId, checkedCountries = [countryId]) {
    let neighbors = _.difference(_.find(countriesJson, {id: countryId}).neighbors, checkedCountries);
    neighbors = _.filter(neighbors, neighbor => _.findKey(countriesJson, {id: neighbor}) in gameState.players[gameState.currentPlayer].countries);
    checkedCountries = checkedCountries.concat(neighbors);
    return neighbors.concat(_.flatMap(neighbors, neighbor => getLinkedCurrentPlayerCountries(neighbor, checkedCountries)));
}

/**
 * A function called when clicking another current player country in the fortify stage.
 * Creates a dialog to prompt for the number of troops that should be fortified.
 * If the user submitted a number, the troops are moved and the turn is finished.
 */
function mouseClickCountryToFortify(fortifyingCountryName, evt) {
    initSliderToMoveTroopsFrom(fortifyingCountryName);
    $('#dialogText').text('Enter number of troops to move to this country:');
    $('#numberInputDialog').dialog(_.defaults({
        title: 'Fortify Troops',
        buttons: {
            'Done': () => {
                $('#numberInputDialog').dialog('destroy');
                const troopsToMove = $('#numberSlider').slider('value');
                addTroopsToCurrentPlayerCountry(fortifyingCountryName, -troopsToMove);
                addTroopsToCurrentPlayerCountry($(evt.target).attr('id'), troopsToMove);
                finishTurn();
            }
        }
    }, dialogDefaultOptions));
}

/**
 * Keep the clicked country highlighted and make only the current player countries that are connected to the clicked country responsive.
 */
export default function click(evt) {
    const selectedCountry = $(evt.target);
    const newHighlight = $('#highlight', svgDoc).clone().attr({
        d: $(`[id='${selectedCountry.attr('id')}']`, svgDoc).attr('d'),
        id: 'newHighlight'
    });
    $('#map', svgDoc).append(newHighlight);

    $('.country', svgDoc).unbind();
    selectedCountry.click(() => {
        newHighlight.remove();
        makeCurrentPlayerCountriesResponsive();
    });

    const linkedCountryIds = getLinkedCurrentPlayerCountries(countriesJson[selectedCountry.attr('id')].id);
    const linkedCountryNames = _.map(linkedCountryIds, linkedId => _.findKey(countriesJson, {id: linkedId}));
    $(_.join(_.map(linkedCountryNames, c => `[id='${c}']`), ', '), svgDoc).on({
        click: _.partial(mouseClickCountryToFortify, selectedCountry.attr('id')),
        mouseenter: mouseEnterCountry,
        mouseleave: mouseLeaveCountry
    });
}