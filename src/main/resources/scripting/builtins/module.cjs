const { createRequire: internalCreateRequire } = internalBinding("module");

module.exports = Object.freeze({
  createRequire(filename) {
    return internalCreateRequire(String(filename));
  },
});
