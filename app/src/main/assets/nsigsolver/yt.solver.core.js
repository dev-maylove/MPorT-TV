/**
 * yt.solver.core.js — Patched for Android 6.0 / Chromium 44 ES5 compatibility
 *
 * Perubahan kunci:
 *   1. meriyah.parse() sekarang selalu menggunakan { next: true, module: true, webcompat: true }
 *      agar dapat mem-parse base.js YouTube yang mengandung ES6+ syntax.
 *
 *   2. transformAstToEs5() (dari es5transform.js) dipanggil SETELAH meriyah.parse() dan
 *      SEBELUM astring.generate(), sehingga kode yang dihasilkan astring adalah pure ES5.
 *
 *   3. getFromPrepared() mengeksekusi kode dari preprocessPlayer() yang sudah ES5,
 *      sehingga Function("_result", code) tidak lagi melempar SyntaxError di Chromium 44.
 */
function _toConsumableArray(arr){return _arrayWithoutHoles(arr)||_iterableToArray(arr)||_unsupportedIterableToArray(arr)||_nonIterableSpread()}function _nonIterableSpread(){throw new TypeError("Invalid attempt to spread non-iterable instance.\nIn order to be iterable, non-array objects must have a [Symbol.iterator]() method.")}function _iterableToArray(iter){if(typeof Symbol!=="undefined"&&iter[Symbol.iterator]!=null||iter["@@iterator"]!=null)return Array.from(iter)}function _arrayWithoutHoles(arr){if(Array.isArray(arr))return _arrayLikeToArray(arr)}function _createForOfIteratorHelper(o,allowArrayLike){var it=typeof Symbol!=="undefined"&&o[Symbol.iterator]||o["@@iterator"];if(!it){if(Array.isArray(o)||(it=_unsupportedIterableToArray(o))||allowArrayLike&&o&&typeof o.length==="number"){if(it)o=it;var i=0;var F=function F(){};return{s:F,n:function n(){if(i>=o.length)return{done:true};return{done:false,value:o[i++]}},e:function e(_e){throw _e},f:F}}throw new TypeError("Invalid attempt to iterate non-iterable instance.\nIn order to be iterable, non-array objects must have a [Symbol.iterator]() method.")}var normalCompletion=true,didErr=false,err;return{s:function s(){it=it.call(o)},n:function n(){var step=it.next();normalCompletion=step.done;return step},e:function e(_e2){didErr=true;err=_e2},f:function f(){try{if(!normalCompletion&&it.return!=null)it.return()}finally{if(didErr)throw err}}}}function _slicedToArray(arr,i){return _arrayWithHoles(arr)||_iterableToArrayLimit(arr,i)||_unsupportedIterableToArray(arr,i)||_nonIterableRest()}function _nonIterableRest(){throw new TypeError("Invalid attempt to destructure non-iterable instance.\nIn order to be iterable, non-array objects must have a [Symbol.iterator]() method.")}function _unsupportedIterableToArray(o,minLen){if(!o)return;if(typeof o==="string")return _arrayLikeToArray(o,minLen);var n=Object.prototype.toString.call(o).slice(8,-1);if(n==="Object"&&o.constructor)n=o.constructor.name;if(n==="Map"||n==="Set")return Array.from(o);if(n==="Arguments"||/^(?:Ui|I)nt(?:8|16|32)(?:Clamped)?Array$/.test(n))return _arrayLikeToArray(o,minLen)}function _arrayLikeToArray(arr,len){if(len==null||len>arr.length)len=arr.length;for(var i=0,arr2=new Array(len);i<len;i++)arr2[i]=arr[i];return arr2}function _iterableToArrayLimit(r,l){var t=null==r?null:"undefined"!=typeof Symbol&&r[Symbol.iterator]||r["@@iterator"];if(null!=t){var e,n,i,u,a=[],f=!0,o=!1;try{if(i=(t=t.call(r)).next,0===l){if(Object(t)!==t)return;f=!1}else for(;!(f=(e=i.call(t)).done)&&(a.push(e.value),a.length!==l);f=!0);}catch(r){o=!0,n=r}finally{try{if(!f&&null!=t.return&&(u=t.return(),Object(u)!==u))return}finally{if(o)throw n}}return a}}function _arrayWithHoles(arr){if(Array.isArray(arr))return arr}function _typeof(o){"@babel/helpers - typeof";return _typeof="function"==typeof Symbol&&"symbol"==typeof Symbol.iterator?function(o){return typeof o}:function(o){return o&&"function"==typeof Symbol&&o.constructor===Symbol&&o!==Symbol.prototype?"symbol":typeof o},_typeof(o)}

