if (typeof globalThis === "undefined") {
  if (typeof window !== "undefined") window.globalThis = window;
  else if (typeof self !== "undefined") self.globalThis = self;
  else if (typeof global !== "undefined") global.globalThis = global;
  else this.globalThis = this;
}
if (!Object.fromEntries) {
  Object.fromEntries = function(entries) {
    var obj = {};
    if (entries) {
      for (var i = 0; i < entries.length; i++) {
        var pair = entries[i];
        if (pair && pair.length >= 2) obj[pair[0]] = pair[1];
      }
    }
    return obj;
  };
}
"use strict";if(Object.hasOwn||(Object.hasOwn=function(t,e){if(null==t)throw new TypeError("Object.hasOwn called on null or undefined");return Object.prototype.hasOwnProperty.call(t,e)}),"undefined"==typeof URL&&(globalThis.URL=function(t){this.href=t;var e=t.match(/^(https?:)\/\/([^\/?#:]+)(?::(\d+))?([^?#]*)?(\?[^#]*)?(#.*)?$/);this.protocol=e?e[1]:"",this.hostname=e?e[2]:"",this.port=e&&e[3]?e[3]:"",this.pathname=e&&e[4]?e[4]:"/",this.search=e&&e[5]?e[5]:"",this.hash=e&&e[6]?e[6]:""}),"undefined"==typeof window&&(globalThis.window=globalThis),"undefined"==typeof self&&(globalThis.self=globalThis),!window.location){var defaultHost="https://localhost/";window.location={_url:defaultHost,_parsed:new URL(defaultHost),set href(t){this._url=t,this._parsed=new URL(t)},get href(){return this._url},get protocol(){return this._parsed.protocol},get hostname(){return this._parsed.hostname},get port(){return this._parsed.port},get host(){return this._parsed.hostname+(this._parsed.port?":"+this._parsed.port:"")},get origin(){return this._parsed.protocol+"//"+this.host},get pathname(){return this._parsed.pathname},get search(){return this._parsed.search},get hash(){return this._parsed.hash}}}"undefined"==typeof navigator&&(globalThis.navigator={userAgent:"V8/Standalone",platform:"V8",language:"en-US"});