var ExtensionClass = function () { };

ExtensionClass.prototype = {
    run: function (arguments) {
        arguments.completionFunction({
            "title": this.getTitle(),
            "url": document.URL,
            "icon": this.getIconLink(),
            "hostname": document.location.hostname,
            "description": this.getDescription(),
            "content": ""
            // "content": document.documentElement.outerHTML,
        });
    },
    getIconLink: function () {
        const iconLink = document.querySelector('link[rel="icon"], link[rel="shortcut icon"], link[rel="apple-touch-icon"], link[rel="apple-touch-icon-precomposed"]')
        return iconLink ? iconLink.getAttribute('href') : ''
    },
    getDescription: function () {
        return document.querySelector('meta[name="twitter:description"]')?.getAttribute('content') ||
            document.querySelector('meta[name="description"]')?.getAttribute('content') ||
            document.querySelector('meta[property="og:description"]')?.getAttribute('content') ||
            ''
    },
    getTitle: function () {
        return document.title || document.querySelector('meta[property="og:title"]')?.getAttribute('content') || `${location.href}${location.pathname}`
    }

};

var ExtensionPreprocessingJS = new ExtensionClass;
