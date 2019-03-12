const serverResponse = {
    players: {
        Player1: {
            color: 'rgb(58,118,207)',
            countries: {
                'North Africa': 2,
                'Western Europe': 6
            }
        },
        Player2: {
            color: 'rgb(108,126,83)',
            countries: {
                'Brazil': 3,
                'Central America': 7,
                'Venezuela': 5
            }
        },
        Player3: {
            color: 'rgb(42,175,157)',
            countries: {
                'Middle East': 4,
                'Ukraine': 2
            }
        }
    },
    currentPlayer: 'Player1'
};

let svgDoc, countriesJson;
$.getJSON('countries.json', obj => countriesJson = obj);

initPlayerCountries = (playerData, playerName) => {
    $('#legendList').append($(`<li style="color:${playerData.color}" />`).text(playerName));

    _.forEach(playerData.countries, (numOfSoldiers, countryName) => {
        const country = $(`[id='${countryName}']`, svgDoc);
        country.attr('fill', playerData.color);
        const boundingBox = country[0].getBBox();
        const textElement = $(document.createElementNS('http://www.w3.org/2000/svg', 'text')).attr({
            transform: `translate(${boundingBox.x + boundingBox.width / 2} ${boundingBox.y + boundingBox.height / 2})`,
            class: 'countryText'
        }).text(numOfSoldiers);
        country.parent().after(textElement);
    })
};

mouseenterCountry = evt => {
    const country = $(evt.target);
    const countryName = country.attr('id');

    const currentPlayerCountries = serverResponse.players[serverResponse.currentPlayer].countries;
    if (!_.has(currentPlayerCountries, countryName)) return;

    country.css('cursor', 'pointer');
    $('#highlight', svgDoc).attr('d', country.attr('d'));
    const continentName = countriesJson[countryName].continent;

    $('#countryName').text(countryName);
    $('#continentName').text(continentName);
    $('#map', svgDoc).children('.neighbor').remove();

    const rivalNeighbors = _.difference(countriesJson[countryName].neighbors, _.map(_.pick(countriesJson, _.keys(currentPlayerCountries)), 'id'));
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

init = () => {
    const svg = document.getElementById('mapObject').contentDocument.getElementById('mapSvg');
    svgDoc = svg.ownerDocument;

    _.forEach(serverResponse.players, initPlayerCountries);

    $('.country', svgDoc).mouseenter(mouseenterCountry);
    $('.country', svgDoc).mouseleave(mouseLeaveCountry);
};

window.addEventListener('load', init);
