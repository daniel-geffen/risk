let highlight;

window.addEventListener("load", init);

function init()
{
    const svg = document.getElementById('mapObject').contentDocument.getElementById("mapSvg");
    const doc = svg.ownerDocument;
    highlight = doc.getElementById( "highlight" );

    const countries = doc.getElementsByClassName('country');
    _.forEach(countries, country => country.addEventListener('mouseover', mouseoverCountry));

    const seas = doc.getElementsByClassName('sea');
    _.forEach(seas, sea => sea.addEventListener('mouseover', mouseoverSea));
}

function mouseoverSea(evt)
{
    const sea = evt.target;
    $("#countryName").text(sea.getAttribute( 'id' ));
    highlight.setAttribute( 'd', 'm0 0' );
}

function mouseoverCountry(evt)
{
    const country = evt.target;
    const outline = country.getAttribute( 'd' );
    highlight.setAttribute( 'd', outline );

    $("#countryName").text(country.getAttribute( 'id' ));
}
