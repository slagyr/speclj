/*
 Speclj + Headless Chrome Test Runner

 Usage:
 node bin/speclj.js [auto]

 auto: will only run specs updated after the last run. (default: false)

 Each run produced/touches a timestamp file, .specljs-timestamp
 */

const path = require('path');
const fs = require("fs");
var autoArg = process.argv.pop();
var timestampFile = path.resolve(__dirname, "../../../.specljs-timestamp");
var specsHTMLFile = path.resolve(__dirname, "specs.html");
var nsPrefix = "speclj"

function lastModified(filepath) {
  try {
    var stats = fs.statSync(filepath);
    return stats.mtime;
  } catch (error) {
    return null;
  }
};

function writeTimestamp() {
  if (lastModified(timestampFile) != null) {
    fs.unlinkSync(timestampFile);
  }
  fs.closeSync(fs.openSync(timestampFile, 'w'));
};

function readTimestamp() {
  return lastModified(timestampFile);
};

function autoMode() {
  return autoArg == "auto" && readTimestamp() != null;
};

function findUpdatedSpecs(rdeps, deps) {
  var minMillis = readTimestamp().getTime();
  var updated = {};
  for(var ns in rdeps) {
    var file = deps.idToPath_[ns];
    var path = file.substring(7);
    if (lastModified(path).getTime() >= minMillis) {
      updated[ns] = true;
    }
  }
  return updated;
};

function buildReverseDeps(deps) {
  var rdeps = {};
  for(var ns in deps.idToPath_) {
    if (ns.startsWith(nsPrefix)) {
      var file = deps.idToPath_[ns];
      var requires = deps.dependencies_[file].requires
      for (var i = 0; i < requires.length; i++) {
        var rdep = requires[i];
        if (rdep.startsWith(nsPrefix)) {
          if (!(rdep in rdeps)) {
            rdeps[rdep] = {}
          }
          rdeps[rdep][ns] = true;
        }
      }
      if(!(ns in rdeps)) {
        rdeps[ns] = {}
      }
    }
  }
  return rdeps;
};


function reduceToSpecs(affected) {
  var result = {};
  for (var ns in affected) {
    if (ns.endsWith("_spec")) {
      result[ns.replace(/_/g, "-")] = true
    }
  }
  return result;
};

function findAffectedSpecs(deps) {
  var rdeps = buildReverseDeps(deps);
  var updated = findUpdatedSpecs(rdeps, deps);

  var result = {};

  var walkDeps = function (nses) {
    for(var ns in nses) {
      if (!(ns in result)) {
        result[ns] = true;
        walkDeps(rdeps[ns])
      }
    }
  };
  walkDeps(updated);

  return reduceToSpecs(result);
};

const puppeteer = require('puppeteer');

(async () => {
  const browser = await puppeteer.launch();
  const page = await browser.newPage();
  await page.exposeFunction('autoMode', () => autoMode());
  await page.exposeFunction('findAffectedSpecs', (deps) => findAffectedSpecs(deps));
  await page.exposeFunction('writeTimestamp', () => writeTimestamp());
  await page.goto('file://' + specsHTMLFile);
  page.on('console', msg => {
    if (msg.text() === "Failed to load resource: net::ERR_FILE_NOT_FOUND") {
      return; // because we aren't loading specs.html over http, many local resources (images, fonts) are not found.
    }
    console.log(msg.text());
  });
  var code = await page.evaluate(async () => {
    try {
      var specs = await window.autoMode() ? await window.findAffectedSpecs(goog.debugLoader_) : null;
      var result = runSpecsFiltered(specs);
      await window.writeTimestamp();
      return result;
    }
    catch (e) {
      console.error(e);
      return 1;
    }
  });

  await browser.close();
  process.exit(code);
})();
