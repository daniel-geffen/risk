export const dialogDefaultOptions = {
    modal: true,
    show: 'blind',
    hide: 'blind'
};
const sliderHandle = $('#sliderHandle');
export const sliderDefaultOptions = {
    create: () => sliderHandle.text($('#numberSlider').slider('value')),
    change: (event, ui) => sliderHandle.text(ui.value),
    slide: (event, ui) => sliderHandle.text(ui.value)
};

export let webSocket, svgDoc, countriesJson, gameState, username, currentStageIndex;
$.getJSON('countries.json', obj => countriesJson = obj);

// TODO - create a setter for index.js to use to set globals (the only file that changes the outer pointer).