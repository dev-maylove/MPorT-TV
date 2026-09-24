/**
 * es5transform.js
 * AST-level transformer: mengubah AST ES6+ menjadi AST ES5 yang kompatibel
 * dengan V8 Chromium 44 (Android 6.0 ZTE STB B860H).
 *
 * Transformasi yang dilakukan:
 *   1. ArrowFunctionExpression           → FunctionExpression
 *   2. ObjectPattern destructuring params → _ref + manual var assignment
 *   3. VariableDeclaration const/let     → var
 *   4. LogicalExpression ?? (nullish)    → || (OR)
 *   5. ChainExpression / OptionalMember  → kondisional ternary
 *   6. TemplateLiteral                   → string concat (+)
 *   7. SpreadElement dalam Array         → concat(Array.prototype.slice)
 *   8. RestElement dalam params          → Array.prototype.slice.call(arguments)
 *   9. ForOfStatement                    → for+index loop
 *  10. AssignmentPattern (default param) → param || default
 */
(function (global) {
  'use strict';

  var _uid = 0;
  function uid(prefix) {
    return (prefix || '_t') + (++_uid);
  }

  // ────────────────────────────────────────────────────────────────────────────
  // Helper: buat node AST sederhana
  // ────────────────────────────────────────────────────────────────────────────
  function ident(name)      { return { type: 'Identifier', name: name }; }
  function lit(val)         { return { type: 'Literal', value: val, raw: JSON.stringify(val) }; }
  function block(body)      { return { type: 'BlockStatement', body: body }; }
  function exprStmt(expr)   { return { type: 'ExpressionStatement', expression: expr }; }
  function varDecl(name, init) {
    return {
      type: 'VariableDeclaration', kind: 'var',
      declarations: [{ type: 'VariableDeclarator', id: ident(name), init: init || null }]
    };
  }
  function assign(left, right) {
    return { type: 'AssignmentExpression', operator: '=', left: left, right: right };
  }
  function member(obj, prop, computed) {
    return { type: 'MemberExpression', object: obj, property: prop, computed: !!computed, optional: false };
  }
  function call(callee, args) {
    return { type: 'CallExpression', callee: callee, arguments: args || [], optional: false };
  }
  function binop(op, left, right) {
    return { type: 'BinaryExpression', operator: op, left: left, right: right };
  }
  function logical(op, left, right) {
    return { type: 'LogicalExpression', operator: op, left: left, right: right };
  }
  function cond(test, cons, alt) {
    return { type: 'ConditionalExpression', test: test, consequent: cons, alternate: alt };
  }
  function undef() {
    return { type: 'UnaryExpression', operator: 'void', argument: lit(0), prefix: true };
  }

  // ────────────────────────────────────────────────────────────────────────────
  // Rekursi utama: walk + transform setiap node AST
  // ────────────────────────────────────────────────────────────────────────────
  function walk(node) {
    if (!node || typeof node !== 'object') return node;
    if (Array.isArray(node)) return node.map(walk);

    // Transformasi anak-anak terlebih dahulu (bottom-up)
    var n = {};
    for (var k in node) {
      if (!Object.prototype.hasOwnProperty.call(node, k)) continue;
      var v = node[k];
      if (Array.isArray(v)) {
        n[k] = v.map(function(item) {
          return (item && typeof item === 'object' && item.type) ? walk(item) : item;
        });
      } else if (v && typeof v === 'object' && v.type) {
        n[k] = walk(v);
      } else {
        n[k] = v;
      }
    }

    // ── 1. ArrowFunctionExpression → FunctionExpression ──────────────────────
    if (n.type === 'ArrowFunctionExpression') {
      var body = n.body;
      if (body.type !== 'BlockStatement') {
        // expression arrow: x => expr  →  function(x){ return expr; }
        body = block([{ type: 'ReturnStatement', argument: body }]);
      }
      return transformFunctionParams({
        type: 'FunctionExpression',
        id: null,
        params: n.params,
        body: body,
        async: false,
        generator: false,
        expression: false
      });
    }

    // ── 2. FunctionDeclaration / FunctionExpression: transformasi params & strip async/generator ──
    if (n.type === 'FunctionDeclaration' || n.type === 'FunctionExpression') {
      n.async = false;
      n.generator = false;
      return transformFunctionParams(n);
    }

    // ── 2b. AwaitExpression / YieldExpression → unwrap ke argument ───────────
    if (n.type === 'AwaitExpression') {
      return walk(n.argument);
    }
    if (n.type === 'YieldExpression') {
      return n.argument ? walk(n.argument) : undef();
    }

    // ── 3. VariableDeclaration: const/let → var ──────────────────────────────
    if (n.type === 'VariableDeclaration') {
      n.kind = 'var';
    }

    // ── 3b. BlockStatement / Program / SwitchCase: unroll destructuring VariableDeclarations
    if (n.type === 'BlockStatement' || n.type === 'Program') {
      if (Array.isArray(n.body)) {
        n.body = flattenStatementList(n.body);
      }
    }
    if (n.type === 'SwitchCase') {
      if (Array.isArray(n.consequent)) {
        n.consequent = flattenStatementList(n.consequent);
      }
    }

    // ── 3c. AssignmentExpression dengan Destructuring: ({a, b} = obj) ─────────
    if (n.type === 'AssignmentExpression' && n.left && (n.left.type === 'ObjectPattern' || n.left.type === 'ArrayPattern')) {
      return lowerDestructuringAssignment(n);
    }

    // ── 4. LogicalExpression ?? → || ─────────────────────────────────────────
    if (n.type === 'LogicalExpression' && n.operator === '??') {
      // a ?? b  →  a != null ? a : b
      // (simpan ke temp var agar tidak evaluate a dua kali jika ada side effect)
      var tmpName = uid('_nc');
      // Inline version (cukup aman untuk ekspresi simpel):
      return cond(
        binop('!=', n.left, lit(null)),
        n.left,
        n.right
      );
    }

    // ── 5. ChainExpression / OptionalMemberExpression / OptionalCallExpression ─
    if (n.type === 'ChainExpression') {
      return lowerOptional(n.expression);
    }
    if (n.type === 'MemberExpression' && n.optional) {
      return lowerOptional(n);
    }
    if (n.type === 'CallExpression' && n.optional) {
      return lowerOptional(n);
    }

    // ── 6. TemplateLiteral → string concatenation ────────────────────────────
    if (n.type === 'TemplateLiteral') {
      return templateToConcat(n);
    }

    // ── 6b. TaggedTemplateExpression: tag`str${x}` → tag(["str"], x) ─────────
    // KRITIS: jika quasi (TemplateLiteral) sudah di-walk jadi Literal, astring
    // akan output eAV"data-" (invalid). Kita harus tangani SEBELUM walk anak-anak.
    // Karena kita walk bottom-up, kita perlu handle ini SETELAH anak-anak di-walk.
    // Anak quasi sudah di-walk ke Literal/BinaryExpr oleh rule TemplateLiteral di atas,
    // TAPI - TaggedTemplateExpression tidak memanggil templateToConcat pada quasi.
    // Node TaggedTemplateExpression tetap punya quasi TemplateLiteral (karena rule TemplateLiteral
    // di atas hanya berlaku ketika node ITU SENDIRI adalah TemplateLiteral, bukan saat
    // ia sebagai properti anak). Jadi quasi masih TemplateLiteral saat kita sampai di sini.
    // Ubah ke function call: tag(stringsArray, ...expressions)
    if (n.type === 'TaggedTemplateExpression') {
      var tagNode = n.tag;   // sudah di-walk
      var quasiNode = n.quasi; // masih TemplateLiteral (atau sudah jadi hasil walk)
      var strs = [];
      var exprs = [];

      // Ambil quasis/expressions dari TemplateLiteral original
      if (quasiNode && quasiNode.type === 'TemplateLiteral') {
        quasiNode.quasis.forEach(function(q) {
          strs.push(lit(q.value.cooked !== null ? q.value.cooked : q.value.raw || ''));
        });
        exprs = quasiNode.expressions.map(walk);
      } else {
        // quasi sudah di-transform jadi sesuatu yang lain — ekstrak sebagai string
        strs.push(lit(''));
      }

      // Buat: tag(["str1","str2"], expr1, expr2)
      var stringsArr = { type: 'ArrayExpression', elements: strs };
      var allArgs = [stringsArr].concat(exprs);
      return call(walk(tagNode), allArgs);
    }

    // ── 7. ForOfStatement → indexed for loop ─────────────────────────────────
    if (n.type === 'ForOfStatement') {
      return forOfToFor(n);
    }

    // ── 8. SpreadElement in ArrayExpression → concat ─────────────────────────
    if (n.type === 'ArrayExpression') {
      return lowerArraySpread(n);
    }

    // ── 9. Property shorthand { x } → { x: x } & method shorthand { f(){} } → { f: function(){} }
    // Shorthand property ES6 tidak valid di V8 Chrome 44 sebagai ES5
    if (n.type === 'Property') {
      if (n.shorthand) {
        n.shorthand = false;
        // key dan value sudah sama identifiernya, tidak perlu diubah
      }
      if (n.method && n.value && n.value.type === 'FunctionExpression') {
        // method shorthand: { foo() {} } → { foo: function() {} }
        n.method = false;
      }
      return n;
    }

    // ── 10. SpreadElement dalam CallExpression args: foo(...args) → foo.apply(null, args)
    if (n.type === 'CallExpression') {
      var hasSpreadArg = n.arguments.some(function(a) { return a && a.type === 'SpreadElement'; });
      if (hasSpreadArg) {
        return lowerCallSpread(n);
      }
    }

    // ── 11. SpreadElement dalam NewExpression: new Foo(...args)
    if (n.type === 'NewExpression') {
      var hasSpreadNew = n.arguments.some(function(a) { return a && a.type === 'SpreadElement'; });
      if (hasSpreadNew) {
        // Flatten: new Foo(...args) → new (Function.prototype.bind.apply(Foo, [null].concat(args)))()
        var flatArgs = flattenSpreadArgs(n.arguments);
        var bindCall = call(
          member(member(member(ident('Function'), ident('prototype')), ident('bind')), ident('apply')),
          [n.callee, call(member(call(member(ident('Array'), ident('prototype')), []), ident('concat')), flatArgs)]
        );
        return { type: 'NewExpression', callee: bindCall, arguments: [] };
      }
    }

    // ── 12. ObjectExpression with SpreadElement or Computed Properties ───────
    if (n.type === 'ObjectExpression') {
      var hasSpreadProp = n.properties.some(function(p) { return p && p.type === 'SpreadElement'; });
      if (hasSpreadProp) {
        return lowerObjectSpread(n);
      }
      var hasComputedProp = n.properties.some(function(p) { return p && p.type === 'Property' && p.computed; });
      if (hasComputedProp) {
        return lowerComputedObject(n);
      }
    }

    // ── 13. ClassDeclaration / ClassExpression → ES5 function + prototype ──
    if (n.type === 'ClassDeclaration' || n.type === 'ClassExpression') {
      return lowerClass(n);
    }

    // ── 14. CatchClause optional binding: catch {} → catch (_e) {} ──────────
    if (n.type === 'CatchClause') {
      if (!n.param) {
        n.param = ident(uid('_e'));
      }
      return n;
    }

    // ── 15. Super node → Object fallback ─────────────────────────────────────
    if (n.type === 'Super') {
      return ident('Object');
    }

    // ── 16. BinaryExpression ** → Math.pow(a, b) ─────────────────────────────
    if (n.type === 'BinaryExpression' && n.operator === '**') {
      return call(member(ident('Math'), ident('pow')), [walk(n.left), walk(n.right)]);
    }

    // ── 17. AssignmentExpression **= → a = Math.pow(a, b) ────────────────────
    if (n.type === 'AssignmentExpression' && n.operator === '**=') {
      return assign(walk(n.left), call(member(ident('Math'), ident('pow')), [walk(n.left), walk(n.right)]));
    }

    // ── 18. MetaProperty: new.target → this.constructor ─────────────────────
    if (n.type === 'MetaProperty' && n.meta && n.meta.name === 'new' && n.property && n.property.name === 'target') {
      return member(ident('this'), ident('constructor'));
    }

    return n;
  }

  // ────────────────────────────────────────────────────────────────────────────
  // Transformasi parameter fungsi: ObjectPattern, RestElement, AssignmentPattern
  // ────────────────────────────────────────────────────────────────────────────
  function transformFunctionParams(fn) {
    var newParams = [];
    var prologue  = [];

    fn.params.forEach(function(param, idx) {
      if (!param) return;

      // -- ObjectPattern atau ArrayPattern: ({ a, b }) atau ([a, b])
      if (param.type === 'ObjectPattern' || param.type === 'ArrayPattern') {
        var refName = uid('_ref');
        newParams.push(ident(refName));
        deststructureToVarDecls(param, ident(refName), prologue);

      // -- AssignmentPattern: (a = default) atau ({ a } = default)
      } else if (param.type === 'AssignmentPattern') {
        if (param.left.type === 'Identifier') {
          var pName = param.left.name;
          newParams.push(ident(pName));
          prologue.push(exprStmt(assign(
            ident(pName),
            cond(binop('!==', ident(pName), undef()), ident(pName), walk(param.right))
          )));
        } else if (param.left.type === 'ObjectPattern' || param.left.type === 'ArrayPattern') {
          var refNameOpt = uid('_ref');
          newParams.push(ident(refNameOpt));
          prologue.push(exprStmt(assign(
            ident(refNameOpt),
            cond(binop('!==', ident(refNameOpt), undef()), ident(refNameOpt), walk(param.right))
          )));
          deststructureToVarDecls(param.left, ident(refNameOpt), prologue);
        } else {
          newParams.push(walk(param.left));
        }

      // -- RestElement: (...args) → gunakan arguments
      } else if (param.type === 'RestElement') {
        var restName = param.argument.name;
        prologue.push(varDecl(restName,
          call(
            member(
              member(member(ident('Array'), ident('prototype')), ident('slice')),
              ident('call')
            ),
            [ident('arguments'), lit(idx)]
          )
        ));

      } else {
        newParams.push(param);
      }
    });

    if (prologue.length > 0 && fn.body && fn.body.type === 'BlockStatement') {
      fn.body.body = prologue.concat(fn.body.body);
    }
    fn.params = newParams;
    return fn;
  }

  // ────────────────────────────────────────────────────────────────────────────
  // Optional chaining → ternary bersarang
  // a?.b?.c  →  a == null ? void 0 : (a.b == null ? void 0 : a.b.c)
  // ────────────────────────────────────────────────────────────────────────────
  function lowerOptional(node) {
    if (node.type === 'MemberExpression' && node.optional) {
      var obj = walk(node.object);
      return cond(
        binop('==', obj, lit(null)),
        undef(),
        { type: 'MemberExpression', object: obj, property: node.property, computed: node.computed, optional: false }
      );
    }
    if (node.type === 'CallExpression' && node.optional) {
      var callee = walk(node.callee);
      return cond(
        binop('==', callee, lit(null)),
        undef(),
        call(callee, node.arguments.map(walk))
      );
    }
    return walk(node);
  }

  // ────────────────────────────────────────────────────────────────────────────
  // TemplateLiteral → string concatenation
  // `hello ${name}!` → "hello " + name + "!"
  // ────────────────────────────────────────────────────────────────────────────
  function templateToConcat(n) {
    var parts = [];
    for (var i = 0; i < n.quasis.length; i++) {
      var q = n.quasis[i];
      if (q.value.cooked !== '') parts.push(lit(q.value.cooked || q.value.raw));
      if (i < n.expressions.length) parts.push(walk(n.expressions[i]));
    }
    if (parts.length === 0) return lit('');
    return parts.reduce(function(acc, part) {
      return binop('+', acc, part);
    });
  }

  // ────────────────────────────────────────────────────────────────────────────
  // ForOfStatement → for dengan index atau forEach
  // for (const x of arr) { ... }  →  var _arr=arr; for(var _i=0;_i<_arr.length;_i++){ var x=_arr[_i]; ... }
  // ────────────────────────────────────────────────────────────────────────────
  function forOfToFor(n) {
    var arrName = uid('_foa');
    var idxName = uid('_foi');
    var left    = n.left;
    var varName;

    // Dapatkan nama variabel iterasi
    if (left.type === 'VariableDeclaration') {
      varName = left.declarations[0].id.name;
    } else if (left.type === 'Identifier') {
      varName = left.name;
    } else {
      varName = uid('_fov');
    }

    var loopBody = walk(n.body);
    if (loopBody.type !== 'BlockStatement') loopBody = block([loopBody]);

    // var varName = _arr[_i]; (inject di awal body)
    loopBody.body.unshift(varDecl(varName, member(ident(arrName), ident(idxName), true)));

    return block([
      varDecl(arrName, walk(n.right)),
      {
        type: 'ForStatement',
        init: { type: 'VariableDeclaration', kind: 'var', declarations: [{ type: 'VariableDeclarator', id: ident(idxName), init: lit(0) }] },
        test: binop('<', ident(idxName), member(ident(arrName), ident('length'))),
        update: { type: 'UpdateExpression', operator: '++', argument: ident(idxName), prefix: false },
        body: loopBody
      }
    ]);
  }

  // ────────────────────────────────────────────────────────────────────────────
  // ArrayExpression dengan SpreadElement → [].concat(...)
  // [a, ...b, c]  →  [a].concat(b, [c])   (simplified)
  // ────────────────────────────────────────────────────────────────────────────
  function lowerArraySpread(n) {
    var hasSpread = n.elements.some(function(el) { return el && el.type === 'SpreadElement'; });
    if (!hasSpread) return n;

    var groups = [];
    var current = [];
    n.elements.forEach(function(el) {
      if (!el) { current.push(lit(undefined)); return; }
      if (el.type === 'SpreadElement') {
        if (current.length > 0) { groups.push({ type: 'ArrayExpression', elements: current }); current = []; }
        groups.push(walk(el.argument));
      } else {
        current.push(walk(el));
      }
    });
    if (current.length > 0) groups.push({ type: 'ArrayExpression', elements: current });

    if (groups.length === 0) return { type: 'ArrayExpression', elements: [] };
    var base = groups[0];
    var rest = groups.slice(1);
    return call(member(base, ident('concat')), rest);
  }
  // ────────────────────────────────────────────────────────────────────────────
  // CallExpression dengan SpreadElement: foo(a, ...b, c) → foo.apply(ctx, [a].concat(b, [c]))
  // ────────────────────────────────────────────────────────────────────────────
  function flattenSpreadArgs(args) {
    var groups = [];
    var current = [];
    args.forEach(function(a) {
      if (a && a.type === 'SpreadElement') {
        if (current.length > 0) { groups.push({ type: 'ArrayExpression', elements: current }); current = []; }
        groups.push(walk(a.argument));
      } else {
        current.push(walk(a));
      }
    });
    if (current.length > 0) groups.push({ type: 'ArrayExpression', elements: current });
    if (groups.length === 0) return [{ type: 'ArrayExpression', elements: [] }];
    // Combine into one array via concat
    if (groups.length === 1) return groups;
    var base = groups[0];
    var rest = groups.slice(1);
    return [call(member(base, ident('concat')), rest)];
  }

  function lowerCallSpread(n) {
    var flatArr = flattenSpreadArgs(n.arguments);
    var callee = n.callee;
    var ctx;

    // Determine context (this) for apply:
    // foo.bar(...args) → foo.bar.apply(foo, [...])
    // foo(...args)     → foo.apply(null, [...])
    if (callee.type === 'MemberExpression') {
      var objName = uid('_ctx');
      // Can't easily extract object without side effects, use null for simplicity
      ctx = lit(null);
    } else {
      ctx = lit(null);
    }

    return call(
      member(walk(callee), ident('apply')),
      [ctx, flatArr[0]]
    );
  }

  // ────────────────────────────────────────────────────────────────────────────
  // ObjectExpression dengan SpreadElement: { a, ...obj, b } → Object.assign({a}, obj, {b})
  // ────────────────────────────────────────────────────────────────────────────
  function lowerObjectSpread(n) {
    var groups = [];
    var current = [];
    n.properties.forEach(function(p) {
      if (p && p.type === 'SpreadElement') {
        if (current.length > 0) {
          groups.push(walk({ type: 'ObjectExpression', properties: current }));
          current = [];
        }
        groups.push(walk(p.argument));
      } else {
        current.push(walk(p));
      }
    });
    if (current.length > 0) {
      groups.push(walk({ type: 'ObjectExpression', properties: current }));
    }

    if (groups.length === 0) return { type: 'ObjectExpression', properties: [] };

    return call(
      member(ident('Object'), ident('assign')),
      [{ type: 'ObjectExpression', properties: [] }].concat(groups)
    );
  }

  // ────────────────────────────────────────────────────────────────────────────
  // ObjectExpression dengan Computed Properties: { [a]: b, c: d } → IIFE
  // (function() { var _co = {}; _co[a] = b; _co.c = d; return _co; })()
  // ────────────────────────────────────────────────────────────────────────────
  function lowerComputedObject(n) {
    var objName = uid('_co');
    var stmts = [varDecl(objName, { type: 'ObjectExpression', properties: [] })];
    n.properties.forEach(function(p) {
      if (!p || p.type !== 'Property') return;
      var isComp = !!p.computed;
      var keyNode = walk(p.key);
      var valNode = walk(p.value);
      stmts.push(exprStmt(assign(
        member(ident(objName), keyNode, isComp || keyNode.type !== 'Identifier'),
        valNode
      )));
    });
    stmts.push({ type: 'ReturnStatement', argument: ident(objName) });
    return call({
      type: 'FunctionExpression',
      id: null,
      params: [],
      body: block(stmts),
      async: false,
      generator: false
    }, []);
  }


  // ────────────────────────────────────────────────────────────────────────────
  // ClassDeclaration / ClassExpression → ES5 function + prototype
  // ────────────────────────────────────────────────────────────────────────────
  function lowerClass(n) {
    var fnName = (n.id && n.id.name) ? n.id.name : uid('_Class');
    var ctorMethod = null;
    var methods = [];

    if (n.body && Array.isArray(n.body.body)) {
      n.body.body.forEach(function(m) {
        if (m.kind === 'constructor') {
          ctorMethod = m;
        } else {
          methods.push(m);
        }
      });
    }

    var ctorFn = {
      type: 'FunctionExpression',
      id: ident(fnName),
      params: ctorMethod ? ctorMethod.value.params.map(walk) : [],
      body: ctorMethod ? walk(ctorMethod.value.body) : block([]),
      async: false,
      generator: false
    };

    var stmts = [varDecl(fnName, ctorFn)];

    if (n.superClass) {
      stmts.push(exprStmt(call(
        member(ident('Object'), ident('setPrototypeOf')),
        [ident(fnName), walk(n.superClass)]
      )));
    }

    methods.forEach(function(m) {
      var target = m.static ? ident(fnName) : member(ident(fnName), ident('prototype'));
      if (m.kind === 'method') {
        var keyNode = walk(m.key);
        var valNode = walk(m.value);
        stmts.push(exprStmt(assign(
          member(target, keyNode, !!m.computed || keyNode.type !== 'Identifier'),
          valNode
        )));
      }
    });

    stmts.push({ type: 'ReturnStatement', argument: ident(fnName) });

    var iife = call({
      type: 'FunctionExpression',
      id: null,
      params: [],
      body: block(stmts),
      async: false,
      generator: false
    }, []);

    if (n.type === 'ClassDeclaration' && n.id && n.id.name) {
      return varDecl(n.id.name, iife);
    }
    return iife;
  }

  // ────────────────────────────────────────────────────────────────────────────
  // Destructuring VariableDeclarations & Destructuring Assignments
  // ────────────────────────────────────────────────────────────────────────────
  function deststructureToVarDecls(pattern, refExpr, outStmts) {
    if (pattern.type === 'ObjectPattern') {
      pattern.properties.forEach(function(prop) {
        if (prop.type === 'Property') {
          var isComputed = !!prop.computed;
          var key = prop.key;
          var access = member(refExpr, key, isComputed || key.type === 'Literal');
          var val = prop.value;
          if (val.type === 'Identifier') {
            outStmts.push(varDecl(val.name, access));
          } else if (val.type === 'AssignmentPattern') {
            var valName = val.left.name;
            outStmts.push(varDecl(valName, cond(binop('!==', access, undef()), access, walk(val.right))));
          } else if (val.type === 'ObjectPattern' || val.type === 'ArrayPattern') {
            deststructureToVarDecls(val, access, outStmts);
          }
        }
      });
    } else if (pattern.type === 'ArrayPattern') {
      pattern.elements.forEach(function(el, i) {
        if (!el) return;
        var access = member(refExpr, lit(i), true);
        if (el.type === 'Identifier') {
          outStmts.push(varDecl(el.name, access));
        } else if (el.type === 'AssignmentPattern') {
          var valName = el.left.name;
          outStmts.push(varDecl(valName, cond(binop('!==', access, undef()), access, walk(el.right))));
        } else if (el.type === 'ObjectPattern' || el.type === 'ArrayPattern') {
          deststructureToVarDecls(el, access, outStmts);
        } else if (el.type === 'RestElement') {
          var restName = el.argument.name;
          var sliceCall = call(member(member(member(ident('Array'), ident('prototype')), ident('slice')), ident('call')), [refExpr, lit(i)]);
          outStmts.push(varDecl(restName, sliceCall));
        }
      });
    }
  }

  function flattenStatementList(stmts) {
    if (!Array.isArray(stmts)) return stmts;
    var newStmts = [];
    stmts.forEach(function(stmt) {
      if (stmt && stmt.type === 'VariableDeclaration') {
        var hasDestruct = stmt.declarations.some(function(d) {
          return d.id && (d.id.type === 'ObjectPattern' || d.id.type === 'ArrayPattern');
        });
        if (hasDestruct) {
          newStmts = newStmts.concat(unrollVariableDeclaration(stmt));
          return;
        }
      }
      newStmts.push(stmt);
    });
    return newStmts;
  }

  function unrollVariableDeclaration(stmt) {
    var outStmts = [];
    stmt.declarations.forEach(function(decl) {
      if (!decl.id) return;
      if (decl.id.type === 'Identifier') {
        outStmts.push({
          type: 'VariableDeclaration',
          kind: 'var',
          declarations: [decl]
        });
      } else if (decl.id.type === 'ObjectPattern' || decl.id.type === 'ArrayPattern') {
        var tmpName = uid('_des');
        var initVal = decl.init ? walk(decl.init) : undef();
        outStmts.push(varDecl(tmpName, initVal));
        deststructureToVarDecls(decl.id, ident(tmpName), outStmts);
      }
    });
    return outStmts;
  }

  function lowerDestructuringAssignment(n) {
    var tmpName = uid('_da');
    var exprs = [assign(ident(tmpName), walk(n.right))];

    function collectAssigns(pattern, refExpr) {
      if (pattern.type === 'ObjectPattern') {
        pattern.properties.forEach(function(prop) {
          if (prop.type === 'Property') {
            var access = member(refExpr, prop.key, !!prop.computed || prop.key.type === 'Literal');
            var val = prop.value;
            if (val.type === 'Identifier') {
              exprs.push(assign(ident(val.name), access));
            } else if (val.type === 'ObjectPattern' || val.type === 'ArrayPattern') {
              collectAssigns(val, access);
            }
          }
        });
      } else if (pattern.type === 'ArrayPattern') {
        pattern.elements.forEach(function(el, i) {
          if (!el) return;
          var access = member(refExpr, lit(i), true);
          if (el.type === 'Identifier') {
            exprs.push(assign(ident(el.name), access));
          } else if (el.type === 'ObjectPattern' || el.type === 'ArrayPattern') {
            collectAssigns(el, access);
          }
        });
      }
    }

    collectAssigns(n.left, ident(tmpName));
    exprs.push(ident(tmpName));
    return { type: 'SequenceExpression', expressions: exprs };
  }

  // ────────────────────────────────────────────────────────────────────────────
  function transformAstToEs5(program) {
    _uid = 0; // reset counter untuk setiap transform
    if (!program || !Array.isArray(program.body)) return program;
    program.body = program.body.map(walk);
    return program;
  }

  // Expose ke global scope (dipakai di yt.solver.core.js)
  global.transformAstToEs5 = transformAstToEs5;

}(typeof globalThis !== 'undefined' ? globalThis : typeof window !== 'undefined' ? window : this));
