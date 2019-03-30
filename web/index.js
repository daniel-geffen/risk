const dialogDefaultOptions = {
    modal: true,
    show: 'blind',
    hide: 'blind'
};
const sliderDefaultOptions = {
    create: () => $('#sliderHandle').text($('#numberSlider').slider('value')),
    slide: (event, ui) => $('#sliderHandle').text(ui.value)
};

let webSocket, svgDoc, countriesJson, gameState, username;
$.getJSON('countries.json', obj => countriesJson = obj);

const handleUsername = usernameInput => {
    $('#usernameForm').hide();
    $('#title').text('Waiting for more players...').show();

    username = usernameInput;
    const message = {
        username
    };

    webSocket.send(JSON.stringify(message));
};

const getRivalNeighbors = countryName => {
    const currentPlayerCountryIds = _.map(_.pick(countriesJson, _.keys(gameState.players[gameState.currentPlayer].countries)), 'id');
    return _.difference(countriesJson[countryName].neighbors, currentPlayerCountryIds);
};

const mouseEnterCountry = evt => {
    const country = $(evt.target);
    const countryName = country.attr('id');

    $('#highlight', svgDoc).attr('d', country.attr('d'));
    const continentName = countriesJson[countryName].continent;

    $('#countryName').text(`${countryName}, ${continentName}`);
    $('#map', svgDoc).children('.neighbor').remove();

    const rivalNeighbors = getRivalNeighbors(countryName);
    _.forEach(rivalNeighbors, neighborId => {
        const neighborName = _.findKey(countriesJson, {id: neighborId});
        const neighborOutline = $(`[id='${neighborName}']`, svgDoc).attr('d');
        const neighborHighlight = $('#highlight', svgDoc).clone().attr({
            d: neighborOutline,
            id: neighborId,
            class: 'neighbor'
        });
        $('#map', svgDoc).append(neighborHighlight);
    })
};

const mouseLeaveCountry = () => {
    $('#countryName').text('Choose a country');
    $('#highlight', svgDoc).attr('d', 'm0 0');
    $('#map', svgDoc).children('.neighbor').remove();
};

const mouseEnterLeaveNeighbor = evt => {
    const neighborName = $(evt.target).attr('id');
    $(`#${countriesJson[neighborName].id}`, svgDoc).toggleClass('selectedNeighbor');
};

const conquerCountry = (attackingCountryId, defendingCountry, troopsToMove) => {
    _.map(gameState.players, v => v.countries = _.omit(v.countries, defendingCountry.attr('id')));
    gameState.players[gameState.currentPlayer].countries[_.findKey(countriesJson, {id: attackingCountryId})] -= troopsToMove;
    gameState.players[gameState.currentPlayer].countries[defendingCountry.attr('id')] = troopsToMove;

    const attackingText = $(`#${attackingCountryId}Text`, svgDoc);
    attackingText.text(attackingText.text() - troopsToMove);

    const defendingCountryId = countriesJson[defendingCountry.attr('id')].id;
    $(`#${defendingCountryId}`, svgDoc).remove();
    $(`#${defendingCountryId}Text`, svgDoc).text(troopsToMove);
    defendingCountry.attr('fill', gameState.players[gameState.currentPlayer].color);
    makeCurrentPlayerCountriesResponsive();
};

const mouseClickNeighbor = (attackingCountryName, evt) => {
    const defendingCountry = $(evt.target);
    const attackingCountryId = countriesJson[attackingCountryName].id;
    const attackingText = $(`#${attackingCountryId}Text`, svgDoc);

    $('#numberSlider').slider(_.defaults({
        min: 1,
        max: parseInt(attackingText.text()) - 1,
        value: 1
    }, sliderDefaultOptions));

    $('#dialogText').text('Enter number of troops to move to the conquered country:');
    $('#numberInputDialog').dialog(_.defaults({
        title: 'You Won!',
        buttons: {
            'Done': () => {
                $('#numberInputDialog').dialog('close');
                conquerCountry(attackingCountryId, defendingCountry, $('#numberSlider').slider('value'));
            }
        }
    }, dialogDefaultOptions));
};

const makeCurrentPlayerCountriesResponsive = () => {
    $('.country', svgDoc).unbind();

    const currentPlayerCountries = _.keys(gameState.players[gameState.currentPlayer].countries);
    $(_.join(_.map(currentPlayerCountries, c => `[id='${c}']`), ', '), svgDoc).on({
        mouseenter: mouseEnterCountry,
        click: mouseClickCountry,
        mouseleave: mouseLeaveCountry
    });
};

const mouseClickCountry = evt => {
    const selectedCountry = $(evt.target);

    if (gameState.newTroops) {
        gameState.players[gameState.currentPlayer].countries[selectedCountry.attr('id')]++;
        const selectedCountryText = $(`#${countriesJson[selectedCountry.attr('id')].id}Text`, svgDoc);
        selectedCountryText.text(parseInt(selectedCountryText.text()) + 1);
        $('#troopsToDistribute').text(--gameState.newTroops);
    } else {
        $('.country', svgDoc).unbind();
        selectedCountry.click(makeCurrentPlayerCountriesResponsive);
        const attackingTargets = _.map(getRivalNeighbors(selectedCountry.attr('id')), id => _.findKey(countriesJson, {id}));
        const attackingTargetsSelector = _.join(_.map(attackingTargets, c => `[id='${c}']`), ', ');
        $(attackingTargetsSelector, svgDoc).on({
            mouseenter: mouseEnterLeaveNeighbor,
            click: _.partial(mouseClickNeighbor, selectedCountry.attr('id')),
            mouseleave: mouseEnterLeaveNeighbor
        });
    }
};

const addPlayerListItem = (playerData, playerName, message) => {
    const listItem = $(`<li class="legendItem" />`).css('color', playerData.color).text(playerName);
    if (playerName === message.currentPlayer)
        listItem.addClass('selectedLegendItem');
    if (playerName === username)
        listItem.addClass('userLegendItem');
    $('#legendList').append(listItem);
};

const paintCountry = (color, numOfTroops, countryName) => {
    const country = $(`[id='${countryName}']`, svgDoc);
    country.attr('fill', color);
    const boundingBox = country[0].getBBox();
    const textElement = $(document.createElementNS('http://www.w3.org/2000/svg', 'text')).attr({
        id: `${countriesJson[countryName].id}Text`,
        transform: `translate(${boundingBox.x + boundingBox.width / 2} ${boundingBox.y + boundingBox.height / 2})`,
        class: 'countryText'
    }).text(numOfTroops);
    country.parent().after(textElement);
};

const onServerMessage = evt => {
    gameState = JSON.parse(evt.data);

    $('#title').text(`${gameState.currentPlayer}'s turn`);
    if (gameState.currentPlayer === username) {
        $('#countryName').show();
        $('#troopsToDistribute').text(gameState.newTroops).parent().show();
        makeCurrentPlayerCountriesResponsive();
    }
    _.forEach(gameState.players, (playerData, playerName) => {
        addPlayerListItem(playerData, playerName, gameState);

        _.forEach(playerData.countries, _.partial(paintCountry, playerData.color));
    });

};

const init = () => {
    const svg = document.getElementById('mapObject').contentDocument.getElementById('mapSvg');
    svgDoc = svg.ownerDocument;

    webSocket = new WebSocket('ws://localhost:8080/Risk_war_exploded/ws');
    webSocket.onmessage = onServerMessage;
};

window.addEventListener('load', init);
