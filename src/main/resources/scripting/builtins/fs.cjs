const { Support } = internalBinding("fs");

function encodingOf(options) {
  if (typeof options === "string") return options;
  if (options && typeof options.encoding === "string") return options.encoding;
  return null;
}

function boolOption(options, name) {
  return !!(options && typeof options === "object" && options[name]);
}

function bytesToUint8Array(bytes) {
  return Uint8Array.from(Java.from(bytes), (value) => value < 0 ? value + 256 : value);
}

function byteArrayFromBuffer(buffer) {
  const view = buffer instanceof Uint8Array ? buffer : new Uint8Array(buffer.buffer || buffer);
  return Java.to(Array.from(view, (value) => value > 127 ? value - 256 : value), "byte[]");
}

function positionOrCurrent(position) {
  return position === null || position === undefined ? -1 : Number(position);
}

function readFileSync(path, options) {
  const encoding = encodingOf(options);
  return encoding ? Support.readFileString(String(path), encoding) : bytesToUint8Array(Support.readFileBytes(String(path)));
}

function writeFileSync(path, data, options) {
  Support.writeFileString(String(path), String(data), encodingOf(options) || "utf8", false);
}

function appendFileSync(path, data, options) {
  Support.writeFileString(String(path), String(data), encodingOf(options) || "utf8", true);
}

function openSync(path, flags = "r") {
  return Support.open(String(path), String(flags));
}

function closeSync(fd) {
  Support.close(Number(fd));
}

function fstatSync(fd) {
  return Support.fstat(Number(fd));
}

function fsyncSync(fd) {
  Support.fsync(Number(fd), true);
}

function fdatasyncSync(fd) {
  Support.fsync(Number(fd), false);
}

function ftruncateSync(fd, len = 0) {
  Support.ftruncate(Number(fd), Number(len));
}

function readSync(fd, buffer, offset = 0, length = buffer.length - offset, position = null) {
  const temp = Support.byteArray(Number(length));
  const read = Support.readFd(Number(fd), temp, 0, Number(length), positionOrCurrent(position));
  const bytes = Java.from(temp);
  for (let index = 0; index < read; index++) {
    buffer[Number(offset) + index] = bytes[index] < 0 ? bytes[index] + 256 : bytes[index];
  }
  return read;
}

function writeSync(fd, buffer, offset = undefined, length = undefined, position = null) {
  if (typeof buffer === "string") {
    return Support.writeFdString(Number(fd), buffer, typeof length === "string" ? length : "utf8", positionOrCurrent(offset));
  }
  const bytes = byteArrayFromBuffer(buffer);
  const start = Number(offset || 0);
  const writeLength = length === undefined ? bytes.length - start : Number(length);
  return Support.writeFdBytes(Number(fd), bytes, start, writeLength, positionOrCurrent(position));
}

function existsSync(path) {
  return Support.exists(String(path));
}

function accessSync(path) {
  Support.access(String(path));
}

function statSync(path) {
  return Support.stat(String(path), true);
}

function lstatSync(path) {
  return Support.stat(String(path), false);
}

function readdirSync(path) {
  return Java.from(Support.readdir(String(path)));
}

function mkdirSync(path, options) {
  Support.mkdir(String(path), boolOption(options, "recursive"));
}

function rmSync(path, options) {
  Support.remove(String(path), boolOption(options, "recursive"));
}

function rmdirSync(path) {
  Support.remove(String(path), false);
}

function unlinkSync(path) {
  Support.remove(String(path), false);
}

function renameSync(oldPath, newPath) {
  Support.rename(String(oldPath), String(newPath));
}

function copyFileSync(source, target) {
  Support.copyFile(String(source), String(target));
}

function cpSync(source, target, options) {
  Support.cp(String(source), String(target), boolOption(options, "recursive"));
}

function realpathSync(path) {
  return Support.realpath(String(path));
}

function readlinkSync(path) {
  return Support.readlink(String(path));
}

function symlinkSync(target, path) {
  Support.symlink(String(target), String(path));
}

function truncateSync(path, len = 0) {
  Support.truncate(String(path), Number(len));
}

function utimesSync(path, atime, mtime) {
  Support.utimes(String(path), Number(atime), Number(mtime));
}

module.exports = Object.freeze({
  readFileSync,
  writeFileSync,
  appendFileSync,
  existsSync,
  accessSync,
  openSync,
  closeSync,
  fstatSync,
  fsyncSync,
  fdatasyncSync,
  ftruncateSync,
  readSync,
  writeSync,
  statSync,
  lstatSync,
  readdirSync,
  mkdirSync,
  rmSync,
  rmdirSync,
  unlinkSync,
  renameSync,
  copyFileSync,
  cpSync,
  realpathSync,
  readlinkSync,
  symlinkSync,
  truncateSync,
  utimesSync,
  constants: Object.freeze({}),
});
