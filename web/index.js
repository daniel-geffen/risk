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
        const country = svgDoc.getElementById(countryName);
        country.setAttribute('fill', playerData.color);
        const boundingBox = country.getBBox();
        const textElement = document.createElementNS('http://www.w3.org/2000/svg', 'text');
        textElement.setAttribute('transform', 'translate(' + (boundingBox.x + boundingBox.width/2) + ' ' + (boundingBox.y + boundingBox.height/2) + ')');
        textElement.setAttribute('class', 'countryText');
        textElement.textContent = numOfSoldiers;
        country.parentNode.insertBefore(textElement, country.nextSibling);
    })
};

initCountry = country => {
    country.addEventListener('mouseover', mouseoverCountry);
};

removeNeighbors = () => {
    const map = svgDoc.getElementById('map');
    while (map.getElementsByClassName('neighbor').length) {
        map.removeChild(map.getElementsByClassName('neighbor')[0]);
    }
};

mouseoverCountry = evt => {
    const country = evt.target;
    const outline = country.getAttribute('d');

    const countryHighlight = svgDoc.getElementById('Country Highlight');
    const countryName = country.getAttribute('id');
    const continentName = _.findKey(json, countryObj => _.includes(_.keys(countryObj), countryName));

    countryHighlight.setAttribute( 'd', outline);
    $('#countryName').text(countryName);
    $('#continentName').text(continentName);
    removeNeighbors();

    _.forEach(json[continentName][countryName]['neighbors'], neighborId => {
        const neighborName = _.findKey(_.merge(..._.values(json)), {id: neighborId});
        const neighborOutline = svgDoc.getElementById(neighborName).getAttribute('d');
        const neighborHighlight = countryHighlight.cloneNode(true);
        neighborHighlight.setAttribute('d', neighborOutline);
        neighborHighlight.setAttribute('id', neighborId);
        neighborHighlight.setAttribute('class', 'neighbor');
        neighborHighlight.setAttribute('opacity', '0.2');
        neighborHighlight.style['pointer-events'] = 'none';
        svgDoc.getElementById('map').appendChild(neighborHighlight);
    })
};

mouseoverSea = evt => {
    const sea = evt.target;
    $('#countryName').text(sea.getAttribute( 'id' ));
    $('#continentName').text('');
    svgDoc.getElementById('Country Highlight').setAttribute( 'd', 'm0 0' );
    removeNeighbors();
};

init = () => {
    const svg = document.getElementById('mapObject').contentDocument.getElementById('mapSvg');
    svgDoc = svg.ownerDocument;

    _.forEach(serverResponse, initPlayerCountries);

    _.forEach(svgDoc.getElementsByClassName('country'), initCountry);
    _.forEach(svgDoc.getElementsByClassName('sea'), sea => sea.addEventListener('mouseover', mouseoverSea));
};

window.addEventListener('load', init);
