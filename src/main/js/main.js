import Alpine from 'alpinejs'
import htmx from 'htmx.org'

// Expose to window for global access in templates if needed
window.Alpine = Alpine
window.htmx = htmx

Alpine.start()