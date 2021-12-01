# Changelog

All notable changes to this project will be documented in this file. See [standard-version](https://github.com/conventional-changelog/standard-version) for commit guidelines.

### [5.1.2](https://github.com/keptn-sandbox/keptn-jenkins-library/compare/5.1.1...5.1.2) (2021-11-30)

Automation improvements

### Other

* Added .versionrc.json ([#68](https://github.com/keptn-sandbox/keptn-jenkins-library/issues/68)) ([e63c9db](https://github.com/keptn-sandbox/keptn-jenkins-library/commit/e63c9dbb9b4ce36b812675f1f00714796d39c02a))
* added release pipelines ([#67](https://github.com/keptn-sandbox/keptn-jenkins-library/issues/67)) ([a8be742](https://github.com/keptn-sandbox/keptn-jenkins-library/commit/a8be74239ac1b79eee31e7ec2fe07004e6efb4ee))
* Improve release pipeline ([#72](https://github.com/keptn-sandbox/keptn-jenkins-library/issues/72)) ([f35a626](https://github.com/keptn-sandbox/keptn-jenkins-library/commit/f35a62668b946f2ab4050279cdfa4447f9526f6d))
* pre-release pipeline should not require a dedicated access token ([#69](https://github.com/keptn-sandbox/keptn-jenkins-library/issues/69)) ([b071970](https://github.com/keptn-sandbox/keptn-jenkins-library/commit/b071970f6dadf612b0715900a17cf76832339d04))
* release pipeline hardening ([#73](https://github.com/keptn-sandbox/keptn-jenkins-library/issues/73)) ([dc33f47](https://github.com/keptn-sandbox/keptn-jenkins-library/commit/dc33f47fc3109b223b93e398e9b1822ff0e56968))
* release pipeline should not require a dedicated access token ([#70](https://github.com/keptn-sandbox/keptn-jenkins-library/issues/70)) ([238238c](https://github.com/keptn-sandbox/keptn-jenkins-library/commit/238238cf0a0bea02322c4f592d0dea61e99d6fa4))

### [5.1.1](https://github.com/keptn-sandbox/keptn-jenkins-library/compare/5.1...5.1.1) (2021-11-29)

First release that adheres to semantic versioning.
### Other

* Added semantic pr check ([0323702](https://github.com/keptn-sandbox/keptn-jenkins-library/commit/0323702da4f8bf7922003e42ca02ba3fe61e1753))

### [5.1](https://github.com/keptn-sandbox/keptn-jenkins-library/compare/5.0...5.1) (2021-11-11)

This version of the Jenkins Library supports Keptn 0.9.x and 0.10.0.

**Breaking Change**: Removed outdated Keptn 0.5/0.6 related functions such as `sendConfigurationChangedEvent` and `sendConfigurationTriggeredEvent` (https://github.com/keptn-sandbox/keptn-jenkins-library/issues/44)
### Features

* Added a function to send a .finished CloudEvent (e.g., test.finished) (https://github.com/keptn-sandbox/keptn-jenkins-library/issues/42)



### [5.0](https://github.com/keptn-sandbox/keptn-jenkins-library/compare/4.1...5.0) (2021-10-209)

This version of the Jenkins Library supports Keptn 0.9.x and 0.10.0.

### Features

* Added a function to send a .finished CloudEvent (e.g., test.finished) (https://github.com/keptn-sandbox/keptn-jenkins-library/issues/42)
* **Breaking Change**: Removed outdated Keptn 0.5/0.6 related functions such as `sendConfigurationChangedEvent` and `sendConfigurationTriggeredEvent` (https://github.com/keptn-sandbox/keptn-jenkins-library/issues/44)



### [4.1](https://github.com/keptn-sandbox/keptn-jenkins-library/compare/4.0...4.1) (2021-10-19)

This version of the Jenkins Library supports Keptn 0.8.x and 0.9.x.

Fixes

* Fix time format for newer Java Versions #34 
* Fixing evaluation.triggered event as it needs to contain the stage
* Fix wait-condition for evaluating quality-gate

### Older Releases

You can find release notes of older releases here: 

* [4.0](https://github.com/keptn-sandbox/keptn-jenkins-library/releases/tag/4.0)
* [3.5](https://github.com/keptn-sandbox/keptn-jenkins-library/releases/tag/3.5)
* [3.4](https://github.com/keptn-sandbox/keptn-jenkins-library/releases/tag/3.4)
* [3.3](https://github.com/keptn-sandbox/keptn-jenkins-library/releases/tag/3.3)
* [2.2](https://github.com/keptn-sandbox/keptn-jenkins-library/releases/tag/2.2)
* [3.1](https://github.com/keptn-sandbox/keptn-jenkins-library/releases/tag/3.1)
* [3.0](https://github.com/keptn-sandbox/keptn-jenkins-library/releases/tag/3.0)
* [2.1](https://github.com/keptn-sandbox/keptn-jenkins-library/releases/tag/2.1)
* [2.0](https://github.com/keptn-sandbox/keptn-jenkins-library/releases/tag/2.0)
* [1.0](https://github.com/keptn-sandbox/keptn-jenkins-library/releases/tag/1.0)
