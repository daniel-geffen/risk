const serverResponse = {
    'Player 1': {
        'color': 'rgb(198,138,49)',
        'countries': {
            'North Africa': 2,
            'Western Europe': 6
        }
    },
    'Player 2': {
        'color': 'rgb(156,195,90)',
        'countries': {
            'Brazil': 3,
            'Central America': 7,
            'Venezuela': 5
        }
    }
};

let svgDoc, json;
$.getJSON('continents.json', obj => json = obj);

initPlayerCountries = (playerData, playerName) => {
    $('#legendList').append($(`<li style="color:${playerData.color}" />`).text(playerName));

    _.forEach(playerData.countries, (numOfSoldiers, countryName) => {
        const country = $(`[id='${countryName}']`, svgDoc);
        country.attr('fill', playerData.color);
        const boundingBox = country[0].getBBox();
        const textElement = $(document.createElementNS('http://www.w3.org/2000/svg', 'text')).attr({
            transform: `translate(${boundingBox.x + boundingBox.width/2} ${boundingBox.y + boundingBox.height/2})`,
            class: 'countryText'
        }).text(numOfSoldiers);
        country.parent().after(textElement);
    })
};

mouseoverCountry = evt => {
    const country = $(evt.target);

    $('#highlight', svgDoc).attr('d', country.attr('d'));
    const countryName = country.attr('id');
    const continentName = _.findKey(json, countryObj => _.includes(_.keys(countryObj), countryName));

    $('#countryName').text(countryName);
    $('#continentName').text(continentName);
    $('#map', svgDoc).children('.neighbor').remove();

    _.forEach(json[continentName][countryName]['neighbors'], neighborId => {
        const neighborName = _.findKey(_.merge(..._.values(json)), {id: neighborId});
        const neighborOutline = $(`[id='${neighborName}']`, svgDoc).attr('d');
        const neighborHighlight = $('#highlight', svgDoc).clone().attr({
            d: neighborOutline,
            id: neighborId,
            class: 'neighbor'
        });
        $('#map', svgDoc).append(neighborHighlight);
    })
};

mouseoverSea = evt => {
    $('#countryName').text($(evt.target).attr( 'id' ));
    $('#continentName').text('');
    $('#highlight', svgDoc).attr('d', 'm0 0');
    $('#map', svgDoc).children('.neighbor').remove();
};

init = () => {
    const svg = document.getElementById('mapObject').contentDocument.getElementById('mapSvg');
    svgDoc = svg.ownerDocument;

    _.forEach(serverResponse, initPlayerCountries);

    $('.country', svgDoc).mouseover(mouseoverCountry);
    $('.sea', svgDoc).mouseover(mouseoverSea);
};

window.addEventListener('load', init);
