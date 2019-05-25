const dialogDefaultOptions = {
    modal: true,
    show: 'blind',
    hide: 'blind'
};
const sliderDefaultOptions = {
    create: () => $('#sliderHandle').text($('#numberSlider').slider('value')),
    change: (event, ui) => $('#sliderHandle').text(ui.value),
    slide: (event, ui) => $('#sliderHandle').text(ui.value)
};

let webSocket, svgDoc, countriesJson, gameState, username, currentStageIndex;
$.getJSON('countries.json', obj => countriesJson = obj);

/**
 * A function that adds a number of troops to a country of the current player (even if the country is currently not occupied by him).
 * Updated both gameState and the text on the screen.
 * @param countryName The name of the country.
 * @param troopsToAdd The number of troops to add.
 * @param shouldAnimate Whether the change should be animated (the numbers go up until all the troops are added).
 */
function addTroopsToCurrentPlayerCountry(countryName, troopsToAdd, shouldAnimate = true) {
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
function getRivalNeighbors(countryName) {
    const currentPlayerCountryIds = _.map(_.pick(countriesJson, _.keys(gameState.players[gameState.currentPlayer].countries)), 'id');
    return _.difference(countriesJson[countryName].neighbors, currentPlayerCountryIds);
}

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
 * An array representing the stages the current player goes through during his turn.
 * Every stage has a name, and a function to invoke when the current player clicks one of his countries during the stage.
*/
const stages = [
    {
        name: 'Draft',
        /**
         * Add a troop to the clicked country and decrease the number of troops left to distribute.
         * If this was the last troop to distribute advance to the next stage.
         */
        click: evt => {
            addTroopsToCurrentPlayerCountry($(evt.target).attr('id'), 1, false);
            $('#troopsToDistribute').text(--gameState.newTroops);
            if (!gameState.newTroops) advanceStage();
        }
    },
    {
        name: 'Attack',
        /**
         * Keep the clicked country highlighted, and change only its rival neighbors to be responsive.
         */
        click: evt => {
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
    },
    {
        name: 'Fortify',
        /**
         * Keep the clicked country highlighted and make only the current player countries that are connected to the clicked country responsive.
         */
        click: evt => {
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
    }
];

/**
 * Handle submitting of the username by sending it to the server.
 */
function handleUsername(usernameInput) {
    $('#usernameForm').hide();
    $('#title').text('Waiting for more players...').show();

    username = usernameInput;
    const message = {
        username
    };

    webSocket.send(JSON.stringify(message));
}

/**
 * A function that is invoked when the mouse enters a current player country.
 * It writes the country and continent name on the screen.
 * It creates a strong highlight around the country and a weaker highlight around its rival neighbors (attack options).
 */
function mouseEnterCountry(evt) {
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
function mouseLeaveCountry() {
    $('#countryName').text('Choose a country');
    $('#highlight', svgDoc).attr('d', 'm0 0');
    $('#map', svgDoc).children('.neighbor').remove();
}

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
 * Initialize the slider to have numbers from 1 to the number of troops that can be moved from the country.
 */
function initSliderToMoveTroopsFrom(countryName) {
    $('#numberSlider').slider(_.defaults({
        min: 1,
        max: gameState.players[gameState.currentPlayer].countries[countryName] - 1,
        value: 1
    }, sliderDefaultOptions));
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
 * Finishes the turn. Hides current player stuff, creates the message for the server with the new game state and sends it.
 */
function finishTurn() {
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
 * Makes the countries of the current player responsive with the basic functions (according to the current stage).
 */
function makeCurrentPlayerCountriesResponsive() {
    $('.country', svgDoc).unbind();

    const currentPlayerCountries = _.keys(gameState.players[gameState.currentPlayer].countries);
    $(_.join(_.map(currentPlayerCountries, c => `[id='${c}']`), ', '), svgDoc).on({
        click: stages[currentStageIndex].click,
        mouseenter: mouseEnterCountry,
        mouseleave: mouseLeaveCountry
    });
}

/**
 * Advances to the next stage.
 * If there is a next stage, writes the new stage name on the screen and changes the click function of countries
 * according to the new stage.
 * If not, finishes the turn.
 */
function advanceStage() {
    currentStageIndex++;
    if (currentStageIndex < stages.length) {
        const currentStage = stages[currentStageIndex];
        $('#stageName').text(`${currentStage.name} Stage`);
        makeCurrentPlayerCountriesResponsive();
    } else {
        finishTurn();
    }
}

/**
 * Creates a list element for the player with the correct classes and adds it to the legend.
 */
function addPlayerListItem(playerName, playerColor) {
    const listItem = $(`<li class="legendItem" />`).css('color', playerColor).text(playerName);
    if (playerName === gameState.currentPlayer) listItem.addClass('selectedLegendItem');
    if (playerName === username) listItem.addClass('userLegendItem');
    if (_.isEmpty(gameState.players[playerName].countries)) listItem.addClass('loserLegendItem');
    $('#legendList').append(listItem);
}

/**
 * Paints the country with the correct color and writes the number of troops on it.
 */
function paintCountry(color, numOfTroops, countryName) {
    const country = $(`[id='${countryName}']`, svgDoc);
    country.attr('fill', color);

    let countryText = $(`#${countriesJson[countryName].id}Text`, svgDoc);
    if (!countryText.length) {
        const boundingBox = country[0].getBBox();
        country.parent().after($(document.createElementNS('http://www.w3.org/2000/svg', 'text')).attr({
            id: `${countriesJson[countryName].id}Text`,
            transform: `translate(${boundingBox.x + boundingBox.width / 2} ${boundingBox.y + boundingBox.height / 2})`,
            class: 'countryText'
        }));
        countryText = $(`#${countriesJson[countryName].id}Text`, svgDoc);
    }

    $({troops: parseInt(countryText.text() || '0', 10)}).animate({troops: numOfTroops}, {
        duration: 500,
        easing: 'linear',
        step: num => countryText.text(Math.floor(num))
    });
}

/**
 * A function invoked when a message is received from the server.
 * Initializes gameState with the received data.
 * Updates the player list and the countries.
 * If there is a winner displays his name.
 * If not starts the turn for the current player.
 */
function onServerMessage(evt) {
    gameState = JSON.parse(evt.data);

    $('#title').text(`${gameState.currentPlayer}'s turn`);
    $('#legendList').empty();
    _.forEach(gameState.players, (playerData, playerName) => {
        addPlayerListItem(playerName, playerData.color);
        _.forEach(playerData.countries, _.partial(paintCountry, playerData.color));
    });

    const winnerName = _.findKey(gameState.players, playerData => _.size(playerData.countries) === _.size(countriesJson));
    if (winnerName) {
        $('#title').text(`${winnerName} has won the game!`);
    } else if (gameState.currentPlayer === username) {
        currentStageIndex = -1;
        $('#troopsToDistribute').text(gameState.newTroops);
        $('#currentPlayerInfo').show();
        advanceStage();
    }
}

/**
 * A function called when the window is loaded.
 * Saves the svg document and initializes a web socket with the server.
 */
function init() {
    const svg = document.getElementById('mapObject').contentDocument.getElementById('mapSvg');
    svgDoc = svg.ownerDocument;

    webSocket = new WebSocket(`ws://${window.location.hostname}:8080/Risk_war_exploded/ws`);
    webSocket.onmessage = onServerMessage;
}

window.addEventListener('load', init);
