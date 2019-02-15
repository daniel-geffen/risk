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

let highlight;
let doc;

initPlayerCountries = (playerData, playerName) => {
    $('#legendList').append($(`<li style="color:${playerData.color}" />`).text(playerName));

    _.forEach(playerData.countries, (numOfSoldiers, countryName) => {
        const country = doc.getElementById(countryName);
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

mouseoverCountry = evt => {
    const country = evt.target;
    const outline = country.getAttribute( 'd' );
    highlight.setAttribute( 'd', outline );
    $('#countryName').text(country.getAttribute( 'id' ));
};

mouseoverSea = evt => {
    const sea = evt.target;
    $('#countryName').text(sea.getAttribute( 'id' ));
    highlight.setAttribute( 'd', 'm0 0' );
};

init = () => {
    const svg = document.getElementById('mapObject').contentDocument.getElementById('mapSvg');
    doc = svg.ownerDocument;
    highlight = doc.getElementById('highlight');

    _.forEach(serverResponse, initPlayerCountries);

    const countries = doc.getElementsByClassName('country');

    _.forEach(countries, initCountry);
    const seas = doc.getElementsByClassName('sea');

    _.forEach(seas, sea => sea.addEventListener('mouseover', mouseoverSea));
};

window.addEventListener('load', init);
