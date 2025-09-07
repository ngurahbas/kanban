import htmx from 'htmx.org';

window.htmx = htmx;

htmx.config.globalViewTransitions = true;

document.body.addEventListener("htmx:afterSwap", function(evt) {
    if (evt.detail.target) {
        htmx.process(evt.detail.target);
    }
});