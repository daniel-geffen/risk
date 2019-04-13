import {countriesJson, svgDoc, gameState, sliderDefaultOptions, currentStageIndex} from './globals.js';
import draftClick from './draft.js';
import attackClick from './attack.js';
import fortifyClick from './fortify.js';

/**
 * An array representing the stages the current player goes through during his turn.
 * Every stage has a name, and a function to invoke when the current player clicks one of his countries during the stage.
 */
const stages = [
    {name: 'Draft', click: draftClick},
    {name: 'Attack', click: attackClick},
    {name: 'Fortify', click: fortifyClick}
];

/**
 * A function that adds a number of troops to a country of the current player (even if the country is currently not occupied by him).
 * Updated both gameState and the text on the screen.
 * @param countryName The name of the country.
 * @param troopsToAdd The number of troops to add.
 * @param shouldAnimate Whether the change should be animated (the numbers go up until all the troops are added).
 */
export function addTroopsToCurrentPlayerCountry(countryName, troopsToAdd, shouldAnimate = true) {
    const currentPlayerCountries = gameState.players[gameState.currentPlayer].countries;
    const initial = {troops: (currentPlayerCountries[countryName] || 0)};
    currentPlayerCountries[countryName] = (currentPlayerCountries[countryName] || 0) + troopsToAdd;
    if (shouldAnimate) {
        $(initial).animate({troops: currentPlayerCountries[countryName]}, {
            duration: 500,
            easing: 'linear',
            step: num => $(`#${countriesJson[countryName].id}Text`, svgDoc).text(Math.floor(num))
        });
    } else {
        $(`#${countriesJson[countryName].id}Text`, svgDoc).text(currentPlayerCountries[countryName]);
    }
}

/**
 * @returns {Array} An array of the names of countries that are neighbors of the taken country and controlled by the rival player.
 */
export function getRivalNeighbors(countryName) {
    const currentPlayerCountryIds = _.map(_.pick(countriesJson, _.keys(gameState.players[gameState.currentPlayer].countries)), 'id');
    return _.difference(countriesJson[countryName].neighbors, currentPlayerCountryIds);
}

/**
 * Initialize the slider to have numbers from 1 to the number of troops that can be moved from the country.
 */
export function initSliderToMoveTroopsFrom(countryName) {
    $('#numberSlider').slider(_.defaults({
        min: 1,
        max: gameState.players[gameState.currentPlayer].countries[countryName] - 1,
        value: 1
    }, sliderDefaultOptions));
}

/**
 * A function that is invoked when the mouse enters a current player country.
 * It writes the country and continent name on the screen.
 * It creates a strong highlight around the country and a weaker highlight around its rival neighbors (attack options).
 */
export function mouseEnterCountry(evt) {
    const country = $(evt.target);
    const countryName = country.attr('id');

    $('#highlight', svgDoc).attr('d', country.attr('d'));
    const continentName = countriesJson[countryName].continent;

    $('#countryName').text(`${countryName}, ${continentName}`);
    $('#map', svgDoc).children('.neighbor').remove();

    const rivalNeighbors = getRivalNeighbors(countryName);
    _.forEach(rivalNeighbors, neighborId => {
        const neighborName = _.findKey(countriesJson, {id: neighborId});
        $('#map', svgDoc).append($('#highlight', svgDoc).clone().attr({
            d: $(`[id='${neighborName}']`, svgDoc).attr('d'),
            id: neighborId,
            class: 'neighbor'
        }));
    })
}

/**
 * A function that is invoked when the mouse leaves a current player country.
 * It resets the texts and removes the highlights.
 */
export function mouseLeaveCountry() {
    $('#countryName').text('Choose a country');
    $('#highlight', svgDoc).attr('d', 'm0 0');
    $('#map', svgDoc).children('.neighbor').remove();
}

/**
 * Makes the countries of the current player responsive with the basic functions (according to the current stage).
 */
export function makeCurrentPlayerCountriesResponsive() {
    $('.country', svgDoc).unbind();

    const currentPlayerCountries = _.keys(gameState.players[gameState.currentPlayer].countries);
    $(_.join(_.map(currentPlayerCountries, c => `[id='${c}']`), ', '), svgDoc).on({
        click: stages[currentStageIndex].click,
        mouseenter: mouseEnterCountry,
        mouseleave: mouseLeaveCountry
    });
}

/**
 * Finishes the turn. Hides current player stuff, creates the message for the server with the new game state and sends it.
 */
export function finishTurn() {
    $('.country', svgDoc).unbind();
    $('#newHighlight', svgDoc).hide();
    $('#currentPlayerInfo').hide();

    const gameStateToSend = _.pick(_.clone(gameState), ['gameId', 'players']);
    _.forEach(gameStateToSend.players, playerCountries => {
        playerCountries.countries = _.mapKeys(playerCountries.countries, (troops, country) => countriesJson[country].id);
    });

    webSocket.send(JSON.stringify(gameStateToSend));
}

/**
 * Advances to the next stage.
 * If there is a next stage, writes the new stage name on the screen and changes the click function of countries
 * according to the new stage.
 * If not, finishes the turn.
 */
export function advanceStage() {
    currentStageIndex++;
    if (currentStageIndex < stages.length) {
        const currentStage = stages[currentStageIndex];
        $('#stageName').text(`${currentStage.name} Stage`);
        makeCurrentPlayerCountriesResponsive();
    } else {
        finishTurn();
    }
}