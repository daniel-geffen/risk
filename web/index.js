let webSocket, svgDoc, countriesJson, gameState, username;
$.getJSON('countries.json', obj => countriesJson = obj);

handleUsername = usernameInput => {
    $('#usernameForm').hide();
    $('#title').text("Waiting for more players...").show();

    username = usernameInput;
    const message = {
        type: 0,
        username
    };

    webSocket.send(JSON.stringify(message));
};

getRivalNeighbors = countryName => {
    const currentPlayerCountryIds = _.map(_.pick(countriesJson, _.keys(gameState.players[gameState.currentPlayer].countries)), 'id');
    return _.difference(countriesJson[countryName].neighbors, currentPlayerCountryIds);
};

mouseEnterCountry = evt => {
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

mouseLeaveCountry = () => {
    $('#countryName').text("Choose a country");
    $('#highlight', svgDoc).attr('d', 'm0 0');
    $('#map', svgDoc).children('.neighbor').remove();
};

mouseEnterLeaveNeighbor = evt => {
    const neighborName = $(evt.target).attr('id');
    $(`#${countriesJson[neighborName].id}`, svgDoc).toggleClass('selectedNeighbor');
};

conquerCountry = (defendingCountry, troopsMoved) => {
    const defendingCountryId = countriesJson[defendingCountry.attr('id')].id;
    $(`#${defendingCountryId}`, svgDoc).remove();
    $(`#${defendingCountryId}Text`, svgDoc).text(troopsMoved);
    defendingCountry.attr('fill', gameState.players[gameState.currentPlayer].color);
};

mouseClickNeighbor = (attackingCountry, evt) => {
    const defendingCountry = $(evt.target);
    const attackingCountryId = countriesJson[attackingCountry].id;
    const defendingCountryId = countriesJson[defendingCountry.attr('id')].id;

    const troopsToMove = prompt("Enter number of troops to move to conquered country");
    const attackingText = $(`#${attackingCountryId}Text`, svgDoc);
    attackingText.text(attackingText.text() - troopsToMove);
    conquerCountry(defendingCountry, troopsToMove);
};

makeCurrentPlayerCountriesResponsive = () => {
    $('.country', svgDoc).unbind();

    const currentPlayerCountries = _.keys(gameState.players[gameState.currentPlayer].countries);
    const responsiveCountries = $(_.join(_.map(currentPlayerCountries, c => `[id='${c}']`), ', '), svgDoc);
    responsiveCountries.mouseenter(mouseEnterCountry);
    responsiveCountries.click(mouseClickCountry);
    responsiveCountries.mouseleave(mouseLeaveCountry);
};

mouseClickCountry = evt => {
    $('.country', svgDoc).unbind();

    const selectedCountry = $(evt.target);
    selectedCountry.click(makeCurrentPlayerCountriesResponsive);
    const attackingTargets = _.map(getRivalNeighbors(selectedCountry.attr('id')), id => _.findKey(countriesJson, {id}));
    const attackingTargetsSelector = _.join(_.map(attackingTargets, c => `[id='${c}']`), ', ');
    $(attackingTargetsSelector, svgDoc).mouseenter(mouseEnterLeaveNeighbor);
    $(attackingTargetsSelector, svgDoc).mouseleave(mouseEnterLeaveNeighbor);
    $(attackingTargetsSelector, svgDoc).click(_.partial(mouseClickNeighbor, selectedCountry.attr('id')));
};

addPlayerListItem = (playerData, playerName, message) => {
    const listItem = $(`<li class="legendItem" />`).css('color', playerData.color).text(playerName);
    if (playerName === message.currentPlayer)
        listItem.addClass('selectedLegendItem');
    if (playerName === username)
        listItem.addClass('userLegendItem');
    $('#legendList').append(listItem);
};

paintCountry = (color, numOfTroops, countryName) => {
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

onServerMessage = evt => {
    gameState = JSON.parse(evt.data);

    $('#title').text(`${gameState.currentPlayer}'s turn`);
    if (gameState.currentPlayer === username)
        makeCurrentPlayerCountriesResponsive();
    _.forEach(gameState.players, (playerData, playerName) => {
        addPlayerListItem(playerData, playerName, gameState);

        _.forEach(playerData.countries, _.partial(paintCountry, playerData.color));
    });

};

init = () => {
    const svg = document.getElementById('mapObject').contentDocument.getElementById('mapSvg');

    svgDoc = svg.ownerDocument;

    webSocket = new WebSocket("ws://localhost:8080/Risk_war_exploded/ws");
    webSocket.onmessage = onServerMessage;
};

window.addEventListener('load', init);
