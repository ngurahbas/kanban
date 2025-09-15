import htmx from 'htmx.org';
import Alpine from 'alpinejs';

window.htmx = htmx;
window.Alpine = Alpine;

htmx.config.globalViewTransitions = true;

Alpine.start();

document.body.addEventListener("htmx:afterSwap", function(evt) {
    if (evt.detail.target) {
        htmx.process(evt.detail.target);
    }
});