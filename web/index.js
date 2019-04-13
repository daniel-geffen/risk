import {username, gameState, svgDoc, countriesJson} from './globals.js';
import {advanceStage} from './funcs.js';

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

    if (!$(`#${countriesJson[countryName].id}Text`, svgDoc).length) {
        const boundingBox = country[0].getBBox();
        country.parent().after($(document.createElementNS('http://www.w3.org/2000/svg', 'text')).attr({
            id: `${countriesJson[countryName].id}Text`,
            transform: `translate(${boundingBox.x + boundingBox.width / 2} ${boundingBox.y + boundingBox.height / 2})`,
            class: 'countryText'
        }));
    }

    $(`#${countriesJson[countryName].id}Text`, svgDoc).text(numOfTroops);
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

    webSocket = new WebSocket('ws://localhost:8080/Risk_war_exploded/ws');
    webSocket.onmessage = onServerMessage;
}

window.addEventListener('load', init);
