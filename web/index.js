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

mouseenterCountry = evt => {
    const country = $(evt.target);
    const countryName = country.attr('id');

    country.css('cursor', 'pointer');
    $('#highlight', svgDoc).attr('d', country.attr('d'));
    const continentName = countriesJson[countryName].continent;

    $('#countryName').text(countryName);
    $('#continentName').text(continentName);
    $('#map', svgDoc).children('.neighbor').remove();

    const currentPlayerCountries = _.keys(gameState.players[gameState.currentPlayer].countries);
    const rivalNeighbors = _.difference(countriesJson[countryName].neighbors, _.map(_.pick(countriesJson, currentPlayerCountries), 'id'));
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
    $('#continentName').text('');
    $('#highlight', svgDoc).attr('d', 'm0 0');
    $('#map', svgDoc).children('.neighbor').remove();
};

makeCurrentPlayerCountriesResponsive = currentPlayerCountries => {
    $('.country').unbind();
    const responsiveCountries = $(_.join(_.map(currentPlayerCountries, c => `[id='${c}']`), ', '), svgDoc);
    responsiveCountries.mouseenter(mouseenterCountry);
    responsiveCountries.mouseleave(mouseLeaveCountry);
};

addPlayerListItem = (playerData, playerName, message) => {
    const listItem = $(`<li class="legendItem" />`).css('color', playerData.color).text(playerName);
    if (playerName === message.currentPlayer)
        listItem.addClass('selectedLegendItem');
    if (playerName === username)
        listItem.addClass('userLegendItem');
    $('#legendList').append(listItem);
};

paintCountry = (color, numOfSoldiers, countryName) => {
    const country = $(`[id='${countryName}']`, svgDoc);
    country.attr('fill', color);
    const boundingBox = country[0].getBBox();
    const textElement = $(document.createElementNS('http://www.w3.org/2000/svg', 'text')).attr({
        transform: `translate(${boundingBox.x + boundingBox.width / 2} ${boundingBox.y + boundingBox.height / 2})`,
        class: 'countryText'
    }).text(numOfSoldiers);
    country.parent().after(textElement);
};

onServerMessage = evt => {
    gameState = JSON.parse(evt.data);

    $('#title').text(`${gameState.currentPlayer}'s turn`);
    if (gameState.currentPlayer === username)
        makeCurrentPlayerCountriesResponsive(_.keys(gameState.players[gameState.currentPlayer].countries));
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
