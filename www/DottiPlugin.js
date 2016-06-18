var exec = require('cordova/exec');

function DottiPlugin() { 
}

DottiPlugin.prototype = {
    constructor: DottiPlugin,

    refresh: function(success, failure) { 
        exec(success, failure, "DottiPlugin", "refresh", []);
    },

    list: function(success, failure) { 
        exec(success, failure, "DottiPlugin", "list", []);
    },

    test: function(success, failure) { 
        exec(success, failure, "DottiPlugin", "test", []);
    },

    init_icons: function(mac, success, failure) {
        args = [mac];
        exec(success, failure, "DottiPlugin", "init_icons", args);
    },

    set_pixel: function(mac, pixel, red, green, blue, success, failure) {
        args = [mac, pixel, red, green, blue];
        exec(success, failure, "DottiPlugin", "set_pixel", args);
    },

    save_icon: function(mac, icon, success, failure) {
        args = [mac, icon];
        exec(success, failure, "DottiPlugin", "save_icon", args);
    },

    show_icon: function(mac, icon, success, failure) {
        args = [mac, icon];
        exec(success, failure, "DottiPlugin", "show_icon", args);
    },

}

var dottiPlugin = new DottiPlugin();
module.exports = dottiPlugin;
