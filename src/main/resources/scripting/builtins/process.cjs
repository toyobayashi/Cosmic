const { Process } = internalBinding("process");

function normalizeBuiltinName(specifier) {
  const name = String(specifier);
  return name.startsWith("node:") ? name.slice(5) : name;
}

function ensureUrl(globalObject) {
  if (typeof globalObject.URL === "function") {
    return;
  }
  globalObject.URL = class URL {
    constructor(value, base) {
      const text = String(value);
      this.href = base && !/^[a-zA-Z][a-zA-Z0-9+.-]*:/.test(text) ? String(base).replace(/[^/]*$/, "") + text : text;
      const match = /^([a-zA-Z][a-zA-Z0-9+.-]*:)(.*)$/.exec(this.href);
      this.protocol = match ? match[1] : "";
      this.pathname = this.protocol === "file:" ? this.href.replace(/^file:\/\//, "") : (match ? match[2] : this.href);
    }

    toString() {
      return this.href;
    }
  };
}

const env = new Proxy(Object.create(null), {
  get(target, property) {
    if (typeof property === "symbol") return target[property];
    const value = Process.env(String(property));
    return value === null ? undefined : value;
  },
  ownKeys() {
    return Java.from(Process.envKeys());
  },
  getOwnPropertyDescriptor(target, property) {
    if (typeof property === "symbol") return undefined;
    const value = Process.env(String(property));
    if (value === null) return undefined;
    return { configurable: true, enumerable: true, writable: true, value };
  },
});

const versions = {};
Object.defineProperty(versions, "node", {
  get() {
    return Process.nodeVersion();
  },
  enumerable: true,
});
Object.freeze(versions);

const processObject = {};
Object.defineProperties(processObject, {
  version: {
    get() {
      return Process.version();
    },
    enumerable: true,
  },
  versions: {
    value: versions,
    enumerable: true,
  },
  arch: {
    get() {
      return Process.arch();
    },
    enumerable: true,
  },
  platform: {
    get() {
      return Process.platform();
    },
    enumerable: true,
  },
  cwd: {
    value() {
      return Process.cwd();
    },
    enumerable: true,
  },
  env: {
    value: env,
    enumerable: true,
  },
  argv: {
    get() {
      return Java.from(Process.argv());
    },
    enumerable: true,
  },
});

ensureUrl(globalThis);
Object.defineProperty(processObject, "getBuiltinModule", {
  value(specifier) {
    const name = normalizeBuiltinName(specifier);
    if (name === "process") return processObject;
    if (name === "fs" || name === "path" || name === "module") return require(name);
    return undefined;
  },
  enumerable: false,
});

module.exports = processObject;
