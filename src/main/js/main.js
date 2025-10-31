import htmx from 'htmx.org';
import Alpine from 'alpinejs';
import persist from '@alpinejs/persist'


window.htmx = htmx;
window.Alpine = Alpine;

htmx.config.globalViewTransitions = true;

Alpine.plugin(persist)
Alpine.start();

document.body.addEventListener("htmx:afterSwap", function(evt) {
    if (evt.detail && evt.detail.target) {
        htmx.process(evt.detail.target);
        try {
            Alpine.initTree(evt.detail.target);
        } catch (e) {
            console.error("Error initializing Alpine tree after htmx swap.", e);
        }
    }
});