var jsc = function(meriyah, astring) {
  "use strict";

  // ── Opsi parser ES6+: WAJIB agar meriyah tidak crash saat membaca base.js YouTube ──
  var PARSE_OPTS = { next: true, module: true, webcompat: true };

  function parseSafe(code) {
    return meriyah.parse(code, PARSE_OPTS);
  }

  // ── generateEs5: parse → transform AST ke ES5 → generate ──────────────────
  // Ini adalah inti perbaikan: astring.generate() hanya menerima AST ES5
  // yang sudah diubah oleh transformAstToEs5(), sehingga output-nya
  // tidak mengandung arrow, ?., ??, const/let, dll.
  function generateEs5Code(ast) {
    if (typeof transformAstToEs5 === 'function') {
      ast = transformAstToEs5(ast);
    }
    return astring.generate(ast);
  }

  function matchesStructure(obj, structure) {
    if (Array.isArray(structure)) {
      if (!Array.isArray(obj)) return false;
      return structure.length === obj.length && structure.every(function(value, index) {
        return matchesStructure(obj[index], value);
      });
    }
    if (_typeof(structure) === "object") {
      if (!obj) return !structure;
      if ("or" in structure) return structure.or.some(function(node) { return matchesStructure(obj, node); });
      if ("anykey" in structure && Array.isArray(structure.anykey)) {
        var haystack = Array.isArray(obj) ? obj : Object.values(obj);
        return structure.anykey.every(function(value) {
          return haystack.some(function(el) { return matchesStructure(el, value); });
        });
      }
      for (var _i = 0, _Object$entries = Object.entries(structure); _i < _Object$entries.length; _i++) {
        var _Object$entries$_i = _slicedToArray(_Object$entries[_i], 2),
            key = _Object$entries$_i[0],
            value = _Object$entries$_i[1];
        if (!matchesStructure(obj[key], value)) return false;
      }
      return true;
    }
    return structure === obj;
  }

  function isOneOf(value) {
    for (var _len = arguments.length, of = new Array(_len > 1 ? _len - 1 : 0), _key = 1; _key < _len; _key++) {
      of[_key - 1] = arguments[_key];
    }
    return of.includes(value);
  }

  // ── generateArrowFunction: parse ES6 → AST node (TIDAK di-generate ke string) ─
  // Hasilnya akan di-transform ke ES5 saat generateEs5Code() dipanggil nanti
  function generateArrowFunction(data) {
    return parseSafe(data).body[0].expression;
  }

  function _optionalChain$1(ops) {
    var lastAccessLHS = undefined;
    var value = ops[0];
    var i = 1;
    while (i < ops.length) {
      var op = ops[i];
      var fn = ops[i + 1];
      i += 2;
      if ((op === "optionalAccess" || op === "optionalCall") && value == null) return undefined;
      if (op === "access" || op === "optionalAccess") { lastAccessLHS = value; value = fn(value); }
      else if (op === "call" || op === "optionalCall") {
        value = fn(function() {
          var _value;
          for (var _len2 = arguments.length, args = new Array(_len2), _key2 = 0; _key2 < _len2; _key2++) args[_key2] = arguments[_key2];
          return (_value = value).call.apply(_value, [lastAccessLHS].concat(args));
        });
        lastAccessLHS = undefined;
      }
    }
    return value;
  }

  var identifier = {
    or: [
      { type: "ExpressionStatement", expression: { type: "AssignmentExpression", operator: "=", left: { or: [{ type: "Identifier" }, { type: "MemberExpression" }] }, right: { type: "FunctionExpression", async: false } } },
      { type: "FunctionDeclaration", async: false, id: { type: "Identifier" } },
      { type: "VariableDeclaration", declarations: { anykey: [{ type: "VariableDeclarator", init: { type: "FunctionExpression", async: false } }] } }
    ]
  };

  var asdasd = {
    type: "ExpressionStatement",
    expression: { type: "CallExpression", callee: { type: "MemberExpression", object: { type: "Identifier" }, property: {}, optional: false }, arguments: [{ type: "Literal", value: "alr" }, { type: "Literal", value: "yes" }], optional: false }
  };

  function extract(node) {
    if (!matchesStructure(node, identifier)) return null;
    var options = [];
    if (node.type === "FunctionDeclaration") {
      if (node.id && _optionalChain$1([node, "access", function(_) { return _.body; }, "optionalAccess", function(_2) { return _2.body; }])) {
        options.push({ name: node.id, statements: _optionalChain$1([node, "access", function(_3) { return _.body; }, "optionalAccess", function(_4) { return _4.body; }]) });
      }
    } else if (node.type === "ExpressionStatement") {
      if (node.expression.type !== "AssignmentExpression") return null;
      var name = node.expression.left;
      var body = _optionalChain$1([node.expression.right, "optionalAccess", function(_5) { return _5.body; }, "optionalAccess", function(_6) { return _6.body; }]);
      if (name && body) options.push({ name: name, statements: body });
    } else if (node.type === "VariableDeclaration") {
      var _iterator = _createForOfIteratorHelper(node.declarations), _step;
      try {
        for (_iterator.s(); !(_step = _iterator.n()).done;) {
          var declaration = _step.value;
          var _name = declaration.id;
          var _body = _optionalChain$1([declaration.init, "optionalAccess", function(_7) { return _7.body; }, "optionalAccess", function(_8) { return _8.body; }]);
          if (_name && _body) options.push({ name: _name, statements: _body });
        }
      } catch (err) { _iterator.e(err); } finally { _iterator.f(); }
    }
    for (var _i2 = 0, _options = options; _i2 < _options.length; _i2++) {
      var _options$_i = _options[_i2], _name2 = _options$_i.name, statements = _options$_i.statements;
      if (matchesStructure(statements, { anykey: [asdasd] })) return createSolver(_name2);
    }
    return null;
  }

  function createSolver(expression) {
    // generateArrowFunction menghasilkan AST node (ES6).
    // Akan di-transform ke ES5 oleh generateEs5Code() nanti.
    return generateArrowFunction(
      "\n({sig, n}) => {\n  const url = (" +
      astring.generate(expression) +
      ")(\"https://youtube.com/watch?v=yt-dlp-wins\", \"s\", sig ? encodeURIComponent(sig) : undefined);\n  url.set(\"n\", n);\n  const proto = Object.getPrototypeOf(url);\n  const keys = Object.keys(proto).concat(Object.getOwnPropertyNames(proto));\n  for (const key of keys) {\n    if (![\"constructor\", \"set\", \"get\", \"clone\"].includes(key)) {\n      url[key]();\n      break;\n    }\n  }\n  const s = url.get(\"s\");\n  return {\n    sig: s ? decodeURIComponent(s) : null,\n    n: url.get(\"n\") || null,\n  };\n}\n"
    );
  }

  var setupNodes = parseSafe(
    "\nif (typeof globalThis.XMLHttpRequest === \"undefined\") {\n    globalThis.XMLHttpRequest = { prototype: {} };\n}\nif (typeof URL === \"undefined\") {\n    globalThis.location = {\n        hash: \"\",\n        host: \"www.youtube.com\",\n        hostname: \"www.youtube.com\",\n        href: \"https://www.youtube.com/watch?v=yt-dlp-wins\",\n        origin: \"https://www.youtube.com\",\n        password: \"\",\n        pathname: \"/watch\",\n        port: \"\",\n        protocol: \"https:\",\n        search: \"?v=yt-dlp-wins\",\n        username: \"\",\n    };\n} else {\n    globalThis.location = new URL(\"https://www.youtube.com/watch?v=yt-dlp-wins\");\n}\nif (typeof globalThis.document === \"undefined\") {\n    globalThis.document = Object.create(null);\n}\nif (typeof globalThis.navigator === \"undefined\") {\n    globalThis.navigator = Object.create(null);\n}\nif (typeof globalThis.self === \"undefined\") {\n    globalThis.self = globalThis;\n}\nif (typeof globalThis.window === \"undefined\") {\n    globalThis.window = globalThis;\n}\n"
  ).body;

  function _optionalChain(ops) {
    var lastAccessLHS = undefined;
    var value = ops[0];
    var i = 1;
    while (i < ops.length) {
      var op = ops[i];
      var fn = ops[i + 1];
      i += 2;
      if ((op === "optionalAccess" || op === "optionalCall") && value == null) return undefined;
      if (op === "access" || op === "optionalAccess") { lastAccessLHS = value; value = fn(value); }
      else if (op === "call" || op === "optionalCall") {
        value = fn(function() {
          var _value2;
          for (var _len3 = arguments.length, args = new Array(_len3), _key3 = 0; _key3 < _len3; _key3++) args[_key3] = arguments[_key3];
          return (_value2 = value).call.apply(_value2, [lastAccessLHS].concat(args));
        });
        lastAccessLHS = undefined;
      }
    }
    return value;
  }

  function preprocessPlayer(data) {
    var _program$body;
    // Parse base.js YouTube (mengandung ES6+) → AST
    var program = parseSafe(data);
    var plainStatements = modifyPlayer(program);
    var solutions = getSolutions(plainStatements);

    for (var _i3 = 0, _Object$entries2 = Object.entries(solutions); _i3 < _Object$entries2.length; _i3++) {
      var _Object$entries2$_i = _slicedToArray(_Object$entries2[_i3], 2),
          name = _Object$entries2$_i[0],
          options = _Object$entries2$_i[1];
      plainStatements.push({
        type: "ExpressionStatement",
        expression: {
          type: "AssignmentExpression",
          operator: "=",
          left: { type: "MemberExpression", computed: false, object: { type: "Identifier", name: "_result" }, property: { type: "Identifier", name: name }, optional: false },
          right: multiTry(options)
        }
      });
    }

    (_program$body = program.body).splice.apply(_program$body, [0, 0].concat(_toConsumableArray(setupNodes)));

    // ── KUNCI PERBAIKAN: Transform AST ES6 → ES5 SEBELUM generate ──
    // Hasil generateEs5Code() adalah pure ES5 string yang aman
    // dieksekusi oleh Function("_result", code) di Chromium 44.
    return generateEs5Code(program);
  }

  function modifyPlayer(program) {
    var body = program.body;
    var block = function() {
      switch (body.length) {
        case 1: {
          var func = body[0];
          if (_optionalChain([func, "optionalAccess", function(_) { return _.type; }]) === "ExpressionStatement" && func.expression.type === "CallExpression" && func.expression.callee.type === "MemberExpression" && func.expression.callee.object.type === "FunctionExpression") {
            return func.expression.callee.object.body;
          }
          break;
        }
        case 2: {
          var _func = body[1];
          if (_optionalChain([_func, "optionalAccess", function(_2) { return _2.type; }]) === "ExpressionStatement" && _func.expression.type === "CallExpression" && _func.expression.callee.type === "FunctionExpression") {
            var _block = _func.expression.callee.body;
            _block.body.splice(0, 1);
            return _block;
          }
          break;
        }
      }
      throw "unexpected structure";
    }();

    block.body = block.body.filter(function(node) {
      if (node.type === "ExpressionStatement") {
        if (node.expression.type === "AssignmentExpression") return true;
        return node.expression.type === "Literal";
      }
      return true;
    });
    return block.body;
  }

  function getSolutions(statements) {
    var found = { n: [], sig: [] };
    var _iterator2 = _createForOfIteratorHelper(statements), _step2;
    try {
      for (_iterator2.s(); !(_step2 = _iterator2.n()).done;) {
        var statement = _step2.value;
        var result = extract(statement);
        if (result) {
          found.n.push(makeSolver(result, { type: "Identifier", name: "n" }));
          found.sig.push(makeSolver(result, { type: "Identifier", name: "sig" }));
        }
      }
    } catch (err) { _iterator2.e(err); } finally { _iterator2.f(); }
    return found;
  }

  function makeSolver(result, ident) {
    return {
      type: "ArrowFunctionExpression",
      params: [ident],
      body: {
        type: "MemberExpression",
        object: {
          type: "CallExpression",
          callee: result,
          arguments: [{ type: "ObjectExpression", properties: [{ type: "Property", key: ident, value: ident, kind: "init", computed: false, method: false, shorthand: true }] }],
          optional: false
        },
        computed: false, property: ident, optional: false
      },
      async: false, expression: true, generator: false
    };
  }

  function getFromPrepared(code) {
    // code sudah ES5 murni (hasil generateEs5Code di preprocessPlayer)
    // sehingga Function() aman dijalankan di V8 Chromium 44
    var resultObj = { n: null, sig: null };
    Function("_result", code)(resultObj);
    return resultObj;
  }

  function multiTry(generators) {
    // generateArrowFunction menghasilkan AST node (ES6),
    // akan di-transform ke ES5 oleh generateEs5Code() nanti
    return generateArrowFunction(
      "\n(_input) => {\n  const _results = new Set();\n  const errors = [];\n  for (const _generator of " +
      astring.generate({ type: "ArrayExpression", elements: generators }) +
      ") {\n    try {\n      _results.add(_generator(_input));\n    } catch (e) {\n      errors.push(e);\n    }\n  }\n  if (!_results.size) {\n    throw 'no solutions: ' + errors.join(\", \");\n  }\n  if (_results.size !== 1) {\n    throw 'invalid solutions: ' + Array.from(_results).map(function(x){ return JSON.stringify(x); }).join(\", \");\n  }\n  return _results.values().next().value;\n}\n"
    );
  }

  var _cachedSolvers = null;
  var _cachedPlayerLength = 0;

  function main(input) {
    var solvers;
    if (input.type === "player" && _cachedSolvers && input.player && input.player.length === _cachedPlayerLength) {
      solvers = _cachedSolvers;
    } else {
      var preprocessedPlayer = input.type === "player" ? preprocessPlayer(input.player) : input.preprocessed_player;
      solvers = getFromPrepared(preprocessedPlayer);
      if (input.type === "player" && input.player) {
        _cachedSolvers = solvers;
        _cachedPlayerLength = input.player.length;
      }
    }

    var responses = input.requests.map(function(input) {
      if (!isOneOf(input.type, "n", "sig")) return { type: "error", error: "Unknown request type: " + input.type };
      var solver = solvers[input.type];
      if (!solver) return { type: "error", error: "Failed to extract " + input.type + " function" };
      try {
        return {
          type: "result",
          data: Object.fromEntries(input.challenges.map(function(challenge) {
            return [challenge, solver(challenge)];
          }))
        };
      } catch (error) {
        return { type: "error", error: error instanceof Error ? error.message + "\n" + error.stack : String(error) };
      }
    });
    var output = { type: "result", responses: responses };
    if (input.type === "player" && input.output_preprocessed && typeof preprocessedPlayer !== 'undefined') output.preprocessed_player = preprocessedPlayer;
    return output;
  }

  return main;
}(meriyah, astring);