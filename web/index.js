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

let webSocket, svgDoc, countriesJson, gameState, username, currentStageIndex = -1;
$.getJSON('countries.json', obj => countriesJson = obj);

function addTroopsToCurrentPlayerCountry(countryName, troopsToAdd, shouldAnimate = true) {
    const currentPlayerCountries = gameState.players[gameState.currentPlayer].countries;
    const initial = {troops: (currentPlayerCountries[countryName] || 0)};
    currentPlayerCountries[countryName] = (currentPlayerCountries[countryName] || 0) + troopsToAdd;
    if (shouldAnimate) {
        $(initial).animate({troops: currentPlayerCountries[countryName]}, {
            duration: 1000,
            easing: 'linear',
            step: num => $(`#${countriesJson[countryName].id}Text`, svgDoc).text(Math.floor(num))
        });
    } else {
        $(`#${countriesJson[countryName].id}Text`, svgDoc).text(currentPlayerCountries[countryName]);
    }
}

function getLinkedCurrentPlayerCountries(countryId, checkedCountries = [countryId]) {
    let neighbors = _.difference(_.find(countriesJson, {id: countryId}).neighbors, checkedCountries);
    neighbors = _.filter(neighbors, neighbor => _.findKey(countriesJson, {id: neighbor}) in gameState.players[gameState.currentPlayer].countries);
    checkedCountries = checkedCountries.concat(neighbors);
    return neighbors.concat(_.flatMap(neighbors, neighbor => getLinkedCurrentPlayerCountries(neighbor, checkedCountries)));
}

const stages = [
    {
        name: 'Draft',
        click: evt => {
            if (gameState.newTroops > 0) {
                addTroopsToCurrentPlayerCountry($(evt.target).attr('id'), 1, false);
                $('#troopsToDistribute').text(--gameState.newTroops);
            }
        }
    },
    {
        name: 'Attack',
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

function handleUsername(usernameInput) {
    $('#usernameForm').hide();
    $('#title').text('Waiting for more players...').show();

    username = usernameInput;
    const message = {
        username
    };

    webSocket.send(JSON.stringify(message));
}

function getRivalNeighbors(countryName) {
    const currentPlayerCountryIds = _.map(_.pick(countriesJson, _.keys(gameState.players[gameState.currentPlayer].countries)), 'id');
    return _.difference(countriesJson[countryName].neighbors, currentPlayerCountryIds);
}

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

function mouseLeaveCountry() {
    $('#countryName').text('Choose a country');
    $('#highlight', svgDoc).attr('d', 'm0 0');
    $('#map', svgDoc).children('.neighbor').remove();
}

function conquerCountry(attackingCountryName, defendingCountryName, troopsToMove) {
    _.forEach(gameState.players, v => v.countries = _.omit(v.countries, defendingCountryName));

    addTroopsToCurrentPlayerCountry(attackingCountryName, -troopsToMove);
    addTroopsToCurrentPlayerCountry(defendingCountryName, troopsToMove);

    $(`#${countriesJson[defendingCountryName].id}`, svgDoc).remove();
    $(`[id='${defendingCountryName}']`, svgDoc).attr('fill', gameState.players[gameState.currentPlayer].color);
    makeCurrentPlayerCountriesResponsive();
}

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

function initSliderToMoveTroopsFrom(countryName) {
    $('#numberSlider').slider(_.defaults({
        min: 1,
        max: gameState.players[gameState.currentPlayer].countries[countryName] - 1,
        value: 1
    }, sliderDefaultOptions));
}

function displayWinningDialog(attackingCountryName, defendingCountryName) {
    initSliderToMoveTroopsFrom(attackingCountryName);
    $('#dialogText').text('Enter number of troops to move to the conquered country:');
    $('#numberInputDialog').dialog(_.defaults({
        title: 'You Won!',
        buttons: {
            'Done': () => {
                $('#numberInputDialog').dialog('close');
                conquerCountry(attackingCountryName, defendingCountryName, $('#numberSlider').slider('value'));
            }
        },
        dialogClass: 'remove-close',
        closeOnEscape: false
    }, dialogDefaultOptions));
}

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

function mouseClickCountryToFortify(fortifyingCountryName, evt) {
    initSliderToMoveTroopsFrom(fortifyingCountryName);
    $('#dialogText').text('Enter number of troops to move to this country:');
    $('#numberInputDialog').dialog(_.defaults({
        buttons: {
            'Done': () => {
                $('#numberInputDialog').dialog('close');
                const troopsToMove = $('#numberSlider').slider('value');
                addTroopsToCurrentPlayerCountry(fortifyingCountryName, -troopsToMove);
                addTroopsToCurrentPlayerCountry($(evt.target).attr('id'), troopsToMove);
            }
        }
    }, dialogDefaultOptions));
}

function makeCurrentPlayerCountriesResponsive() {
    $('.country', svgDoc).unbind();

    const currentPlayerCountries = _.keys(gameState.players[gameState.currentPlayer].countries);
    $(_.join(_.map(currentPlayerCountries, c => `[id='${c}']`), ', '), svgDoc).on({
        click: stages[currentStageIndex].click,
        mouseenter: mouseEnterCountry,
        mouseleave: mouseLeaveCountry
    });
}

function advanceStage() {
    const currentStage = stages[++currentStageIndex];
    $('#stageName').text(`${currentStage.name} Stage`);
    makeCurrentPlayerCountriesResponsive();
}

function addPlayerListItem(playerData, playerName, message) {
    const listItem = $(`<li class="legendItem" />`).css('color', playerData.color).text(playerName);
    if (playerName === message.currentPlayer)
        listItem.addClass('selectedLegendItem');
    if (playerName === username)
        listItem.addClass('userLegendItem');
    $('#legendList').append(listItem);
}

function paintCountry(color, numOfTroops, countryName) {
    const country = $(`[id='${countryName}']`, svgDoc);
    country.attr('fill', color);
    const boundingBox = country[0].getBBox();
    const textElement = $(document.createElementNS('http://www.w3.org/2000/svg', 'text')).attr({
        id: `${countriesJson[countryName].id}Text`,
        transform: `translate(${boundingBox.x + boundingBox.width / 2} ${boundingBox.y + boundingBox.height / 2})`,
        class: 'countryText'
    }).text(numOfTroops);
    country.parent().after(textElement);
}

function onServerMessage(evt) {
    gameState = JSON.parse(evt.data);

    $('#title').text(`${gameState.currentPlayer}'s turn`);
    if (gameState.currentPlayer === username) {
        $('#troopsToDistribute').text(gameState.newTroops);
        $('#currentPlayerInfo').show();
        advanceStage();
    }
    _.forEach(gameState.players, (playerData, playerName) => {
        addPlayerListItem(playerData, playerName, gameState);
        _.forEach(playerData.countries, _.partial(paintCountry, playerData.color));
    });

}

function init() {
    const svg = document.getElementById('mapObject').contentDocument.getElementById('mapSvg');
    svgDoc = svg.ownerDocument;

    webSocket = new WebSocket('ws://localhost:8080/Risk_war_exploded/ws');
    webSocket.onmessage = onServerMessage;
}

window.addEventListener('load', init);
