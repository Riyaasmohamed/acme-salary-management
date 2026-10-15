// STUDY NOTES: Configuration for Karma, the tool that runs the Angular unit tests (*.spec.ts) inside a real browser.
// STUDY NOTES: angular.json points to this file ("karmaConfig"), so `npm test` and `npm run test:ci` use it.
// Karma configuration file, see link for more information
// https://karma-runner.github.io/1.0/config/configuration-file.html

// WHY: Karma loads this file as a Node.js module; it calls this function and passes in the config object to fill.
module.exports = function (config) {
  // WHY: config.set(...) is where all the Karma settings are given in one object.
  config.set({
    // WHY: An empty basePath means "paths are relative to this file's folder" (the frontend folder).
    basePath: '',
    // WHY: Jasmine is the test framework: it gives describe(), it() and expect() used in the spec files.
    frameworks: ['jasmine'],
    // WHY: Plugins are loaded explicitly so Karma knows each extra feature we use.
    plugins: [
      // WHY: Connects Jasmine to Karma.
      require('karma-jasmine'),
      // WHY: Lets Karma start Chrome (normal or headless) to run the tests.
      require('karma-chrome-launcher'),
      // WHY: Shows test results as a web page in the browser window (the 'kjhtml' reporter).
      require('karma-jasmine-html-reporter'),
      // WHY: Measures code coverage (which lines the tests executed).
      require('karma-coverage'),
    ],
    // WHY: Settings that are passed to the code running inside the browser.
    client: {
      jasmine: {
        // you can add configuration options for Jasmine here
        // the possible options are listed at https://jasmine.github.io/api/edge/Configuration.html
        // for example, you can disable the random execution with `random: false`
        // or set a specific seed with `seed: 4321`
      },
    },
    // WHY: Avoids printing the same error stack twice (once in the terminal, once in the HTML page).
    jasmineHtmlReporter: {
      suppressAll: true // removes the duplicated traces
    },
    // WHY: Where and how the coverage report is written when coverage is turned on.
    coverageReporter: {
      // WHY: Writes the report into frontend/coverage/frontend so it does not mix with source code.
      dir: require('path').join(__dirname, './coverage/frontend'),
      // WHY: '.' puts the files directly in that folder, not in one sub-folder per browser.
      subdir: '.',
      reporters: [
        // WHY: An HTML report you can open in a browser to see uncovered lines.
        { type: 'html' },
        // WHY: A short summary printed in the terminal.
        { type: 'text-summary' }
      ]
    },
    // WHY: 'progress' prints results in the terminal; 'kjhtml' shows them in the browser page.
    reporters: ['progress', 'kjhtml'],
    // WHY: By default (`npm test`) tests run in a normal, visible Chrome window.
    browsers: ['Chrome'],
    // WHY: A custom launcher is our own named browser setup, used by `npm run test:ci`.
    customLaunchers: {
      // Headless Chrome for CI and servers where the Chrome sandbox is unavailable.
      // INTERVIEW: --no-sandbox is only for CI/Linux servers (often inside containers); it is never used for the real app.
      ChromeHeadlessCI: {
        // WHY: Headless Chrome has no window, so it works on servers without a screen.
        base: 'ChromeHeadless',
        // WHY: The Chrome sandbox often cannot start on Linux servers, so it is turned off here.
        flags: ['--no-sandbox']
      }
    },
    // WHY: In watch mode, when a file changes, the running test pass restarts instead of finishing old results.
    restartOnFileChange: true
  });
};
